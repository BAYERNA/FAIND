export function StatCard({ value, label, alert }: { value: string | number; label: string; alert?: boolean }) {
  return (
    <div className="stat-card" style={alert ? { borderColor: 'var(--color-alert)' } : undefined}>
      <div className={`n${alert ? ' alert' : ''}`}>{value}</div>
      <div className="l">{label}</div>
    </div>
  )
}
