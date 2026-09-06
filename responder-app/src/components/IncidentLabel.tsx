import { useQuery } from '@tanstack/react-query'
import { getIncident } from '../api/incidents'

// USR-002 보고서 목록에서 report.incidentId만으로는 어떤 출동인지 알 수 없어, 출동번호·주소를 곁들인다.
export function IncidentLabel({ incidentId }: { incidentId: string }) {
  const query = useQuery({ queryKey: ['incident', incidentId], queryFn: () => getIncident(incidentId) })
  if (!query.data) return <span className="alert-meta">불러오는 중…</span>
  return (
    <span className="alert-meta">
      {query.data.incidentNumber} · {query.data.address ?? '주소 정보 없음'}
    </span>
  )
}
