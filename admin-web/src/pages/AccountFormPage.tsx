import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AdminLayout } from '../components/AdminLayout'
import { Banner } from '../components/Banner'
import { ApiError } from '../api/client'
import { getAccount, registerAccount, updateAccount, type AccountFormInput } from '../api/accounts'

const EMPTY_FORM: AccountFormInput = { name: '', role: 'RESPONDER', badgeNumber: '', team: '', phone: '' }

// ADM-003 계정 등록·수정 (FR-10)
// QA 최우선 재검증 대상이었던 두 결함의 재발 방지가 이 화면의 핵심 목표:
//  1) "수정" 진입 시 폼이 비어 있던 문제 -> useEffect로 getAccount() 응답을 form 상태에 그대로 채운다.
//  2) 저장 버튼 클릭해도 반응 없던 문제 -> mutation 성공/실패를 항상 배너로 명시한다 (NFR-02).
export function AccountFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const { userId } = useParams<{ userId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [form, setForm] = useState<AccountFormInput>(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)
  const [issuedPassword, setIssuedPassword] = useState<string | null>(null)

  const accountQuery = useQuery({
    queryKey: ['account', userId],
    queryFn: () => getAccount(userId!),
    enabled: mode === 'edit' && !!userId,
  })

  // 수정 모드 진입 시 서버 응답으로 폼 전체 필드를 프리필한다.
  useEffect(() => {
    if (mode === 'edit' && accountQuery.data) {
      const account = accountQuery.data
      setForm({
        name: account.name,
        role: account.role,
        badgeNumber: account.badgeNumber,
        team: account.team ?? '',
        phone: account.phone ?? '',
      })
    }
  }, [mode, accountQuery.data])

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (mode === 'create') {
        return registerAccount(form)
      }
      const updated = await updateAccount(userId!, form)
      return { account: updated, issuedTemporaryPassword: null as string | null }
    },
    onSuccess: (result) => {
      setError(null)
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      if (result.issuedTemporaryPassword) {
        setIssuedPassword(result.issuedTemporaryPassword)
      } else {
        navigate('/accounts')
      }
    },
    onError: (err) => {
      setError(err instanceof ApiError ? err.message : '저장에 실패했습니다.')
    },
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    saveMutation.mutate()
  }

  if (mode === 'edit' && accountQuery.isLoading) {
    return (
      <AdminLayout screenId="ADM-003" title="계정 수정">
        <div className="spinner-text">불러오는 중…</div>
      </AdminLayout>
    )
  }

  if (issuedPassword) {
    return (
      <AdminLayout screenId="ADM-003" title="계정 등록 완료">
        <div className="wf" style={{ maxWidth: 480 }}>
          <div className="wf-header">
            <span>계정이 등록되었습니다</span>
          </div>
          <div className="wf-body">
            <div className="wf-box">
              <span className="label">발급된 임시 비밀번호 (최초 1회만 표시)</span>
              <strong style={{ fontFamily: 'var(--font-mono)', fontSize: 16 }}>{issuedPassword}</strong>
            </div>
            <div className="form-actions">
              <button type="button" className="wf-btn primary" onClick={() => navigate('/accounts')}>
                계정 목록으로
              </button>
            </div>
          </div>
        </div>
      </AdminLayout>
    )
  }

  return (
    <AdminLayout screenId="ADM-003" title={mode === 'create' ? '계정 등록' : `계정 수정 — ${form.name}`}>
      <form className="wf" style={{ maxWidth: 560 }} onSubmit={handleSubmit}>
        <div className="wf-header">
          <span>{mode === 'create' ? '계정 신규 등록' : `계정 수정 — ${form.name}`}</span>
        </div>
        <div className="wf-body">
          <div className="form-grid">
            <div>
              <label className="field-label" htmlFor="name">
                이름
              </label>
              <input
                id="name"
                className="wf-field"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                required
              />
            </div>
            <div>
              <label className="field-label" htmlFor="role">
                역할
              </label>
              <select
                id="role"
                className="wf-field"
                value={form.role}
                onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
              >
                <option value="RESPONDER">대원</option>
                <option value="COMMANDER">지휘관</option>
                <option value="ADMIN">관리자</option>
              </select>
            </div>
            <div>
              <label className="field-label" htmlFor="team">
                소속
              </label>
              <input
                id="team"
                className="wf-field"
                value={form.team}
                onChange={(e) => setForm((f) => ({ ...f, team: e.target.value }))}
              />
            </div>
            <div>
              <label className="field-label" htmlFor="badgeNumber">
                사번
              </label>
              <input
                id="badgeNumber"
                className="wf-field"
                value={form.badgeNumber}
                onChange={(e) => setForm((f) => ({ ...f, badgeNumber: e.target.value }))}
                disabled={mode === 'edit'}
                required={mode === 'create'}
              />
            </div>
            <div>
              <label className="field-label" htmlFor="phone">
                연락처
              </label>
              <input
                id="phone"
                className="wf-field"
                value={form.phone}
                onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
              />
            </div>
          </div>
          {mode === 'create' && (
            <div className="wf-box" style={{ marginTop: 10 }}>
              등록 시 임시 비밀번호가 자동 생성되어 등록 완료 화면에 1회 표시됩니다.
            </div>
          )}
          <div className="form-actions">
            <button type="button" className="wf-btn" onClick={() => navigate('/accounts')}>
              취소
            </button>
            <button type="submit" className="wf-btn primary" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? '저장 중…' : '저장'}
            </button>
          </div>
          {error && <Banner kind="error" message={error} />}
        </div>
      </form>
    </AdminLayout>
  )
}
