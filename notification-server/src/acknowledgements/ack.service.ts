import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AlertsGateway } from '../alerts/alerts.gateway';
import { Alert } from '../alerts/entities/alert.entity';
import { FreshnessDto } from './dto/freshness.dto';
import { AlertAcknowledgement } from './entities/alert-acknowledgement.entity';

const ALERT_ACKNOWLEDGED_EVENT = 'alert:acknowledged';
const STALE_MINUTES_THRESHOLD = 10;

// FR-22: 인수인계 확인 흐름 (등록→수신→확인). DB설계서 §3.8 — 같은 alert를 여러 명이,
// 또는 재확인으로 여러 번 기록할 수 있다.
@Injectable()
export class AckService {
  constructor(
    @InjectRepository(AlertAcknowledgement) private readonly ackRepository: Repository<AlertAcknowledgement>,
    @InjectRepository(Alert) private readonly alertRepository: Repository<Alert>,
    private readonly gateway: AlertsGateway,
  ) {}

  // USR-001 "확인했어요" 버튼
  async acknowledge(alertId: string, userId: string): Promise<AlertAcknowledgement> {
    const alert = await this.alertRepository.findOne({ where: { alertId } });
    if (!alert) {
      throw new NotFoundException('해당 알림을 찾을 수 없습니다.');
    }
    const ack = await this.ackRepository.save(this.ackRepository.create({ alertId, userId }));
    this.gateway.broadcastToIncident(alert.incidentId, ALERT_ACKNOWLEDGED_EVENT, {
      alertId,
      userId,
      acknowledgedAt: ack.acknowledgedAt,
    });
    return ack;
  }

  // CMD-002 annot#3: 확인자 목록 + "N분 전 확인, 갱신 필요" 신선도 표시.
  async getFreshness(alertId: string): Promise<FreshnessDto> {
    const acks = await this.ackRepository.find({ where: { alertId }, order: { acknowledgedAt: 'DESC' } });
    const lastAcknowledgedAt = acks[0]?.acknowledgedAt ?? null;
    const isStale = lastAcknowledgedAt
      ? Date.now() - lastAcknowledgedAt.getTime() > STALE_MINUTES_THRESHOLD * 60_000
      : true;
    return {
      alertId,
      acknowledgedUserIds: [...new Set(acks.map((a) => a.userId))],
      lastAcknowledgedAt: lastAcknowledgedAt ? lastAcknowledgedAt.toISOString() : null,
      staleMinutesThreshold: STALE_MINUTES_THRESHOLD,
      isStale,
    };
  }
}
