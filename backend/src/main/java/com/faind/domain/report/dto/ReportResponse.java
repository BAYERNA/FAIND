package com.faind.domain.report.dto;

import com.faind.domain.report.entity.Report;
import java.time.LocalDateTime;
import java.util.UUID;

// NFR-02: 저장/제출 성공 여부를 프론트가 명확히 판단할 수 있도록 status·updatedAt을 항상 함께 내려준다.
public record ReportResponse(
    UUID reportId,
    UUID incidentId,
    UUID authorId,
    String content,
    String videoRef,
    String status,
    LocalDateTime submittedAt,
    LocalDateTime updatedAt) {

  public static ReportResponse from(Report report) {
    return new ReportResponse(
        report.getReportId(),
        report.getIncidentId(),
        report.getAuthorId(),
        report.getContent(),
        report.getVideoRef(),
        report.getStatus(),
        report.getSubmittedAt(),
        report.getUpdatedAt());
  }
}
