import { apiRequest } from './client'
import type { AccountResponse } from '../types'

// CMD-002/003: 대원 이름·소속 식별용. 목록·등록·수정은 ADMIN 전용이지만 단건 조회는 COMMANDER도 허용된다.
export function getAccount(userId: string): Promise<AccountResponse> {
  return apiRequest<AccountResponse>(`/api/v1/accounts/${userId}`)
}
