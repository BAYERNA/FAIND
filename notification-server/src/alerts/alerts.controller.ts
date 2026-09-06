import { Body, Controller, Get, Param, ParseUUIDPipe, Post, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../auth/current-user.decorator';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { AlertsService } from './alerts.service';
import { CreateEntryInfoDto } from './dto/create-entry-info.dto';
import { CreateRiskWarningDto } from './dto/create-risk-warning.dto';
import { CreateSupplyRequestDto } from './dto/create-supply-request.dto';
import { Alert } from './entities/alert.entity';
import type { AuthenticatedUser } from '../auth/authenticated-user.interface';

// USR-001(입력)/CMD-002(수신): FR-18 진입정보, FR-23 지원요청, FR-06 위험알림
@Controller('incidents/:incidentId/alerts')
@UseGuards(JwtAuthGuard)
export class AlertsController {
  constructor(private readonly alertsService: AlertsService) {}

  @Get()
  list(@Param('incidentId', new ParseUUIDPipe()) incidentId: string): Promise<Alert[]> {
    return this.alertsService.listByIncident(incidentId);
  }

  @Post('entry-info')
  createEntryInfo(
    @Param('incidentId', new ParseUUIDPipe()) incidentId: string,
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateEntryInfoDto,
  ): Promise<Alert> {
    return this.alertsService.createEntryInfo(incidentId, user.userId, dto);
  }

  @Post('supply-request')
  createSupplyRequest(
    @Param('incidentId', new ParseUUIDPipe()) incidentId: string,
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateSupplyRequestDto,
  ): Promise<Alert> {
    return this.alertsService.createSupplyRequest(incidentId, user.userId, dto);
  }

  @Post('risk-warning')
  createRiskWarning(
    @Param('incidentId', new ParseUUIDPipe()) incidentId: string,
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateRiskWarningDto,
  ): Promise<Alert> {
    return this.alertsService.createRiskWarning(incidentId, user.userId, dto);
  }
}
