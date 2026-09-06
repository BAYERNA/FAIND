import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

// CMN-001 annot#2: 최초 로그인 시 CMN-002로 강제 이동 (is_initial_password = true)
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (user.initialPassword && location.pathname !== '/initial-password') {
    return <Navigate to="/initial-password" replace />
  }

  if (user.role !== 'COMMANDER') {
    return (
      <div style={{ padding: 32, fontFamily: 'var(--font-sans)' }}>
        <h2>접근 권한이 없습니다</h2>
        <p>지휘관 태블릿은 COMMANDER 역할 계정만 사용할 수 있습니다.</p>
      </div>
    )
  }

  return <>{children}</>
}
