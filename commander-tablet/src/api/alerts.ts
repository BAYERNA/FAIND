import { notifyRequest } from './client'
import type { AckFreshnessResponse, AlertResponse } from '../types'

// CMD-002 알림 피드
export function listAlerts(incidentId: string): Promise<AlertResponse[]> {
  return notifyRequest<AlertResponse[]>(`/incidents/${incidentId}/alerts`)
}

// FR-18(진입정보)·FR-23(지원요청)은 responder-app(USR-001, 통신담당·배정 대원)이 입력을 담당한다 —
// CMD-002는 수신·확인현황(FR-22)만 다룬다. 위험정보(FR-06)는 지휘관이 현장을 내려다보는 입장이라
// 여기서도 직접 발신할 수 있게 열어뒀다.

// FR-06 위험정보 알림 (사람이 직접)
export function postRiskWarning(
  incidentId: string,
  payload: { targetUserId?: string; channel: 'VOICE' | 'TEXT'; message: string },
): Promise<AlertResponse> {
  return notifyRequest<AlertResponse>(`/incidents/${incidentId}/alerts/risk-warning`, { method: 'POST', body: payload })
}

// FR-22 "확인했어요"
export function acknowledgeAlert(alertId: string): Promise<void> {
  return notifyRequest<void>(`/alerts/${alertId}/acknowledgements`, { method: 'POST' })
}

// FR-22 확인자 목록 + 신선도
export function getAckFreshness(alertId: string): Promise<AckFreshnessResponse> {
  return notifyRequest<AckFreshnessResponse>(`/alerts/${alertId}/acknowledgements/freshness`)
}
