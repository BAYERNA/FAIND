import { notifyRequest } from './client'
import type { AckFreshnessResponse, AlertResponse } from '../types'

// CMD-002 알림 피드
export function listAlerts(incidentId: string): Promise<AlertResponse[]> {
  return notifyRequest<AlertResponse[]>(`/incidents/${incidentId}/alerts`)
}

// FR-18 진입정보 공유
export function postEntryInfo(
  incidentId: string,
  payload: { infoCategory: 'ENTRY' | 'HAZARD'; locationLabel: string; statusTag: 'PASSABLE' | 'BLOCKED' | 'DANGER'; message?: string },
): Promise<AlertResponse> {
  return notifyRequest<AlertResponse>(`/incidents/${incidentId}/alerts/entry-info`, { method: 'POST', body: payload })
}

// FR-23 장비·인력 지원요청
export function postSupplyRequest(
  incidentId: string,
  requestedItems: { item: string; qty: number }[],
): Promise<AlertResponse> {
  return notifyRequest<AlertResponse>(`/incidents/${incidentId}/alerts/supply-request`, {
    method: 'POST',
    body: { requestedItems },
  })
}

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
