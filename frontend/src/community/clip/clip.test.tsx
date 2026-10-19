// 문장 수집·필사 노트 화면 (specs/015 US3)
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { useRef } from 'react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from '../../auth/AuthContext'
import { server } from '../../test/server'
import type { Clip } from './clipApi'
import { tidySelection } from './clipApi'
import CollectorCount from './CollectorCount'
import LibraryPage from './LibraryPage'
import SentenceCollector from './SentenceCollector'

function loggedIn() {
  server.use(
    http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 3, nickname: '독자', admin: false } })),
  )
}

function renderRoutes(routes: { path: string; element: React.ReactElement }[], url = routes[0].path) {
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  return render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
}

function Body() {
  const ref = useRef<HTMLDivElement>(null)
  return (
    <div ref={ref} data-testid="body">
      <p>
        오늘은 <strong>바다가 유난히</strong> 맑았다.
      </p>
      <SentenceCollector postId={12} container={ref} />
    </div>
  )
}

/** 본문의 첫 문단을 처음부터 끝까지 고른다 */
function selectFirstParagraph() {
  const paragraph = screen.getByTestId('body').querySelector('p')!
  const range = document.createRange()
  range.selectNodeContents(paragraph)
  const selection = window.getSelection()!
  selection.removeAllRanges()
  selection.addRange(range)
  fireEvent.mouseUp(screen.getByTestId('body'))
}

function clip(overrides: Partial<Clip> = {}): Clip {
  return {
    clipId: 1,
    text: '파도는 낮고',
    createdAt: '2026-10-19T03:00:00Z',
    post: { postId: 12, title: '바다 일기', blogName: '바다 블로그', available: true },
    ...overrides,
  }
}

describe('고른 글자 다듬기', () => {
  it('앞뒤 공백을 지우고 줄바꿈과 여러 칸 공백을 한 칸으로', () => {
    expect(tidySelection('  맑았다.\n\n파도는   낮고 ')).toBe('맑았다. 파도는 낮고')
  })
})

describe('문장 수집 단추', () => {
  it('본문에서 문장을 고르면 수집 단추가 뜨고, 누르면 고른 문장을 보내 필사 노트에 옮겨 적었다고 알린다', async () => {
    loggedIn()
    let sent: unknown = null
    server.use(
      http.post('/api/v1/me/clips', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json(clip({ text: '오늘은 바다가 유난히 맑았다.' }), { status: 201 })
      }),
    )
    renderRoutes([{ path: '/posts/12', element: <Body /> }])
    await screen.findByText('맑았다.', { exact: false })
    await act(async () => {}) // 로그인 상태를 읽을 때까지

    selectFirstParagraph()
    fireEvent.click(await screen.findByRole('button', { name: /수집/ }))

    expect(await screen.findByText('필사 노트에 옮겨 적었어요')).toBeInTheDocument()
    expect(sent).toEqual({ postId: 12, text: '오늘은 바다가 유난히 맑았다.' })
    expect(screen.getByRole('link', { name: '필사 노트 보기' })).toHaveAttribute('href', '/mypage/library')
    expect(screen.queryByRole('button', { name: /수집/ })).toBeNull()
  })

  it('서버가 거절하면 그 문구를 보여 준다', async () => {
    loggedIn()
    server.use(
      http.post('/api/v1/me/clips', () =>
        HttpResponse.json(
          { code: 'CLIP_LIMIT_EXCEEDED', message: '※ 필사 노트에는 문장을 1000개까지 모을 수 있습니다' },
          { status: 409 },
        ),
      ),
    )
    renderRoutes([{ path: '/posts/12', element: <Body /> }])
    await act(async () => {})
    selectFirstParagraph()
    fireEvent.click(await screen.findByRole('button', { name: /수집/ }))
    expect(await screen.findByText(/1000개까지/)).toBeInTheDocument()
  })

  it('본문을 고른 뒤 본문 밖의 글자를 고르면 단추를 거둔다', async () => {
    loggedIn()
    render(
      <AuthProvider>
        <RouterProvider
          router={createMemoryRouter([
            {
              path: '/',
              element: (
                <>
                  <Body />
                  <p data-testid="outside">댓글 글자</p>
                </>
              ),
            },
          ])}
        />
      </AuthProvider>,
    )
    selectFirstParagraph()
    expect(await screen.findByRole('button', { name: /수집/ })).toBeInTheDocument()

    const range = document.createRange()
    range.selectNodeContents(screen.getByTestId('outside'))
    const selection = window.getSelection()!
    selection.removeAllRanges()
    selection.addRange(range)
    fireEvent(document, new Event('selectionchange'))
    expect(screen.queryByRole('button', { name: /수집/ })).toBeNull()
  })

  it('아무것도 고르지 않으면 단추가 없다', async () => {
    renderRoutes([{ path: '/posts/12', element: <Body /> }])
    window.getSelection()?.removeAllRanges()
    fireEvent.mouseUp(screen.getByTestId('body'))
    expect(screen.queryByRole('button', { name: /수집/ })).toBeNull()
  })
})

