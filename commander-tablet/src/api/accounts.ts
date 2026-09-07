import { apiRequest } from './client'
import type { AccountResponse, Page } from '../types'

// CMD-002/003: 대원 이름·소속 식별용. 등록·수정·비활성화는 ADMIN 전용이지만 목록·단건 조회는
// COMMANDER도 허용된다 (backend AccountController 참조).
export function getAccount(userId: string): Promise<AccountResponse> {
  return apiRequest<AccountResponse>(`/api/v1/accounts/${userId}`)
}

// CMD-002 대원 배정 화면: 아직 이 출동에 배정되지 않은 RESPONDER를 이름·사번·소속으로 검색한다 (NFR-05).
export function searchResponders(keyword: string): Promise<Page<AccountResponse>> {
  return apiRequest<Page<AccountResponse>>('/api/v1/accounts', {
    query: { keyword, role: 'RESPONDER', size: 20 },
  })
}
