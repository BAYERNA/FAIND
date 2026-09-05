package com.faind.domain.incident.service;

import com.faind.domain.device.dto.NearestDroneResponse;
import com.faind.domain.device.service.DeviceService;
import com.faind.domain.incident.dto.DroneDispatchResponse;
import com.faind.domain.incident.dto.RouteEstimateResponse;
import com.faind.domain.incident.entity.AiJudgmentLog;
import com.faind.domain.incident.entity.DroneDispatch;
import com.faind.domain.incident.entity.Incident;
import com.faind.domain.incident.repository.AiJudgmentLogRepository;
import com.faind.domain.incident.repository.DroneDispatchRepository;
import com.faind.domain.incident.repository.IncidentRepository;
import com.faind.global.error.BusinessException;
import com.faind.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// FR-25/26: incident가 DISPATCHED로 확정되는 즉시 가장 가까운 드론을 자동 배정하고,
// 드론이 현장에서 보내는 정찰 결과를 ai_judgment_logs(DRONE_RECON)로 적재한다.
@Service
public class DroneDispatchService {

  private static final Logger log = LoggerFactory.getLogger(DroneDispatchService.class);

  private final IncidentRepository incidentRepository;
  private final DroneDispatchRepository droneDispatchRepository;
  private final AiJudgmentLogRepository aiJudgmentLogRepository;
  private final DeviceService deviceService;
  private final RoutingApiClient routingApiClient;

  public DroneDispatchService(
      IncidentRepository incidentRepository,
      DroneDispatchRepository droneDispatchRepository,
      AiJudgmentLogRepository aiJudgmentLogRepository,
      DeviceService deviceService,
      RoutingApiClient routingApiClient) {
    this.incidentRepository = incidentRepository;
    this.droneDispatchRepository = droneDispatchRepository;
    this.aiJudgmentLogRepository = aiJudgmentLogRepository;
    this.deviceService = deviceService;
    this.routingApiClient = routingApiClient;
  }

  // REQUIRES_NEW: IncidentCreatedListener(AFTER_COMMIT)에서 호출 — 이유는 IncidentService.savePreAnalysisResult 주석 참조.
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public Optional<DroneDispatchResponse> autoDispatch(UUID incidentId) {
    Incident incident = incidentRepository.findById(incidentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.INCIDENT_NOT_FOUND));

    Optional<NearestDroneResponse> nearestDrone =
        deviceService.findNearestAvailableDrone(incident.getLatitude(), incident.getLongitude());
    if (nearestDrone.isEmpty()) {
      log.info("배정 가능한 드론이 없어 FR-25 자동배정을 건너뜁니다 (incidentId={})", incidentId);
      return Optional.empty();
    }

    NearestDroneResponse drone = nearestDrone.get();
    DroneDispatch dispatch = new DroneDispatch(incidentId, drone.droneId());
    droneDispatchRepository.save(dispatch);

    RouteEstimateResponse route = routingApiClient.estimateDroneRoute(
        incidentId.toString(), drone.latitude(), drone.longitude(), incident.getLatitude(), incident.getLongitude(),
        drone.stationLabel());
    log.info(
        "드론 자동배정 완료: incidentId={}, droneId={}, distanceKm={}, etaSeconds={}",
        incidentId, drone.droneId(), route.distanceKm(), route.etaSeconds());

    return Optional.of(DroneDispatchResponse.from(dispatch));
  }

  // FR-26: 드론 도착 후 정찰 영상을 AI가 분석한 결과를 CMD-002에 표시하기 위해 기록.
  @Transactional
  public void recordReconResult(UUID dispatchId, BigDecimal confidenceScore, String summary, String videoRef) {
    DroneDispatch dispatch = droneDispatchRepository.findById(dispatchId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "드론 출동 이력을 찾을 수 없습니다."));
    dispatch.markOnSite(videoRef);
    aiJudgmentLogRepository.save(
        new AiJudgmentLog("DRONE_RECON", dispatch.getIncidentId(), null, dispatch.getDroneId(), confidenceScore, summary));
  }

  // FR-27: ADM-009 골든타임 단축효과 — "드론 활용 시 현장 최초 도착" 평균값의 소스.
  public OptionalDouble averageDroneArrivalSeconds() {
    List<DroneDispatch> arrived = droneDispatchRepository.findAll().stream()
        .filter(d -> d.getArrivedAt() != null)
        .toList();
    if (arrived.isEmpty()) {
      return OptionalDouble.empty();
    }
    return arrived.stream()
        .mapToLong(d -> {
          Incident incident = incidentRepository.findById(d.getIncidentId()).orElse(null);
          if (incident == null) {
            return -1;
          }
          return Duration.between(incident.getReportedAt(), d.getArrivedAt()).getSeconds();
        })
        .filter(seconds -> seconds >= 0)
        .average();
  }
}
