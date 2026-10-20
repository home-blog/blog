// 마이페이지 "AI 비서 연결" (specs/020 US1, T009·T010): 열쇠 목록, 만들기(원문은 한 번만), 끊기, 앱마다 붙이는 법
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import AiConnectCard from './AiConnectCard'
import type { AccessKey, AccessKeyList } from './connectApi'

const TOKEN = 'gk_abcdEFGH1234567890abcdEFGH1234567890abcde'
const FUTURE = '2099-01-01T00:00:00Z'

function key(overrides: Partial<AccessKey> = {}): AccessKey {
  return {
    keyId: 1,
    name: 'Claude Code',
    hint: 'gk_Zx9Q1',
    scope: 'read',
    createdAt: '2026-10-01T00:00:00Z',
    expiresAt: FUTURE,
    lastUsedAt: null,
    ...overrides,
  }
}

function serve(list: Partial<AccessKeyList> = {}) {
  const body: AccessKeyList = { items: [], maxCount: 5, allowedDays: [30, 90, 365], ...list }
  server.use(http.get('/api/v1/me/access-keys', () => HttpResponse.json(body)))
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('AI 비서 연결', () => {
  it('열쇠마다 이름·힌트·할 수 있는 일·기한을 보여 주고, 지난 열쇠는 기한 지남으로 표시한다', async () => {
    serve({
      items: [
        key({ keyId: 2, name: '노트북 Claude', scope: 'draft', lastUsedAt: '2026-10-19T03:00:00Z' }),
        key({ keyId: 1, name: '옛 열쇠', hint: 'gk_old00', expiresAt: '2026-01-01T00:00:00Z' }),
      ],
    })
    render(<AiConnectCard />)
    const list = await screen.findByRole('list', { name: '내 열쇠' })
    const rows = within(list).getAllByRole('listitem')
    expect(rows).toHaveLength(2)
    expect(rows[0]).toHaveTextContent('노트북 Claude')
    expect(rows[0]).toHaveTextContent('gk_Zx9Q1…')
    expect(rows[0]).toHaveTextContent('읽기 + 초안')
    expect(rows[0]).not.toHaveTextContent('아직 안 씀')
    expect(rows[1]).toHaveTextContent('읽기만')
    expect(rows[1]).toHaveTextContent('기한 지남')
    expect(rows[1]).toHaveTextContent('아직 안 씀')
    // 붙이는 법은 열쇠 자리를 비워 둔 채 늘 보인다
    expect(screen.getByText(/claude mcp add --transport http geulgyeol/)).toHaveTextContent(
      `${window.location.origin}/api/v1/mcp`,
    )
    expect(screen.getByText(/claude mcp add/)).toHaveTextContent('Bearer <열쇠>')
  })

  it('열쇠를 만들면 원문을 한 번만 보여 주고, 붙이는 법에도 채워 넣으며, 다 적어 두면 사라진다', async () => {
    serve()
    let sent: unknown = null
    server.use(
      http.post('/api/v1/me/access-keys', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json(
          {
            keyId: 9,
            name: '데스크톱',
            hint: TOKEN.slice(0, 8),
            scope: 'draft',
            createdAt: '2026-10-20T00:00:00Z',
            expiresAt: FUTURE,
            token: TOKEN,
          },
          { status: 201 },
        )
      }),
    )
    const user = userEvent.setup()
    render(<AiConnectCard />)
    await user.type(await screen.findByLabelText('열쇠 이름'), '  데스크톱 ')
    await user.click(screen.getByRole('radio', { name: /읽기 \+ 초안 남기기/ }))
    await user.selectOptions(screen.getByLabelText('기한'), '90')
    await user.click(screen.getByRole('button', { name: '열쇠 만들기' }))

    const issued = await screen.findByRole('region', { name: '새 열쇠' })
    expect(sent).toEqual({ name: '데스크톱', scope: 'draft', days: 90 })
    expect(within(issued).getByText(TOKEN)).toBeInTheDocument()
    expect(within(issued).getByText(/지금 한 번만 보입니다/)).toBeInTheDocument()
    expect(screen.getByText(/claude mcp add/)).toHaveTextContent(`Bearer ${TOKEN}`)
    expect(screen.getByLabelText('열쇠 이름')).toHaveValue('')
    expect(within(screen.getByRole('list', { name: '내 열쇠' })).getAllByRole('listitem')).toHaveLength(1)

    await user.click(within(issued).getByRole('button', { name: '다 적어 두었어요' }))
    expect(screen.queryByRole('region', { name: '새 열쇠' })).toBeNull()
    expect(screen.queryByText(TOKEN)).toBeNull()
    expect(screen.getByText(/claude mcp add/)).toHaveTextContent('Bearer <열쇠>')
  })

  it('이름이 비면 보내지 않고, 서버가 거절하면 그 말을 보여 준다', async () => {
    serve()
    let posts = 0
    server.use(
      http.post('/api/v1/me/access-keys', () => {
        posts++
        return HttpResponse.json(
          { code: 'ACCESS_KEY_LIMIT', message: '※ 열쇠는 5개까지 만들 수 있습니다. 쓰지 않는 열쇠를 끊어 주세요' },
          { status: 409 },
        )
      }),
    )
    const user = userEvent.setup()
    render(<AiConnectCard />)
    const name = await screen.findByLabelText('열쇠 이름')
    await user.type(name, '   ')
    await user.click(screen.getByRole('button', { name: '열쇠 만들기' }))
    expect(await screen.findByText('※ 열쇠 이름을 적어 주세요')).toBeInTheDocument()
    expect(name).toHaveAttribute('aria-invalid', 'true')
    expect(posts).toBe(0)

    await user.type(name, '두 번째')
    await user.click(screen.getByRole('button', { name: '열쇠 만들기' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('5개까지')
    expect(posts).toBe(1)
  })

  it('열쇠가 가득 차면 만들기를 막고 끊으라고 알려 준다', async () => {
    serve({ items: [1, 2, 3].map((id) => key({ keyId: id, name: `열쇠 ${id}` })), maxCount: 3 })
    render(<AiConnectCard />)
    expect(await screen.findByRole('button', { name: '열쇠 만들기' })).toBeDisabled()
    expect(screen.getByText(/3개까지 만들 수 있어요/)).toBeInTheDocument()
  })

  it('끊기는 한 번 더 묻고, 끊으면 목록에서 빠진다. 그만두면 그대로다', async () => {
    serve({ items: [key({ keyId: 3, name: '회사 노트북' }), key({ keyId: 4, name: '집' })] })
    const deleted: string[] = []
    server.use(
      http.delete('/api/v1/me/access-keys/:id', ({ params }) => {
        deleted.push(String(params.id))
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const user = userEvent.setup()
    render(<AiConnectCard />)
    await user.click(await screen.findByRole('button', { name: '집 끊기' }))
    const confirm = screen.getByRole('group', { name: '집 열쇠를 끊을까요?' })
    await user.click(within(confirm).getByRole('button', { name: '그만두기' }))
    expect(deleted).toEqual([])
    expect(within(screen.getByRole('list', { name: '내 열쇠' })).getAllByRole('listitem')).toHaveLength(2)

    await user.click(screen.getByRole('button', { name: '회사 노트북 끊기' }))
    await user.click(
      within(screen.getByRole('group', { name: '회사 노트북 열쇠를 끊을까요?' })).getByRole('button', {
        name: '끊기',
      }),
    )
    expect(await screen.findByText('회사 노트북 열쇠를 끊었습니다')).toHaveAttribute('role', 'status')
    expect(deleted).toEqual(['3'])
    const rows = within(screen.getByRole('list', { name: '내 열쇠' })).getAllByRole('listitem')
    expect(rows).toHaveLength(1)
    expect(rows[0]).toHaveTextContent('집')
  })

  it('복사 단추는 열쇠를 클립보드에 넣고, 앱마다 붙이는 법을 탭으로 바꿔 본다', async () => {
    serve({ items: [] })
    server.use(
      http.post('/api/v1/me/access-keys', () =>
        HttpResponse.json(
          { ...key({ keyId: 5, name: 'Desktop' }), token: TOKEN, lastUsedAt: undefined },
          { status: 201 },
        ),
      ),
    )
    const user = userEvent.setup()
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
    render(<AiConnectCard />)
    await user.type(await screen.findByLabelText('열쇠 이름'), 'Desktop')
    await user.click(screen.getByRole('button', { name: '열쇠 만들기' }))
    const issued = await screen.findByRole('region', { name: '새 열쇠' })
    await user.click(within(issued).getByRole('button', { name: '열쇠 복사' }))
    expect(writeText).toHaveBeenCalledWith(TOKEN)
    expect(within(issued).getByRole('button', { name: '열쇠 복사' })).toHaveTextContent('복사했어요')
    // 화면 읽기 도구에도 알린다
    expect(screen.getByText('열쇠: 복사했어요')).toHaveAttribute('role', 'status')

    await user.click(screen.getByRole('tab', { name: 'Claude Desktop' }))
    expect(screen.getByRole('tab', { name: 'Claude Desktop' })).toHaveAttribute('aria-selected', 'true')
    const panel = screen.getByRole('tabpanel')
    expect(panel).toHaveTextContent('mcp-remote')
    expect(panel).toHaveTextContent(`Bearer ${TOKEN}`)
    await user.click(within(panel).getByRole('button', { name: '설정 복사' }))
    expect(writeText).toHaveBeenLastCalledWith(expect.stringContaining('"mcpServers"'))

    await user.click(screen.getByRole('tab', { name: '다른 앱' }))
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Streamable HTTP')
  })

  it('불러오지 못하면 다시 시도할 수 있다', async () => {
    let calls = 0
    server.use(
      http.get('/api/v1/me/access-keys', () => {
        calls++
        return calls === 1
          ? HttpResponse.json({ code: 'INTERNAL', message: '잠시 뒤' }, { status: 500 })
          : HttpResponse.json({ items: [key()], maxCount: 5, allowedDays: [30] })
      }),
    )
    const user = userEvent.setup()
    render(<AiConnectCard />)
    expect(await screen.findByRole('alert')).toHaveTextContent('열쇠를 불러오지 못했습니다')
    await user.click(screen.getByRole('button', { name: '다시 시도' }))
    expect(await screen.findByRole('list', { name: '내 열쇠' })).toBeInTheDocument()
  })
})
