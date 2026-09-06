import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { searchResponders } from '../api/accounts'
import { assignResponder } from '../api/incidents'
import { ApiError } from '../api/client'
import { Banner } from './Banner'

// CMD-002 대원 배정 (FR-19): 최초 배정자는 backend에서 자동으로 선발대·통신담당이 된다.
// 이미 배정된 대원은 목록에서 제외해 UNIQUE(incident_id, user_id) 위반을 사전에 막는다.
export function AssignResponderPanel({
  incidentId,
  assignedUserIds,
}: {
  incidentId: string
  assignedUserIds: string[]
}) {
  const queryClient = useQueryClient()
  const [expanded, setExpanded] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [selectedUserId, setSelectedUserId] = useState('')
  const [roleInIncident, setRoleInIncident] = useState('')
  const [error, setError] = useState<string | null>(null)

  const searchQuery = useQuery({
    queryKey: ['responder-search', keyword],
    queryFn: () => searchResponders(keyword),
    enabled: expanded,
  })

  const candidates = (searchQuery.data?.content ?? []).filter((a) => !assignedUserIds.includes(a.userId))

  const assignMutation = useMutation({
    mutationFn: () => assignResponder(incidentId, { userId: selectedUserId, roleInIncident: roleInIncident || undefined }),
    onSuccess: () => {
      setSelectedUserId('')
      setRoleInIncident('')
      setKeyword('')
      queryClient.invalidateQueries({ queryKey: ['monitoring', incidentId] })
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : '배정에 실패했습니다.'),
  })

  if (!expanded) {
    return (
      <button type="button" className="wf-btn small" onClick={() => setExpanded(true)}>
        + 대원 배정
      </button>
    )
  }

  return (
    <div style={{ marginTop: 10, borderTop: '1px dashed var(--color-border-dashed)', paddingTop: 10 }}>
      <div className="form-row">
        <input
          className="wf-field"
          placeholder="이름·사번·소속 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button type="button" className="wf-btn small" style={{ flexShrink: 0, whiteSpace: 'nowrap' }} onClick={() => setExpanded(false)}>
          닫기
        </button>
      </div>

      {searchQuery.isLoading && <div className="spinner-text">검색 중…</div>}
      {!searchQuery.isLoading && candidates.length === 0 && (
        <div className="spinner-text">배정 가능한 대원이 없습니다.</div>
      )}
      {candidates.map((account) => (
        <label key={account.userId} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0', fontSize: 12.5 }}>
          <input
            type="radio"
            name="responder-candidate"
            checked={selectedUserId === account.userId}
            onChange={() => setSelectedUserId(account.userId)}
          />
          {account.name} · {account.badgeNumber}
          {account.team ? ` · ${account.team}` : ''}
        </label>
      ))}

      <div className="form-row" style={{ marginTop: 8 }}>
        <input
          className="wf-field"
          placeholder="역할 (예: 화점진압, 인명구조)"
          value={roleInIncident}
          onChange={(e) => setRoleInIncident(e.target.value)}
        />
        <button
          type="button"
          className="wf-btn primary"
          style={{ flexShrink: 0, whiteSpace: 'nowrap' }}
          disabled={!selectedUserId || assignMutation.isPending}
          onClick={() => assignMutation.mutate()}
        >
          {assignMutation.isPending ? '배정 중…' : '배정'}
        </button>
      </div>
      {error && <Banner kind="error" message={error} />}
    </div>
  )
}
