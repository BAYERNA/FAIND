// backend(Java)의 각 도메인 응답 DTO와 1:1로 맞춘 타입. 필드명은 Jackson 기본 camelCase 직렬화를 그대로 따른다.

export type Role = 'ADMIN' | 'COMMANDER' | 'RESPONDER'

export interface LoginResponse {
  accessToken: string
  userId: string
  name: string
  role: Role
  initialPassword: boolean
}

export interface AccountResponse {
  userId: string
  name: string
  role: Role
  badgeNumber: string
  team: string | null
  phone: string | null
  status: 'ACTIVE' | 'INACTIVE'
  updatedAt: string | null
}

export interface AccountCreatedResponse {
  account: AccountResponse
  issuedTemporaryPassword: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export type DeviceType = 'BODYCAM' | 'SMARTPHONE' | 'DIGITAL_MASK' | 'SENSOR' | 'CCTV' | 'DRONE'

export interface DeviceResponse {
  deviceId: string
  deviceType: DeviceType
  serialNo: string
  connectionType: string | null
  currentUserId: string | null
  mappedUserName: string | null
  mappedUserTeam: string | null
  latitude: number | null
  longitude: number | null
  status: 'NORMAL' | 'WARNING' | 'DISCONNECTED'
  batteryLevel: number | null
  registeredAt: string
}

export type IncidentStatus = 'AI_SUSPECTED' | 'DISPATCHED' | 'IN_PROGRESS' | 'CLOSED'
export type IncidentSource = 'MANUAL_REPORT' | 'CCTV_AUTO_DETECTION'

export interface IncidentResponse {
  incidentId: string
  incidentNumber: string
  incidentType: string
  address: string | null
  latitude: number | null
  longitude: number | null
  reportedAt: string
  closedAt: string | null
  status: IncidentStatus
  source: IncidentSource
  confirmedBy: string | null
  commanderId: string | null
}

export interface AiSuspectedQueueItem {
  incidentId: string
  judgmentId: string | null
  sourceDeviceId: string | null
  address: string | null
  detectedAt: string
  confidenceScore: number | null
  status: IncidentStatus
}

export interface LabeledCount {
  label: string
  count: number
}

export interface GoldenTimeStats {
  existingAverageSeconds: number
  droneAverageArrivalSeconds: number | null
  reductionSeconds: number | null
}

export interface RecentJudgmentItem {
  createdAt: string
  judgmentType: string
  relatedIncidentId: string | null
  confidenceScore: number | null
}

export interface StatisticsSummary {
  totalAiJudgments: number
  sopMatchAccuracyPercent: number
  reviewCompletionRatePercent: number
  averageJudgmentSeconds: number
  goldenTime: GoldenTimeStats
  monthlyJudgmentCounts: LabeledCount[]
  judgmentTypeFrequency: LabeledCount[]
  recentJudgments: RecentJudgmentItem[]
}

export interface ApiErrorBody {
  code: string
  message: string
  timestamp: string
  fieldErrors: { field: string; reason: string }[]
}
