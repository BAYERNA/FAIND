import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest, getStoredToken, setStoredToken } from './client'

// NFR-02 "성공/실패를 명시적으로 구분"이 실제로 지켜지는지 검증하는 핵심 지점 — 이 파일이
// 깨지면 지휘관 태블릿 화면 전체의 저장/제출 피드백이 조용히 무너진다.
describe('setStoredToken/getStoredToken', () => {
  beforeEach(() => localStorage.clear())

  it('토큰을 저장하면 getStoredToken으로 다시 읽을 수 있다', () => {
    setStoredToken('abc.def.ghi')
    expect(getStoredToken()).toBe('abc.def.ghi')
  })

  it('null을 저장하면 기존 토큰이 지워진다', () => {
    setStoredToken('abc.def.ghi')
    setStoredToken(null)
    expect(getStoredToken()).toBeNull()
  })
})

describe('apiRequest', () => {
  beforeEach(() => {
    localStorage.clear()
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('저장된 토큰이 있으면 Authorization 헤더를 붙인다', async () => {
    setStoredToken('token-123')
    vi.mocked(fetch).mockResolvedValue(new Response('{}', { status: 200 }))

    await apiRequest('/api/v1/incidents')

    const [, init] = vi.mocked(fetch).mock.calls[0]
    const headers = init!.headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer token-123')
  })

  it('토큰이 없으면 Authorization 헤더를 붙이지 않는다', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('{}', { status: 200 }))

    await apiRequest('/api/v1/incidents')

    const [, init] = vi.mocked(fetch).mock.calls[0]
    const headers = init!.headers as Record<string, string>
    expect(headers.Authorization).toBeUndefined()
  })

  it('query 파라미터 중 undefined·빈문자열은 URL에서 빠진다', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('{}', { status: 200 }))

    await apiRequest('/api/v1/incidents', { query: { status: 'DISPATCHED', team: undefined, keyword: '' } })

    const [calledUrl] = vi.mocked(fetch).mock.calls[0]
    expect(calledUrl).toBe('/api/v1/incidents?status=DISPATCHED')
  })

  it('204 응답은 본문을 파싱하지 않고 undefined를 반환한다', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }))

    await expect(apiRequest('/api/v1/incidents/1')).resolves.toBeUndefined()
  })

  it('실패 응답은 서버가 준 message를 담은 ApiError를 던진다', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: 'BAD_REQUEST', message: '잘못된 요청입니다', fieldErrors: [] }), {
        status: 400,
      }),
    )

    const error = await apiRequest('/api/v1/incidents').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).message).toBe('잘못된 요청입니다')
  })

  it('본문 없는 실패 응답은 HTTP 상태코드를 담은 기본 메시지로 ApiError를 던진다', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 500 }))

    await expect(apiRequest('/api/v1/incidents')).rejects.toThrow('요청이 실패했습니다 (HTTP 500)')
  })
})
