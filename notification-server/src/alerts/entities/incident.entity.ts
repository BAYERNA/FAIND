import { Column, Entity, PrimaryColumn } from 'typeorm'

// DB설계서 §3.3 incidents. 테이블은 backend(Java) Flyway가 소유·생성한다 — incident-assignment.entity.ts와
// 동일한 타협. EscalationService가 "이미 종료된 출동의 알림은 재알림하지 않는다"를 판단하는
// 용도로만 status를 읽는다(쓰기는 하지 않는다).
@Entity({ name: 'incidents' })
export class Incident {
  @PrimaryColumn({ name: 'incident_id', type: 'uuid' })
  incidentId: string

  @Column({ type: 'varchar', length: 15 })
  status: string
}
