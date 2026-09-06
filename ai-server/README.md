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

## LLM / 화재감지 모델이 없을 때

- `FAIND_LLM_PROVIDER=none`(기본값)이면 사전분석·SOP대조는 휴리스틱(키워드 매칭, 결정론적 추정치)으로
  동작한다 — 항상 응답은 반환하되, 실제 LLM 판단이 아님을 `source` 필드 등으로 명시한다.
- `FAIND_YOLO_MODEL_PATH`(기본 `models/fire_yolov8.pt`)에 화재/연기로 **파인튜닝된** 가중치가 없으면
  화재감지는 항상 `detected: false`를 반환한다. 공개 배포되는 기본 YOLOv8 가중치(COCO)는 fire/smoke
  클래스가 없으므로, 실제로 동작하게 하려면 파인튜닝된 모델 파일을 이 경로에 둬야 한다.

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
