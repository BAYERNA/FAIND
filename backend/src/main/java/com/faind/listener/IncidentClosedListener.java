package com.faind.listener;

import com.faind.domain.incident.event.IncidentClosedEvent;
import com.faind.domain.incident.service.DroneDispatchService;
import com.faind.domain.report.service.ReportService;
import com.faind.integration.notification.NotificationPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// FR-05 QA 최우선 재검증 대상의 재설계 지점 (MSA아키텍처설계서 §4.2 "IncidentClosed 이벤트 기반 재설계").
// "종료 확정 시 대원별 사후보고서 작성 알림 + reports row 생성이 실제로 일어나는가"가 이 클래스의 존재 이유.
@Component
public class IncidentClosedListener {

  private static final Logger log = LoggerFactory.getLogger(IncidentClosedListener.class);

  private final ReportService reportService;
  private final NotificationPort notificationPort;
  private final DroneDispatchService droneDispatchService;

  public IncidentClosedListener(
      ReportService reportService, NotificationPort notificationPort, DroneDispatchService droneDispatchService) {
    this.reportService = reportService;
    this.notificationPort = notificationPort;
    this.droneDispatchService = droneDispatchService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onIncidentClosed(IncidentClosedEvent event) {
    try {
      droneDispatchService.releaseDronesForIncident(event.incidentId());
    } catch (Exception e) {
      log.error("FR-25 드론 배차 해제 실패 — 드론이 DISPATCHED 상태로 남아있을 수 있습니다 (incidentId={})", event.incidentId(), e);
    }
    if (event.assignedResponderIds().isEmpty()) {
      log.info("배정된 대원이 없어 사후보고서 초안을 생성하지 않습니다 (incidentId={})", event.incidentId());
      return;
    }
    try {
      List<UUID> reportIds = reportService.createDraftsForIncident(event.incidentId(), event.assignedResponderIds());
      notificationPort.notifyReportDraftCreated(event.incidentId(), event.assignedResponderIds(), reportIds);
    } catch (Exception e) {
      log.error(
          "FR-05 종료 후처리 실패 — 사후보고서/알림이 정상적으로 생성되지 않았을 수 있습니다 (incidentId={})",
          event.incidentId(), e);
    }
  }
}
