package com.faind.domain.incident.event;

import java.util.List;
import java.util.UUID;

// FR-05 QA 최우선 재검증 대상의 재설계 지점. IncidentService.close()가 트랜잭션 커밋 직전에
// 발행하고, IncidentClosedListener가 AFTER_COMMIT 시점에 받아 사후보고서 초안 생성 +
// 대원 알림 발송을 수행한다 (커밋 전에 실행되면 아직 존재하지 않는 incident 상태를 참조할 위험이 있다).
public record IncidentClosedEvent(UUID incidentId, List<UUID> assignedResponderIds) {}
