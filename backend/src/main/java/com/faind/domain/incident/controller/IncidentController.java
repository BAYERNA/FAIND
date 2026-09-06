package com.faind.domain.incident.controller;

import com.faind.domain.incident.dto.AiJudgmentSummaryResponse;
import com.faind.domain.incident.dto.AssignmentRequest;
import com.faind.domain.incident.dto.AssignmentResponse;
import com.faind.domain.incident.dto.DashboardSummaryResponse;
import com.faind.domain.incident.dto.IncidentCreateRequest;
import com.faind.domain.incident.dto.IncidentListItemResponse;
import com.faind.domain.incident.dto.IncidentResponse;
import com.faind.domain.incident.dto.MonitoringResponse;
import com.faind.domain.incident.dto.PreAnalysisResponse;
import com.faind.domain.incident.dto.DroneReconRequest;
import com.faind.domain.incident.dto.ResponderStatusRequest;
import com.faind.domain.incident.service.DroneDispatchService;
import com.faind.domain.incident.service.IncidentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CMD-001 출동지령·사전분석(FR-02) / CMD-002 현장 모니터링(FR-03) / CMD-003 대원 상세(FR-04) /
// CMD-006 출동 종료 처리(FR-05) / FR-12,19,20
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

  private final IncidentService incidentService;
  private final DroneDispatchService droneDispatchService;

  public IncidentController(IncidentService incidentService, DroneDispatchService droneDispatchService) {
    this.incidentService = incidentService;
    this.droneDispatchService = droneDispatchService;
  }

  @PostMapping
  public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentCreateRequest request) {
    return ResponseEntity.ok(incidentService.create(request));
  }

  // ADM-001 관리자 홈 통계 카드 (FR-09)
  @GetMapping("/dashboard-summary")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
    return ResponseEntity.ok(incidentService.getDashboardSummary());
  }

  // ADM-001 "최근 출동 목록"
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<IncidentListItemResponse>> listRecent(Pageable pageable) {
    return ResponseEntity.ok(incidentService.listRecent(pageable));
  }

  // CMD-001 지휘관 태블릿 진입 화면: DISPATCHED/IN_PROGRESS 전체 (CCTV 출처는 commanderId가 없어 개인별 필터 불가)
  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('COMMANDER','ADMIN')")
  public ResponseEntity<List<IncidentResponse>> listActive() {
    return ResponseEntity.ok(incidentService.listActive());
  }

  @GetMapping("/{incidentId}")
  public ResponseEntity<IncidentResponse> get(@PathVariable UUID incidentId) {
    return ResponseEntity.ok(incidentService.getIncident(incidentId));
  }

  // CMD-001: 사전분석 결과 + NFR-03 검증용 소요시간
  @GetMapping("/{incidentId}/pre-analysis")
  public ResponseEntity<PreAnalysisResponse> getPreAnalysis(@PathVariable UUID incidentId) {
    return ResponseEntity.ok(incidentService.getPreAnalysis(incidentId));
  }

  // CMD-002 드론 정찰 카드(FR-26) 등에서 사용하는 AI 판단 이력
  @GetMapping("/{incidentId}/ai-judgments")
  public ResponseEntity<List<AiJudgmentSummaryResponse>> getAiJudgments(@PathVariable UUID incidentId) {
    return ResponseEntity.ok(incidentService.getAiJudgments(incidentId));
  }

  // FR-19: 배정 시 선발대/통신담당 자동 산출
  @PostMapping("/{incidentId}/assignments")
  @PreAuthorize("hasAnyRole('COMMANDER','ADMIN')")
  public ResponseEntity<AssignmentResponse> assign(@PathVariable UUID incidentId, @Valid @RequestBody AssignmentRequest request) {
    return ResponseEntity.ok(incidentService.assign(incidentId, request));
  }

  // FR-19: 지휘관이 통신 담당 재지정
  @PatchMapping("/{incidentId}/assignments/{userId}/comms-lead")
  @PreAuthorize("hasAnyRole('COMMANDER','ADMIN')")
  public ResponseEntity<AssignmentResponse> reassignCommsLead(@PathVariable UUID incidentId, @PathVariable UUID userId) {
    return ResponseEntity.ok(incidentService.reassignCommsLead(incidentId, userId));
  }

  // FR-03/04/12: 대원 실시간 상태 적재 (웨어러블·앱에서 주기적으로 전송)
  @PostMapping("/{incidentId}/responder-status")
  public ResponseEntity<Void> recordResponderStatus(
      @PathVariable UUID incidentId, @Valid @RequestBody ResponderStatusRequest request) {
    incidentService.recordResponderStatus(incidentId, request);
    return ResponseEntity.ok().build();
  }

  // CMD-002 현장 모니터링 대시보드 전체 데이터
  @GetMapping("/{incidentId}/monitoring")
  public ResponseEntity<MonitoringResponse> getMonitoring(@PathVariable UUID incidentId) {
    return ResponseEntity.ok(incidentService.getMonitoring(incidentId));
  }

  // FR-05 CMD-006 "종료 확정" — QA 최우선 재검증 대상
  @PatchMapping("/{incidentId}/close")
  @PreAuthorize("hasAnyRole('COMMANDER','ADMIN')")
  public ResponseEntity<IncidentResponse> close(@PathVariable UUID incidentId) {
    return ResponseEntity.ok(incidentService.close(incidentId));
  }

  // FR-26: 드론 도착 후 정찰 결과 콜백 (ai-server → Java 모놀리식)
  @PostMapping("/drone-dispatches/{dispatchId}/recon-result")
  public ResponseEntity<Void> recordDroneReconResult(
      @PathVariable UUID dispatchId, @Valid @RequestBody DroneReconRequest request) {
    droneDispatchService.recordReconResult(dispatchId, request.confidenceScore(), request.summary(), request.videoRef());
    return ResponseEntity.ok().build();
  }
}
