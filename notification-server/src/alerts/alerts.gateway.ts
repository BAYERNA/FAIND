import { Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  SubscribeMessage,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { verifyFaindJwt } from '../auth/jwt.util';

// CMD-002/USR-001이 구독하는 실시간 채널. incident:{id} 룸 단위로 pub/sub한다
// (코드구조설계서 §4: "WebSocket Gateway — incident:{id} 채널 pub/sub").
@WebSocketGateway({ cors: { origin: '*' } })
export class AlertsGateway implements OnGatewayConnection {
  private readonly logger = new Logger(AlertsGateway.name);

  @WebSocketServer()
  server: Server;

  constructor(private readonly configService: ConfigService) {}

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

  @SubscribeMessage('join')
  handleJoin(@ConnectedSocket() client: Socket, @MessageBody() incidentId: string): void {
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
