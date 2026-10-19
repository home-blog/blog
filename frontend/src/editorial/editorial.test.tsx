// 편집실 화면 (specs/018): 관리자만, 운영 현황, 검토 대기 펼치기·내림·반려, 처리 기록·잠금 풀기
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { useAuth } from '../auth/useAuth'
import { server } from '../test/server'
import EditorialPage from './EditorialPage'

function loggedIn(admin: boolean) {
  server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 1, nickname: '편집장', admin } })))
}

function Who() {
  const { member } = useAuth()
  return member ? <p>{member.nickname}님</p> : null
}

function renderAt(url = '/editorial') {
  const router = createMemoryRouter([{ path: '/editorial', element: <EditorialPage /> }], { initialEntries: [url] })
  render(
    <AuthProvider>
      <Who />
      <RouterProvider router={router} />
    </AuthProvider>,
  )
  return router
}

const SUMMARY = {
  members: 120,
  membersToday: 3,
  publicPosts: 980,
  publicPostsToday: 7,
  boardPosts: 42,
  pendingReports: 2,
}
const PENDING = {
  totalCount: 1,
  page: 1,
  totalPages: 1,
  pageSize: 20,
  items: [
    {
      postId: 9,
      blogId: 4,
      blogName: '바다 일기',
      title: '광고 글',
      visibility: 'public',
      locked: false,
      reportCount: 2,
      spam: 1,
      abuse: 0,
      adult: 0,
      other: 1,
      firstAt: '2026-10-19T01:00:00Z',
      lastAt: '2026-10-19T02:00:00Z',
    },
  ],
}
const REVIEW = {
  postId: 9,
  blogName: '바다 일기',
  title: '광고 글',
  excerpt: '싸게 팝니다',
  visibility: 'public',
  locked: false,
  lockReason: null,
  reports: [
    { reason: 'SPAM', detail: null, createdAt: '2026-10-19T01:00:00Z' },
    { reason: 'OTHER', detail: '링크만 있어요', createdAt: '2026-10-19T02:00:00Z' },
  ],
}

describe('편집실', () => {
  it('관리자가 아니면 들어올 수 없다고 알린다', async () => {
    loggedIn(false)
    renderAt()
    expect(await screen.findByRole('alert')).toHaveTextContent('관리자만')
  })

  it('운영 현황과 검토 대기를 보이고, 펼쳐서 사유를 적고 내린다', async () => {
    loggedIn(true)
    const sent: unknown[] = []
    let pending = PENDING
    server.use(
      http.get('/api/v1/editorial/summary', () => HttpResponse.json(SUMMARY)),
      http.get('/api/v1/editorial/reports/posts', () => HttpResponse.json(pending)),
      http.get('/api/v1/editorial/reports/posts/9', () => HttpResponse.json(REVIEW)),
      http.post('/api/v1/editorial/posts/9/take-down', async ({ request }) => {
        sent.push(await request.json())
        pending = { ...PENDING, totalCount: 0, items: [] }
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderAt()
    const cards = await screen.findByRole('list', { name: '운영 현황' })
    expect(within(cards).getByText('980')).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: /광고 글/ }))
    expect(await screen.findByText('링크만 있어요')).toBeInTheDocument()
    expect(screen.getByText('싸게 팝니다')).toBeInTheDocument()

    // 사유 없이 내리면 보내지 않는다
    fireEvent.click(screen.getByRole('button', { name: '내림' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('사유를')
    expect(sent).toHaveLength(0)

    fireEvent.change(screen.getByLabelText(/내림 사유/), { target: { value: '광고 링크만 있는 글' } })
    fireEvent.click(screen.getByRole('button', { name: '내림' }))
    expect(await screen.findByRole('status')).toHaveTextContent('글을 내렸습니다')
    expect(sent[0]).toEqual({ reason: '광고 링크만 있는 글' })
    expect(await screen.findByText('검토할 신고가 없습니다.')).toBeInTheDocument()
  })

  it('이미 처리한 신고면 서버 문구를 보이고 목록을 다시 읽는다', async () => {
    loggedIn(true)
    server.use(
      http.get('/api/v1/editorial/summary', () => HttpResponse.json(SUMMARY)),
      http.get('/api/v1/editorial/reports/posts', () => HttpResponse.json(PENDING)),
      http.get('/api/v1/editorial/reports/posts/9', () => HttpResponse.json(REVIEW)),
      http.post('/api/v1/editorial/posts/9/dismiss', () =>
        HttpResponse.json({ code: 'REPORT_ALREADY_RESOLVED', message: '※ 이미 처리한 신고입니다' }, { status: 409 }),
      ),
    )
    renderAt()
    fireEvent.click(await screen.findByRole('button', { name: /광고 글/ }))
    fireEvent.click(await screen.findByRole('button', { name: '반려' }))
    expect(await screen.findByRole('status')).toHaveTextContent('이미 처리한 신고입니다')
    expect(screen.queryByRole('button', { name: '반려' })).not.toBeInTheDocument()
  })

  it('처리 기록에서 내린 글의 잠금을 푼다', async () => {
    loggedIn(true)
    let unlocked = false
    server.use(
      http.get('/api/v1/editorial/summary', () => HttpResponse.json(SUMMARY)),
      http.get('/api/v1/editorial/actions', () =>
        HttpResponse.json({
          totalCount: 1,
          page: 1,
          totalPages: 1,
          pageSize: 20,
          items: [
            {
              actionId: 1,
              targetKind: 'post',
              postId: 9,
              boardPostId: null,
              title: '광고 글',
              action: 'taken_down',
              reason: '광고',
              reportCount: 2,
              createdAt: '2026-10-19T03:00:00Z',
            },
          ],
        }),
      ),
      http.post('/api/v1/editorial/posts/9/unlock', () => {
        unlocked = true
        return HttpResponse.json({ unlocked: true })
      }),
    )
    renderAt('/editorial?tab=history')
    expect(await screen.findByText('내림')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '잠금 풀기' }))
    await waitFor(() => expect(unlocked).toBe(true))
    expect(await screen.findByRole('status')).toHaveTextContent('잠금을 풀었습니다')
  })
})
