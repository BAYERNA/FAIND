# FAIND AI Analysis Server

Python 3.11 + FastAPI + LangGraph. FR-02(사전분석) · FR-08(SOP 대조) · FR-24/26(화재감지·드론정찰)
3개 에이전트를 REST로 노출한다 (코드구조설계서 §3).

## 로컬 실행

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.lock.txt   # 재현 가능한 정확한 버전. requirements.txt는 직접 의존성만.
cp .env.example .env                    # 필요 시 값 채우기

uvicorn app.main:app --reload --port 8001
```

`GET /health`로 기동 확인. Swagger는 FastAPI 기본 경로인 `/docs`에서 볼 수 있다.

## 계약 (backend와의 연동)

| 방향 | 엔드포인트 | 대응 backend 코드 |
|---|---|---|
| backend → ai-server | `POST /api/v1/pre-analysis` | `AiAnalysisHttpAdapter.requestPreAnalysis` |
| backend → ai-server | `POST /api/v1/sop-match` | `AiAnalysisHttpAdapter.requestSopMatch` |
| ai-server → backend | `POST /api/v1/incidents/dispatch/cctv-detections` | `DispatchController.receiveCctvDetection` (FR-24) |
| ai-server → backend | `POST /api/v1/incidents/drone-dispatches/{id}/recon-result` | `IncidentController.recordDroneReconResult` (FR-26) |

JSON은 모두 camelCase로 주고받는다 (`app/core/camel_model.py` — Java Jackson 기본 직렬화와 맞춤).

`ai-server → backend` 두 콜백은 로그인 사용자 JWT가 아니라 `FAIND_BACKEND_SERVICE_TOKEN` 공유 토큰으로
인증한다. backend의 `faind.security.internal-service-token`(env: `INTERNAL_SERVICE_TOKEN`)과 반드시
같은 값을 써야 한다 — 비워두면(둘 다 기본값) backend의 `InternalServiceAuthFilter`가 로컬 데모 편의상
검증을 생략한다.

## LLM / 화재감지 모델이 없을 때

- `FAIND_LLM_PROVIDER=none`(기본값)이면 사전분석·SOP대조는 휴리스틱(키워드 매칭, 결정론적 추정치)으로
  동작한다 — 항상 응답은 반환하되, 실제 LLM 판단이 아님을 `source` 필드 등으로 명시한다.
- `FAIND_YOLO_MODEL_PATH`(기본 `models/fire_yolov8.pt`)에 화재/연기로 **파인튜닝된** 가중치가 없으면
  화재감지는 항상 `detected: false`를 반환한다. 공개 배포되는 기본 YOLOv8 가중치(COCO)는 fire/smoke
  클래스가 없으므로, 실제로 동작하게 하려면 파인튜닝된 모델 파일을 이 경로에 둬야 한다. 저장소에는
  MIT+CC BY 4.0 라이선스의 실제 파인튜닝 모델이 이미 이 경로에 포함되어 있다 — 출처는
  `models/NOTICE.md` 참조.

## 화재감지 위험도·확산 신호 (FR-24/26 고도화)

`FireDetectionResult`는 단순 감지 여부를 넘어 아래 신호를 함께 반환한다 (`app/services/yolo_service.py`):

- `areaRatio` — 감지된 화재/연기 박스가 프레임에서 차지하는 비율
- `dangerLevel`(SAFE/WARNING/DANGER/CRITICAL) · `dangerScore`(0~100) — confidence·area_ratio·확산 여부를
  근거로 계산. 실제 카메라 영상으로 추가 튜닝이 필요한 초기값이다.
- `isFlickerVerified` — 정적인 붉은/회색 물체를 오탐하는 것을 줄이기 위한 필터. CCTV 폴링(스트림 URL)
  경로에서만 폴링 1회당 짧은 연속 프레임(burst, 기본 5장·0.15초 간격 — `FAIND_FIRE_BURST_*`)을 찍어
  판단한다. base64/URL 단일 이미지 요청이나 burst 확보에 실패한 경우는 `null`("판단 보류")을
  반환한다 — 근거 없이 참/거짓을 지어내지 않는다.
- `growthRatio` / `spreadDirection` / `spreadSpeedPxPerSec` — 카메라(device_id)별로 최근 관측 이력을
  프로세스 메모리에 보관해, 이전 관측 대비 화재/연기 영역이 얼마나·어느 방향으로·얼마나 빠르게
  커지는지 계산한다. 같은 카메라에서 아직 이전 관측이 없으면 `null`이다. ai-server가 재시작되면
  이 이력은 초기화된다(데모 규모 전제 — 단일 프로세스 메모리 상주).

## 자동화 테스트

```bash
pip install -r requirements-dev.txt
pytest tests/
```

`tests/test_yolo_service.py`가 위험도 점수·등급 계산(`_score_danger`/`_level_for_score`)과
카메라별 확산 추적(`_track_spread`)을 검증한다 — 실제 YOLO 추론(`detect_fire_burst`)은 모델
로드가 필요해 범위 밖이고, 그 결과를 소비하는 순수 계산 로직만 다룬다.

## 테스트해본 방법 (수동 curl)

```bash
curl -X POST localhost:8001/api/v1/pre-analysis \
  -H 'Content-Type: application/json' \
  -d '{"incidentId":"...","address":"서울 강남구 테헤란로 123"}'

curl -X POST localhost:8001/api/v1/sop-match \
  -H 'Content-Type: application/json' \
  -d '{"reportId":"...","reportContent":"화재 인지 후 무전 보고..."}'
```

## 알려진 함정 (backend 쪽에서 이미 고쳐둔 것)

Java `RestClient`가 기본 `java.net.http.HttpClient`로 이 서버를 호출하면 HTTP/2 업그레이드 헤더
때문에 uvicorn의 엄격한 파서(httptools)가 `400 Invalid HTTP request received.`로 거부하는 문제가
있었다. `AiAnalysisHttpAdapter`/`NotificationHttpAdapter`에서 `HttpClient.Version.HTTP_1_1`을
명시해 고쳤다 — 이 서버를 다른 클라이언트로 호출할 계획이 있다면 같은 함정을 주의할 것.
