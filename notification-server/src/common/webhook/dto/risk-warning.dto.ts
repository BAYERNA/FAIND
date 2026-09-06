import { IsString, IsUUID, MaxLength } from 'class-validator';

// backend NotificationPort.broadcastRiskWarning / RiskWarningWebhookDto와 1:1.
export class RiskWarningDto {
  @IsUUID()
  incidentId: string;

  @IsString()
  @MaxLength(2000)
  message: string;
}
