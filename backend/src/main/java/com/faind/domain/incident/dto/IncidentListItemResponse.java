package com.faind.domain.incident.dto;

import com.faind.domain.incident.entity.Incident;
import java.time.LocalDateTime;
import java.util.UUID;

// ADM-001 "최근 출동 목록" 표의 한 행.
public record IncidentListItemResponse(
    UUID incidentId, String incidentNumber, String incidentType, String status, LocalDateTime reportedAt,
    long assignedResponderCount) {

  public static IncidentListItemResponse from(Incident incident, long assignedResponderCount) {
    return new IncidentListItemResponse(
        incident.getIncidentId(), incident.getIncidentNumber(), incident.getIncidentType().name(),
        incident.getStatus().name(), incident.getReportedAt(), assignedResponderCount);
  }
}
