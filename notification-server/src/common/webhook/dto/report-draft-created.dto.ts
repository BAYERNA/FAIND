import { ArrayNotEmpty, IsArray, IsUUID } from 'class-validator';

// backend NotificationPort.notifyReportDraftCreated / ReportDraftCreatedWebhookDto와 1:1.
export class ReportDraftCreatedDto {
  @IsUUID()
  incidentId: string;

  @IsArray()
  @ArrayNotEmpty()
  @IsUUID('4', { each: true })
  responderIds: string[];

  @IsArray()
  @ArrayNotEmpty()
  @IsUUID('4', { each: true })
  reportIds: string[];
}
