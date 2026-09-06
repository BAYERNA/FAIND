import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './auth/RequireAuth'
import { LoginPage } from './pages/LoginPage'
import { SetInitialPasswordPage } from './pages/SetInitialPasswordPage'
import { ResponderHomePage } from './pages/ResponderHomePage'
import { ReportListPage } from './pages/ReportListPage'
import { ReportEditPage } from './pages/ReportEditPage'
import { ReportAnalysisPage } from './pages/ReportAnalysisPage'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/initial-password"
              element={
                <RequireAuth>
                  <SetInitialPasswordPage />
                </RequireAuth>
              }
            />
            <Route
              path="/"
              element={
                <RequireAuth>
                  <ResponderHomePage />
                </RequireAuth>
              }
            />
            <Route
              path="/reports"
              element={
                <RequireAuth>
                  <ReportListPage />
                </RequireAuth>
              }
            />
            <Route
              path="/reports/:reportId"
              element={
                <RequireAuth>
                  <ReportEditPage />
                </RequireAuth>
              }
            />
            <Route
              path="/reports/:reportId/analysis"
              element={
                <RequireAuth>
                  <ReportAnalysisPage />
                </RequireAuth>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
