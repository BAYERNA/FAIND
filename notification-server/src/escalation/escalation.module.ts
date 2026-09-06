import { Module } from '@nestjs/common'
import { TypeOrmModule } from '@nestjs/typeorm'
import { AlertsModule } from '../alerts/alerts.module'
import { Alert } from '../alerts/entities/alert.entity'
import { Incident } from '../alerts/entities/incident.entity'
import { AlertAcknowledgement } from '../acknowledgements/entities/alert-acknowledgement.entity'
import { EscalationService } from './escalation.service'

@Module({
  imports: [TypeOrmModule.forFeature([Alert, AlertAcknowledgement, Incident]), AlertsModule],
  providers: [EscalationService],
})
export class EscalationModule {}
