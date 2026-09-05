package com.faind.domain.report.controller;

import com.faind.domain.report.dto.ReportAnalysisResponse;
import com.faind.domain.report.dto.ReportResponse;
import com.faind.domain.report.dto.ReportSaveRequest;
import com.faind.domain.report.service.ReportService;
import com.faind.global.security.AuthenticatedUser;
import com.faind.global.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// USR-002 사후보고서 작성 (FR-07) / USR-003 내 개인 리포트 (FR-08)
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/me")
  public ResponseEntity<List<ReportResponse>> myReports(@CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.ok(reportService.getMyReports(currentUser.userId()));
  }

  @GetMapping("/{reportId}")
  public ResponseEntity<ReportResponse> get(@PathVariable UUID reportId, @CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.ok(reportService.get(reportId, currentUser.userId()));
  }

  // FR-07 "임시 저장" 버튼
  @PutMapping("/{reportId}/draft")
  public ResponseEntity<ReportResponse> saveDraft(
      @PathVariable UUID reportId, @CurrentUser AuthenticatedUser currentUser, @RequestBody ReportSaveRequest request) {
    return ResponseEntity.ok(reportService.saveDraft(reportId, currentUser.userId(), request));
  }

  // FR-07 "제출" 버튼
  @PatchMapping("/{reportId}/submit")
  public ResponseEntity<ReportResponse> submit(
      @PathVariable UUID reportId, @CurrentUser AuthenticatedUser currentUser, @RequestBody ReportSaveRequest request) {
    return ResponseEntity.ok(reportService.submit(reportId, currentUser.userId(), request));
  }

  // FR-08 SOP 교차 검증 결과표 (USR-003)
  @GetMapping("/{reportId}/analysis")
  public ResponseEntity<ReportAnalysisResponse> getAnalysis(
      @PathVariable UUID reportId, @CurrentUser AuthenticatedUser currentUser) {
    return ResponseEntity.ok(reportService.getAnalysis(reportId, currentUser.userId()));
  }
}
