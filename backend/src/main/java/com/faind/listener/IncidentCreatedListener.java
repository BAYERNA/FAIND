package com.faind.listener;

import com.faind.domain.incident.dto.IncidentResponse;
import com.faind.domain.incident.event.IncidentCreatedEvent;
import com.faind.domain.incident.service.DroneDispatchService;
import com.faind.domain.incident.service.IncidentService;
import com.faind.integration.ai.AiAnalysisPort;
import com.faind.integration.ai.dto.PreAnalysisRequestDto;
import com.faind.integration.ai.dto.PreAnalysisResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 코드구조설계서 §2: "IncidentCreatedEvent → 드론 자동배정(같은 프로세스) + AI서버 호출(REST)".
// AFTER_COMMIT에서 실행해, AI서버·드론배정 실패가 출동 생성 자체(핵심 트랜잭션)를 롤백시키지 않게 한다.
// 리스너는 원래 호출부와 같은 스레드에서 커밋 직후 동기 실행되므로 NFR-03(3초 이내)에도 부합한다.
@Component
public class IncidentCreatedListener {

  private static final Logger log = LoggerFactory.getLogger(IncidentCreatedListener.class);

  private final IncidentService incidentService;
  private final DroneDispatchService droneDispatchService;
  private final AiAnalysisPort aiAnalysisPort;

  public IncidentCreatedListener(
      IncidentService incidentService, DroneDispatchService droneDispatchService, AiAnalysisPort aiAnalysisPort) {
    this.incidentService = incidentService;
    this.droneDispatchService = droneDispatchService;
    this.aiAnalysisPort = aiAnalysisPort;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onIncidentCreated(IncidentCreatedEvent event) {
    runPreAnalysis(event);
    if (event.droneEligible()) {
      runDroneAutoDispatch(event);
    }
  }

  private void runPreAnalysis(IncidentCreatedEvent event) {
    try {
      IncidentResponse incident = incidentService.getIncident(event.incidentId());
      PreAnalysisResultDto result = aiAnalysisPort.requestPreAnalysis(
          new PreAnalysisRequestDto(incident.incidentId(), incident.address(), incident.latitude(), incident.longitude()));
      incidentService.savePreAnalysisResult(
          event.incidentId(), result.buildingInfo(), result.hazardInfo(), result.fireHistoryInfo(),
          "소방청 공공데이터 API");
    } catch (Exception e) {
      log.error("FR-02 사전분석 처리 실패 (incidentId={})", event.incidentId(), e);
    }
  }

  private void runDroneAutoDispatch(IncidentCreatedEvent event) {
    try {
      droneDispatchService.autoDispatch(event.incidentId());
    } catch (Exception e) {
      log.error("FR-25 드론 자동배정 실패 (incidentId={})", event.incidentId(), e);
    }
  }
}
