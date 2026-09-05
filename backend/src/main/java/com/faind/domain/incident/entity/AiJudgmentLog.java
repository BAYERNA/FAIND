package com.faind.domain.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

// DB설계서 §3.11 ai_judgment_logs. incident 패키지가 쓰기(사전분석·CCTV감지·드론정찰 로그 생성)를
// 소유하고, statistics 패키지는 같은 테이블을 읽기 전용 read model로 재사용한다(§1 원칙 5 "재사용 우선").
@Entity
@Table(name = "ai_judgment_logs")
public class AiJudgmentLog {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "judgment_id")
  private UUID judgmentId;

  @Column(name = "judgment_type", nullable = false, length = 30)
  private String judgmentType; // PRE_ANALYSIS/REPORT_SOP_MATCH/RISK_DETECTION/CCTV_DETECTION/DRONE_RECON

  @Column(name = "related_incident_id")
  private UUID relatedIncidentId;

  @Column(name = "related_report_id")
  private UUID relatedReportId;

  @Column(name = "source_device_id")
  private UUID sourceDeviceId;

  @Column(name = "confidence_score", precision = 5, scale = 2)
  private BigDecimal confidenceScore;

  @Column
  private String summary;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  protected AiJudgmentLog() {}

  public AiJudgmentLog(
      String judgmentType, UUID relatedIncidentId, UUID relatedReportId, UUID sourceDeviceId,
      BigDecimal confidenceScore, String summary) {
    this.judgmentType = judgmentType;
    this.relatedIncidentId = relatedIncidentId;
    this.relatedReportId = relatedReportId;
    this.sourceDeviceId = sourceDeviceId;
    this.confidenceScore = confidenceScore;
    this.summary = summary;
    this.createdAt = LocalDateTime.now();
  }

  public void linkToIncident(UUID incidentId) {
    this.relatedIncidentId = incidentId;
  }

  public UUID getJudgmentId() {
    return judgmentId;
  }

  public String getJudgmentType() {
    return judgmentType;
  }

  public UUID getRelatedIncidentId() {
    return relatedIncidentId;
  }

  public UUID getRelatedReportId() {
    return relatedReportId;
  }

  public UUID getSourceDeviceId() {
    return sourceDeviceId;
  }

  public BigDecimal getConfidenceScore() {
    return confidenceScore;
  }

  public String getSummary() {
    return summary;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
