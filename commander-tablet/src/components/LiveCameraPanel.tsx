import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { buildLiveStreamUrl, getCameras } from '../api/devices'

// CMD-002 라이브 카메라 뷰 (FR-24/26 Phase 2). 지휘관이 등록된 CCTV 중 보고 싶은 카메라를 직접
// 골라서 본다 — 출동과 카메라를 시스템이 자동으로 연결해주는 기능은 아니다(DB에 그 연결 정보가
// 없다). 이 패널은 순수 영상만 보여준다: 위험 판단·경고는 여전히 알림 피드 쪽 몫이다.
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
              <div key={camera.deviceId}>
                <div style={{ fontSize: 12, marginBottom: 4, color: 'var(--color-ink-soft)' }}>
                  {camera.serialNo}
                </div>
                {failedIds.includes(camera.deviceId) ? (
                  <div className="wf-box" style={{ color: 'var(--color-alert)' }}>
                    영상 연결 실패 — 카메라 전원·네트워크를 확인하세요.
                  </div>
                ) : (
                  <img
                    alt={`${camera.serialNo} 실시간 영상`}
                    src={buildLiveStreamUrl(camera.streamUrl!)}
                    style={{ width: '100%', borderRadius: 6, border: '1px solid var(--color-border-soft)', display: 'block' }}
                    onError={() => setFailedIds((prev) => (prev.includes(camera.deviceId) ? prev : [...prev, camera.deviceId]))}
                  />
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
