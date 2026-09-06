import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { AlertsGateway } from '../../alerts/alerts.gateway';
import { AlertsService } from '../../alerts/alerts.service';
import { InternalWebhookGuard } from '../../auth/internal-webhook.guard';
import { ReportDraftCreatedDto } from './dto/report-draft-created.dto';
import { RiskWarningDto } from './dto/risk-warning.dto';

const REPORT_DRAFT_CREATED_EVENT = 'report:draft-created';

// backend(Java) NotificationHttpAdapter가 호출하는 인바운드 웹훅.
// 코드구조설계서 §4 "Java 모놀리식이 호출하는 인바운드 웹훅 수신".
@Controller('webhook')
@UseGuards(InternalWebhookGuard)
export class WebhookController {
  constructor(
    private readonly alertsService: AlertsService,
    private readonly gateway: AlertsGateway,
  ) {}

  // FR-05: 출동 종료 확정 시 대원별 "사후보고서 작성" 알림. alerts 테이블에는 대응하는 alert_type이
  // 없으므로(§3.7) DB에 남기지 않고, 대상 대원 개인 채널로 실시간 푸시만 한다.
  @Post('report-draft-created')
  notifyReportDraftCreated(@Body() dto: ReportDraftCreatedDto): { received: true } {
    dto.responderIds.forEach((responderId, index) => {
      this.gateway.broadcastToUser(responderId, REPORT_DRAFT_CREATED_EVENT, {
        incidentId: dto.incidentId,
        reportId: dto.reportIds[index],
      });
    });
    return { received: true };
  }

  // FR-06: AI/시스템이 생성하는 위험정보 알림 (author_id=NULL로 alerts에 영속화 + 실시간 전파).
  @Post('risk-warning')
  async broadcastRiskWarning(@Body() dto: RiskWarningDto): Promise<{ received: true }> {
    await this.alertsService.createFromSystem({
      incidentId: dto.incidentId,
      alertType: 'RISK_WARNING',
      message: dto.message,
      sourceType: 'AI',
    });
    return { received: true };
  }
}
