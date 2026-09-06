import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { buildLiveStreamUrl, getCameras, getLiveDanger } from '../api/devices'
import type { CameraResponse } from '../types'

const DANGER_POLL_INTERVAL_MS = 5000

const DANGER_LABEL: Record<string, string> = { SAFE: '안전', WARNING: '주의', DANGER: '위험', CRITICAL: '심각' }
const DANGER_CLASS: Record<string, string> = {
  SAFE: 'risk-normal',
  WARNING: 'risk-caution',
  DANGER: 'risk-danger',
  CRITICAL: 'risk-critical',
}

const SPREAD_LABEL: Record<string, string> = {
  UP: '위로 확산',
  DOWN: '아래로 확산',
  LEFT: '왼쪽으로 확산',
  RIGHT: '오른쪽으로 확산',
}

// 카메라 한 대의 영상 + 위험도 배지. 위험도는 영상 위에 겹쳐 그리지 않는다 — ai-server의
// /streams/mjpeg(영상)와 /streams/danger(판단)가 애초에 분리된 채널이라 UI도 그 경계를 유지한다.
function LiveCameraTile({
  camera,
  failed,
  onError,
}: {
  camera: CameraResponse
  failed: boolean
  onError: () => void
}) {
  const dangerQuery = useQuery({
    queryKey: ['live-danger', camera.deviceId],
    queryFn: () => getLiveDanger(camera.streamUrl!, camera.deviceId),
    enabled: !failed,
    refetchInterval: DANGER_POLL_INTERVAL_MS,
    retry: false,
  })
  const danger = dangerQuery.data

  return (
    <div>
      <div style={{ fontSize: 12, marginBottom: 4, color: 'var(--color-ink-soft)' }}>{camera.serialNo}</div>
      {failed ? (
        <div className="wf-box" style={{ color: 'var(--color-alert)' }}>
          영상 연결 실패 — 카메라 전원·네트워크를 확인하세요.
        </div>
      ) : (
        <img
          alt={`${camera.serialNo} 실시간 영상`}
          src={buildLiveStreamUrl(camera.streamUrl!)}
          style={{ width: '100%', borderRadius: 6, border: '1px solid var(--color-border-soft)', display: 'block' }}
          onError={onError}
        />
      )}
      {!failed && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6, fontSize: 12 }}>
          {danger ? (
            <>
              <span className={`tag ${DANGER_CLASS[danger.dangerLevel] ?? ''}`}>
                {DANGER_LABEL[danger.dangerLevel] ?? danger.dangerLevel} · {Math.round(danger.dangerScore)}
              </span>
              {danger.growthRatio != null && danger.growthRatio > 1 && (
                <span style={{ color: 'var(--color-ink-soft)' }}>
                  확산 {danger.growthRatio.toFixed(1)}배
                  {danger.spreadDirection && ` · ${SPREAD_LABEL[danger.spreadDirection] ?? danger.spreadDirection}`}
                </span>
              )}
              {danger.isFlickerVerified === false && (
                <span style={{ color: 'var(--color-ink-soft)' }}>오탐 의심(깜빡임 없음)</span>
              )}
            </>
          ) : dangerQuery.isError ? (
            <span style={{ color: 'var(--color-ink-soft)' }}>위험도 확인 실패</span>
          ) : (
            <span style={{ color: 'var(--color-ink-soft)' }}>위험도 판단 중…</span>
          )}
        </div>
      )}
    </div>
  )
}

// CMD-002 라이브 카메라 뷰 (FR-24/26). 지휘관이 등록된 CCTV 중 보고 싶은 카메라를 직접
// 골라서 본다 — 출동과 카메라를 시스템이 자동으로 연결해주는 기능은 아니다(DB에 그 연결 정보가
// 없다). 영상은 순수 중계이고, 위험도 배지는 지금 보고 있는 그 카메라의 스냅샷을 주기적으로
// 다시 읽어와 옆에만 표시한다(영상 위에 겹쳐 그리지 않는다).
export function LiveCameraPanel() {
  const [expanded, setExpanded] = useState(false)
  const [watchingIds, setWatchingIds] = useState<string[]>([])
  const [failedIds, setFailedIds] = useState<string[]>([])

  const camerasQuery = useQuery({ queryKey: ['cameras'], queryFn: getCameras, enabled: expanded })
  const cameras = camerasQuery.data ?? []
  const watching = cameras.filter((c) => watchingIds.includes(c.deviceId))

  function toggleWatch(deviceId: string) {
    setFailedIds((prev) => prev.filter((id) => id !== deviceId))
    setWatchingIds((prev) => (prev.includes(deviceId) ? prev.filter((id) => id !== deviceId) : [...prev, deviceId]))
  }

  return (
    <div className="wf" style={{ marginBottom: 14 }}>
      <div className="wf-header">
        <span>라이브 카메라 (FR-24/26)</span>
        <button type="button" className="wf-btn small" onClick={() => setExpanded((v) => !v)}>
          {expanded ? '목록 닫기' : '카메라 선택'}
        </button>
      </div>
      <div className="wf-body">
        {expanded && (
          <div style={{ marginBottom: watching.length > 0 ? 10 : 0 }}>
            {camerasQuery.isLoading && <div className="spinner-text">카메라 목록 불러오는 중…</div>}
            {!camerasQuery.isLoading && cameras.length === 0 && (
              <div className="spinner-text">등록된 CCTV가 없습니다. ADM-006에서 먼저 등록하세요.</div>
            )}
            {cameras.map((camera) => (
              <label
                key={camera.deviceId}
                style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0', fontSize: 12.5 }}
              >
                <input
                  type="checkbox"
                  checked={watchingIds.includes(camera.deviceId)}
                  disabled={!camera.streamUrl}
                  onChange={() => toggleWatch(camera.deviceId)}
                />
                {camera.serialNo}
                {!camera.streamUrl && <span style={{ color: 'var(--color-ink-soft)' }}> · 스트림 주소 미등록</span>}
              </label>
            ))}
          </div>
        )}

        {watching.length === 0 && !expanded && (
          <div className="spinner-text">"카메라 선택"을 눌러 볼 카메라를 고르세요.</div>
        )}

        {watching.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10 }}>
            {watching.map((camera) => (
              <LiveCameraTile
                key={camera.deviceId}
                camera={camera}
                failed={failedIds.includes(camera.deviceId)}
                onError={() => setFailedIds((prev) => (prev.includes(camera.deviceId) ? prev : [...prev, camera.deviceId]))}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
