package com.faind.domain.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

// DB설계서 §3.5 incident_assignments. FR-19: is_first_wave/is_comms_lead는 IncidentService가
// 배정 시점에 자동 산출한다 (해당 출동에서 assigned_at이 가장 이른 배정 = 선발대).
@Entity
@Table(name = "incident_assignments")
public class IncidentAssignment {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "assignment_id")
  private UUID assignmentId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "role_in_incident", length = 30)
  private String roleInIncident;

  @Column(name = "is_first_wave", nullable = false)
  private boolean firstWave;

  @Column(name = "is_comms_lead", nullable = false)
  private boolean commsLead;

  @Column(name = "assigned_at", nullable = false)
  private LocalDateTime assignedAt;

  protected IncidentAssignment() {}

  public IncidentAssignment(UUID incidentId, UUID userId, String roleInIncident) {
    this.incidentId = incidentId;
    this.userId = userId;
    this.roleInIncident = roleInIncident;
    this.firstWave = false;
    this.commsLead = false;
    this.assignedAt = LocalDateTime.now();
  }

  public void markAsFirstWave() {
    this.firstWave = true;
  }

  // FR-19: 지휘관이 CMD-002에서 통신 담당을 재지정할 때, 기존 담당은 해제하고 새 담당만 세운다
  // (DB의 PARTIAL UNIQUE(incident_id) WHERE is_comms_lead=true 제약과 짝을 맞춘 애플리케이션 로직).
  public void setCommsLead(boolean commsLead) {
    this.commsLead = commsLead;
  }

  public UUID getAssignmentId() {
    return assignmentId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getRoleInIncident() {
    return roleInIncident;
  }

  public boolean isFirstWave() {
    return firstWave;
  }

  public boolean isCommsLead() {
    return commsLead;
  }

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }
}