describe('필사 노트', () => {
  it('모은 문장을 원고지 카드로, 볼 수 없게 된 원문은 "볼 수 없는 글"로 보여 준다', async () => {
    loggedIn()
    server.use(
      http.get('/api/v1/me/clips', () =>
        HttpResponse.json({
          items: [
            clip(),
            clip({
              clipId: 2,
              text: '곧 사라질',
              post: { postId: null, title: '지운 글', blogName: '바다 블로그', available: false },
            }),
          ],
          page: 1,
          totalPages: 1,
          totalCount: 2,
        }),
      ),
    )
    renderRoutes([{ path: '/mypage/library', element: <LibraryPage /> }])
    const cards = within(await screen.findByRole('list', { name: '모은 문장' })).getAllByRole('listitem')
    expect(cards).toHaveLength(2)
    expect(within(cards[0]).getByText('파도는 낮고')).toBeInTheDocument()
    expect(within(cards[0]).getByRole('link', { name: '원문 보기' })).toHaveAttribute('href', '/posts/12')
    expect(within(cards[1]).getByText('볼 수 없는 글')).toBeInTheDocument()
    expect(within(cards[1]).queryByRole('link', { name: '원문 보기' })).toBeNull()
  })

  it('지우면 목록을 다시 읽는다', async () => {
    loggedIn()
    let items = [clip()]
    server.use(
      http.get('/api/v1/me/clips', () =>
        HttpResponse.json({ items, page: 1, totalPages: 1, totalCount: items.length }),
      ),
      http.delete('/api/v1/me/clips/1', () => {
        items = []
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderRoutes([{ path: '/mypage/library', element: <LibraryPage /> }])
    fireEvent.click(await screen.findByRole('button', { name: '지우기' }))
    expect(await screen.findByText('글 본문에서 문장을 끌어 고르면 수집 단추가 나타납니다.')).toBeInTheDocument()
  })
})

describe('모은 사람 수 (글 주인)', () => {
  it('모은 사람이 있으면 수를 보여 준다', async () => {
    server.use(http.get('/api/v1/posts/12/clips/collectors', () => HttpResponse.json({ collectors: 3 })))
    renderRoutes([{ path: '/', element: <CollectorCount postId={12} /> }])
    expect(await screen.findByText(/3명이 이 글에서 문장을 수집했어요/)).toBeInTheDocument()
  })

  it('아무도 모으지 않았으면 그리지 않는다', async () => {
    server.use(http.get('/api/v1/posts/12/clips/collectors', () => HttpResponse.json({ collectors: 0 })))
    const { container } = renderRoutes([{ path: '/', element: <CollectorCount postId={12} /> }])
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(container.querySelector('.clip-collectors')).toBeNull()
  })
})
