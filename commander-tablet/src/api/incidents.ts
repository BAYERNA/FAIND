import { apiRequest } from './client'
import type {
  AiJudgmentSummaryResponse,
  AssignmentResponse,
  IncidentResponse,
  MonitoringResponse,
  PreAnalysisResponse,
} from '../types'

// CMD-001 진입 화면: DISPATCHED/IN_PROGRESS 전체 (역할=COMMANDER/ADMIN 전용, backend에서 강제)
export function getActiveIncidents(): Promise<IncidentResponse[]> {
  return apiRequest<IncidentResponse[]>('/api/v1/incidents/active')
}

export function getIncident(incidentId: string): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/api/v1/incidents/${incidentId}`)
}

// CMD-001: 사전분석 결과 + NFR-03 검증용 소요시간
export function getPreAnalysis(incidentId: string): Promise<PreAnalysisResponse> {
  return apiRequest<PreAnalysisResponse>(`/api/v1/incidents/${incidentId}/pre-analysis`)
}

// CMD-002 현장 모니터링 대시보드 전체 데이터
export function getMonitoring(incidentId: string): Promise<MonitoringResponse> {
  return apiRequest<MonitoringResponse>(`/api/v1/incidents/${incidentId}/monitoring`)
}

// CMD-002 드론 정찰 카드(FR-26) AI 판단 이력
export function getAiJudgments(incidentId: string): Promise<AiJudgmentSummaryResponse[]> {
  return apiRequest<AiJudgmentSummaryResponse[]>(`/api/v1/incidents/${incidentId}/ai-judgments`)
}

// FR-19: 지휘관이 통신 담당 재지정
export function reassignCommsLead(incidentId: string, userId: string): Promise<AssignmentResponse> {
  return apiRequest<AssignmentResponse>(`/api/v1/incidents/${incidentId}/assignments/${userId}/comms-lead`, {
    method: 'PATCH',
  })
}

// FR-05 CMD-006 "종료 확정" — QA 최우선 재검증 대상
export function closeIncident(incidentId: string): Promise<IncidentResponse> {
  return apiRequest<IncidentResponse>(`/api/v1/incidents/${incidentId}/close`, { method: 'PATCH' })
}
