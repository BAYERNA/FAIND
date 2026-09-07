package com.faind.domain.report.entity;

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

// DB설계서 §3.10 report_analyses. FR-08: 제출된 보고서에 대한 SOP 대조 분석 결과 (1:1).
@Entity
@Table(name = "report_analyses")
public class ReportAnalysis {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "analysis_id")
  private UUID analysisId;

  @Column(name = "report_id", nullable = false, unique = true)
  private UUID reportId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "sop_match_result")
  private Map<String, Object> sopMatchResult;

  @Column(name = "risk_pattern")
  private String riskPattern;

  @Column
  private String recommendation;

  @Column(name = "pdf_url")
  private String pdfUrl;

  @Column(name = "review_status", nullable = false, length = 10)
  private String reviewStatus = "PENDING"; // PENDING / REVIEWED

  @Column(name = "analyzed_at", nullable = false)
  private LocalDateTime analyzedAt;

  protected ReportAnalysis() {}

  public ReportAnalysis(UUID reportId, Map<String, Object> sopMatchResult, String riskPattern, String recommendation) {
    this.reportId = reportId;
    this.sopMatchResult = sopMatchResult;
    this.riskPattern = riskPattern;
    this.recommendation = recommendation;
    this.reviewStatus = "PENDING";
    this.analyzedAt = LocalDateTime.now();
  }

  public void markReviewed() {
    this.reviewStatus = "REVIEWED";
  }

  public void attachPdf(String pdfUrl) {
    this.pdfUrl = pdfUrl;
  }

  public UUID getAnalysisId() {
    return analysisId;
  }

  public UUID getReportId() {
    return reportId;
  }

  public Map<String, Object> getSopMatchResult() {
    return sopMatchResult;
  }

  public String getRiskPattern() {
    return riskPattern;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public String getPdfUrl() {
    return pdfUrl;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public LocalDateTime getAnalyzedAt() {
    return analyzedAt;
  }
}
