import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AlertsController } from './alerts.controller';
import { AlertsGateway } from './alerts.gateway';
import { AlertsService } from './alerts.service';
import { Alert } from './entities/alert.entity';
import { IncidentAssignment } from './entities/incident-assignment.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Alert, IncidentAssignment])],
  controllers: [AlertsController],
  providers: [AlertsService, AlertsGateway],
  exports: [AlertsService, AlertsGateway],
})
export class AlertsModule {}
