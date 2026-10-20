// 구독·알림 화면 (specs/016): 받아 보기 단추, 우편함, 원고 편지 그만 받기, 받아 보는 글
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { useAuth } from '../auth/useAuth'
import { server } from '../test/server'
import LetterStopPage from './LetterStopPage'
import Mailbox from './Mailbox'
import SubscribeButton from './SubscribeButton'
import SubscribedPage from './SubscribedPage'
import type { Notice } from './subscriptionApi'

function loggedIn() {
  server.use(
    http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 3, nickname: '독자', admin: false } })),
  )
}

/** 로그인 상태를 다 읽었는지 보이게 하는 표시 */
function Who() {
  const { member } = useAuth()
  return member ? <p>{member.nickname}님</p> : null
}

function renderAt(routes: { path: string; element: React.ReactElement }[], url = routes[0].path) {
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  render(
    <AuthProvider>
      <Who />
      <RouterProvider router={router} />
    </AuthProvider>,
  )
  return router
}

function notice(overrides: Partial<Notice> = {}): Notice {
  return {
    notificationId: 1,
    kind: 'post',
    postId: 12,
    boardPostId: null,
    postTitle: '바다 일기',
    actorName: '바다 블로그',
    available: true,
    read: false,
    createdAt: '2026-10-19T03:00:00Z',
    ...overrides,
  }
}

