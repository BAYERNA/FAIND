import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request } from 'express';

// backend(Java)의 NotificationHttpAdapter가 호출하는 인바운드 웹훅 전용 가드.
// 로그인 사용자 JWT가 아니라 두 서비스만 공유하는 내부 토큰으로 검증한다.
// INTERNAL_WEBHOOK_TOKEN을 설정하지 않으면(로컬 데모 기본값) 검증을 건너뛴다 —
// backend 쪽 AiAnalysisPort/PublicDataApiAdapter가 서비스 키 없을 때 폴백하는 것과 같은 관용.
@Injectable()
export class InternalWebhookGuard implements CanActivate {
  constructor(private readonly configService: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const expectedToken = this.configService.get<string>('INTERNAL_WEBHOOK_TOKEN');
    if (!expectedToken) {
      return true;
    }
    const request = context.switchToHttp().getRequest<Request>();
    const header = request.headers.authorization;
    const token = header?.startsWith('Bearer ') ? header.substring('Bearer '.length) : undefined;
    if (token !== expectedToken) {
      throw new UnauthorizedException('내부 웹훅 토큰이 유효하지 않습니다.');
    }
    return true;
  }
}
