// 로그인 입력 (specs/001 US4, T033): 사용자가 보는 화면 기준으로 확인한다
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { resetCsrfToken } from '../api/client'
import { server } from '../test/server'
import { AuthProvider } from './AuthContext'
import LoginForm from './LoginForm'

function renderForm() {
  const onLoggedIn = vi.fn()
  render(
    <AuthProvider>
      <LoginForm idPrefix="t" onLoggedIn={onLoggedIn} />
    </AuthProvider>,
  )
  return { onLoggedIn, user: userEvent.setup() }
}

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>, email: string, password: string) {
  if (email) await user.type(screen.getByLabelText('이메일'), email)
  if (password) await user.type(screen.getByLabelText('비밀번호'), password)
  await user.click(screen.getByRole('button', { name: '로그인' }))
}

beforeEach(() => resetCsrfToken())

describe('로그인 입력', () => {
  it('정상: 맞는 이메일·비밀번호면 로그인하고 알린다', async () => {
    let sent: unknown
    server.use(
      http.post('/api/v1/auth/login', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json({ member: { id: 1, nickname: '글쓴이' } })
      }),
    )
    const { onLoggedIn, user } = renderForm()
    await fillAndSubmit(user, '  writer@example.com ', 'abc123!@')
    await waitFor(() => expect(onLoggedIn).toHaveBeenCalledTimes(1)) // 클릭이 끝나도 로그인 응답은 늦게 올 수 있다
    expect(sent).toEqual({ email: 'writer@example.com', password: 'abc123!@' }) // 이메일 앞뒤 공백은 지운다
  })

  it('예외: 빈 칸이면 서버에 보내지 않고 안내하며 빈 칸으로 옮긴다', async () => {
    const login = vi.fn()
    server.use(http.post('/api/v1/auth/login', () => login()))
    const { user } = renderForm()
    await fillAndSubmit(user, 'writer@example.com', '')
    expect(screen.getByRole('alert')).toHaveTextContent('이메일과 비밀번호를 입력해 주세요')
    expect(screen.getByLabelText('비밀번호')).toHaveFocus()
    expect(login).not.toHaveBeenCalled()
  })

  it('예외: 틀리면 서버 문구를 보여 주고 비밀번호 칸을 비운다', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json(
          { code: 'LOGIN_FAILED', message: '이메일 또는 비밀번호가 올바르지 않습니다' },
          { status: 401 },
        ),
      ),
    )
    const { onLoggedIn, user } = renderForm()
    await fillAndSubmit(user, 'writer@example.com', 'wrong-pass1!')
    expect(await screen.findByRole('alert')).toHaveTextContent('이메일 또는 비밀번호가 올바르지 않습니다')
    expect(screen.getByLabelText('비밀번호')).toHaveValue('')
    expect(onLoggedIn).not.toHaveBeenCalled()
  })

  it.each([
    ['경계: 60초는 1분', 60, '1분 뒤에'],
    ['경계: 61초는 올림해 2분', 61, '2분 뒤에'],
    ['예외: 0초여도 최소 1분', 0, '1분 뒤에'],
  ])('잠금 %s', async (_, seconds, expected) => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json(
          {
            code: 'ACCOUNT_LOCKED',
            message: '비밀번호를 5회 잘못 입력해 잠겼습니다. 10분 뒤에 다시 시도해 주세요',
            retryAfterSeconds: seconds,
          },
          { status: 423 },
        ),
      ),
    )
    const { user } = renderForm()
    await fillAndSubmit(user, 'writer@example.com', 'abc123!@')
    expect(await screen.findByRole('alert')).toHaveTextContent(expected)
  })

  it('예외: 정지된 계정이면 언제까지·사유를 정지 도장과 함께 보여 준다 (specs/022 S2-2)', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json(
          {
            code: 'ACCOUNT_SUSPENDED',
            message: '※ 2026-10-27 15:00까지 이용이 정지된 계정입니다. 사유: 광고 댓글',
            retryAfterSeconds: 604800,
          },
          { status: 403 },
        ),
      ),
    )
    const { onLoggedIn, user } = renderForm()
    await fillAndSubmit(user, 'writer@example.com', 'abc123!@')
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('2026-10-27 15:00까지 이용이 정지된 계정입니다. 사유: 광고 댓글')
    expect(alert.querySelector('.suspend-seal')).toHaveTextContent('정지')
    expect(onLoggedIn).not.toHaveBeenCalled()
  })
})
