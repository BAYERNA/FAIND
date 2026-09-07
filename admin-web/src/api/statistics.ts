import { apiRequest } from './client'
import type { StatisticsSummary } from '../types'

export function getStatisticsSummary(): Promise<StatisticsSummary> {
  return apiRequest<StatisticsSummary>('/api/v1/statistics/summary')
}
