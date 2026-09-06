# FAIND 대원 앱

React 19 + TypeScript + Vite. 와이어프레임의 USR-001/002/003 화면을 backend(Java)와
notification-server(Node) 양쪽에 실제로 연동해 구현했다. admin-web(관리자)·commander-tablet(지휘관)에
이어 세 번째 클라이언트로, 현장 대원이 한 손으로 조작하는 것을 전제로 좁은 단일 컬럼 + 하단 탭바
레이아웃을 쓴다 (태블릿·데스크톱 폭을 가정한 다른 두 앱과 다른 폼팩터).

## 로컬 실행

```bash
npm install
npm run dev
```

`http://localhost:5175`에서 접속 (admin-web 5173·commander-tablet 5174와 동시에 띄울 수 있게 포트를
분리했다). dev server가 `/api`는 backend로, `/notify`·`/socket.io`는 notification-server로
프록시하므로 둘 다 먼저 기동해야 한다.

backend의 `faind.cors.allowed-origins`와 notification-server의 `CORS_ALLOWED_ORIGINS`에 이 포트가
등록되어 있어야 브라우저에서 정상 동작한다 (둘 다 리포지토리 기본값에 이미 포함).

## 화면 구성

| 경로 | 화면 | 기능 |
|---|---|---|
| `/login` | CMN-001 | 통합 로그인 (FR-01) — RESPONDER 역할 전용 |
| `/initial-password` | CMN-002 | 최초 비밀번호 설정 |
| `/` | USR-001 | 현장 대응 — 내 배정 출동, 상태 보고(FR-03/04/12), 진입정보(FR-18)·지원요청(FR-23)·위험정보(FR-06) 발신, 확인(FR-22), 실시간 WebSocket |
| `/reports` | USR-002 | 내 보고서 목록 — FR-05 종료 확정 시 개인 채널로 오는 신규 작성 요청을 실시간 반영 |
| `/reports/:reportId` | USR-002 | 사후보고서 작성 — 임시저장/제출 (FR-07) |
| `/reports/:reportId/analysis` | USR-003 | SOP 교차 검증 결과 — "보고서 기록 / 실행 여부 / 추천 SOP 근거" 3열 표 (FR-08, NFR-07) |

## 설계 원칙

- **backend 갭 보강**: 이 대원이 배정된, 아직 종료되지 않은 출동을 찾는 `GET /api/v1/incidents/my-active`가
  없어서 새로 추가했다 (`hasRole('RESPONDER')`) — commander-tablet의 `/active`(전체)와 달리 이건
  `incident_assignments.user_id` 기준으로 본인 배정만 좁힌다.
- **FR-18 발신 권한**: 진입정보 공유는 이 출동의 통신담당(`is_comms_lead`)에게만 탭이 노출된다 —
  notification-server 쪽 주석("controller가 아니라 클라이언트 쪽 화면 노출 조건으로 처리")을 그대로
  따른 것이다. 지원요청·위험정보는 배정된 대원 누구나 보낼 수 있게 열어뒀다 — 현장에서 위험을
  발견한 사람이 곧바로 알릴 수 있어야 한다는 판단.
- **FR-03/04/12 상태 보고는 수동 입력**: 실제 웨어러블 연동이 없는 상태에서 값을 임의로 만들어
  전송하는 대신, 대원이 직접 입력해 보내는 폼으로 뒀다 — CMD-002/003이 그대로 보여주는 값과
  같은 출처다.
- **FR-05 실시간 반영**: 출동 종료 확정 시 backend가 만드는 DRAFT 보고서는 notification-server가
  `user:{userId}` 개인 채널로 `report:draft-created`를 쏜다. USR-002는 이 이벤트를 받아 배너로
  알리고 목록을 즉시 갱신한다 — 새로고침 없이 실시간으로 검증했다.
- **USR-003 표는 원본을 벗어나지 않는다**: `sopMatchResult`가 ai-server의 `{items:[{reportRecord,
  executed,recommendedSop}], ...}` 형태를 벗어나면(고정 스키마가 아니므로) 3열 표 대신 원본 키-값을
  그대로 나열한다 — 실제로 없는 형태를 임의로 맞춰 보여주지 않는다.

## 알려진 제약

- Google Fonts를 CDN에서 불러온다 — 다른 두 앱과 동일하게 오프라인 환경에서는 시스템 폰트로
  자연스럽게 폴백된다.
- 대원 상태 보고는 수동 입력이라, 실제 웨어러블 기기와의 자동 연동은 범위 밖이다.
