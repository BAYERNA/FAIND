import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

// DB설계서 §3.8 alert_acknowledgements. FR-22 — 같은 alert를 여러 명이, 또는 같은 사람이 여러 번
// (재확인) 기록할 수 있어 UNIQUE 제약이 없다. "마지막 확인 시각"은 조회 시점에 MAX(acknowledged_at)로 계산한다.
@Entity({ name: 'alert_acknowledgements' })
export class AlertAcknowledgement {
  @PrimaryGeneratedColumn('uuid', { name: 'ack_id' })
  ackId: string;

  @Column({ name: 'alert_id', type: 'uuid' })
  alertId: string;

  @Column({ name: 'user_id', type: 'uuid' })
  userId: string;

  @CreateDateColumn({ name: 'acknowledged_at', type: 'timestamp' })
  acknowledgedAt: Date;
}
