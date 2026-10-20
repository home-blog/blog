// 편집실 운영 강화 화면 (specs/022): 댓글 탭(가리기·반려), 회원 탭(찾기·정지·풀기), 처리 기록의 되돌리기, 현황 숫자
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeAll, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { server } from '../test/server'
import EditorialPage from './EditorialPage'

beforeAll(() => {
  HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
    this.removeAttribute('open')
  }
})

const SUMMARY = {
  members: 120,
  membersToday: 3,
  publicPosts: 980,
  publicPostsToday: 7,
  boardPosts: 42,
  pendingReports: 5,
  pendingCommentReports: 3,
  suspendedMembers: 1,
}

const page = <T,>(items: T[]) => ({ totalCount: items.length, page: 1, totalPages: 1, pageSize: 20, items })

function admin() {
  server.use(
    http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 1, nickname: '편집장', admin: true } })),
    http.get('/api/v1/editorial/summary', () => HttpResponse.json(SUMMARY)),
  )
}

function renderAt(url: string) {
  const router = createMemoryRouter([{ path: '/editorial', element: <EditorialPage /> }], { initialEntries: [url] })
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
}

const COMMENT_ROW = {
  commentId: 31,
  postId: 9,
  postTitle: '제주 올레',
  excerpt: '싸게 파는 곳 알려 드려요',
  writerId: 5,
  writerNickname: '광고왕',
  hidden: true,
  hiddenBy: 'auto',
  reportCount: 3,
  spam: 3,
  abuse: 0,
  adult: 0,
  other: 0,
  firstAt: '2026-10-19T01:00:00Z',
  lastAt: '2026-10-19T02:00:00Z',
}

