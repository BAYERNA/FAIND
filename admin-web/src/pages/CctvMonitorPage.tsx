import { useQueries, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { AdminLayout } from '../components/AdminLayout'
import { buildLiveStreamUrl, getCameras, getLiveDanger } from '../api/devices'
import type { CameraResponse, LiveDangerSnapshot } from '../types'

const DANGER_POLL_INTERVAL_MS = 8000

const DANGER_LABEL: Record<string, string> = { SAFE: '안전', WARNING: '주의', DANGER: '위험', CRITICAL: '심각' }
const DANGER_CLASS: Record<string, string> = {
  SAFE: 'risk-normal',
  WARNING: 'risk-caution',
  DANGER: 'risk-danger',
  CRITICAL: 'risk-critical',
}
// 정렬 우선순위 계산용. 아직 판단이 안 된(로딩·오류) 카메라는 SAFE와 동급으로 취급해 화면이
// 매 폴링마다 크게 들썩이지 않게 한다 — 실제로 위험하다고 확인된 카메라만 앞으로 끌어올린다.
const DANGER_RANK: Record<string, number> = { CRITICAL: 3, DANGER: 2, WARNING: 1, SAFE: 0 }

function CameraTile({ camera, danger, dangerError }: { camera: CameraResponse; danger?: LiveDangerSnapshot; dangerError: boolean }) {
  return (
    <div className="wf" style={{ marginBottom: 0 }}>
      <div className="wf-header">
        <span>{camera.serialNo}</span>
        {danger && (
          <span className={`tag ${DANGER_CLASS[danger.dangerLevel] ?? ''}`}>
            {DANGER_LABEL[danger.dangerLevel] ?? danger.dangerLevel} · {Math.round(danger.dangerScore)}
          </span>
        )}
      </div>
      <div className="wf-body" style={{ padding: 0 }}>
        <img
          alt={`${camera.serialNo} 실시간 영상`}
          src={buildLiveStreamUrl(camera.streamUrl!)}
          style={{ width: '100%', display: 'block' }}
        />
      </div>
      {(dangerError || (danger && danger.isFlickerVerified === false)) && (
        <div className="wf-body" style={{ paddingTop: 6, fontSize: 12, color: 'var(--color-ink-soft)' }}>
          {dangerError ? '위험도 확인 실패' : '오탐 의심(깜빡임 없음)'}
        </div>
      )}
    </div>
  )
}

// ADM-010 전체 CCTV 상시 감시(Phase 4). 원 와이어프레임 15개 화면 스펙 밖의 신규 추가 화면 —
// 출동과 무관하게 등록된 CCTV 전체를 관제실이 상시 훑어볼 수 있게 한다. CMD-002 라이브 카메라
// 뷰(Phase 2/3)는 지휘관이 특정 출동에서 카메라를 수동으로 고르는 화면이지만, 여기는 그 반대 —
// 출동이 생기기 전부터 이상 징후를 먼저 포착하려는 상시 모니터링 목적이라 자동으로 전부 띄운다.
// 위험도가 높은 카메라를 먼저 보여주되(위험도순 정렬은 CMD-002 대원 목록과 동일한 패턴), 영상
// 위에 감지 박스는 여전히 그리지 않는다 — 영상과 판단 채널을 분리해 온 원칙을 그대로 지킨다.
export function CctvMonitorPage() {
  const camerasQuery = useQuery({ queryKey: ['cameras'], queryFn: getCameras, refetchInterval: 30000 })
  const cameras = camerasQuery.data ?? []
  const withStream = cameras.filter((c) => c.streamUrl)
  const withoutStream = cameras.filter((c) => !c.streamUrl)

  const dangerQueries = useQueries({
    queries: withStream.map((camera) => ({
      queryKey: ['live-danger', camera.deviceId],
      queryFn: () => getLiveDanger(camera.streamUrl!, camera.deviceId),
      refetchInterval: DANGER_POLL_INTERVAL_MS,
      retry: false,
    })),
  })

  const tiles = withStream
    .map((camera, i) => ({ camera, danger: dangerQueries[i]?.data, error: dangerQueries[i]?.isError ?? false }))
    .sort((a, b) => (DANGER_RANK[b.danger?.dangerLevel ?? 'SAFE'] ?? 0) - (DANGER_RANK[a.danger?.dangerLevel ?? 'SAFE'] ?? 0))

  return (
    <AdminLayout screenId="ADM-010" title="전체 CCTV 상시 감시">
      {camerasQuery.isLoading && <div className="spinner-text">카메라 목록 불러오는 중…</div>}
      {!camerasQuery.isLoading && cameras.length === 0 && (
        <div className="wf-box">등록된 CCTV가 없습니다. ADM-006에서 먼저 등록하세요.</div>
      )}

      {tiles.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 14, marginBottom: 14 }}>
          {tiles.map(({ camera, danger, error }) => (
            <CameraTile key={camera.deviceId} camera={camera} danger={danger} dangerError={error} />
          ))}
        </div>
      )}

      {withoutStream.length > 0 && (
        <div className="wf">
          <div className="wf-header">
            <span>스트림 미등록 카메라 ({withoutStream.length}대)</span>
          </div>
          <div className="wf-body">
            {withoutStream.map((camera) => (
              <div key={camera.deviceId} style={{ padding: '4px 0', fontSize: 12.5, color: 'var(--color-ink-soft)' }}>
                {camera.serialNo}
              </div>
            ))}
            <div style={{ marginTop: 8 }}>
              <Link to="/devices" className="wf-btn small">
                ADM-006에서 스트림 주소 등록
              </Link>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  )
}
