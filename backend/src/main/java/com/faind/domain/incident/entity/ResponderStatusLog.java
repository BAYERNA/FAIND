package com.faind.domain.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

// DB설계서 §3.6 responder_status_logs. CMD-002 위험도 자동정렬(FR-03), CMD-003 상세뷰(FR-04)의 소스.
@Entity
@Table(name = "responder_status_logs")
public class ResponderStatusLog {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "log_id")
  private UUID logId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "recorded_at", nullable = false)
  private LocalDateTime recordedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "biometric_data")
  private Map<String, Object> biometricData;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "environment_data")
  private Map<String, Object> environmentData;

  @Column(name = "risk_level", length = 10)
  private String riskLevel; // NORMAL / CAUTION / DANGER

  @Column(name = "connection_status", length = 15)
  private String connectionStatus; // CONNECTED / MESH / SMS / DISCONNECTED

  protected ResponderStatusLog() {}

  public ResponderStatusLog(
      UUID incidentId, UUID userId, LocalDateTime recordedAt, Map<String, Object> biometricData,
      Map<String, Object> environmentData, String riskLevel, String connectionStatus) {
    this.incidentId = incidentId;
    this.userId = userId;
    this.recordedAt = recordedAt;
    this.biometricData = biometricData;
    this.environmentData = environmentData;
    this.riskLevel = riskLevel;
    this.connectionStatus = connectionStatus;
  }

  public UUID getLogId() {
    return logId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public UUID getUserId() {
    return userId;
  }

  public LocalDateTime getRecordedAt() {
    return recordedAt;
  }

  public Map<String, Object> getBiometricData() {
    return biometricData;
  }

  public Map<String, Object> getEnvironmentData() {
    return environmentData;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public String getConnectionStatus() {
    return connectionStatus;
  }
}
