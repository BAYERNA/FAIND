# FAIND 관리자 웹 콘솔

React 19 + TypeScript + Vite. 와이어프레임의 CMN-001/002, ADM-001/002/003/006/009 화면을
backend(Java) API와 실제로 연동해 구현했다.

## 로컬 실행

```bash
npm install
npm run dev
```

`vite.config.ts`의 dev proxy가 `/api` 요청을 `VITE_BACKEND_URL`(기본 `http://localhost:8080`)로
전달한다. backend를 먼저 기동해 두어야 한다 (`../backend/README.md` 참조).

## 자동화 테스트

```bash
npm run test
```

Vitest + React Testing Library. `api/client.ts`(NFR-02 성공/실패 구분, 토큰 첨부, query 직렬화)와
`auth/AuthContext.tsx`(로그인 시 토큰·사용자 정보 영속화, 로그아웃/최초 비밀번호 변경 시 상태 갱신)를
검증한다. 실제 backend 호출 없이 `fetch`를 목(mock)으로 대체한다.

### E2E (Playwright)

```bash
npm run test:e2e
```

지금까지 매 Phase마다 수동으로 해온 브라우저 검증(로그인 성공/실패, 보호된 화면 접근)을
코드화했다. 이 앱의 dev server(`webServer`)는 자동으로 띄우지만, **backend(8080)와
Postgres·Redis는 미리 떠 있어야 한다** — `globalSetup`이 `../scripts/seed-e2e-accounts.sql`을
psql로 적용해 전용 계정(E2E-ADMIN 등, 비밀번호는 스크립트 주석 참조)을 심고 이전 실행의
로그인 실패 카운터(Phase 10 레이트리미팅)를 초기화한다. 이 환경에 미리 설치된 Chromium
리비전이 `@playwright/test`가 기본 요구하는 리비전과 다를 수 있어 `playwright.config.ts`가
`executablePath`를 직접 지정한다 — 다른 환경에서 이 경로가 없다면 `npx playwright install`로
받은 뒤 그 옵션을 지워도 된다.

## 화면 구성

| 경로 | 화면 | 기능 |
|---|---|---|
| `/login` | CMN-001 | 통합 로그인 (FR-01) |
| `/initial-password` | CMN-002 | 최초 비밀번호 설정 — 로그인 후 `RequireAuth`가 강제 이동 |
| `/` | ADM-001 | 관리자 홈 — 통계 카드(FR-09), AI 의심감지 대기열(FR-24, NFR-08), 최근 출동 |
| `/accounts`, `/accounts/new`, `/accounts/:id/edit` | ADM-002/003 | 계정 목록(검색·필터, NFR-05) / 등록·수정 / 임시 비밀번호 재발급 / 비활성화 |
| `/devices` | ADM-006 | 기기 등록·매핑 (FR-11, CCTV/드론 자산 등록 겸용) / CCTV·드론 위치 수정 |
| `/statistics` | ADM-009 | 기관 통계 대시보드 (FR-13, FR-27 골든타임 단축효과) |

## 설계 원칙

- **NFR-01 색상 토큰화**: `src/styles/tokens.css`의 `--color-*` 변수 밖의 색상 하드코딩을 하지 않는다.
- **NFR-02 명시적 성공/실패 피드백**: 모든 저장/제출은 React Query mutation의 성공/실패를
  `Banner` 컴포넌트로 명시한다. 지난 QA에서 "버튼을 눌러도 반응 없음"으로 반복됐던 결함의
  재발 방지 지점.
- **NFR-05 검색·필터**: 계정/기기 목록 모두 keyword·필터 값을 React Query의 `queryKey`에 그대로
  넣어, 값이 바뀔 때마다 실제로 새 API 요청이 나가는 것을 보장한다.
- **ADM-003 프리필**: 수정 진입 시 `getAccount()` 응답으로 폼 전 필드를 채운다 — QA 최우선
  재검증 대상이었던 "수정 진입 시 폼이 비어 있음" 결함의 재발 방지 지점.
- **전체 화면 기능 점검(사후)**: backend에 이미 있던 임시 비밀번호 재발급·계정 비활성화(ADM-002)와
  CCTV·드론 위치 수정(ADM-006) API가 어떤 화면에도 연결돼 있지 않았던 결함을 점검 과정에서 발견해
  해당 화면에 버튼을 추가했다 — API 함수는 만들어 두고 화면에서 호출을 빠뜨리는 패턴의 재발 방지
  참고 사례.

## 알려진 제약

- Google Fonts(IBM Plex Sans KR/Mono)를 CDN에서 불러온다 — 오프라인/제한된 네트워크 환경에서는
  시스템 폰트로 자연스럽게 폴백된다(레이아웃에는 영향 없음).
- ADM-002 목록의 "직급"/"최근접속" 열은 DB설계서에 대응 컬럼이 없어(§3.1 users) 와이어프레임에서
  제외했다 — 실제 데이터로 채울 수 없는 값을 임의로 만들어 보여주지 않는다.
- "근무 대원"(ADM-001) 카드는 근무편성(ADM-005, Won't Have) 없이 "활성 대원 계정 수"로 근사한다
  (backend `AccountService.countActiveResponders()` 주석 참조).
