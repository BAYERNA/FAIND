import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AlertsGateway } from './alerts.gateway';
import { CreateEntryInfoDto } from './dto/create-entry-info.dto';
import { CreateRiskWarningDto } from './dto/create-risk-warning.dto';
import { CreateSupplyRequestDto } from './dto/create-supply-request.dto';
import { Alert } from './entities/alert.entity';

const ALERT_CREATED_EVENT = 'alert:created';

@Injectable()
export class AlertsService {
  constructor(
    @InjectRepository(Alert) private readonly alertRepository: Repository<Alert>,
    private readonly gateway: AlertsGateway,
  ) {}

  async listByIncident(incidentId: string): Promise<Alert[]> {
    return this.alertRepository.find({ where: { incidentId }, order: { sentAt: 'DESC' } });
  }

  // FR-18: 선발대 진입정보 공유. is_comms_lead 권한 확인은 controller가 아니라 이 흐름을 호출하는
  // 클라이언트(USR-001) 쪽 화면 노출 조건으로 처리한다 — backend의 incident_assignments가 진실
  // 소스이므로, 엄격한 서버측 재검증이 필요해지면 backend에 조회 API를 추가해 여기서 호출한다.
  async createEntryInfo(incidentId: string, authorId: string, dto: CreateEntryInfoDto): Promise<Alert> {
    const alert = this.alertRepository.create({
      incidentId,
      authorId,
      alertType: 'ENTRY_INFO',
      infoCategory: dto.infoCategory,
      locationLabel: dto.locationLabel,
      statusTag: dto.statusTag,
      message: dto.message ?? null,
      sourceType: 'HUMAN',
    });
    return this.saveAndBroadcast(alert);
  }

  // FR-23: 장비·인력 지원 요청
  async createSupplyRequest(incidentId: string, authorId: string, dto: CreateSupplyRequestDto): Promise<Alert> {
    const alert = this.alertRepository.create({
      incidentId,
      authorId,
      alertType: 'SUPPLY_REQUEST',
      requestedItems: dto.requestedItems,
      sourceType: 'HUMAN',
    });
    return this.saveAndBroadcast(alert);
  }

  // FR-06: 사람이 직접 보내는 위험정보 알림
  async createRiskWarning(incidentId: string, authorId: string, dto: CreateRiskWarningDto): Promise<Alert> {
    const alert = this.alertRepository.create({
      incidentId,
      authorId,
      targetUserId: dto.targetUserId ?? null,
      alertType: 'RISK_WARNING',
      channel: dto.channel,
      message: dto.message,
      sourceType: 'HUMAN',
    });
    return this.saveAndBroadcast(alert);
  }

  // common/webhook에서 호출 — backend(AI/시스템)가 author_id 없이 보내는 알림.
  async createFromSystem(params: {
    incidentId: string;
    alertType: Alert['alertType'];
    message: string;
    sourceType: Alert['sourceType'];
  }): Promise<Alert> {
    const alert = this.alertRepository.create({
      incidentId: params.incidentId,
      authorId: null,
      alertType: params.alertType,
      message: params.message,
      sourceType: params.sourceType,
      channel: 'TEXT',
    });
    return this.saveAndBroadcast(alert);
  }

  private async saveAndBroadcast(alert: Alert): Promise<Alert> {
    const saved = await this.alertRepository.save(alert);
    this.gateway.broadcastToIncident(saved.incidentId, ALERT_CREATED_EVENT, saved);
    return saved;
  }
}
