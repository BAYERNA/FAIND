import { useQuery } from '@tanstack/react-query'
import { getAccount } from '../api/accounts'
import type { ResponderStatusResponse } from '../types'

const RISK_LABEL: Record<string, string> = { DANGER: '위험', CAUTION: '주의', NORMAL: '정상' }
const RISK_CLASS: Record<string, string> = { DANGER: 'risk-danger', CAUTION: 'risk-caution', NORMAL: 'risk-normal' }
const CONNECTION_LABEL: Record<string, string> = {
  CONNECTED: '정상연결',
  MESH: '메시망',
  SMS: 'SMS',
  DISCONNECTED: '연결끊김',
}

// CMD-002 annot#1: 위험도 순 자동정렬된 목록의 한 행 — 클릭 시 CMD-003 대원 상세로 이동.
export function ResponderRow({ status, onClick }: { status: ResponderStatusResponse; onClick: () => void }) {
  const accountQuery = useQuery({
    queryKey: ['account', status.userId],
    queryFn: () => getAccount(status.userId),
  })

  const riskLevel = status.riskLevel ?? 'NORMAL'

  return (
    <button type="button" className="responder-row" onClick={onClick}>
      <span>
        <div className="responder-name">
          {accountQuery.data?.name ?? status.userId.slice(0, 8)}
          {accountQuery.data?.team && <span className="responder-meta"> · {accountQuery.data.team}</span>}
        </div>
        <div className="responder-meta">
          {CONNECTION_LABEL[status.connectionStatus ?? ''] ?? status.connectionStatus ?? '연결상태 미상'} ·{' '}
          {new Date(status.recordedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 갱신
        </div>
      </span>
      <span className={`tag ${RISK_CLASS[riskLevel] ?? ''}`}>{RISK_LABEL[riskLevel] ?? riskLevel}</span>
    </button>
  )
}
