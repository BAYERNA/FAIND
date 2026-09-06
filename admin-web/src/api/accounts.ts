import { apiRequest } from './client'
import type { AccountCreatedResponse, AccountResponse, Page } from '../types'

export interface AccountListParams {
  keyword?: string
  role?: string
  page?: number
  size?: number
}

// NFR-05: 검색·필터가 실제 쿼리 파라미터로 나가는지가 이 함수의 존재 이유 (ADM-002 QA 재검증 대상).
export function listAccounts(params: AccountListParams): Promise<Page<AccountResponse>> {
  return apiRequest<Page<AccountResponse>>('/api/v1/accounts', {
    query: { keyword: params.keyword, role: params.role, page: params.page, size: params.size },
  })
}

export function getAccount(userId: string): Promise<AccountResponse> {
  return apiRequest<AccountResponse>(`/api/v1/accounts/${userId}`)
}

export interface AccountFormInput {
  name: string
  role: string
  badgeNumber?: string
  team?: string
  phone?: string
}

export function registerAccount(input: AccountFormInput): Promise<AccountCreatedResponse> {
  return apiRequest<AccountCreatedResponse>('/api/v1/accounts', { method: 'POST', body: input })
}

export function updateAccount(userId: string, input: AccountFormInput): Promise<AccountResponse> {
  return apiRequest<AccountResponse>(`/api/v1/accounts/${userId}`, { method: 'PUT', body: input })
}

export function reissuePassword(userId: string): Promise<AccountCreatedResponse> {
  return apiRequest<AccountCreatedResponse>(`/api/v1/accounts/${userId}/password/reissue`, { method: 'PATCH' })
}

export function deactivateAccount(userId: string): Promise<void> {
  return apiRequest<void>(`/api/v1/accounts/${userId}`, { method: 'DELETE' })
}
