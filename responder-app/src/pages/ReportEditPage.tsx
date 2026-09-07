import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { MobileLayout } from '../components/MobileLayout'
import { Banner } from '../components/Banner'
import { IncidentLabel } from '../components/IncidentLabel'
import { ApiError } from '../api/client'
import { getReport, saveDraft, submitReport } from '../api/reports'

// USR-002 사후보고서 작성 (FR-07). QA 최우선 재검증 대상이었던 "임시저장/제출 미작동"의
// 재발 방지 원칙(NFR-02)을 그대로 따른다 — 성공/실패를 항상 배너로 명시한다.
export function ReportEditPage() {
  const { reportId } = useParams<{ reportId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [content, setContent] = useState('')
  const [videoRef, setVideoRef] = useState('')
  const [banner, setBanner] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)

  const reportQuery = useQuery({ queryKey: ['report', reportId], queryFn: () => getReport(reportId!), enabled: !!reportId })

  useEffect(() => {
    if (reportQuery.data) {
      setContent(reportQuery.data.content ?? '')
      setVideoRef(reportQuery.data.videoRef ?? '')
    }
  }, [reportQuery.data])

  const draftMutation = useMutation({
    mutationFn: () => saveDraft(reportId!, { content, videoRef: videoRef || undefined }),
    onSuccess: () => {
      setBanner({ kind: 'success', text: '임시 저장되었습니다.' })
      queryClient.invalidateQueries({ queryKey: ['report', reportId] })
      queryClient.invalidateQueries({ queryKey: ['my-reports'] })
    },
    onError: (err) => setBanner({ kind: 'error', text: err instanceof ApiError ? err.message : '임시 저장에 실패했습니다.' }),
  })

  const submitMutation = useMutation({
    mutationFn: () => submitReport(reportId!, { content, videoRef: videoRef || undefined }),
    onSuccess: () => {
      setBanner({ kind: 'success', text: '제출되었습니다. SOP 대조 분석이 곧 완료됩니다.' })
      queryClient.invalidateQueries({ queryKey: ['report', reportId] })
      queryClient.invalidateQueries({ queryKey: ['my-reports'] })
    },
    onError: (err) => setBanner({ kind: 'error', text: err instanceof ApiError ? err.message : '제출에 실패했습니다.' }),
  })

  if (!reportId) return null

  const report = reportQuery.data
  const isSubmitted = report?.status === 'SUBMITTED'

  return (
    <MobileLayout screenId="USR-002" title="사후보고서 작성" onBack={() => navigate('/reports')}>
      {reportQuery.isLoading && <div className="spinner-text">불러오는 중…</div>}
      {report && (
        <div className="wf">
          <div className="wf-header">
            <IncidentLabel incidentId={report.incidentId} />
            <span className={`tag ${isSubmitted ? 'status-active' : 'status-inactive'}`}>
              {isSubmitted ? '제출완료' : '작성중'}
            </span>
          </div>
          <div className="wf-body">
            <label className="field-label" htmlFor="content">
              보고 내용
            </label>
            <textarea
              id="content"
              className="wf-field"
              style={{ minHeight: 160, resize: 'vertical', fontFamily: 'var(--font-sans)' }}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={isSubmitted}
              placeholder="현장 상황, 조치 내용, 특이사항을 기록하세요."
            />
            <label className="field-label" htmlFor="videoRef" style={{ marginTop: 10, display: 'block' }}>
              첨부 영상/사진 참조 (선택)
            </label>
            <input
              id="videoRef"
              className="wf-field"
              value={videoRef}
              onChange={(e) => setVideoRef(e.target.value)}
              disabled={isSubmitted}
              placeholder="예: s3://faind-reports/..."
            />

            {!isSubmitted && (
              <div className="form-actions">
                <button
                  type="button"
                  className="wf-btn"
                  disabled={draftMutation.isPending || submitMutation.isPending}
                  onClick={() => draftMutation.mutate()}
                >
                  {draftMutation.isPending ? '저장 중…' : '임시 저장'}
                </button>
                <button
                  type="button"
                  className="wf-btn primary"
                  disabled={draftMutation.isPending || submitMutation.isPending || !content.trim()}
                  onClick={() => submitMutation.mutate()}
                >
                  {submitMutation.isPending ? '제출 중…' : '제출'}
                </button>
              </div>
            )}
            {isSubmitted && (
              <div className="form-actions">
                <button type="button" className="wf-btn primary" onClick={() => navigate(`/reports/${reportId}/analysis`)}>
                  SOP 대조 결과 보기 →
                </button>
              </div>
            )}
            {banner && <Banner kind={banner.kind} message={banner.text} />}
          </div>
        </div>
      )}
    </MobileLayout>
  )
}