describe('원고 받아 보기 단추', () => {
  it('누르면 받아 보는 중이 되고 수가 오르며, 메일로도 받기를 고를 수 있다', async () => {
    loggedIn()
    const sent: unknown[] = []
    server.use(
      http.put('/api/v1/blogs/7/subscription', async ({ request }) => {
        const body = (await request.json()) as { emailEnabled: boolean }
        sent.push(body)
        return HttpResponse.json({ subscribed: true, emailEnabled: body.emailEnabled, subscriberCount: 4 })
      }),
    )
    renderAt([
      {
        path: '/blog/7',
        element: <SubscribeButton blogId={7} isOwner={false} initial={{ subscriberCount: 3, subscribedByMe: false }} />,
      },
    ])
    await screen.findByText('독자님') // 로그인 상태를 읽을 때까지
    fireEvent.click(screen.getByRole('button', { name: /원고 받아 보기/ }))
    expect(await screen.findByRole('button', { name: /받아 보는 중/ })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('4')).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('새 원고를 메일로도 받기'))
    await waitFor(() => expect(screen.getByLabelText('새 원고를 메일로도 받기')).toBeChecked())
    expect(sent).toEqual([{ emailEnabled: false }, { emailEnabled: true }])
  })

  it('내 블로그에서는 수만 보이고 단추가 없다', () => {
    renderAt([
      {
        path: '/',
        element: <SubscribeButton blogId={7} isOwner initial={{ subscriberCount: 12, subscribedByMe: false }} />,
      },
    ])
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('서버가 거절하면 문구를 보여 준다', async () => {
    loggedIn()
    server.use(
      http.put('/api/v1/blogs/7/subscription', () =>
        HttpResponse.json(
          { code: 'SUBSCRIPTION_LIMIT_EXCEEDED', message: '※ 받아 보는 블로그는 500개까지입니다' },
          { status: 409 },
        ),
      ),
    )
    renderAt([
      {
        path: '/',
        element: <SubscribeButton blogId={7} isOwner={false} initial={{ subscriberCount: 0, subscribedByMe: false }} />,
      },
    ])
    await screen.findByText('독자님')
    fireEvent.click(screen.getByRole('button', { name: /원고 받아 보기/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('500개까지')
  })
})

describe('우편함', () => {
  it('안 읽은 수를 우표로 보이고, 열면 소식마다 우표와 글 제목, 볼 수 없는 글은 그렇게 적는다', async () => {
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 12 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({
          items: [
            notice(),
            notice({ notificationId: 2, kind: 'comment', actorName: '파도', read: true }),
            notice({ notificationId: 3, available: false, postId: null, postTitle: null }),
          ],
          page: 1,
          totalPages: 1,
          unreadCount: 2,
        }),
      ),
    )
    renderAt([{ path: '/', element: <Mailbox /> }])
    const button = await screen.findByRole('button', { name: '우편함, 안 읽은 소식 12개' })
    expect(within(button).getByText('9+')).toBeInTheDocument()

    fireEvent.click(button)
    const panel = await screen.findByRole('dialog', { name: '우편함' })
    const items = await within(panel).findAllByRole('listitem')
    expect(items).toHaveLength(3)
    expect(within(items[0]).getByText('원고')).toBeInTheDocument()
    expect(within(items[0]).getByText('바다 일기')).toBeInTheDocument()
    expect(within(items[1]).getByText('답장')).toBeInTheDocument()
    expect(within(items[1]).getByText(/댓글을 남겼어요/)).toBeInTheDocument()
    expect(within(items[2]).getByText('볼 수 없는 글')).toBeInTheDocument()
  })

  it('소식을 누르면 읽음으로 바꾸고 그 글로 간다', async () => {
    let read = 0
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 1 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({ items: [notice()], page: 1, totalPages: 1, unreadCount: 1 }),
      ),
      http.post('/api/v1/me/notifications/1/read', () => {
        read++
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const router = renderAt([
      { path: '/', element: <Mailbox /> },
      { path: '/posts/:id', element: <p>글 화면</p> },
    ])
    fireEvent.click(await screen.findByRole('button', { name: /우편함/ }))
    fireEvent.click(await screen.findByRole('button', { name: /바다 일기/ }))
    expect(await screen.findByText('글 화면')).toBeInTheDocument()
    expect(read).toBe(1)
    expect(router.state.location.pathname).toBe('/posts/12')
  })

  it('답글 소식은 답글 우표로 보이고, 누르면 그 글의 댓글 자리로 간다 (specs/019)', async () => {
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 1 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({
          items: [notice({ kind: 'reply', actorName: '파도' })],
          page: 1,
          totalPages: 1,
          unreadCount: 1,
        }),
      ),
      http.post('/api/v1/me/notifications/1/read', () => new HttpResponse(null, { status: 204 })),
    )
    const router = renderAt([
      { path: '/', element: <Mailbox /> },
      { path: '/posts/:id', element: <p>글 화면</p> },
    ])
    fireEvent.click(await screen.findByRole('button', { name: /우편함/ }))
    const item = await screen.findByRole('button', { name: /바다 일기/ })
    expect(within(item).getByText('답글')).toBeInTheDocument()
    expect(within(item).getByText(/내 댓글에 답글을 달았어요/)).toBeInTheDocument()
    fireEvent.click(item)
    expect(await screen.findByText('글 화면')).toBeInTheDocument()
    expect(router.state.location.hash).toBe('#comments')
  })

  it('방명록 소식은 엽서·답장 우표로 보이고, 누르면 그 블로그의 방명록으로 간다 (specs/021)', async () => {
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 2 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({
          items: [
            notice({
              notificationId: 1,
              kind: 'guestbook',
              postId: null,
              blogId: 7,
              postTitle: '토요일의 대패밥',
              actorName: '파도',
            }),
            notice({
              notificationId: 2,
              kind: 'gb_reply',
              postId: null,
              blogId: 9,
              postTitle: '바다 마중 가는 길',
              actorName: '바다마중',
            }),
          ],
          page: 1,
          totalPages: 1,
          unreadCount: 2,
        }),
      ),
      http.post('/api/v1/me/notifications/1/read', () => new HttpResponse(null, { status: 204 })),
    )
    const router = renderAt([
      { path: '/', element: <Mailbox /> },
      { path: '/blog/:id/guestbook', element: <p>방명록 화면</p> },
    ])
    fireEvent.click(await screen.findByRole('button', { name: /우편함/ }))
    const written = await screen.findByRole('button', { name: /토요일의 대패밥/ })
    expect(within(written).getByText('엽서')).toBeInTheDocument()
    expect(within(written).getByText(/방명록에 엽서를 남겼어요/)).toBeInTheDocument()
    expect(within(written).getByText(/토요일의 대패밥 방명록/)).toBeInTheDocument()
    const replied = screen.getByRole('button', { name: /바다 마중 가는 길/ })
    expect(within(replied).getByText('회신')).toBeInTheDocument()
    expect(within(replied).getByText(/내 엽서에 답장했어요/)).toBeInTheDocument()
    fireEvent.click(written)
    expect(await screen.findByText('방명록 화면')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/blog/7/guestbook')
  })

  it('모두 읽음을 누르면 수가 사라진다', async () => {
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 2 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({ items: [notice(), notice({ notificationId: 2 })], page: 1, totalPages: 1, unreadCount: 2 }),
      ),
      http.post('/api/v1/me/notifications/read-all', () => new HttpResponse(null, { status: 204 })),
    )
    renderAt([{ path: '/', element: <Mailbox /> }])
    fireEvent.click(await screen.findByRole('button', { name: '우편함, 안 읽은 소식 2개' }))
    // 목록이 오기 전에는 모두 읽음을 누를 수 없다
    expect(await screen.findAllByRole('listitem')).toHaveLength(2)
    fireEvent.click(await screen.findByRole('button', { name: '모두 읽음' }))
    expect(await screen.findByRole('button', { name: '우편함' })).toBeInTheDocument()
    // 이번에 읽은 편지마다 소인이 찍히고, 머리에는 "모두 읽었어요"
    const panel = screen.getByRole('dialog', { name: '우편함' })
    expect(panel.querySelectorAll('.postmark.is-stamping')).toHaveLength(2)
    expect(within(panel).getByText('모두 읽었어요')).toBeInTheDocument()
  })

  it('안 읽음만 고르면 읽은 편지는 빠지고, 다 읽었으면 그렇게 알려 준다 (0.15.0)', async () => {
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 1 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({
          items: [
            notice({ postTitle: '안 읽은 원고' }),
            notice({ notificationId: 2, postTitle: '읽은 원고', read: true }),
          ],
          page: 1,
          totalPages: 1,
          unreadCount: 1,
        }),
      ),
      http.post('/api/v1/me/notifications/read-all', () => new HttpResponse(null, { status: 204 })),
    )
    renderAt([{ path: '/', element: <Mailbox /> }])
    fireEvent.click(await screen.findByRole('button', { name: /우편함/ }))
    const panel = await screen.findByRole('dialog', { name: '우편함' })
    expect(await within(panel).findAllByRole('listitem')).toHaveLength(2)
    // 처음부터 읽은 편지는 소인이 조용히 찍혀 있다 (움직임 없음)
    expect(panel.querySelectorAll('.postmark')).toHaveLength(1)
    expect(panel.querySelectorAll('.postmark.is-stamping')).toHaveLength(0)

    fireEvent.click(within(panel).getByRole('button', { name: '안 읽음' }))
    expect(within(panel).getByRole('button', { name: '안 읽음' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(panel).getAllByRole('listitem')).toHaveLength(1)
    expect(within(panel).getByText('안 읽은 원고')).toBeInTheDocument()
    expect(within(panel).queryByText('읽은 원고')).not.toBeInTheDocument()

    fireEvent.click(within(panel).getByRole('button', { name: '모두 읽음' }))
    expect(await within(panel).findByText('최근 편지 중에는 안 읽은 편지가 없어요.')).toBeInTheDocument()
    expect(within(panel).queryAllByRole('listitem')).toHaveLength(0)
  })

  it('"안 읽음"에서 볼 수 없는 글의 편지를 읽으면 목록에서 빠지고 키보드 자리는 "안 읽음" 단추로 간다', async () => {
    loggedIn()
    server.use(
      http.get('/api/v1/me/notifications/unread-count', () => HttpResponse.json({ unreadCount: 1 })),
      http.get('/api/v1/me/notifications', () =>
        HttpResponse.json({
          items: [notice({ postTitle: '지운 원고', available: false })],
          page: 1,
          totalPages: 1,
          unreadCount: 1,
        }),
      ),
      http.post('/api/v1/me/notifications/1/read', () => new HttpResponse(null, { status: 204 })),
    )
    renderAt([{ path: '/', element: <Mailbox /> }])
    fireEvent.click(await screen.findByRole('button', { name: /우편함/ }))
    const panel = await screen.findByRole('dialog', { name: '우편함' })
    await within(panel).findAllByRole('listitem')
    fireEvent.click(within(panel).getByRole('button', { name: '안 읽음' }))
    const letter = within(panel).getByText('볼 수 없는 글').closest('button')!
    letter.focus()
    fireEvent.click(letter)
    expect(await within(panel).findByText('최근 편지 중에는 안 읽은 편지가 없어요.')).toBeInTheDocument()
    expect(within(panel).getByRole('button', { name: '안 읽음' })).toHaveFocus()
  })
})

