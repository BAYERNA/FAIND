import { apiRequest } from './client'
import type { LoginResponse } from '../types'

export function login(badgeNumber: string, password: string): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: { badgeNumber, password },
  })
}

export function setInitialPassword(newPassword: string, newPasswordConfirm: string): Promise<void> {
  return apiRequest<void>('/api/v1/auth/password/initial', {
    method: 'PATCH',
    body: { newPassword, newPasswordConfirm },
  })
}
