// 가짜 서버 (MSW): 테스트마다 server.use(...)로 필요한 응답만 정한다.
// CSRF 토큰과 "로그인 안 함"은 거의 모든 화면이 처음에 부르므로 기본으로 둔다
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

export const CSRF = { headerName: 'X-XSRF-TOKEN', token: 'test-token' }

export const defaultHandlers = [
  http.get('/api/v1/auth/csrf', () => HttpResponse.json(CSRF)),
  http.get('/api/v1/auth/me', () =>
    HttpResponse.json({ code: 'UNAUTHENTICATED', message: '로그인이 필요합니다' }, { status: 401 }),
  ),
]

export const server = setupServer(...defaultHandlers)
