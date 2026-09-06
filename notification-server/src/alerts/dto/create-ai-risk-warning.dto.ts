import { IsString, MaxLength } from 'class-validator'

// FR-06/Phase 6: CMD-002가 지켜보는 카메라의 위험도가 CRITICAL로 올라갔을 때 프론트가 대신
// 보고하는 경로. 사람이 직접 쓴 위험정보(CreateRiskWarningDto)와 달리 author_id=NULL·
// sourceType='AI'로 남는다 — 실제로 판단한 게 사람이 아니라 AI라는 사실을 흐리지 않기 위함.
// createFromSystem과 로직은 같지만(webhook/risk-warning), 그건 backend↔notification-server
// 내부 서비스 토큰 전용이라 로그인 세션(JWT)에서는 호출할 수 없어 별도 경로로 열었다.
export class CreateAiRiskWarningDto {
  @IsString()
  @MaxLength(2000)
  message: string
}
