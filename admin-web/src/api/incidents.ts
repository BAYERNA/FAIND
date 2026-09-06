import { apiRequest } from './client'
import type { AiSuspectedQueueItem, IncidentResponse } from '../types'

// ADM-001 "AI 의심감지 대기열" (FR-24)
export function listAiSuspectedQueue(): Promise<AiSuspectedQueueItem[]> {
  return apiRequest<AiSuspectedQueueItem[]>('/api/v1/incidents/dispatch/ai-suspected')
}

// NFR-08: role=ADMIN만 호출 가능 — 서버가 최종 검증하지만, 화면에서도 관리자만 노출한다.
export function confirmIncident(incidentId: string): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/api/v1/incidents/dispatch/${incidentId}/confirm`, { method: 'PATCH' })
}

export function rejectIncident(incidentId: string): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/api/v1/incidents/dispatch/${incidentId}/reject`, { method: 'PATCH' })
}
