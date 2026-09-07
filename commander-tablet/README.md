# FAIND 지휘관 태블릿

React 19 + TypeScript + Vite. 와이어프레임의 CMD-001/002/003/006 화면을 backend(Java)와
notification-server(Node) 양쪽에 실제로 연동해 구현했다.

## 로컬 실행

```bash
npm install
npm run dev
```

`http://localhost:5174`에서 접속 (admin-web이 5173을 쓰므로 지휘관 태블릿은 5174). `vite.config.ts`의
dev proxy가 `/api`를 backend(`VITE_BACKEND_URL`, 기본 `http://localhost:8080`)로, `/notify`와
`/socket.io`를 notification-server(`VITE_NOTIFICATION_URL`, 기본 `http://localhost:3001`)로
전달한다 — 둘 다 먼저 기동해 두어야 한다.

backend의 `faind.cors.allowed-origins`(기본값에 `http://localhost:5174` 포함)와 notification-server의
`CORS_ALLOWED_ORIGINS`에 이 포트가 등록되어 있어야 브라우저에서 정상 동작한다 (둘 다 리포지토리
기본값에 이미 포함).

## 자동화 테스트

```bash
npm run test
```

Vitest + React Testing Library. `api/client.ts`(NFR-02 성공/실패 구분)·`auth/AuthContext.tsx`(로그인
세션 영속화)에 더해 `dangerDisplay.ts`(FR-24/26 위험도·확산 표시 상수 맵)를 검증한다. 실제
backend/notification-server 호출 없이 `fetch`를 목(mock)으로 대체한다.

### E2E (Playwright)

```bash
npm run test:e2e
```

지금까지 매 Phase마다 수동으로 해온 브라우저 검증(로그인 성공/실패, 보호된 화면 접근)을
코드화했다. 이 앱의 dev server(`webServer`)는 자동으로 띄우지만, **backend(8080)와
Postgres·Redis는 미리 떠 있어야 한다** — `globalSetup`이 `../scripts/seed-e2e-accounts.sql`을
psql로 적용해 전용 계정(E2E-COMMANDER 등, 비밀번호는 스크립트 주석 참조)을 심고 이전 실행의
로그인 실패 카운터(Phase 10 레이트리미팅)를 초기화한다. 이 환경에 미리 설치된 Chromium
리비전이 `@playwright/test`가 기본 요구하는 리비전과 다를 수 있어 `playwright.config.ts`가
`executablePath`를 직접 지정한다 — 다른 환경에서 이 경로가 없다면 `npx playwright install`로
받은 뒤 그 옵션을 지워도 된다.

## 화면 구성

| 경로 | 화면 | 기능 |
|---|---|---|
| `/login` | CMN-001 | 통합 로그인 (FR-01) — COMMANDER 역할 전용 |
| `/initial-password` | CMN-002 | 최초 비밀번호 설정 |
| `/` | CMD-001 | 출동지령·사전분석 — DISPATCHED/IN_PROGRESS 출동 목록 + 사전분석(FR-02, NFR-03) + 후발대 경로·ETA(FR-20) |
| `/incidents/:incidentId` | CMD-002 | 현장 모니터링 대시보드 — 대원 배정(FR-19), 위험도순 대원 상태(FR-03), 통신담당 재지정(FR-19), 드론 정찰(FR-26), 알림·인수인계 피드(FR-06/18/22/23), 실시간 WebSocket |
| `/incidents/:incidentId/responders/:userId` | CMD-003 | 대원 상세 — 최신 생체·환경 데이터(FR-04), 관련 알림 이력 |
| CMD-002 내 다이얼로그 | CMD-006 | 출동 종료 확정 (FR-05) |

## 설계 원칙

- **CMD-001 "진입 화면" 정의**: CCTV 자동감지 출동은 `commander_id`가 배정되지 않으므로(관제 확인
  전엔 담당 지휘관이 정해지지 않음), 지휘관별 필터 대신 backend `GET /api/v1/incidents/active`가
  DISPATCHED/IN_PROGRESS 전체를 반환한다 — `@PreAuthorize("hasAnyRole('COMMANDER','ADMIN')")`로
  역할을 제한한다 (backend 갭 보강 작업 참조).
- **최소 권한 확장**: 지휘관이 현장 대원·드론을 식별하고 배정하려면 `GET /api/v1/accounts`(목록
  검색)·`GET /api/v1/accounts/{id}`, `GET /api/v1/devices/{id}` 조회가 필요해, 계정 등록·수정·
  비활성화는 ADMIN 전용으로 유지한 채 이 조회 엔드포인트만 COMMANDER에게 열었다.
- **대원 배정(FR-19)**: CMD-002 "배정 대원" 패널의 "+ 대원 배정"에서 이름·사번·소속으로 RESPONDER를
  검색해 배정한다. 이미 배정된 대원은 후보 목록에서 제외해 `UNIQUE(incident_id, user_id)` 위반을
  UI 단에서 미리 막고, 해당 출동의 최초 배정자는 backend가 자동으로 선발대·통신담당으로 지정한다.
- **NFR-01/02/05**: admin-web과 동일한 `tokens.css`/`components.css`를 그대로 재사용해 시각적
  일관성을 유지하고, 모든 저장·발송 액션은 `Banner`로 성공/실패를 명시한다.
- **실시간 갱신**: CMD-002는 notification-server의 `incident:{id}` WebSocket 채널에 join해
  `alert:created`/`alert:acknowledged` 수신 시 즉시 React Query를 무효화한다 — 10초 폴링은 백업
  경로.
- **생체·환경 데이터 표시 원칙**: `biometricData`/`environmentData`는 DB설계서상 자유 JSONB라
  고정 스키마가 없다. `{heartRate, bodyTemperature}` / `{ambientTemperature, gasLevel}` 관례를
  기대하되, 없는 키는 원본 그대로 나열한다 — 실제로 없는 값(예: 시계열 추세)을 임의로 만들어
  보여주지 않는다.
- **알림 발신 범위**: FR-18(진입정보)·FR-23(지원요청)은 USR-001(대원 앱)이 입력하는 것을 전제로
  CMD-002는 수신·확인현황(FR-22)만 표시한다. FR-06(위험정보)은 지휘관이 현장을 내려다보는 입장이므로
  이 화면에서 직접 발신(전체 브로드캐스트 또는 대원 지정)할 수 있게 했다.

- **FR-20 후발대 경로·ETA**: DB설계서에 소방서·차량 위치를 추적할 테이블이 없어(드론처럼 device로
  위치를 알 수 없음), backend가 관할 소방서 고정 좌표(`faind.routing.fire-station-*`)를 출발지로
  근사해 출동 확정 시 1회 계산·캐시한다(TTL 10분). CMD-001이 이 값을 사전분석과 나란히 보여주고,
  캐시가 없으면(TTL 만료 등) 재계산하지 않고 "정보 없음"을 그대로 표시한다.

## 알려진 제약

- Google Fonts를 CDN에서 불러온다 — admin-web과 동일하게 오프라인 환경에서는 시스템 폰트로 자연스럽게
  폴백된다.
- 대원 상태의 시계열(추세) 조회 API가 backend에 없어(최신 1건만 집계), CMD-003은 추세 그래프 대신
  최신 스냅샷만 보여준다.
