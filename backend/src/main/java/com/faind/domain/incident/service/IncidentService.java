package com.faind.domain.incident.service;

import com.faind.domain.auth.service.AccountService;
import com.faind.domain.device.service.DeviceService;
import com.faind.domain.incident.dto.AiJudgmentSummaryResponse;
import com.faind.domain.incident.dto.AssignmentRequest;
import com.faind.domain.incident.dto.AssignmentResponse;
import com.faind.domain.incident.dto.DashboardSummaryResponse;
import com.faind.domain.incident.dto.DroneDispatchResponse;
import com.faind.domain.incident.dto.IncidentCreateRequest;
import com.faind.domain.incident.dto.IncidentListItemResponse;
import com.faind.domain.incident.dto.IncidentResponse;
import com.faind.domain.incident.dto.MonitoringResponse;
import com.faind.domain.incident.dto.PreAnalysisResponse;
import com.faind.domain.incident.dto.ResponderStatusRequest;
import com.faind.domain.incident.dto.ResponderStatusResponse;
import com.faind.domain.incident.dto.RouteEstimateResponse;
import com.faind.domain.incident.entity.Incident;
import com.faind.domain.incident.entity.IncidentAssignment;
import com.faind.domain.incident.entity.IncidentType;
import com.faind.domain.incident.entity.PreAnalysisResult;
import com.faind.domain.incident.entity.ResponderStatusLog;
import com.faind.domain.incident.entity.IncidentStatus;
import com.faind.domain.incident.event.IncidentClosedEvent;
import com.faind.domain.incident.event.IncidentCreatedEvent;
import com.faind.domain.incident.repository.AiJudgmentLogRepository;
import com.faind.domain.incident.repository.DroneDispatchRepository;
import com.faind.domain.incident.repository.IncidentAssignmentRepository;
import com.faind.domain.incident.repository.IncidentRepository;
import com.faind.domain.incident.repository.PreAnalysisResultRepository;
import com.faind.domain.incident.repository.ResponderStatusLogRepository;
import com.faind.global.error.BusinessException;
import com.faind.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// FR-02,03,04,05,12,19,20 핵심 로직. 출동 생성/조회/배정/종료를 다룬다.
@Service
@Transactional(readOnly = true)
public class IncidentService {

  // CMD-002 annot#1: 위험도 순 자동정렬 (위험 → 주의 → 정상 → 알 수 없음)
  private static final Map<String, Integer> RISK_ORDER = Map.of("DANGER", 0, "CAUTION", 1, "NORMAL", 2);

  private final IncidentRepository incidentRepository;
  private final IncidentAssignmentRepository assignmentRepository;
  private final PreAnalysisResultRepository preAnalysisResultRepository;
  private final ResponderStatusLogRepository responderStatusLogRepository;
  private final DroneDispatchRepository droneDispatchRepository;
  private final IncidentNumberGenerator incidentNumberGenerator;
  private final ApplicationEventPublisher eventPublisher;
  private final AccountService accountService;
  private final DeviceService deviceService;
  private final AiJudgmentLogRepository aiJudgmentLogRepository;
  private final RoutingApiClient routingApiClient;

  public IncidentService(
      IncidentRepository incidentRepository,
      IncidentAssignmentRepository assignmentRepository,
      PreAnalysisResultRepository preAnalysisResultRepository,
      ResponderStatusLogRepository responderStatusLogRepository,
      DroneDispatchRepository droneDispatchRepository,
      IncidentNumberGenerator incidentNumberGenerator,
      ApplicationEventPublisher eventPublisher,
      AccountService accountService,
      DeviceService deviceService,
      AiJudgmentLogRepository aiJudgmentLogRepository,
      RoutingApiClient routingApiClient) {
    this.incidentRepository = incidentRepository;
    this.assignmentRepository = assignmentRepository;
    this.preAnalysisResultRepository = preAnalysisResultRepository;
    this.responderStatusLogRepository = responderStatusLogRepository;
    this.droneDispatchRepository = droneDispatchRepository;
    this.incidentNumberGenerator = incidentNumberGenerator;
    this.eventPublisher = eventPublisher;
    this.accountService = accountService;
    this.deviceService = deviceService;
    this.aiJudgmentLogRepository = aiJudgmentLogRepository;
    this.routingApiClient = routingApiClient;
  }

