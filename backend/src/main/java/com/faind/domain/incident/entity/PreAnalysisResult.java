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

// DB설계서 §3.4 pre_analysis_results. FR-02, NFR-03(3초 이내 표시 목표).
@Entity
@Table(name = "pre_analysis_results")
public class PreAnalysisResult {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "result_id")
  private UUID resultId;

  @Column(name = "incident_id", nullable = false, unique = true)
  private UUID incidentId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "building_info")
  private Map<String, Object> buildingInfo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "hazard_info")
  private Map<String, Object> hazardInfo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "fire_history_info")
  private Map<String, Object> fireHistoryInfo;

  @Column(name = "data_source")
  private String dataSource = "소방청 공공데이터 API";

  @Column(name = "analyzed_at", nullable = false)
  private LocalDateTime analyzedAt;

  protected PreAnalysisResult() {}

  public PreAnalysisResult(
      UUID incidentId, Map<String, Object> buildingInfo, Map<String, Object> hazardInfo,
      Map<String, Object> fireHistoryInfo, String dataSource) {
    this.incidentId = incidentId;
    this.buildingInfo = buildingInfo;
    this.hazardInfo = hazardInfo;
    this.fireHistoryInfo = fireHistoryInfo;
    this.dataSource = dataSource;
    this.analyzedAt = LocalDateTime.now();
  }

  public UUID getResultId() {
    return resultId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public Map<String, Object> getBuildingInfo() {
    return buildingInfo;
  }

  public Map<String, Object> getHazardInfo() {
    return hazardInfo;
  }

  public Map<String, Object> getFireHistoryInfo() {
    return fireHistoryInfo;
  }

  public String getDataSource() {
    return dataSource;
  }

  public LocalDateTime getAnalyzedAt() {
    return analyzedAt;
  }
}
