package com.faind.domain.report.entity;

import com.faind.global.error.BusinessException;
import com.faind.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

// DB설계서 §3.9 reports. FR-07 QA 최우선 재검증 대상 — "임시저장, 제출 미작동".
@Entity
@Table(name = "reports")
public class Report {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "report_id")
  private UUID reportId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column
  private String content;

  @Column(name = "video_ref")
  private String videoRef;

  @Column(nullable = false, length = 10)
  private String status = "DRAFT"; // DRAFT / SUBMITTED

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected Report() {}

  public Report(UUID incidentId, UUID authorId) {
    this.incidentId = incidentId;
    this.authorId = authorId;
    this.status = "DRAFT";
    this.createdAt = LocalDateTime.now();
  }

  // FR-07: "임시 저장" — QA 재검증 대상이라, 호출될 때마다 updated_at이 갱신되는지가 검증 포인트.
  public void saveDraft(String content, String videoRef) {
    ensureNotSubmitted();
    this.content = content;
    if (videoRef != null) {
      this.videoRef = videoRef;
    }
    this.updatedAt = LocalDateTime.now();
  }

  // FR-07: "제출" — 제출 후에는 재수정 불가(SUBMITTED는 종단 상태).
  public void submit(String content, String videoRef) {
    ensureNotSubmitted();
    this.content = content;
    if (videoRef != null) {
      this.videoRef = videoRef;
    }
    this.status = "SUBMITTED";
    this.submittedAt = LocalDateTime.now();
    this.updatedAt = this.submittedAt;
  }

  private void ensureNotSubmitted() {
    if ("SUBMITTED".equals(status)) {
      throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
    }
  }

  public UUID getReportId() {
    return reportId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public UUID getAuthorId() {
    return authorId;
  }

  public String getContent() {
    return content;
  }

  public String getVideoRef() {
    return videoRef;
  }

  public String getStatus() {
    return status;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