describe('원고 편지 그만 받기', () => {
  it('링크만 열어서는 끄지 않고, 단추를 눌러야 끈다', async () => {
    let calls = 0
    server.use(
      http.post('/api/v1/newsletter/unsubscribe', () => {
        calls++
        return HttpResponse.json({ blogName: '바다 일기' })
      }),
    )
    renderAt([{ path: '/letters/stop', element: <LetterStopPage /> }], '/letters/stop?token=12.abc')
    const button = await screen.findByRole('button', { name: '메일 그만 받기' })
    expect(calls).toBe(0)
    fireEvent.click(button)
    expect(await screen.findByRole('status')).toHaveTextContent('"바다 일기"의 원고 편지를 그만 받습니다')
  })

  it('잘못된 값이면 서버 문구를 보여 준다', async () => {
    server.use(
      http.post('/api/v1/newsletter/unsubscribe', () =>
        HttpResponse.json(
          { code: 'NEWSLETTER_TOKEN_INVALID', message: '※ 메일의 그만 받기 주소가 올바르지 않습니다' },
          { status: 400 },
        ),
      ),
    )
    renderAt([{ path: '/letters/stop', element: <LetterStopPage /> }], '/letters/stop?token=bad')
    fireEvent.click(await screen.findByRole('button', { name: '메일 그만 받기' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('올바르지 않습니다')
  })
})

describe('받아 보는 글', () => {
  it('받아 보는 블로그가 없으면 안내와 함께 빈 목록', async () => {
    loggedIn()
    server.use(
      http.get('/api/v1/me/subscriptions/feed', () =>
        HttpResponse.json({ totalCount: 0, page: 1, totalPages: 1, pageSize: 12, posts: [] }),
      ),
      http.get('/api/v1/me/subscriptions', () => HttpResponse.json({ items: [] })),
    )
    renderAt([{ path: '/subscriptions', element: <SubscribedPage /> }])
    expect(await screen.findByText(/원고 받아 보기'를 누르면/)).toBeInTheDocument()
    expect(await screen.findByText('아직 받아 보는 블로그가 없습니다.')).toBeInTheDocument()
  })

  it('받아 보는 블로그 목록을 못 읽으면 빈 목록이 아니라 오류를 보여 준다', async () => {
    loggedIn()
    server.use(
      http.get('/api/v1/me/subscriptions/feed', () =>
        HttpResponse.json({ totalCount: 0, page: 1, totalPages: 1, pageSize: 12, posts: [] }),
      ),
      http.get('/api/v1/me/subscriptions', () =>
        HttpResponse.json({ code: 'INTERNAL_ERROR', message: '※ 잠시 뒤 다시 시도해 주세요' }, { status: 500 }),
      ),
    )
    renderAt([{ path: '/subscriptions', element: <SubscribedPage /> }])
    expect(await screen.findByRole('alert')).toHaveTextContent('잠시 뒤 다시 시도해 주세요')
    expect(screen.queryByText('아직 받아 보는 블로그가 없습니다.')).not.toBeInTheDocument()
  })

  it('받아 보는 블로그 목록에서 그만 받으면 다시 읽는다', async () => {
    loggedIn()
    let items = [
      { blogId: 7, blogName: '바다 블로그', nickname: '파도', emailEnabled: false, createdAt: '2026-10-19T03:00:00Z' },
    ]
    server.use(
      http.get('/api/v1/me/subscriptions/feed', () =>
        HttpResponse.json({ totalCount: 0, page: 1, totalPages: 1, pageSize: 12, posts: [] }),
      ),
      http.get('/api/v1/me/subscriptions', () => HttpResponse.json({ items })),
      http.delete('/api/v1/blogs/7/subscription', () => {
        items = []
        return HttpResponse.json({ subscribed: false, emailEnabled: false, subscriberCount: 0 })
      }),
    )
    renderAt([{ path: '/subscriptions', element: <SubscribedPage /> }])
    fireEvent.click(await screen.findByRole('button', { name: '그만 받기' }))
    expect(await screen.findByText('아직 받아 보는 블로그가 없습니다.')).toBeInTheDocument()
  })
})
