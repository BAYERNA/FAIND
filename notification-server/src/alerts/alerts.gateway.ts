import { Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import {
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  SubscribeMessage,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Repository } from 'typeorm';
import { verifyFaindJwt } from '../auth/jwt.util';
import { IncidentAssignment } from './entities/incident-assignment.entity';

// CMD-002/USR-001이 구독하는 실시간 채널. incident:{id} 룸 단위로 pub/sub한다
// (코드구조설계서 §4: "WebSocket Gateway — incident:{id} 채널 pub/sub").
@WebSocketGateway({ cors: { origin: '*' } })
export class AlertsGateway implements OnGatewayConnection {
  private readonly logger = new Logger(AlertsGateway.name);

  @WebSocketServer()
  server: Server;

  constructor(
    private readonly configService: ConfigService,
    @InjectRepository(IncidentAssignment) private readonly assignmentRepository: Repository<IncidentAssignment>,
  ) {}

  handleConnection(client: Socket): void {
    const token = this.extractToken(client);
    if (!token) {
      this.logger.warn(`토큰 없이 연결 시도 — 연결을 끊습니다 (socket=${client.id})`);
      client.disconnect(true);
      return;
    }
    try {
      const secret = this.configService.getOrThrow<string>('JWT_SECRET');
      const user = verifyFaindJwt(token, secret);
      client.data.user = user;
      // 개인 알림(예: FR-05 사후보고서 작성 알림)을 위해 접속과 동시에 개인 채널에 자동 합류시킨다.
      void client.join(this.userRoom(user.userId));
    } catch {
      this.logger.warn(`유효하지 않은 토큰 — 연결을 끊습니다 (socket=${client.id})`);
      client.disconnect(true);
    }
  }

  // Java backend의 IncidentController와 동일한 권한 경계: COMMANDER/ADMIN은 배정 무관하게 어떤
  // 출동이든 볼 수 있지만(CCTV 출처는 commanderId가 없어 개인별 필터가 애초에 불가능), RESPONDER는
  // 자신이 배정된 출동만 join할 수 있다 — 그래야 위험경고·진입정보 같은 alert:created가 배정되지
  // 않은 대원에게 새지 않는다. incident_assignments는 backend가 소유한 테이블을 읽기 전용으로 참조.
  @SubscribeMessage('join')
  async handleJoin(@ConnectedSocket() client: Socket, @MessageBody() incidentId: string): Promise<void> {
    const user = client.data.user as { userId: string; role: string } | undefined;
    if (user?.role === 'RESPONDER') {
      const assigned = await this.assignmentRepository.findOne({ where: { incidentId, userId: user.userId } });
      if (!assigned) {
        this.logger.warn(`배정되지 않은 출동 join 시도 — 거부 (user=${user.userId}, incident=${incidentId})`);
        client.emit('join:denied', { incidentId, reason: '배정된 출동만 구독할 수 있습니다.' });
        return;
      }
    }
    void client.join(this.room(incidentId));
  }

  @SubscribeMessage('leave')
  handleLeave(@ConnectedSocket() client: Socket, @MessageBody() incidentId: string): void {
    void client.leave(this.room(incidentId));
  }

  broadcastToIncident(incidentId: string, event: string, payload: unknown): void {
    this.server.to(this.room(incidentId)).emit(event, payload);
  }

  broadcastToUser(userId: string, event: string, payload: unknown): void {
    this.server.to(this.userRoom(userId)).emit(event, payload);
  }

  private extractToken(client: Socket): string | undefined {
    const authToken = client.handshake.auth?.token as string | undefined;
    if (authToken) {
      return authToken;
    }
    const header = client.handshake.headers.authorization;
    return header?.startsWith('Bearer ') ? header.substring('Bearer '.length) : undefined;
  }

  private room(incidentId: string): string {
    return `incident:${incidentId}`;
  }

  private userRoom(userId: string): string {
    return `user:${userId}`;
  }
}
