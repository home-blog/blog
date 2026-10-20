// 방명록 엽서함 (specs/021 US1·US2): 목록, 봉한 엽서, 엽서 쓰기(우표 고르기·글자 수), 오류, 답장, 지우기
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeAll, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { server } from '../test/server'
import GuestbookPage from './GuestbookPage'
import type { GuestbookEntry, GuestbookPage as Page } from './guestbookApi'

beforeAll(() => {
  HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
    this.removeAttribute('open')
  }
})

const STAMPS = ['pine', 'wave', 'moon', 'plum', 'crane'] as const

function entry(over: Partial<GuestbookEntry> & { entryId: number }): GuestbookEntry {
  return {
    author: { id: 2, nickname: '바다마중', withdrawn: false },
    body: `엽서 ${over.entryId}`,
    stamp: 'pine',
    secret: false,
    sealed: false,
    createdAt: '2026-10-19T03:00:00Z',
    reply: null,
    repliedAt: null,
    canDelete: false,
    canReply: false,
    ...over,
  }
}

function page(items: GuestbookEntry[], over: Partial<Page> = {}): Page {
  return {
    totalCount: items.length,
    page: 1,
    totalPages: 1,
    pageSize: 12,
    canWrite: false,
    bodyMaxLength: 300,
    stamps: [...STAMPS],
    items,
    ...over,
  }
}

function blog(isOwner = false) {
  return {
    blogId: 7,
    name: '토요일의 대패밥',
    intro: '',
    isOwner,
    subscriberCount: 0,
    subscribedByMe: false,
    subscriptionEmail: false,
  }
}

function loggedIn(id = 3) {
  server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id, nickname: '나', admin: false } })))
}

function serve(data: Page, isOwner = false) {
  server.use(
    http.get('/api/v1/blogs/7', () => HttpResponse.json(blog(isOwner))),
    http.get('/api/v1/blogs/7/guestbook', () => HttpResponse.json(data)),
  )
}

function renderPage(url = '/blog/7/guestbook') {
  const router = createMemoryRouter(
    [
      { path: '/blog/:blogId/guestbook', element: <GuestbookPage /> },
      { path: '/blog/:blogId', element: <p>블로그 첫 화면</p> },
    ],
    { initialEntries: [url] },
  )
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
  return router
}

