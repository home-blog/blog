// 요청 도구 (specs/001 T015): CSRF 토큰 싣기, 공통 오류 모양, 401 신호
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CSRF, server } from '../test/server'
import { api, ApiError, onUnauthenticated, resetCsrfToken } from './client'

beforeEach(() => resetCsrfToken())

describe('CSRF 토큰', () => {
  it('변경 요청(POST)에는 토큰을 머리글에 싣는다', async () => {
    let header: string | null = null
    server.use(
      http.post('/api/v1/posts', ({ request }) => {
        header = request.headers.get(CSRF.headerName)
        return HttpResponse.json({ postId: 1 }, { status: 201 })
      }),
    )
    await expect(api('/api/v1/posts', { method: 'POST', body: { title: '제목' } })).resolves.toEqual({ postId: 1 })
    expect(header).toBe(CSRF.token)
  })

  it('읽기 요청(GET)에는 토큰을 받지도 싣지도 않는다', async () => {
    const csrfCalls = vi.fn()
    let header: string | null = 'unset'
    server.use(
      http.get('/api/v1/auth/csrf', () => {
        csrfCalls()
        return HttpResponse.json(CSRF)
      }),
      http.get('/api/v1/posts', ({ request }) => {
        header = request.headers.get(CSRF.headerName)
        return HttpResponse.json({ items: [] })
      }),
    )
    await api('/api/v1/posts')
    expect(csrfCalls).not.toHaveBeenCalled()
    expect(header).toBeNull()
  })

  it('토큰은 한 번만 받아 다음 변경 요청에도 쓴다', async () => {
    const csrfCalls = vi.fn()
    server.use(
      http.get('/api/v1/auth/csrf', () => {
        csrfCalls()
        return HttpResponse.json(CSRF)
      }),
      http.delete('/api/v1/posts/:id', () => new HttpResponse(null, { status: 204 })),
    )
    await api('/api/v1/posts/1', { method: 'DELETE' })
    await api('/api/v1/posts/2', { method: 'DELETE' })
    expect(csrfCalls).toHaveBeenCalledTimes(1)
  })

  it('403 CSRF_TOKEN_INVALID를 받으면 다음 요청 때 토큰을 새로 받는다', async () => {
    const csrfCalls = vi.fn()
    let first = true
    server.use(
      http.get('/api/v1/auth/csrf', () => {
        csrfCalls()
        return HttpResponse.json(CSRF)
      }),
      http.post('/api/v1/posts', () => {
        if (first) {
          first = false
          return HttpResponse.json({ code: 'CSRF_TOKEN_INVALID', message: '다시 시도해 주세요' }, { status: 403 })
        }
        return HttpResponse.json({ postId: 2 })
      }),
    )
    await expect(api('/api/v1/posts', { method: 'POST', body: {} })).rejects.toBeInstanceOf(ApiError)
    await api('/api/v1/posts', { method: 'POST', body: {} })
    expect(csrfCalls).toHaveBeenCalledTimes(2)
  })
})

describe('오류', () => {
  it('서버의 공통 오류 모양을 ApiError로 넘기고 칸별 문구를 찾을 수 있다', async () => {
    server.use(
      http.post('/api/v1/posts', () =>
        HttpResponse.json(
          {
            code: 'VALIDATION_FAILED',
            message: '입력값을 확인해 주세요',
            fieldErrors: [{ field: 'title', code: 'TITLE_REQUIRED', message: '제목을 입력해 주세요' }],
          },
          { status: 400 },
        ),
      ),
    )
    const error = await api('/api/v1/posts', { method: 'POST', body: {} }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    const apiError = error as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.code).toBe('VALIDATION_FAILED')
    expect(apiError.messageFor('title')).toBe('제목을 입력해 주세요')
    expect(apiError.messageFor('content')).toBeUndefined()
  })

  it('예외: 본문이 JSON이 아니면 기본 문구를 쓴다', async () => {
    server.use(http.get('/api/v1/posts', () => new HttpResponse('Bad Gateway', { status: 502 })))
    const error = (await api('/api/v1/posts').catch((e: unknown) => e)) as ApiError
    expect(error.status).toBe(502)
    expect(error.code).toBe('INTERNAL_ERROR')
    expect(error.message).toBe('※ 잠시 뒤 다시 시도해 주세요')
  })

  it('401 UNAUTHENTICATED면 "로그인 필요" 신호를 낸다', async () => {
    const listener = vi.fn()
    const off = onUnauthenticated(listener)
    server.use(
      http.get('/api/v1/me/blog', () =>
        HttpResponse.json({ code: 'UNAUTHENTICATED', message: '로그인이 필요합니다' }, { status: 401 }),
      ),
    )
    await expect(api('/api/v1/me/blog')).rejects.toBeInstanceOf(ApiError)
    expect(listener).toHaveBeenCalledTimes(1)
    off()
  })

  it('notifyUnauthenticated: false면 401이어도 신호를 내지 않는다 (로그인했는지 묻기만 할 때)', async () => {
    const listener = vi.fn()
    const off = onUnauthenticated(listener)
    await expect(api('/api/v1/auth/me', { notifyUnauthenticated: false })).rejects.toBeInstanceOf(ApiError)
    expect(listener).not.toHaveBeenCalled()
    off()
  })
})

it('본문이 없는 응답(204)은 undefined', async () => {
  server.use(http.delete('/api/v1/posts/:id', () => new HttpResponse(null, { status: 204 })))
  await expect(api('/api/v1/posts/1', { method: 'DELETE' })).resolves.toBeUndefined()
})
