// backend(Java)/notification-server 각 응답 DTO와 1:1로 맞춘 타입. Jackson 기본 camelCase를 그대로 따른다.

export type Role = 'ADMIN' | 'COMMANDER' | 'RESPONDER'

export interface LoginResponse {
  accessToken: string
  userId: string
  name: string
  role: Role
  initialPassword: boolean
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

export type RiskLevel = 'NORMAL' | 'CAUTION' | 'DANGER'
export type ConnectionStatus = 'CONNECTED' | 'MESH' | 'SMS' | 'DISCONNECTED'

export interface AssignmentResponse {
  assignmentId: string
  userId: string
  roleInIncident: string | null
  firstWave: boolean
  commsLead: boolean
  assignedAt: string
}

export interface ResponderStatusResponse {
  userId: string
  biometricData: Record<string, unknown> | null
  environmentData: Record<string, unknown> | null
  riskLevel: RiskLevel | string | null
  connectionStatus: ConnectionStatus | string | null
  recordedAt: string
}

export interface DroneDispatchResponse {
  dispatchId: string
  droneId: string
  dispatchedAt: string
  arrivedAt: string | null
  status: string
  videoRef: string | null
}

// USR-001이 GET /{incidentId}/monitoring을 그대로 재사용해 내 배정정보(통신담당 여부 등)를 찾는다.
export interface MonitoringResponse {
  incident: IncidentResponse
  responders: ResponderStatusResponse[]
  assignments: AssignmentResponse[]
  droneDispatches: DroneDispatchResponse[]
}

// notification-server 계약 (alerts.entity.ts / ack.service.ts 실제 응답 형태를 그대로 따른다)
export type AlertType = 'RISK_WARNING' | 'EVACUATION' | 'STATUS_CHANGE' | 'ENTRY_INFO' | 'SUPPLY_REQUEST'
export type InfoCategory = 'ENTRY' | 'HAZARD'
export type StatusTag = 'PASSABLE' | 'BLOCKED' | 'DANGER'
export type AlertSourceType = 'HUMAN' | 'SENSOR' | 'AI' | 'EXTERNAL'
export type AlertChannel = 'VOICE' | 'TEXT'

export interface RequestedItem {
  item: string
  qty: number
}

export interface AlertResponse {
  alertId: string
  incidentId: string
  targetUserId: string | null
  authorId: string | null
  alertType: AlertType | null
  infoCategory: InfoCategory | null
  locationLabel: string | null
  statusTag: StatusTag | null
  requestedItems: RequestedItem[] | null
  sourceType: AlertSourceType
  channel: AlertChannel | null
  message: string | null
  sentAt: string
}

// CMD-002 annot#3와 동일한 확인자 목록 + 신선도 표시를 USR-001에서도 재사용한다.
export interface AckFreshnessResponse {
  alertId: string
  acknowledgedUserIds: string[]
  lastAcknowledgedAt: string | null
  staleMinutesThreshold: number
  isStale: boolean
}

// USR-002/003 — backend report 패키지 DTO
export type ReportStatus = 'DRAFT' | 'SUBMITTED'

export interface ReportResponse {
  reportId: string
  incidentId: string
  authorId: string
  content: string | null
  videoRef: string | null
  status: ReportStatus
  submittedAt: string | null
  updatedAt: string | null
}

export interface ReportAnalysisResponse {
  reportId: string
  sopMatchResult: Record<string, unknown> | null
  riskPattern: string | null
  recommendation: string | null
  pdfUrl: string | null
  reviewStatus: 'PENDING' | 'REVIEWED'
}

export interface ApiErrorBody {
  code: string
  message: string
  timestamp: string
  fieldErrors: { field: string; reason: string }[]
}