  @Transactional
  public IncidentResponse create(IncidentCreateRequest request) {
    Incident incident = Incident.manualReport(
        incidentNumberGenerator.next(),
        IncidentType.valueOf(request.incidentType()),
        request.address(),
        request.latitude(),
        request.longitude(),
        request.reportedAt() != null ? request.reportedAt() : LocalDateTime.now(),
        request.commanderId());
    incidentRepository.save(incident);
    // §0.3: source 무관하게 "실제 출동(DISPATCHED)이 확정된 순간"에만 발행 — 사람 신고는 접수 즉시가 그 순간이다.
    eventPublisher.publishEvent(new IncidentCreatedEvent(incident.getIncidentId(), true));
    return IncidentResponse.from(incident);
  }

  public IncidentResponse getIncident(UUID incidentId) {
    return IncidentResponse.from(findIncident(incidentId));
  }

  public PreAnalysisResponse getPreAnalysis(UUID incidentId) {
    Incident incident = findIncident(incidentId);
    PreAnalysisResult result = preAnalysisResultRepository.findByIncidentId(incidentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아직 사전분석 결과가 없습니다."));
    return PreAnalysisResponse.from(result, incident.getReportedAt());
  }

  @Transactional
  public AssignmentResponse assign(UUID incidentId, AssignmentRequest request) {
    findIncident(incidentId); // 존재 검증
    boolean isFirstAssignmentOfIncident = !assignmentRepository.existsByIncidentId(incidentId);

    IncidentAssignment assignment = new IncidentAssignment(incidentId, request.userId(), request.roleInIncident());
    if (isFirstAssignmentOfIncident) {
      // FR-19: 해당 출동의 최초 배정자 = 선발대, 그중 최선임자를 통신 담당으로 자동 지정.
      // MVP는 "최초 배정 = 최선임"으로 단순화한다 (계급 데이터가 없으면 배정 순서로 결정).
      assignment.markAsFirstWave();
      assignment.setCommsLead(true);
    }
    assignmentRepository.save(assignment);
    return AssignmentResponse.from(assignment);
  }

  // FR-19: 지휘관이 CMD-002에서 통신 담당을 재지정.
  @Transactional
  public AssignmentResponse reassignCommsLead(UUID incidentId, UUID newCommsLeadUserId) {
    assignmentRepository.findByIncidentIdAndCommsLeadTrue(incidentId).ifPresent(prev -> prev.setCommsLead(false));
    IncidentAssignment next = assignmentRepository.findByIncidentIdAndUserId(incidentId, newCommsLeadUserId)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "해당 출동에 배정되지 않은 대원입니다."));
    next.setCommsLead(true);
    return AssignmentResponse.from(next);
  }

  // GET /{incidentId}, /{incidentId}/monitoring 컨트롤러의 RESPONDER 권한 경계용 — 배정되지 않은
  // 출동의 모니터링(동료 대원 생체·환경 데이터 포함)을 아무 대원이나 조회하지 못하게 한다.
  public boolean isResponderAssigned(UUID incidentId, UUID userId) {
    return assignmentRepository.findByIncidentIdAndUserId(incidentId, userId).isPresent();
  }

  public List<UUID> getAssignedResponderIds(UUID incidentId) {
    return assignmentRepository.findByIncidentIdOrderByAssignedAtAsc(incidentId).stream()
        .map(IncidentAssignment::getUserId)
        .toList();
  }

  @Transactional
  public void recordResponderStatus(UUID incidentId, ResponderStatusRequest request, UUID currentUserId) {
    if (!request.userId().equals(currentUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "본인 명의로만 상태를 보고할 수 있습니다.");
    }
    Incident incident = findIncident(incidentId);
    // 현장 대원의 상태 보고가 처음 들어온 시점 = 실제로 현장 활동이 시작됐다고 볼 수 있는 가장
    // 이른 신호. DISPATCHED("출동중")에서 IN_PROGRESS("진행중")로 이때 전환한다 — 이 전환을 트리거할
    // 다른 이벤트(예: 도착 확인)가 아직 없으므로, 이미 IN_PROGRESS면 조용히 건너뛴다.
    if (incident.getStatus() == IncidentStatus.DISPATCHED) {
      incident.markInProgress();
    }
    ResponderStatusLog log = new ResponderStatusLog(
        incidentId, request.userId(), LocalDateTime.now(), request.biometricData(), request.environmentData(),
        request.riskLevel(), request.connectionStatus());
    responderStatusLogRepository.save(log);
  }

  // CMD-002 현장 모니터링 대시보드 전체 집계.
  public MonitoringResponse getMonitoring(UUID incidentId) {
    Incident incident = findIncident(incidentId);
    List<ResponderStatusResponse> responders = latestStatusPerResponder(incidentId);
    List<AssignmentResponse> assignments = assignmentRepository.findByIncidentIdOrderByAssignedAtAsc(incidentId).stream()
        .map(AssignmentResponse::from)
        .toList();
    List<DroneDispatchResponse> drones = droneDispatchRepository.findByIncidentIdOrderByDispatchedAtDesc(incidentId).stream()
        .map(DroneDispatchResponse::from)
        .toList();
    return new MonitoringResponse(IncidentResponse.from(incident), responders, assignments, drones);
  }

  private List<ResponderStatusResponse> latestStatusPerResponder(UUID incidentId) {
    // 로그 테이블 특성상 user_id별 여러 건이 쌓이므로, 최신순으로 가져와 user_id당 첫 항목(최신)만 남긴다.
    Map<UUID, ResponderStatusLog> latestByUser = new LinkedHashMap<>();
    for (ResponderStatusLog log : responderStatusLogRepository.findByIncidentIdOrderByRecordedAtDesc(incidentId)) {
      latestByUser.putIfAbsent(log.getUserId(), log);
    }
    return latestByUser.values().stream()
        .sorted(Comparator.comparing(log -> RISK_ORDER.getOrDefault(log.getRiskLevel(), 3)))
        .map(ResponderStatusResponse::from)
        .toList();
  }

  // FR-05, QA 최우선 재검증 대상. status=CLOSED 전환과 IncidentClosedEvent 발행을
  // 반드시 같은 트랜잭션 메서드 안에서 함께 수행해, "종료는 됐는데 알림/리포트가 안 생기는" 결함을 막는다.
  @Transactional
  public IncidentResponse close(UUID incidentId) {
    Incident incident = findIncident(incidentId);
    List<UUID> responderIds = getAssignedResponderIds(incidentId);
    incident.close();
    eventPublisher.publishEvent(new IncidentClosedEvent(incidentId, responderIds));
    return IncidentResponse.from(incident);
  }

  // ADM-001 관리자 홈 (FR-09) 통계 카드. device/auth 패키지 접근은 각 서비스 인터페이스를 거친다.
  public DashboardSummaryResponse getDashboardSummary() {
    LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
    long todayDispatchCount = incidentRepository.countByReportedAtAfter(startOfToday);
    long inProgressCount = incidentRepository.countByStatus(com.faind.domain.incident.entity.IncidentStatus.IN_PROGRESS)
        + incidentRepository.countByStatus(com.faind.domain.incident.entity.IncidentStatus.DISPATCHED);
    long onDutyResponderCount = accountService.countActiveResponders();
    long deviceAnomalyCount = deviceService.countAnomalies();
    return new DashboardSummaryResponse(todayDispatchCount, inProgressCount, onDutyResponderCount, deviceAnomalyCount);
  }

  // ADM-001 "최근 출동 목록"
  public Page<IncidentListItemResponse> listRecent(Pageable pageable) {
    return incidentRepository.findAllByOrderByReportedAtDesc(pageable)
        .map(incident -> IncidentListItemResponse.from(incident, assignmentRepository.countByIncidentId(incident.getIncidentId())));
  }

  // CMD-001 지휘관 태블릿 진입 화면. CCTV 출처 출동은 commanderId가 배정되지 않으므로
  // commander별 필터 대신 DISPATCHED/IN_PROGRESS 전체를 노출한다 (컨트롤러에서 COMMANDER/ADMIN 권한으로 제한).
  public List<IncidentResponse> listActive() {
    return incidentRepository.findByStatusInOrderByReportedAtDesc(List.of(IncidentStatus.DISPATCHED, IncidentStatus.IN_PROGRESS))
        .stream()
        .map(IncidentResponse::from)
        .toList();
  }

  // USR-001 대원 앱 진입 화면: 이 대원이 배정된, 아직 종료되지 않은 출동. 여러 건에 배정될 일은
  // 드물지만(동시 출동), 실제로 있을 수 있으므로 배정 최신순으로 전부 반환한다.
  public List<IncidentResponse> listMyActiveIncidents(UUID userId) {
    List<UUID> incidentIds = assignmentRepository.findByUserIdOrderByAssignedAtDesc(userId).stream()
        .map(IncidentAssignment::getIncidentId)
        .distinct()
        .toList();
    return incidentRepository.findAllById(incidentIds).stream()
        .filter(incident -> incident.getStatus() != IncidentStatus.CLOSED)
        .sorted(Comparator.comparing(Incident::getReportedAt).reversed())
        .map(IncidentResponse::from)
        .toList();
  }

  // CMD-002 드론 정찰 카드(FR-26) 등에서 해당 출동에 얽힌 AI 판단 이력을 시간순으로 보여줄 때 사용.
  public List<AiJudgmentSummaryResponse> getAiJudgments(UUID incidentId) {
    findIncident(incidentId); // 존재 검증
    return aiJudgmentLogRepository.findByRelatedIncidentIdOrderByCreatedAtDesc(incidentId).stream()
        .map(AiJudgmentSummaryResponse::from)
        .toList();
  }

  // FR-20 CMD-001/002: 배정 확정 시 1회 계산돼 캐시된 후발대 경로·ETA. 캐시가 없으면(TTL 만료 등)
  // 재계산하지 않고 그대로 "정보 없음"을 알린다 — 실제로 계산되지 않은 값을 임의로 만들어내지 않는다.
  public RouteEstimateResponse getGroundRouteEstimate(UUID incidentId) {
    findIncident(incidentId); // 존재 검증
    return routingApiClient.getCachedGroundRoute(incidentId.toString())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아직 후발대 경로 정보가 없습니다."));
  }

  // statistics 패키지가 FR-13(평균 판정 시간) 계산에 필요한 reported_at만 배치 조회할 때 사용.
  public Map<UUID, LocalDateTime> getReportedAtByIds(List<UUID> incidentIds) {
    return incidentRepository.findAllById(incidentIds).stream()
        .collect(java.util.stream.Collectors.toMap(Incident::getIncidentId, Incident::getReportedAt));
  }

  Incident findIncident(UUID incidentId) {
    return incidentRepository.findById(incidentId).orElseThrow(() -> new BusinessException(ErrorCode.INCIDENT_NOT_FOUND));
  }

  // REQUIRES_NEW: IncidentCreatedListener(AFTER_COMMIT)에서 호출된다. 원래 트랜잭션은 이미 물리적으로
  // 커밋 처리 중이라(afterCommit 콜백은 cleanupAfterCompletion 이전에 실행됨) 기본 REQUIRED로 걸면
  // 그 트랜잭션에 "합류"만 하고 실제 커밋이 안 되는(조용히 유실되는) 스프링 트랜잭션 함정이 있다.
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void savePreAnalysisResult(UUID incidentId, Map<String, Object> buildingInfo, Map<String, Object> hazardInfo,
      Map<String, Object> fireHistoryInfo, String dataSource) {
    preAnalysisResultRepository.save(new PreAnalysisResult(incidentId, buildingInfo, hazardInfo, fireHistoryInfo, dataSource));
  }
}
