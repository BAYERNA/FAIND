import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AlertsModule } from '../alerts/alerts.module';
import { Alert } from '../alerts/entities/alert.entity';
import { AckController } from './ack.controller';
import { AckService } from './ack.service';
import { AlertAcknowledgement } from './entities/alert-acknowledgement.entity';

@Module({
  imports: [TypeOrmModule.forFeature([AlertAcknowledgement, Alert]), AlertsModule],
  controllers: [AckController],
  providers: [AckService],
})
export class AckModule {}
