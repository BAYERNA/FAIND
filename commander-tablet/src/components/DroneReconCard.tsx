import { useQuery } from '@tanstack/react-query'
import { getDevice } from '../api/devices'
import type { AiJudgmentSummaryResponse, DroneDispatchResponse } from '../types'

const STATUS_LABEL: Record<string, string> = {
  EN_ROUTE: '출동중',
  ON_SITE: '현장도착',
  RETURNED: '복귀',
}

function formatTime(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

// CMD-002 드론 정찰 카드 (FR-26): 드론 출동 현황 + 배터리/상태 + AI 판단(정찰 결과) 이력.
export function DroneReconCard({
  dispatch,
  judgments,
}: {
  dispatch: DroneDispatchResponse
  judgments: AiJudgmentSummaryResponse[]
}) {
  const deviceQuery = useQuery({ queryKey: ['device', dispatch.droneId], queryFn: () => getDevice(dispatch.droneId) })
  const relatedJudgments = judgments.filter((j) => j.sourceDeviceId === dispatch.droneId)

  return (
    <div className="wf" style={{ marginBottom: 10 }}>
      <div className="wf-header">
        <span>
          드론 {deviceQuery.data?.serialNo ?? dispatch.droneId.slice(0, 8)} · {STATUS_LABEL[dispatch.status] ?? dispatch.status}
        </span>
        <span>
          {deviceQuery.data?.batteryLevel != null ? `배터리 ${deviceQuery.data.batteryLevel}%` : ''}
        </span>
      </div>
      <div className="wf-body">
        <div style={{ fontSize: 12.5, marginBottom: 8 }}>
          출동 {formatTime(dispatch.dispatchedAt)} · 도착 {formatTime(dispatch.arrivedAt)}
        </div>
        {dispatch.videoRef && (
          <div className="wf-box" style={{ marginBottom: 8 }}>
            <span className="label">정찰 영상</span>
            <div>{dispatch.videoRef}</div>
          </div>
        )}
        {relatedJudgments.length === 0 && <div className="spinner-text">아직 AI 판단 결과가 없습니다.</div>}
        {relatedJudgments.map((judgment) => (
          <div key={judgment.judgmentId} className="wf-box" style={{ marginBottom: 6 }}>
            <span className="label">
              {formatTime(judgment.createdAt)} ·{' '}
              {judgment.confidenceScore != null ? `신뢰도 ${Math.round(judgment.confidenceScore)}%` : '신뢰도 미상'}
            </span>
            <div>{judgment.summary ?? '요약 없음'}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
