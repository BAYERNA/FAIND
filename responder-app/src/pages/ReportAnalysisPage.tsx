import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { MobileLayout } from '../components/MobileLayout'
import { ApiError } from '../api/client'
import { getReportAnalysis } from '../api/reports'

interface SopMatchItem {
  reportRecord: string
  executed: string
  recommendedSop: string
}

// USR-003 내 개인 리포트 — SOP 교차 검증 결과표 (FR-08, NFR-07 "AI 제안 vs 실제 행동 대조").
// ai-server가 만드는 sop_match_result가 {items:[{reportRecord,executed,recommendedSop}], ...}
// 형태를 벗어나면(고정 스키마가 아니므로) 원본을 그대로 나열해 값을 임의로 지어내지 않는다.
export function ReportAnalysisPage() {
  const { reportId } = useParams<{ reportId: string }>()
  const navigate = useNavigate()

  const query = useQuery({
    queryKey: ['report-analysis', reportId],
    queryFn: () => getReportAnalysis(reportId!),
    enabled: !!reportId,
    retry: false,
  })

  const items = Array.isArray(query.data?.sopMatchResult?.items) ? (query.data!.sopMatchResult!.items as SopMatchItem[]) : null

  return (
    <MobileLayout screenId="USR-003" title="SOP 대조 결과" onBack={() => navigate('/reports')}>
      {query.isLoading && <div className="spinner-text">불러오는 중…</div>}
      {query.isError && (
        <div className="wf-box">
          {query.error instanceof ApiError ? query.error.message : '아직 SOP 대조 분석이 완료되지 않았습니다.'}
        </div>
      )}

      {query.data && (
        <>
          <div className="wf" style={{ marginBottom: 14 }}>
            <div className="wf-header">
              <span>위험 패턴 · 권고</span>
              <span className={`tag ${query.data.reviewStatus === 'REVIEWED' ? 'status-active' : 'status-inactive'}`}>
                {query.data.reviewStatus === 'REVIEWED' ? '검토완료' : '검토대기'}
              </span>
            </div>
            <div className="wf-body">
              <div className="wf-box" style={{ marginBottom: 8 }}>
                <span className="label">위험 패턴</span>
                <div>{query.data.riskPattern ?? '—'}</div>
              </div>
              <div className="wf-box">
                <span className="label">개선 권고</span>
                <div>{query.data.recommendation ?? '—'}</div>
              </div>
            </div>
          </div>

          <div className="wf">
            <div className="wf-header">
              <span>내 보고서 기록 / 실행 여부 / 추천 SOP 근거</span>
            </div>
            <div className="wf-body" style={{ overflowX: 'auto' }}>
              {items ? (
                <table className="wf-table">
                  <thead>
                    <tr>
                      <th>보고서 기록</th>
                      <th>실행 여부</th>
                      <th>추천 SOP 근거</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item, index) => (
                      <tr key={index}>
                        <td>{item.reportRecord}</td>
                        <td style={{ color: item.executed === '일치' ? 'var(--color-success)' : 'var(--color-alert)' }}>
                          {item.executed}
                        </td>
                        <td>{item.recommendedSop}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="wf-box">
                  {Object.entries(query.data.sopMatchResult ?? {}).map(([key, value]) => (
                    <div key={key}>
                      {key}: {String(value)}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </MobileLayout>
  )
}
