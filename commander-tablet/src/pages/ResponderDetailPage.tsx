import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { TabletLayout } from '../components/TabletLayout'
import { getAccount } from '../api/accounts'
import { getMonitoring } from '../api/incidents'
import { listAlerts } from '../api/alerts'
import './ResponderDetailPage.css'

const RISK_LABEL: Record<string, string> = { DANGER: '위험', CAUTION: '주의', NORMAL: '정상' }
const RISK_CLASS: Record<string, string> = { DANGER: 'risk-danger', CAUTION: 'risk-caution', NORMAL: 'risk-normal' }
const CONNECTION_LABEL: Record<string, string> = {
  CONNECTED: '정상연결',
  MESH: '메시망',
  SMS: 'SMS',
  DISCONNECTED: '연결끊김',
}

// biometricData/environmentData는 DB설계서상 자유 JSONB라 고정 스키마가 없다. 웨어러블 연동 시
// {heartRate, bodyTemperature} / {ambientTemperature, gasLevel} 관례를 기대하되, 없으면 원본 키를
// 그대로 나열한다 — 실제로 없는 값을 임의로 만들어 보여주지 않는다(admin-web ADM-002와 동일 원칙).
function MetricCard({ value, label }: { value: string; label: string }) {
  return (
    <div className="metric-card">
      <div className="value">{value}</div>
      <div className="label">{label}</div>
    </div>
  )
}

function RawDataBox({ label, data, skip }: { label: string; data: Record<string, unknown> | null; skip: string[] }) {
  const entries = data ? Object.entries(data).filter(([key]) => !skip.includes(key)) : []
  if (entries.length === 0) return null
  return (
    <div className="wf-box" style={{ marginBottom: 10 }}>
      <span className="label">{label} (원본)</span>
      {entries.map(([key, value]) => (
        <div key={key}>
          {key}: {String(value)}
        </div>
      ))}
    </div>
  )
}

// CMD-003 대원 상세뷰 (FR-04). 생체·환경 데이터의 최신 스냅샷과 이 대원과 관련된 알림 이력을 보여준다.
// backend가 상태 로그의 시계열 조회 API를 제공하지 않으므로(최신 1건만 집계), 추세 그래프는 만들지 않는다.
export function ResponderDetailPage() {
  const { incidentId, userId } = useParams<{ incidentId: string; userId: string }>()
  const navigate = useNavigate()

  const accountQuery = useQuery({ queryKey: ['account', userId], queryFn: () => getAccount(userId!), enabled: !!userId })
  const monitoringQuery = useQuery({
    queryKey: ['monitoring', incidentId],
    queryFn: () => getMonitoring(incidentId!),
    enabled: !!incidentId,
  })
  const alertsQuery = useQuery({
    queryKey: ['alerts', incidentId],
    queryFn: () => listAlerts(incidentId!),
    enabled: !!incidentId,
  })

  if (!incidentId || !userId) return null

  const status = monitoringQuery.data?.responders.find((r) => r.userId === userId)
  const assignment = monitoringQuery.data?.assignments.find((a) => a.userId === userId)
  const personalAlerts = (alertsQuery.data ?? []).filter(
    (alert) => alert.targetUserId === userId || (alert.targetUserId === null && alert.alertType === 'RISK_WARNING'),
  )

  const biometric = status?.biometricData as { heartRate?: number; bodyTemperature?: number } | null | undefined
  const environment = status?.environmentData as { ambientTemperature?: number; gasLevel?: number } | null | undefined

  return (
    <TabletLayout
      screenId="CMD-003"
      title={accountQuery.data ? `${accountQuery.data.name} 대원 상세` : '대원 상세'}
      onBack={() => navigate(`/incidents/${incidentId}`)}
    >
      {(monitoringQuery.isLoading || accountQuery.isLoading) && <div className="spinner-text">불러오는 중…</div>}

      {accountQuery.data && (
        <div className="wf" style={{ marginBottom: 14 }}>
          <div className="wf-header">
            <span>
              {accountQuery.data.name} · {accountQuery.data.badgeNumber} · {accountQuery.data.team ?? '소속 미상'}
            </span>
            {status && (
              <span className={`tag ${RISK_CLASS[status.riskLevel ?? ''] ?? ''}`}>
                {RISK_LABEL[status.riskLevel ?? ''] ?? status.riskLevel ?? '상태 미상'}
              </span>
            )}
          </div>
          <div className="wf-body">
            {assignment && (
              <div style={{ fontSize: 12.5, marginBottom: 12 }}>
                {assignment.roleInIncident ?? '역할 미지정'}
                {assignment.firstWave ? ' · 선발대' : ''}
                {assignment.commsLead ? ' · 통신담당' : ''} · 연결:{' '}
                {CONNECTION_LABEL[status?.connectionStatus ?? ''] ?? status?.connectionStatus ?? '미상'}
              </div>
            )}

            {!status && <div className="spinner-text">아직 수신된 생체·환경 데이터가 없습니다.</div>}

            {status && (
              <>
                <div className="section-title" style={{ marginBottom: 6 }}>
                  최신 생체·환경 데이터 (
                  {new Date(status.recordedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 기준)
                </div>
                <div className="metric-row">
                  {biometric?.heartRate != null && <MetricCard value={`${biometric.heartRate} bpm`} label="심박수" />}
                  {biometric?.bodyTemperature != null && (
                    <MetricCard value={`${biometric.bodyTemperature}℃`} label="체온" />
                  )}
                  {environment?.ambientTemperature != null && (
                    <MetricCard value={`${environment.ambientTemperature}℃`} label="주변온도" />
                  )}
                  {environment?.gasLevel != null && <MetricCard value={`${environment.gasLevel}`} label="가스농도" />}
                </div>
                <RawDataBox label="생체데이터" data={status.biometricData} skip={['heartRate', 'bodyTemperature']} />
                <RawDataBox label="환경데이터" data={status.environmentData} skip={['ambientTemperature', 'gasLevel']} />
              </>
            )}
          </div>
        </div>
      )}

      <div className="wf">
        <div className="wf-header">
          <span>이 대원 관련 알림 이력</span>
        </div>
        <div className="wf-body">
          {personalAlerts.length === 0 && <div className="spinner-text">관련 알림이 없습니다.</div>}
          {personalAlerts.map((alert) => (
            <div key={alert.alertId} className="alert-item">
              <div>{alert.message ?? '(내용 없음)'}</div>
              <div className="alert-meta">{new Date(alert.sentAt).toLocaleString('ko-KR')}</div>
            </div>
          ))}
        </div>
      </div>
    </TabletLayout>
  )
}
