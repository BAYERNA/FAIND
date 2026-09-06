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
| `notification-server/` | Node.js + NestJS | 구현 완료 (FR-06, FR-18, FR-22, FR-23) |
| `admin-web/` | React 19 + TypeScript + Vite | 구현 완료 (CMN-001/002, ADM-001/002/003/006/009) |

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

`AI_SERVER_BASE_URL`(기본 `http://localhost:8001`), `NOTIFICATION_SERVER_BASE_URL`(기본
`http://localhost:3001`)로 각 서비스 주소를 지정한다. 둘 다 없어도 backend는 정상 동작한다 —
AiAnalysisHttpAdapter/NotificationHttpAdapter가 CircuitBreaker+fallback으로 처리한다.

## notification-server 로컬 실행

```bash
cd notification-server
npm install
cp .env.example .env   # JWT_SECRET은 backend와 반드시 동일해야 함
npm run build && npm run start
```

backend를 먼저 기동해 Flyway로 `alerts`/`alert_acknowledgements` 테이블을 만든 뒤 실행할 것 —
이 서비스는 `synchronize: false`로 매핑만 하고 스키마를 직접 만들지 않는다. 자세한 REST/WebSocket
계약은 `notification-server/README.md` 참조.

## admin-web 로컬 실행

```bash
cd admin-web
npm install
npm run dev
```

`http://localhost:5173`에서 접속. dev server가 `/api` 요청을 backend(`VITE_BACKEND_URL`, 기본
`http://localhost:8080`)로 프록시하므로 backend를 먼저 기동해야 한다. 화면 구성과 설계 원칙은
`admin-web/README.md` 참조.

## 전체 스택 실행

```bash
docker compose up --build
```

postgres → redis → backend(Flyway 적용, healthcheck) → ai-server/notification-server 순으로
의존성이 걸려 있다.
