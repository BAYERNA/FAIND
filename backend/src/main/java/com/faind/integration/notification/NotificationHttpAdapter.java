package com.faind.integration.notification;

import com.faind.integration.notification.dto.ReportDraftCreatedWebhookDto;
import com.faind.integration.notification.dto.RiskWarningWebhookDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// notification-server(Node/NestJS)의 인바운드 웹훅(common/webhook)을 호출하는 구현체.
// FR-05 QA 재검증 대상: 이 호출이 IncidentClosedListener(AFTER_COMMIT)에서 실제로 나가는지가
// "종료 확정 시 알림 미발송" 결함의 재발 방지 포인트다.
@Component
public class NotificationHttpAdapter implements NotificationPort {

  private static final Logger log = LoggerFactory.getLogger(NotificationHttpAdapter.class);

  private final RestClient restClient;

  public NotificationHttpAdapter(RestClient.Builder restClientBuilder, Environment env) {
    String baseUrl = env.getProperty("faind.integration.notification-server.base-url", "http://localhost:3001");
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
  }

  @Override
  @CircuitBreaker(name = "notificationServer", fallbackMethod = "notifyReportDraftCreatedFallback")
  public void notifyReportDraftCreated(UUID incidentId, List<UUID> responderIds, List<UUID> reportIds) {
    restClient.post()
        .uri("/webhook/report-draft-created")
        .body(new ReportDraftCreatedWebhookDto(incidentId, responderIds, reportIds))
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  @CircuitBreaker(name = "notificationServer", fallbackMethod = "broadcastRiskWarningFallback")
  public void broadcastRiskWarning(UUID incidentId, String message) {
    restClient.post()
        .uri("/webhook/risk-warning")
        .body(new RiskWarningWebhookDto(incidentId, message))
        .retrieve()
        .toBodilessEntity();
  }

  @SuppressWarnings("unused")
  private void notifyReportDraftCreatedFallback(
      UUID incidentId, List<UUID> responderIds, List<UUID> reportIds, Throwable throwable) {
    log.error(
        "notification-server 호출 실패 — 대원 {}명에게 보고서 작성 알림이 발송되지 않았습니다 (incidentId={})",
        responderIds.size(), incidentId, throwable);
  }

  @SuppressWarnings("unused")
  private void broadcastRiskWarningFallback(UUID incidentId, String message, Throwable throwable) {
    log.error("notification-server 호출 실패 — 위험정보 알림이 발송되지 않았습니다 (incidentId={})", incidentId, throwable);
  }
}
