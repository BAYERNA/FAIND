package com.faind.domain.incident.event;

import java.util.UUID;

// §6.3 인메모리 이벤트. 발행 시점 = 실제 출동(DISPATCHED)이 확정된 순간.
// (MANUAL_REPORT는 생성 즉시, CCTV_AUTO_DETECTION은 관리자가 확정한 순간 — ADM-001 annot#2)
// 리스너: 드론 자동배정(DroneDispatchService) + AI 사전분석 요청(AiAnalysisPort).
public record IncidentCreatedEvent(UUID incidentId, boolean droneEligible) {}
