package com.faind.integration.notification;

import java.util.List;
import java.util.UUID;

// Node.js 알림서버(notification-server)로 나가는 아웃바운드 호출 캡슐화.
// incident 패키지는 이 인터페이스만 알고, 실제 웹훅 호출은 NotificationHttpAdapter가 담당한다.
public interface NotificationPort {

  // FR-05: 출동 종료 확정 시 대원별 "사후보고서 작성" 알림 (IncidentClosedListener에서 호출).
  void notifyReportDraftCreated(UUID incidentId, List<UUID> responderIds, List<UUID> reportIds);

  // FR-06: 현장 위험정보 알림 (AI/시스템이 생성하는 케이스, author_id=NULL).
  void broadcastRiskWarning(UUID incidentId, String message);
}
