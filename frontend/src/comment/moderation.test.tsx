// 댓글 신고와 편집실이 가린 댓글 (specs/022 US1, S1-1 ~ S1-3)
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeAll, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { server } from '../test/server'
import type { CommentItem } from './commentApi'
import CommentSection from './CommentSection'
import { liveCount } from './thread'

beforeAll(() => {
  HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
    this.removeAttribute('open')
  }
})

const NOBODY = { id: null, nickname: null, withdrawn: false }

function item(over: Partial<CommentItem> & { id: number }): CommentItem {
  return {
    author: { id: 2, nickname: '바다', withdrawn: false },
    body: `댓글 ${over.id}`,
    createdAt: '2026-10-19T01:00:00Z',
    canDelete: false,
    parentId: null,
    secret: false,
    sealed: false,
    deleted: false,
    canReply: true,
    hidden: false,
    canReport: false,
    ...over,
  }
}

function renderSection(comments: CommentItem[]) {
  server.use(
    http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 7, nickname: '나', admin: false } })),
    http.get('/api/v1/posts/1/comments', () =>
      HttpResponse.json({ count: liveCount(comments), comments: [...comments] }),
    ),
  )
  const router = createMemoryRouter([{ path: '/posts/1', element: <CommentSection postId={1} initialCount={0} /> }], {
    initialEntries: ['/posts/1'],
  })
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
}

describe('가린 댓글', () => {
  it('liveCount는 지운 칸과 가린 댓글을 뺀다', () => {
    expect(liveCount([item({ id: 1 }), item({ id: 2, hidden: true, body: '' }), item({ id: 3, deleted: true })])).toBe(
      1,
    )
  })

  it('먹줄 칸으로 보이고, 내용·답글·신고 단추가 없으며, 쓴 사람은 지울 수 있다', async () => {
    renderSection([
      item({ id: 1, hidden: true, body: '', author: NOBODY, canReply: false, canDelete: true }),
      item({ id: 2, parentId: 1, body: '답글은 보여요' }),
    ])
    const cell = await screen.findByText('편집실이 가린 댓글입니다')
    const card = cell.closest('article') as HTMLElement
    expect(card).toHaveClass('is-inked')
    expect(within(card).queryByRole('button', { name: '답글' })).toBeNull()
    expect(within(card).queryByRole('button', { name: '신고' })).toBeNull()
    expect(within(card).getByRole('button', { name: '삭제' })).toBeInTheDocument()
    expect(screen.getByText('답글은 보여요')).toBeInTheDocument()
    // 제목의 수에서도 빠진다
    expect(screen.getByRole('heading', { name: /댓글/ })).toHaveTextContent('댓글 1')
  })
})

describe('댓글 신고', () => {
  it('신고할 수 있는 댓글에만 단추가 있다', async () => {
    renderSection([item({ id: 1, canReport: true }), item({ id: 2, canReport: false, body: '내 댓글' })])
    const mine = (await screen.findByText('내 댓글')).closest('article') as HTMLElement
    expect(within(mine).queryByRole('button', { name: '신고' })).toBeNull()
    const other = screen.getByText('댓글 1').closest('article') as HTMLElement
    expect(within(other).getByRole('button', { name: '신고' })).toBeInTheDocument()
  })

  it('사유를 골라 보내고, 저절로 가려지면 그 자리에서 먹줄 칸이 된다', async () => {
    let sent: unknown = null
    server.use(
      http.post('/api/v1/comments/1/reports', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json({ message: '신고가 접수되었습니다', hidden: true }, { status: 201 })
      }),
    )
    renderSection([item({ id: 1, canReport: true, body: '광고 댓글' })])
    const card = (await screen.findByText('광고 댓글')).closest('article') as HTMLElement
    fireEvent.click(within(card).getByRole('button', { name: '신고' }))
    const dialog = screen.getByRole('dialog', { name: '댓글 신고' })
    fireEvent.click(within(dialog).getByLabelText('스팸'))
    fireEvent.click(within(dialog).getByRole('button', { name: '신고' }))
    await waitFor(() => expect(sent).toEqual({ reason: 'SPAM' }))
    expect(await screen.findByText('편집실이 가린 댓글입니다')).toBeInTheDocument()
    expect(screen.queryByText('광고 댓글')).toBeNull()
    expect(screen.getByRole('status')).toHaveTextContent('신고가 접수되었습니다')
  })

  it('이미 신고했으면 서버 문구를 보여 주고, 그사이 사라진 댓글이면 목록을 다시 읽는다', async () => {
    let reads = 0
    const comments = [item({ id: 1, canReport: true }), item({ id: 2, canReport: true })]
    server.use(
      http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 7, nickname: '나', admin: false } })),
      http.get('/api/v1/posts/1/comments', () => {
        reads++
        return HttpResponse.json({ count: 2, comments })
      }),
      http.post('/api/v1/comments/1/reports', () =>
        HttpResponse.json({ code: 'ALREADY_REPORTED', message: '※ 이미 신고한 댓글입니다' }, { status: 409 }),
      ),
      http.post('/api/v1/comments/2/reports', () =>
        HttpResponse.json({ code: 'COMMENT_NOT_FOUND', message: '※ 존재하지 않는 댓글입니다' }, { status: 404 }),
      ),
    )
    const router = createMemoryRouter([{ path: '/posts/1', element: <CommentSection postId={1} initialCount={0} /> }], {
      initialEntries: ['/posts/1'],
    })
    render(
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>,
    )
    const first = (await screen.findByText('댓글 1')).closest('article') as HTMLElement
    fireEvent.click(within(first).getByRole('button', { name: '신고' }))
    let dialog = screen.getByRole('dialog', { name: '댓글 신고' })
    fireEvent.click(within(dialog).getByLabelText('욕설·혐오'))
    fireEvent.click(within(dialog).getByRole('button', { name: '신고' }))
    expect(await screen.findByText('※ 이미 신고한 댓글입니다')).toBeInTheDocument()

    const second = screen.getByText('댓글 2').closest('article') as HTMLElement
    fireEvent.click(within(second).getByRole('button', { name: '신고' }))
    dialog = screen.getAllByRole('dialog', { name: '댓글 신고' }).find((d) => d.hasAttribute('open')) as HTMLElement
    fireEvent.click(within(dialog).getByLabelText('스팸'))
    fireEvent.click(within(dialog).getByRole('button', { name: '신고' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('존재하지 않는 댓글')
    await waitFor(() => expect(reads).toBeGreaterThanOrEqual(2))
  })
})
