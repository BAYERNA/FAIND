import type { LabeledCount } from '../types'

// 와이어프레임의 단순 막대 스타일(.wf-bars)을 그대로 재현한다. 값이 0건이어도 막대 자체는
// 항상 렌더링해 "데이터 없음"과 "차트 자체가 깨짐"을 시각적으로 구분한다.
export function BarChart({ data, label }: { data: LabeledCount[]; label: string }) {
  const max = Math.max(1, ...data.map((d) => d.count))

  return (
    <div className="wf-chart">
      <div className="chart-label">{label}</div>
      {data.length === 0 ? (
        <div className="spinner-text">데이터가 없습니다.</div>
      ) : (
        <div className="wf-bars">
          {data.map((d) => (
            <div key={d.label} className="wf-bar-col" title={`${d.label}: ${d.count}건`}>
              <div className="wf-bar" style={{ height: `${Math.max(4, (d.count / max) * 100)}%` }} />
              <div className="wf-bar-label">{d.label}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
