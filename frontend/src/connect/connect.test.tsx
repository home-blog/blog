// 위임장 (specs/023 US1, T004): 로그인 먼저, 앱 이름·돌아갈 곳·범위, 허락하면 인장 뒤 앱으로, 거절, 오류
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { LoginPromptContext } from '../auth/loginPrompt'
import { server } from '../test/server'
import ConnectAuthorizePage from './ConnectAuthorizePage'
import type { ConnectRequest } from './oauthApi'

const REQUEST_ID = 'AbCdEfGhIjKlMnOpQrStUvWxYz012345'

function loggedIn() {
  server.use(
    http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 3, nickname: '먹물', admin: false } })),
  )
}

function serveRequest(over: Partial<ConnectRequest> = {}) {
  server.use(
    http.get(`/api/v1/oauth/requests/${REQUEST_ID}`, () =>
      HttpResponse.json({
        clientName: 'Claude',
        redirectHost: 'claude.ai',
        scope: null,
        expiresAt: '2099-01-01T00:00:00Z',
        ...over,
      }),
    ),
  )
}

function renderPage(
  url = `/connect/authorize?request=${REQUEST_ID}`,
  go = vi.fn(),
  openLogin = vi.fn(),
  framed = false,
) {
  const router = createMemoryRouter(
    [{ path: '/connect/authorize', element: <ConnectAuthorizePage go={go} framed={framed} /> }],
    { initialEntries: [url] },
  )
  render(
    <LoginPromptContext.Provider value={{ open: openLogin }}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </LoginPromptContext.Provider>,
  )
  return { go, openLogin }
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('위임장', () => {
  it('로그인하지 않았으면 로그인 창을 먼저 띄운다', async () => {
    serveRequest()
    const { openLogin } = renderPage()
    expect(await screen.findByText('위임장을 보려면 먼저 로그인해 주세요.')).toBeInTheDocument()
    await waitFor(() => expect(openLogin).toHaveBeenCalled())
    expect(screen.queryByText('Claude')).toBeNull()
  })

  it('누가·누구의·무엇을·언제까지를 원고지 칸 표로 보이고, 허락하면 고른 범위로 허락한 뒤 앱으로 돌아간다', async () => {
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: query.includes('reduce'), media: query }))
    loggedIn()
    serveRequest()
    let sent: unknown = null
    server.use(
      http.post(`/api/v1/oauth/requests/${REQUEST_ID}/approve`, async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json({ redirectTo: 'https://claude.ai/api/mcp/auth_callback?code=c&state=s' })
      }),
    )
    const { go } = renderPage()
    expect(await screen.findByRole('heading', { name: '위임장' })).toBeInTheDocument()
    expect(await screen.findByText('Claude')).toBeInTheDocument()
    expect(screen.getByText('claude.ai')).toHaveClass('warrant-host')
    expect(screen.getByText('Claude 공식')).toBeInTheDocument()
    expect(screen.queryByText('확인되지 않은 앱')).toBeNull()
    expect(screen.getByText('먹물')).toBeInTheDocument()
    // 앱이 범위를 정하지 않았으면 읽기만이 먼저 골라져 있다
    expect(screen.getByRole('radio', { name: /읽기만/ })).toBeChecked()
    fireEvent.click(screen.getByRole('radio', { name: /읽기 \+ 초안 남기기/ }))
    fireEvent.click(screen.getByRole('button', { name: '허락하고 인장 찍기' }))
    await waitFor(() => expect(go).toHaveBeenCalledWith('https://claude.ai/api/mcp/auth_callback?code=c&state=s'))
    expect(sent).toEqual({ scope: 'draft' })
    expect(screen.getByRole('status')).toHaveTextContent('Claude(으)로 돌아가는 중')
    expect(screen.getByRole('link', { name: '넘어가지 않으면 여기' })).toHaveAttribute(
      'href',
      'https://claude.ai/api/mcp/auth_callback?code=c&state=s',
    )
    expect(screen.getByRole('button', { name: '허락하고 인장 찍기' })).toBeDisabled()
  })

  it('움직임이 있으면 인장이 찍힌 뒤에 앱으로 간다', async () => {
    loggedIn()
    serveRequest()
    server.use(
      http.post(`/api/v1/oauth/requests/${REQUEST_ID}/approve`, () =>
        HttpResponse.json({ redirectTo: 'https://claude.ai/cb?code=c' }),
      ),
    )
    const { go } = renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '허락하고 인장 찍기' }))
    await waitFor(() => expect(document.querySelector('.warrant.is-sealed')).not.toBeNull())
    expect(go).not.toHaveBeenCalled()
    await waitFor(() => expect(go).toHaveBeenCalledWith('https://claude.ai/cb?code=c'), { timeout: 3000 })
  })

  it('앱이 읽기만 요청했으면 초안은 고를 수 없다', async () => {
    loggedIn()
    serveRequest({ scope: 'read' })
    renderPage()
    expect(await screen.findByRole('radio', { name: /읽기 \+ 초안 남기기/ })).toBeDisabled()
    expect(screen.getByText('이 앱은 읽기만 요청했어요')).toBeInTheDocument()
  })

  it('앱이 초안까지 요청해도 읽기만이 먼저 골라져 있고, 초안은 회원이 골라야 한다', async () => {
    loggedIn()
    serveRequest({ scope: 'draft' })
    renderPage()
    const draft = await screen.findByRole('radio', { name: /읽기 \+ 초안 남기기/ })
    expect(screen.getByRole('radio', { name: /읽기만/ })).toBeChecked()
    expect(draft).not.toBeChecked()
    expect(draft).toBeEnabled()
    fireEvent.click(draft)
    expect(draft).toBeChecked()
  })

  it('Claude 공식 주소가 아니면 돌아갈 주소를 크게 보이고 확인되지 않은 앱이라고 알린다', async () => {
    loggedIn()
    // 앱 이름은 앱이 스스로 적는다: 이름이 Claude여도 주소가 다르면 공식이 아니다
    serveRequest({ clientName: 'Claude', redirectHost: 'claude.ai.evil.example' })
    renderPage()
    expect(await screen.findByText('claude.ai.evil.example')).toHaveClass('warrant-host')
    expect(screen.getByText('확인되지 않은 앱')).toBeInTheDocument()
    expect(screen.queryByText('Claude 공식')).toBeNull()
  })

  it('다른 사이트 안(틀)에 띄워졌으면 허락 단추를 보이지 않는다', async () => {
    loggedIn()
    serveRequest()
    renderPage(undefined, vi.fn(), vi.fn(), true)
    expect(await screen.findByRole('alert')).toHaveTextContent('다른 사이트 안에서는 위임장을 열 수 없어요')
    expect(screen.queryByRole('button', { name: '허락하고 인장 찍기' })).toBeNull()
    expect(screen.getByRole('link', { name: /새 창에서 열기/ })).toHaveAttribute(
      'href',
      `/connect/authorize?request=${REQUEST_ID}`,
    )
  })

  it('거절하면 앱으로 거절을 알린다', async () => {
    loggedIn()
    serveRequest()
    server.use(
      http.post(`/api/v1/oauth/requests/${REQUEST_ID}/deny`, () =>
        HttpResponse.json({ redirectTo: 'https://claude.ai/cb?error=access_denied' }),
      ),
    )
    const { go } = renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '거절' }))
    await waitFor(() => expect(go).toHaveBeenCalledWith('https://claude.ai/cb?error=access_denied'))
    // 거절에는 허락 인장을 찍지 않는다
    expect(document.querySelector('.warrant.is-sealed')).toBeNull()
  })

  it('시간이 지난 요청과 모르는 앱은 알려 주기만 한다', async () => {
    loggedIn()
    server.use(
      http.get(`/api/v1/oauth/requests/${REQUEST_ID}`, () =>
        HttpResponse.json({ code: 'OAUTH_REQUEST_NOT_FOUND', message: '※ 연결 요청이 없거나' }, { status: 404 }),
      ),
    )
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('시간이 지난 위임장이에요')
    expect(screen.queryByRole('button', { name: '허락하고 인장 찍기' })).toBeNull()
  })

  it('앱이 보낸 invalid_client는 로그인하지 않아도 보인다', async () => {
    const { openLogin } = renderPage('/connect/authorize?error=invalid_client')
    expect(await screen.findByRole('alert')).toHaveTextContent('알 수 없는 앱이거나')
    expect(openLogin).not.toHaveBeenCalled()
  })

  it('위임장 전에 난 오류는 앱으로 바로 보내지 않고, 회원이 눌러야 서버가 만든 주소로 돌아간다', async () => {
    server.use(
      http.get('/api/v1/oauth/errors/back123', () =>
        HttpResponse.json({
          clientName: 'Claude',
          redirectHost: 'claude.ai',
          error: 'invalid_scope',
          redirectTo: 'https://claude.ai/api/mcp/auth_callback?error=invalid_scope&state=s',
        }),
      ),
    )
    const { go, openLogin } = renderPage('/connect/authorize?error=invalid_scope&back=back123')
    const link = await screen.findByRole('link', { name: '앱으로 돌아가기 (claude.ai)' })
    expect(link).toHaveAttribute('href', 'https://claude.ai/api/mcp/auth_callback?error=invalid_scope&state=s')
    expect(screen.getByRole('alert')).toHaveTextContent('글결에 없는 권한')
    expect(screen.getByText('Claude')).toBeInTheDocument()
    expect(go).not.toHaveBeenCalled()
    expect(openLogin).not.toHaveBeenCalled()
  })

  it('오류 번호가 없거나 지났으면 돌아갈 길 없이 알려 주기만 한다', async () => {
    server.use(
      http.get('/api/v1/oauth/errors/gone', () =>
        HttpResponse.json({ code: 'OAUTH_REQUEST_NOT_FOUND', message: '※ 없음' }, { status: 404 }),
      ),
    )
    renderPage('/connect/authorize?error=invalid_request&back=gone')
    expect(await screen.findByRole('alert')).toHaveTextContent('앱이 보낸 연결 요청이 규칙에 맞지 않아요')
    await waitFor(() => expect(screen.queryByText('오류를 읽는 중…')).toBeNull())
    expect(screen.queryByRole('link', { name: /앱으로 돌아가기/ })).toBeNull()
  })

  it('허락하다 서버가 거절하면 그 말을 보이고 다시 누를 수 있다', async () => {
    loggedIn()
    serveRequest()
    server.use(
      http.post(`/api/v1/oauth/requests/${REQUEST_ID}/approve`, () =>
        HttpResponse.json({ code: 'SERVICE_UNAVAILABLE', message: '※ 잠시 뒤 다시 시도해 주세요' }, { status: 503 }),
      ),
    )
    const { go } = renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '허락하고 인장 찍기' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('잠시 뒤 다시 시도해 주세요')
    expect(screen.getByRole('button', { name: '허락하고 인장 찍기' })).toBeEnabled()
    expect(go).not.toHaveBeenCalled()
  })
})
