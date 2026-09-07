package com.faind.domain.incident.entity;

// DB설계서 §3.3: v2.2부터 DEFAULT 없음(fail-closed) — 모든 INSERT가 명시적으로 값을 지정해야 한다.
public enum IncidentStatus {
  AI_SUSPECTED, // FR-24: 관제센터 확인 대기 (사람이 확정하기 전까지 실제 출동 아님)
  DISPATCHED,
  IN_PROGRESS,
  CLOSED
}
