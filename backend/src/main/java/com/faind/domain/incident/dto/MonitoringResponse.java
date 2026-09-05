package com.faind.domain.incident.dto;

import java.util.List;

// CMD-002 현장 모니터링 대시보드 화면 하나를 그리기 위한 집계 응답.
public record MonitoringResponse(
    IncidentResponse incident,
    List<ResponderStatusResponse> responders,
    List<AssignmentResponse> assignments,
    List<DroneDispatchResponse> droneDispatches) {}
