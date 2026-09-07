package com.faind.domain.report.dto;

import com.faind.domain.report.entity.ReportAnalysis;
import java.util.Map;
import java.util.UUID;

// USR-003 내 개인 리포트 — SOP 교차 검증 결과표.
public record ReportAnalysisResponse(
    UUID reportId, Map<String, Object> sopMatchResult, String riskPattern, String recommendation, String pdfUrl,
    String reviewStatus) {

  public static ReportAnalysisResponse from(ReportAnalysis analysis) {
    return new ReportAnalysisResponse(
        analysis.getReportId(), analysis.getSopMatchResult(), analysis.getRiskPattern(), analysis.getRecommendation(),
        analysis.getPdfUrl(), analysis.getReviewStatus());
  }
}
