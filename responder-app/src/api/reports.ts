import { apiRequest } from './client'
import type { ReportAnalysisResponse, ReportResponse } from '../types'

export interface ReportSaveRequest {
  content: string
  videoRef?: string
}

// USR-002 내 보고서 목록
export function getMyReports(): Promise<ReportResponse[]> {
  return apiRequest<ReportResponse[]>('/api/v1/reports/me')
}

export function getReport(reportId: string): Promise<ReportResponse> {
  return apiRequest<ReportResponse>(`/api/v1/reports/${reportId}`)
}

// FR-07 "임시 저장"
export function saveDraft(reportId: string, request: ReportSaveRequest): Promise<ReportResponse> {
  return apiRequest<ReportResponse>(`/api/v1/reports/${reportId}/draft`, { method: 'PUT', body: request })
}

// FR-07 "제출" — 제출 즉시 backend가 FR-08 SOP 대조를 비동기로 요청한다.
export function submitReport(reportId: string, request: ReportSaveRequest): Promise<ReportResponse> {
  return apiRequest<ReportResponse>(`/api/v1/reports/${reportId}/submit`, { method: 'PATCH', body: request })
}

// USR-003 FR-08 SOP 교차 검증 결과
export function getReportAnalysis(reportId: string): Promise<ReportAnalysisResponse> {
  return apiRequest<ReportAnalysisResponse>(`/api/v1/reports/${reportId}/analysis`)
}
