# FAIND

AI가 화재를 감지하고 골든타임을 사수하는 지능형 소방 대응 시스템.

## 문서

- 요구사항정의서 v2.2 (FR-01~FR-27, NFR-01~NFR-08)
- MSA 아키텍처설계서 v8 (§6 모듈러 모놀리식 원칙)
- 코드구조설계서 v1.0
- DB설계서 v2.2 (12개 테이블)
- 기술스택 문서 v1.0
- 와이어프레임 (15 SCREENS)

## 구성

| 디렉토리 | 런타임 | 상태 |
|---|---|---|
| `backend/` | Java 21 + Spring Boot 3.5 | 구현 완료 (핵심 도메인 6개: auth/incident/device/report/statistics + integration/listener) |
| `ai-server/` | Python 3.11 + FastAPI + LangGraph | 구현 완료 (FR-02 사전분석, FR-08 SOP대조, FR-24/26 화재감지) |
| `notification-server/` | Node.js + NestJS | 예정 |

## ai-server 로컬 실행

```bash
cd ai-server
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.lock.txt
uvicorn app.main:app --reload --port 8001
```

자세한 계약(backend와 주고받는 엔드포인트), LLM/화재감지 모델 미설정 시 동작은 `ai-server/README.md` 참조.

## backend 로컬 실행

```bash
docker compose up -d postgres redis   # 또는 로컬 PostgreSQL 16 + Redis 7
cd backend
./gradlew bootRun
```

기본 접속 정보(로컬 개발용, `application.yml` 참조):

- DB: `jdbc:postgresql://localhost:5432/faind` (user/password: `faind`/`faind`)
- Redis: `localhost:6379`
- 서버: `http://localhost:8080` (Swagger UI: `/swagger-ui.html`)

Flyway가 기동 시 `backend/src/main/resources/db/migration`의 스키마를 자동 적용한다. 최초 관리자 계정은
시드 데이터가 없으므로 직접 INSERT하거나(비밀번호는 BCrypt 해시) 별도 시딩 스크립트를 추가해야 한다.

`AI_SERVER_BASE_URL`(기본 `http://localhost:8001`)로 ai-server 주소를 지정한다. ai-server가 없어도
backend는 정상 동작한다 — AiAnalysisHttpAdapter가 CircuitBreaker+fallback으로 빈 결과를 대신 반환한다.

전체 스택(Docker Compose로 backend까지 함께)은 저장소 루트의 `docker-compose.yml`을 사용한다:

```bash
docker compose up --build
```
