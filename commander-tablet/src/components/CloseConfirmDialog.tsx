// CMD-006 출동 종료 처리 (FR-05). QA 최우선 재검증 대상 — "종료했는데 알림/보고서가 안 생김" 결함의
// 재발 방지를 위해, 여기서는 확정 여부만 명시적으로 묻고 실제 부수효과(사후보고서 생성, 알림 발송)는
// backend IncidentService.close()가 같은 트랜잭션에서 보장한다.
export function CloseConfirmDialog({
  incidentNumber,
  onConfirm,
  onCancel,
  submitting,
}: {
  incidentNumber: string
  onConfirm: () => void
  onCancel: () => void
  submitting: boolean
}) {
  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-panel">
        <h2>출동 종료 확정</h2>
        <p style={{ fontSize: 13, color: 'var(--color-ink-soft)' }}>
          {incidentNumber} 출동을 종료 처리합니다. 종료 확정 시 배정된 모든 대원에게 사후보고서 작성 알림이
          발송되며, 이후 되돌릴 수 없습니다.
        </p>
        <div className="form-actions">
          <button type="button" className="wf-btn" onClick={onCancel} disabled={submitting}>
            취소
          </button>
          <button type="button" className="wf-btn primary" onClick={onConfirm} disabled={submitting}>
            {submitting ? '처리 중…' : '종료 확정'}
          </button>
        </div>
      </div>
    </div>
  )
}
