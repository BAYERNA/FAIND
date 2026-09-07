// NFR-02: 저장/제출 성공·실패를 항상 명시적으로 표시한다.
export function Banner({ kind, message }: { kind: 'error' | 'success'; message: string }) {
  return <div className={`banner ${kind}`}>{message}</div>
}