describe('방명록 엽서함', () => {
  it('엽서마다 내용·쓴 사람·우표가 보이고, 블로그 머리에 방명록 탭이 지금 자리로 표시된다', async () => {
    serve(
      page([
        entry({ entryId: 2, body: '두 번째', stamp: 'crane' }),
        entry({ entryId: 1, body: '첫 엽서', stamp: 'moon' }),
      ]),
    )
    renderPage()
    const list = await screen.findByRole('list', { name: '엽서' })
    const cards = within(list).getAllByRole('listitem')
    expect(cards).toHaveLength(2)
    expect(cards[0]).toHaveTextContent('두 번째')
    expect(cards[0]).toHaveTextContent('바다마중')
    expect(within(cards[0]).getByRole('img', { name: '학 우표' })).toBeInTheDocument()
    expect(within(cards[1]).getByRole('img', { name: '달 우표' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /토요일의 대패밥/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '방명록' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: '원고' })).toHaveAttribute('href', '/blog/7')
    expect(screen.getByText(/엽서 2장/)).toBeInTheDocument()
  })

  it('봉한 엽서는 내용과 쓴 사람 없이 봉인만 보인다', async () => {
    serve(
      page([
        entry({
          entryId: 1,
          secret: true,
          sealed: true,
          body: '',
          author: { id: null, nickname: null, withdrawn: false },
        }),
      ]),
    )
    renderPage()
    const card = (await screen.findAllByRole('listitem'))[0]
    expect(card).toHaveTextContent('봉한 엽서')
    expect(card).toHaveTextContent('쓴 사람과 블로그 주인만 읽을 수 있어요')
    expect(card).not.toHaveTextContent('바다마중')
    expect(within(card).queryByRole('button', { name: /답장/ })).toBeNull()
  })

  it('탈퇴한 사람의 엽서는 탈퇴한 회원으로 보인다', async () => {
    serve(page([entry({ entryId: 1, author: { id: null, nickname: null, withdrawn: true } })]))
    renderPage()
    expect(await screen.findByText('탈퇴한 회원')).toBeInTheDocument()
  })

  it('로그인하지 않으면 쓰는 칸 대신 로그인 안내가 보인다', async () => {
    serve(page([]))
    renderPage()
    expect(await screen.findByText(/로그인하면 엽서를 남길 수 있어요/)).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: '엽서 내용' })).toBeNull()
    expect(screen.getByText(/아직 꽂힌 엽서가 없어요/)).toBeInTheDocument()
  })

  it('우표를 고르고 적어 보내면 맨 앞에 새 엽서가 꽂히고 칸이 비워진다', async () => {
    loggedIn()
    serve(page([entry({ entryId: 1, body: '먼저 온 엽서' })], { canWrite: true }))
    let sent: unknown = null
    server.use(
      http.post('/api/v1/blogs/7/guestbook', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json(
          entry({
            entryId: 9,
            body: '대패밥 냄새가 나요',
            stamp: 'crane',
            secret: true,
            canDelete: true,
            author: { id: 3, nickname: '나', withdrawn: false },
          }),
          { status: 201 },
        )
      }),
    )
    renderPage()
    const box = await screen.findByRole('textbox', { name: '엽서 내용' })
    expect(box).toHaveAttribute('maxLength', '300')
    // 처음 고른 우표는 소나무
    expect(screen.getByRole('radio', { name: '소나무' })).toBeChecked()
    fireEvent.click(screen.getByRole('radio', { name: '학' }))
    expect(screen.getByRole('radio', { name: '학' })).toBeChecked()
    fireEvent.change(box, { target: { value: '대패밥 냄새가 나요' } })
    expect(screen.getByText('10 / 300')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('checkbox', { name: /비밀 엽서/ }))
    fireEvent.click(screen.getByRole('button', { name: '엽서 보내기' }))

    await waitFor(() => expect(sent).toEqual({ body: '대패밥 냄새가 나요', stamp: 'crane', secret: true }))
    const cards = within(screen.getByRole('list', { name: '엽서' })).getAllByRole('listitem')
    expect(cards).toHaveLength(2)
    expect(cards[0]).toHaveTextContent('대패밥 냄새가 나요')
    expect(cards[0]).toHaveClass('is-new')
    expect(cards[0]).toHaveTextContent('비밀')
    expect(screen.getByRole('textbox', { name: '엽서 내용' })).toHaveValue('')
    expect(screen.getByRole('status')).toHaveTextContent('엽서를 꽂았어요')
  })

  it('비어 있으면 보내지 않고, 서버가 막으면(429) 그 말을 보여 준다', async () => {
    loggedIn()
    serve(page([], { canWrite: true }))
    let posts = 0
    server.use(
      http.post('/api/v1/blogs/7/guestbook', () => {
        posts++
        return HttpResponse.json(
          {
            code: 'GUESTBOOK_DAILY_LIMIT',
            message: '※ 이 방명록에는 오늘 엽서를 3장까지 남길 수 있어요. 내일 또 들러 주세요',
          },
          { status: 429 },
        )
      }),
    )
    renderPage()
    const box = await screen.findByRole('textbox', { name: '엽서 내용' })
    fireEvent.change(box, { target: { value: '   ' } })
    fireEvent.click(screen.getByRole('button', { name: '엽서 보내기' }))
    expect(await screen.findByText('※ 엽서에 몇 자 적어 주세요')).toBeInTheDocument()
    expect(posts).toBe(0)
    fireEvent.change(box, { target: { value: '또 왔어요' } })
    fireEvent.click(screen.getByRole('button', { name: '엽서 보내기' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('오늘 엽서를 3장까지')
    expect(posts).toBe(1)
    // 쓴 글은 남겨 둔다
    expect(box).toHaveValue('또 왔어요')
  })

  it('주인은 쓰는 칸 대신 답장 안내를 보고, 엽서를 뒤집어 답장을 쓴다', async () => {
    loggedIn(1)
    serve(page([entry({ entryId: 5, body: '안녕하세요', canDelete: true, canReply: true })]), true)
    let sent: unknown = null
    server.use(
      http.put('/api/v1/guestbook/5/reply', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json(
          entry({
            entryId: 5,
            body: '안녕하세요',
            canDelete: true,
            canReply: true,
            reply: '반가워요!',
            repliedAt: '2026-10-19T04:00:00Z',
          }),
        )
      }),
    )
    renderPage()
    expect(await screen.findByText(/내 방명록이에요/)).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: '엽서 내용' })).toBeNull()
    const card = screen.getAllByRole('listitem')[0]
    fireEvent.click(within(card).getByRole('button', { name: '답장 쓰기' }))
    const replyBox = within(card).getByRole('textbox', { name: '답장' })
    // 뒤집으면 키보드 자리가 답장 칸으로 간다
    expect(replyBox).toHaveFocus()
    fireEvent.change(replyBox, { target: { value: '반가워요!' } })
    fireEvent.click(within(card).getByRole('button', { name: '답장 보내기' }))
    await waitFor(() => expect(sent).toEqual({ body: '반가워요!' }))
    expect(await within(card).findByText('반가워요!')).toBeInTheDocument()
    expect(within(card).getByRole('button', { name: '답장 고치기' })).toBeInTheDocument()
  })

  it('답장이 있는 엽서는 누구나 뒤집어 볼 수 있다', async () => {
    serve(page([entry({ entryId: 5, reply: '또 오세요', repliedAt: '2026-10-19T04:00:00Z' })]))
    renderPage()
    const card = (await screen.findAllByRole('listitem'))[0]
    const flip = within(card).getByRole('button', { name: '답장 보기' })
    expect(flip).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(flip)
    const back = within(card).getByRole('button', { name: '엽서 앞면' })
    expect(back).toHaveAttribute('aria-expanded', 'true')
    expect(back).toHaveFocus()
    expect(within(card).getByText('또 오세요')).toBeInTheDocument()
    fireEvent.click(back)
    expect(within(card).getByRole('button', { name: '답장 보기' })).toHaveFocus()
    fireEvent.click(within(card).getByRole('button', { name: '답장 보기' }))
    expect(within(card).queryByRole('textbox', { name: '답장' })).toBeNull()
  })

  it('지우기는 한 번 더 묻고, 지우면 엽서가 빠진다', async () => {
    loggedIn()
    serve(
      page([entry({ entryId: 4, body: '지울 엽서', canDelete: true }), entry({ entryId: 3, body: '남을 엽서' })], {
        canWrite: true,
      }),
    )
    const deleted: string[] = []
    server.use(
      http.delete('/api/v1/guestbook/:id', ({ params }) => {
        deleted.push(String(params.id))
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderPage()
    const card = (await screen.findAllByRole('listitem'))[0]
    fireEvent.click(within(card).getByRole('button', { name: '엽서 지우기' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: '지우기' }))
    await waitFor(() => expect(deleted).toEqual(['4']))
    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(1))
    expect(screen.getByText('남을 엽서')).toBeInTheDocument()
  })

  it('주인이 자기 방명록에 쓰려 하면(403) 서버의 말을 보여 준다', async () => {
    loggedIn()
    serve(page([], { canWrite: true }))
    server.use(
      http.post('/api/v1/blogs/7/guestbook', () =>
        HttpResponse.json(
          { code: 'GUESTBOOK_OWN_BLOG', message: '※ 내 방명록에는 엽서 대신 답장을 남길 수 있어요' },
          { status: 403 },
        ),
      ),
    )
    renderPage()
    fireEvent.change(await screen.findByRole('textbox', { name: '엽서 내용' }), { target: { value: '나에게' } })
    fireEvent.click(screen.getByRole('button', { name: '엽서 보내기' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('엽서 대신 답장')
  })

  it('없는 블로그면 없다고 알린다', async () => {
    server.use(
      http.get('/api/v1/blogs/7', () =>
        HttpResponse.json({ code: 'BLOG_NOT_FOUND', message: '※ 존재하지 않는 블로그입니다' }, { status: 404 }),
      ),
      http.get('/api/v1/blogs/7/guestbook', () =>
        HttpResponse.json({ code: 'BLOG_NOT_FOUND', message: '※ 존재하지 않는 블로그입니다' }, { status: 404 }),
      ),
    )
    renderPage()
    expect(await screen.findByText('※ 존재하지 않는 블로그입니다')).toBeInTheDocument()
  })

  it('쪽이 여럿이면 다음 쪽으로 넘긴다', async () => {
    const asked: string[] = []
    server.use(
      http.get('/api/v1/blogs/7', () => HttpResponse.json(blog())),
      http.get('/api/v1/blogs/7/guestbook', ({ request }) => {
        const p = new URL(request.url).searchParams.get('page') ?? '1'
        asked.push(p)
        return HttpResponse.json(
          page([entry({ entryId: Number(p), body: `${p}쪽 엽서` })], {
            totalCount: 13,
            totalPages: 2,
            page: Number(p),
          }),
        )
      }),
    )
    const router = renderPage()
    expect(await screen.findByText('1쪽 엽서')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: '2페이지' }))
    expect(await screen.findByText('2쪽 엽서')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')
  })
})
