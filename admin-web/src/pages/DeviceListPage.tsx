import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { AdminLayout } from '../components/AdminLayout'
import { Banner } from '../components/Banner'
import { ApiError } from '../api/client'
import { listAccounts } from '../api/accounts'
import { listDevices, registerDevice, remapDevice, type DeviceRegisterInput } from '../api/devices'
import type { DeviceType } from '../types'

const DEVICE_TYPE_LABEL: Record<DeviceType, string> = {
  BODYCAM: '웨어러블카메라',
  SMARTPHONE: '스마트폰',
  DIGITAL_MASK: '디지털마스크',
  SENSOR: '센서',
  CCTV: 'CCTV',
  DRONE: '드론',
}
const PERSONAL_TYPES: DeviceType[] = ['BODYCAM', 'SMARTPHONE', 'DIGITAL_MASK', 'SENSOR']
const FIXED_LOCATION_TYPES: DeviceType[] = ['CCTV', 'DRONE']

const EMPTY_FORM: DeviceRegisterInput = { deviceType: 'BODYCAM', serialNo: '', connectionType: 'BLE' }

// ADM-006 기기·장비 매핑 관리 (FR-11, NFR-05, FR-24 CCTV·드론 자산등록)
export function DeviceListPage() {
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [deviceType, setDeviceType] = useState('ALL')
  const [showRegisterForm, setShowRegisterForm] = useState(false)
  const [form, setForm] = useState<DeviceRegisterInput>(EMPTY_FORM)
  const [formError, setFormError] = useState<string | null>(null)
  const [remapTargetId, setRemapTargetId] = useState<string | null>(null)
  const [remapUserId, setRemapUserId] = useState('')
  const [remapError, setRemapError] = useState<string | null>(null)

  const devicesQuery = useQuery({
    queryKey: ['devices', keyword, deviceType],
    queryFn: () => listDevices({ keyword, deviceType, page: 0, size: 50 }),
  })
  const respondersQuery = useQuery({
    queryKey: ['accounts', '', 'RESPONDER', 'for-mapping'],
    queryFn: () => listAccounts({ role: 'RESPONDER', page: 0, size: 200 }),
  })

  const registerMutation = useMutation({
    mutationFn: (input: DeviceRegisterInput) => registerDevice(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['devices'] })
      setShowRegisterForm(false)
      setForm(EMPTY_FORM)
      setFormError(null)
    },
    onError: (err) => setFormError(err instanceof ApiError ? err.message : '등록에 실패했습니다.'),
  })

  const remapMutation = useMutation({
    mutationFn: ({ deviceId, userId }: { deviceId: string; userId: string }) => remapDevice(deviceId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['devices'] })
      setRemapTargetId(null)
      setRemapError(null)
    },
    onError: (err) => setRemapError(err instanceof ApiError ? err.message : '매핑 변경에 실패했습니다.'),
  })

  const isPersonal = PERSONAL_TYPES.includes(form.deviceType)
  const isFixed = FIXED_LOCATION_TYPES.includes(form.deviceType)

  function handleRegisterSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    registerMutation.mutate(form)
  }

  function handleRemapSubmit(e: FormEvent, deviceId: string) {
    e.preventDefault()
    if (!remapUserId) return
    remapMutation.mutate({ deviceId, userId: remapUserId })
  }

  return (
    <AdminLayout screenId="ADM-006" title="기기·장비 매핑 관리">
      <div className="wf">
        <div className="wf-header">
          <span>기기 · 장비 매핑 관리</span>
          <button type="button" className="wf-btn primary small" onClick={() => setShowRegisterForm((v) => !v)}>
            + 신규 기기 등록
          </button>
        </div>
        <div className="wf-body">
          {showRegisterForm && (
            <form className="wf-box" style={{ marginBottom: 14 }} onSubmit={handleRegisterSubmit}>
              <div className="form-grid">
                <div>
                  <label className="field-label" htmlFor="deviceType">
                    기기 유형
                  </label>
                  <select
                    id="deviceType"
                    className="wf-field"
                    value={form.deviceType}
                    onChange={(e) => setForm((f) => ({ ...f, deviceType: e.target.value as DeviceType }))}
                  >
                    {Object.entries(DEVICE_TYPE_LABEL).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label" htmlFor="serialNo">
                    시리얼 번호
                  </label>
                  <input
                    id="serialNo"
                    className="wf-field"
                    value={form.serialNo}
                    onChange={(e) => setForm((f) => ({ ...f, serialNo: e.target.value }))}
                    required
                  />
                </div>
                {isPersonal && (
                  <div>
                    <label className="field-label" htmlFor="mappedUser">
                      매핑 대원
                    </label>
                    <select
                      id="mappedUser"
                      className="wf-field"
                      value={form.currentUserId ?? ''}
                      onChange={(e) => setForm((f) => ({ ...f, currentUserId: e.target.value || undefined }))}
                    >
                      <option value="">미매핑</option>
                      {respondersQuery.data?.content.map((r) => (
                        <option key={r.userId} value={r.userId}>
                          {r.name} ({r.badgeNumber})
                        </option>
                      ))}
                    </select>
                  </div>
                )}
                {isFixed && (
                  <>
                    <div>
                      <label className="field-label" htmlFor="latitude">
                        위도
                      </label>
                      <input
                        id="latitude"
                        type="number"
                        step="any"
                        className="wf-field"
                        value={form.latitude ?? ''}
                        onChange={(e) => setForm((f) => ({ ...f, latitude: Number(e.target.value) }))}
                        required
                      />
                    </div>
                    <div>
                      <label className="field-label" htmlFor="longitude">
                        경도
                      </label>
                      <input
                        id="longitude"
                        type="number"
                        step="any"
                        className="wf-field"
                        value={form.longitude ?? ''}
                        onChange={(e) => setForm((f) => ({ ...f, longitude: Number(e.target.value) }))}
                        required
                      />
                    </div>
                  </>
                )}
              </div>
              <div className="form-actions">
                <button type="button" className="wf-btn" onClick={() => setShowRegisterForm(false)}>
                  취소
                </button>
                <button type="submit" className="wf-btn primary" disabled={registerMutation.isPending}>
                  {registerMutation.isPending ? '등록 중…' : '등록'}
                </button>
              </div>
              {formError && <Banner kind="error" message={formError} />}
            </form>
          )}

          <div className="form-row">
            <div style={{ flex: 1 }}>
              <label className="field-label" htmlFor="deviceKeyword">
                검색
              </label>
              <input
                id="deviceKeyword"
                className="wf-field"
                placeholder="기기ID·매핑대원 검색"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </div>
            <div style={{ width: 160 }}>
              <label className="field-label" htmlFor="deviceTypeFilter">
                필터
              </label>
              <select id="deviceTypeFilter" className="wf-field" value={deviceType} onChange={(e) => setDeviceType(e.target.value)}>
                <option value="ALL">전체 유형</option>
                {Object.entries(DEVICE_TYPE_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {devicesQuery.isLoading && <div className="spinner-text">불러오는 중…</div>}

          {devicesQuery.data && (
            <table className="wf-table">
              <thead>
                <tr>
                  <th>기기유형</th>
                  <th>시리얼</th>
                  <th>연결방식</th>
                  <th>매핑대원</th>
                  <th>소속팀</th>
                  <th>상태</th>
                  <th>배터리</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {devicesQuery.data.content.map((device) => (
                  <tr key={device.deviceId}>
                    <td>{DEVICE_TYPE_LABEL[device.deviceType]}</td>
                    <td>{device.serialNo}</td>
                    <td>{device.connectionType ?? '—'}</td>
                    <td>{device.mappedUserName ?? (PERSONAL_TYPES.includes(device.deviceType) ? '— (미매핑)' : '— (자산)')}</td>
                    <td>{device.mappedUserTeam ?? '—'}</td>
                    <td style={{ color: device.status === 'NORMAL' ? undefined : 'var(--color-alert)' }}>
                      {device.status === 'NORMAL' ? '정상' : device.status === 'WARNING' ? '저전압' : '연결끊김'}
                    </td>
                    <td>{device.batteryLevel != null ? `${device.batteryLevel}%` : '-'}</td>
                    <td>
                      {PERSONAL_TYPES.includes(device.deviceType) &&
                        (remapTargetId === device.deviceId ? (
                          <form style={{ display: 'flex', gap: 4 }} onSubmit={(e) => handleRemapSubmit(e, device.deviceId)}>
                            <select
                              className="wf-field"
                              style={{ padding: '4px 6px', fontSize: 11 }}
                              value={remapUserId}
                              onChange={(e) => setRemapUserId(e.target.value)}
                              autoFocus
                            >
                              <option value="">대원 선택</option>
                              {respondersQuery.data?.content.map((r) => (
                                <option key={r.userId} value={r.userId}>
                                  {r.name}
                                </option>
                              ))}
                            </select>
                            <button type="submit" className="wf-btn primary small" disabled={remapMutation.isPending}>
                              확인
                            </button>
                            <button type="button" className="wf-btn small" onClick={() => setRemapTargetId(null)}>
                              취소
                            </button>
                          </form>
                        ) : (
                          <button
                            type="button"
                            className="wf-btn small"
                            onClick={() => {
                              setRemapTargetId(device.deviceId)
                              setRemapUserId(device.currentUserId ?? '')
                            }}
                          >
                            매핑 변경
                          </button>
                        ))}
                    </td>
                  </tr>
                ))}
                {devicesQuery.data.content.length === 0 && (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center' }}>
                      검색 결과가 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
          {remapError && <Banner kind="error" message={remapError} />}
        </div>
      </div>
    </AdminLayout>
  )
}
