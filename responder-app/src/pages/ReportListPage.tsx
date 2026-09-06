import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { MobileLayout } from '../components/MobileLayout'
import { Banner } from '../components/Banner'
import { IncidentLabel } from '../components/IncidentLabel'
import { getMyReports } from '../api/reports'
import { useIncidentSocket } from '../ws/useIncidentSocket'
import './ReportListPage.css'

// USR-002 사후보고서 작성 (FR-07) 목록. FR-05 종료 확정 시 backend가 만든 DRAFT 보고서가
// user:{userId} 개인 채널로 실시간 푸시되므로(report:draft-created), 여기서 그 알림을 받아 갱신한다.
export function ReportListPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { latestReportDraft } = useIncidentSocket(undefined)

  const reportsQuery = useQuery({ queryKey: ['my-reports'], queryFn: getMyReports })

  useEffect(() => {
    if (!latestReportDraft) return
    queryClient.invalidateQueries({ queryKey: ['my-reports'] })
  }, [latestReportDraft, queryClient])

  const reports = reportsQuery.data ?? []

  return (
    <MobileLayout screenId="USR-002" title="사후보고서">
      {latestReportDraft && (
        <Banner kind="success" message="새 보고서 작성 요청이 도착했습니다." />
      )}
      <div className="wf">
        <div className="wf-header">
          <span>내 보고서 목록</span>
        </div>
        <div className="wf-body">
          {reportsQuery.isLoading && <div className="spinner-text">불러오는 중…</div>}
          {reports.length === 0 && !reportsQuery.isLoading && <div className="spinner-text">작성할 보고서가 없습니다.</div>}
          {reports.map((report) => (
            <div key={report.reportId} className="report-row">
              <div>
                <IncidentLabel incidentId={report.incidentId} />
                <div style={{ fontSize: 12.5, marginTop: 2 }}>
                  <span className={`tag ${report.status === 'SUBMITTED' ? 'status-active' : 'status-inactive'}`}>
                    {report.status === 'SUBMITTED' ? '제출완료' : '작성 필요'}
                  </span>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                {report.status === 'DRAFT' && (
                  <button type="button" className="wf-btn primary small" onClick={() => navigate(`/reports/${report.reportId}`)}>
                    작성하기
                  </button>
                )}
                {report.status === 'SUBMITTED' && (
                  <button type="button" className="wf-btn small" onClick={() => navigate(`/reports/${report.reportId}/analysis`)}>
                    SOP 대조 보기
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </MobileLayout>
  )
}