describe('편집실 댓글 탭', () => {
  it('현황에 댓글 신고 대기와 정지 중 회원이 보인다', async () => {
    admin()
    server.use(http.get('/api/v1/editorial/reports/posts', () => HttpResponse.json(page([]))))
    renderAt('/editorial')
    const cards = await screen.findByRole('list', { name: '운영 현황' })
    expect(within(cards).getByText('댓글 신고 대기').closest('li')).toHaveTextContent('3')
    expect(within(cards).getByText('정지 중 회원').closest('li')).toHaveTextContent('1')
  })

  it('저절로 가린 댓글을 펼쳐 사유와 함께 가리면 쓴 사람에게 알린다', async () => {
    admin()
    let hidden: unknown = null
    server.use(
      http.get('/api/v1/editorial/reports/comments', () => HttpResponse.json(page([COMMENT_ROW]))),
      http.get('/api/v1/editorial/reports/comments/31', () =>
        HttpResponse.json({
          commentId: 31,
          postId: 9,
          postTitle: '제주 올레',
          body: '싸게 파는 곳 알려 드려요',
          writerNickname: '광고왕',
          hidden: true,
          hiddenBy: 'auto',
          reports: [{ reason: 'SPAM', detail: null, createdAt: '2026-10-19T01:00:00Z' }],
        }),
      ),
      http.post('/api/v1/editorial/comments/31/hide', async ({ request }) => {
        hidden = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderAt('/editorial?tab=comments')
    const toggle = await screen.findByRole('button', { name: /제주 올레/ })
    expect(toggle).toHaveTextContent('광고왕')
    expect(toggle).toHaveTextContent('저절로 가림')
    fireEvent.click(toggle)
    expect(await screen.findByText('싸게 파는 곳 알려 드려요', { selector: 'blockquote' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText(/가리기 사유/), { target: { value: '광고 댓글' } })
    fireEvent.click(screen.getByRole('button', { name: '가리기' }))
    await waitFor(() => expect(hidden).toEqual({ reason: '광고 댓글' }))
    expect(await screen.findByRole('status')).toHaveTextContent('댓글을 가렸습니다')
  })

  it('반려하면 저절로 가린 댓글이 다시 보인다고 알린다', async () => {
    admin()
    let dismissed = false
    server.use(
      http.get('/api/v1/editorial/reports/comments', () => HttpResponse.json(page([COMMENT_ROW]))),
      http.get('/api/v1/editorial/reports/comments/31', () =>
        HttpResponse.json({ ...COMMENT_ROW, body: '그냥 댓글', reports: [] }),
      ),
      http.post('/api/v1/editorial/comments/31/dismiss', () => {
        dismissed = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderAt('/editorial?tab=comments')
    fireEvent.click(await screen.findByRole('button', { name: /제주 올레/ }))
    fireEvent.click(await screen.findByRole('button', { name: '반려' }))
    expect(await screen.findByRole('status')).toHaveTextContent('반려했습니다. 댓글이 다시 보입니다')
    expect(dismissed).toBe(true)
  })

  it('처리 기록에서 가린 댓글을 되돌린다', async () => {
    admin()
    let restored = 0
    server.use(
      http.get('/api/v1/editorial/actions', () =>
        HttpResponse.json(
          page([
            {
              actionId: 1,
              targetKind: 'comment',
              postId: null,
              boardPostId: null,
              commentId: 31,
              memberId: null,
              title: '제주 올레',
              action: 'hidden',
              reason: '광고',
              reportCount: 3,
              createdAt: '2026-10-19T03:00:00Z',
            },
            {
              actionId: 2,
              targetKind: 'member',
              postId: null,
              boardPostId: null,
              commentId: null,
              memberId: 5,
              title: '광고왕',
              action: 'suspended',
              reason: '광고 반복',
              reportCount: 0,
              createdAt: '2026-10-19T03:10:00Z',
            },
          ]),
        ),
      ),
      http.post('/api/v1/editorial/comments/31/restore', () => {
        restored++
        return HttpResponse.json({ restored: true })
      }),
    )
    renderAt('/editorial?tab=history')
    const rows = await screen.findAllByRole('listitem')
    const commentRow = rows.find((r) => r.textContent?.includes('제주 올레')) as HTMLElement
    expect(commentRow).toHaveTextContent('가림')
    expect(commentRow).toHaveTextContent('댓글')
    const memberRow = rows.find((r) => r.textContent?.includes('광고왕')) as HTMLElement
    expect(memberRow).toHaveTextContent('정지')
    expect(memberRow).toHaveTextContent('회원')
    fireEvent.click(within(commentRow).getByRole('button', { name: '되돌리기' }))
    expect(await screen.findByRole('status')).toHaveTextContent('댓글을 되돌렸습니다')
    expect(restored).toBe(1)
  })
})

const MEMBERS = {
  ...page([
    {
      memberId: 5,
      nickname: '광고왕',
      blogId: 7,
      blogName: '싸게 팝니다',
      role: 'member',
      joinedAt: '2026-10-01T00:00:00Z',
      suspended: true,
      suspendedUntil: '2026-10-27T06:00:00Z',
      suspendReason: '광고 반복',
    },
    {
      memberId: 6,
      nickname: '바다',
      blogId: 8,
      blogName: '바다 일기',
      role: 'member',
      joinedAt: '2026-10-02T00:00:00Z',
      suspended: false,
      suspendedUntil: null,
      suspendReason: null,
    },
    {
      memberId: 1,
      nickname: '편집장',
      blogId: 1,
      blogName: '편집실',
      role: 'admin',
      joinedAt: '2026-09-01T00:00:00Z',
      suspended: false,
      suspendedUntil: null,
      suspendReason: null,
    },
  ]),
  suspendDays: [1, 3, 7, 30, 365],
}

describe('편집실 회원 탭', () => {
  it('닉네임으로 찾고, 정지된 회원에는 붉은 정지 도장이 찍혀 있다', async () => {
    admin()
    const queries: (string | null)[] = []
    server.use(
      http.get('/api/v1/editorial/members', ({ request }) => {
        queries.push(new URL(request.url).searchParams.get('q'))
        return HttpResponse.json(MEMBERS)
      }),
    )
    renderAt('/editorial?tab=members')
    const list = await screen.findByRole('list', { name: '회원' })
    const rows = within(list).getAllByRole('listitem')
    expect(rows).toHaveLength(3)
    expect(within(rows[0]).getByText('정지')).toHaveClass('suspend-seal')
    expect(rows[0]).toHaveTextContent('광고 반복')
    expect(within(rows[0]).getByRole('button', { name: '정지 풀기' })).toBeInTheDocument()
    // 관리자는 정지 단추가 없다
    expect(within(rows[2]).queryByRole('button', { name: /정지/ })).toBeNull()

    fireEvent.change(screen.getByRole('searchbox', { name: '닉네임' }), { target: { value: '바다' } })
    fireEvent.submit(screen.getByRole('search'))
    await waitFor(() => expect(queries).toContain('바다'))
  })

  it('기간과 사유를 골라 정지하고, 사유가 비면 보내지 않는다', async () => {
    admin()
    let sent: unknown = null
    server.use(
      http.get('/api/v1/editorial/members', () => HttpResponse.json(MEMBERS)),
      http.post('/api/v1/editorial/members/6/suspend', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json({ suspendedUntil: '2026-10-27T06:00:00Z' })
      }),
    )
    renderAt('/editorial?tab=members')
    const list = await screen.findByRole('list', { name: '회원' })
    const row = within(list).getAllByRole('listitem')[1]
    fireEvent.click(within(row).getByRole('button', { name: '정지' }))
    const dialog = screen.getByRole('dialog', { name: '바다 회원 정지' })
    fireEvent.click(within(dialog).getByRole('button', { name: '정지하기' }))
    expect(within(dialog).getByRole('alert')).toHaveTextContent('사유를')
    expect(sent).toBeNull()
    fireEvent.click(within(dialog).getByLabelText('7일'))
    fireEvent.change(within(dialog).getByLabelText(/정지 사유/), { target: { value: '광고 댓글' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '정지하기' }))
    await waitFor(() => expect(sent).toEqual({ days: 7, reason: '광고 댓글' }))
    expect(await screen.findByRole('status')).toHaveTextContent('바다 회원을 정지했습니다')
  })

  it('정지를 풀고, 서버가 거절하면 그 말을 보여 준다', async () => {
    admin()
    server.use(
      http.get('/api/v1/editorial/members', () => HttpResponse.json(MEMBERS)),
      http.post('/api/v1/editorial/members/5/unsuspend', () => HttpResponse.json({ unsuspended: true })),
      http.post('/api/v1/editorial/members/6/suspend', () =>
        HttpResponse.json(
          { code: 'MEMBER_NOT_SUSPENDABLE', message: '※ 관리자는 정지할 수 없습니다' },
          { status: 409 },
        ),
      ),
    )
    renderAt('/editorial?tab=members')
    const list = await screen.findByRole('list', { name: '회원' })
    fireEvent.click(within(within(list).getAllByRole('listitem')[0]).getByRole('button', { name: '정지 풀기' }))
    expect(await screen.findByRole('status')).toHaveTextContent('광고왕 회원의 정지를 풀었습니다')

    fireEvent.click(within(within(list).getAllByRole('listitem')[1]).getByRole('button', { name: '정지' }))
    const dialog = screen.getByRole('dialog', { name: '바다 회원 정지' })
    fireEvent.change(within(dialog).getByLabelText(/정지 사유/), { target: { value: '테스트' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '정지하기' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('관리자는 정지할 수 없습니다')
  })
})
