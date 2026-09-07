package com.faind.domain.incident.dto;

import com.faind.domain.incident.entity.IncidentAssignment;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponse(
    UUID assignmentId, UUID userId, String roleInIncident, boolean firstWave, boolean commsLead, LocalDateTime assignedAt) {

  public static AssignmentResponse from(IncidentAssignment assignment) {
    return new AssignmentResponse(
        assignment.getAssignmentId(),
        assignment.getUserId(),
        assignment.getRoleInIncident(),
        assignment.isFirstWave(),
        assignment.isCommsLead(),
        assignment.getAssignedAt());
  }
}
