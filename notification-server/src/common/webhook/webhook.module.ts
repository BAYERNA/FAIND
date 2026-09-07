import { Module } from '@nestjs/common';
import { AlertsModule } from '../../alerts/alerts.module';
import { WebhookController } from './webhook.controller';

@Module({
  imports: [AlertsModule],
  controllers: [WebhookController],
})
export class WebhookModule {}
