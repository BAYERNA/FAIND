import { apiRequest } from './client'
import type { IncidentResponse, Page } from '../types'

export interface DashboardSummary {
  todayDispatchCount: number
  inProgressCount: number
  onDutyResponderCount: number
  deviceAnomalyCount: number
}

export interface IncidentListItem {
  incidentId: string
  incidentNumber: string
  incidentType: string
  status: IncidentResponse['status']
  reportedAt: string
  assignedResponderCount: number
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return apiRequest<DashboardSummary>('/api/v1/incidents/dashboard-summary')
}

export function listRecentIncidents(page = 0, size = 5): Promise<Page<IncidentListItem>> {
  return apiRequest<Page<IncidentListItem>>('/api/v1/incidents', { query: { page, size } })
}
