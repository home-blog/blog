// 댓글 영역의 답글·봉한 쪽지·지운 칸 (specs/019 US1·US2)
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeAll, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { server } from '../test/server'
import type { CommentItem } from './commentApi'
import CommentSection from './CommentSection'
import { afterDelete, insertReply, liveCount, threads } from './thread'

// jsdom에는 <dialog>의 showModal·close가 없어 확인 창을 열 수 있게 채운다
beforeAll(() => {
  HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
    this.removeAttribute('open')
  }
})

const HIDDEN = { id: null, nickname: null, withdrawn: false }

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

function loggedIn() {
  server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 7, nickname: '나', admin: false } })))
}

function renderSection(comments: CommentItem[]) {
  server.use(
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

describe('답글 묶기 (thread.ts)', () => {
  const list = [item({ id: 1 }), item({ id: 3, parentId: 1 }), item({ id: 2 })]

  it('답글을 원댓글 아래로 묶는다', () => {
    const result = threads(list)
    expect(result.map((t) => t.root.id)).toEqual([1, 2])
    expect(result[0].replies.map((r) => r.id)).toEqual([3])
  })

  it('새 답글은 그 원댓글의 마지막 답글 뒤에 들어간다', () => {
    expect(insertReply(list, item({ id: 9, parentId: 1 })).map((c) => c.id)).toEqual([1, 3, 9, 2])
    expect(insertReply(list, item({ id: 9, parentId: 2 })).map((c) => c.id)).toEqual([1, 3, 2, 9])
  })

  it('답글이 남은 원댓글을 지우면 지운 칸, 마지막 답글을 지우면 지운 칸도 사라진다', () => {
    const erased = afterDelete(list, 1)
    expect(erased[0]).toMatchObject({ id: 1, deleted: true, body: '', author: HIDDEN, canReply: false })
    expect(erased[1]).toMatchObject({ id: 3, canReply: false })
    expect(liveCount(erased)).toBe(2)
    expect(afterDelete(erased, 3).map((c) => c.id)).toEqual([2])
    // 답글이 없는 원댓글은 바로 사라진다
    expect(afterDelete(list, 2).map((c) => c.id)).toEqual([1, 3])
  })
})

describe('댓글 영역', () => {
  it('답글은 원댓글 아래에, 봉한 쪽지와 지운 칸은 내용 없이 보이고 수는 지운 칸을 뺀다', async () => {
    renderSection([
      item({ id: 1, deleted: true, body: '', author: HIDDEN, canReply: false }),
      item({ id: 2, parentId: 1, body: '남은 답글' }),
      item({ id: 3, secret: true, sealed: true, body: '', author: HIDDEN, canReply: false }),
    ])
    expect(await screen.findByText('지운 댓글입니다')).toBeInTheDocument()
    const replies = screen.getByRole('list', { name: '답글' })
    expect(within(replies).getByText('남은 답글')).toBeInTheDocument()
    expect(screen.getByText(/봉한 쪽지입니다/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /댓글/ })).toHaveTextContent('2')
  })

  it('답글 단추로 연 칸에서 쓰면 parentId와 함께 보내고 원댓글 아래에 붙는다', async () => {
    loggedIn()
    let sent: unknown = null
    server.use(
      http.post('/api/v1/posts/1/comments', async ({ request }) => {
        sent = await request.json()
        return HttpResponse.json(
          item({ id: 5, parentId: 1, body: '고마워요', author: { id: 7, nickname: '나', withdrawn: false } }),
          {
            status: 201,
          },
        )
      }),
    )
    renderSection([item({ id: 1, body: '첫 댓글' }), item({ id: 2, body: '둘째 댓글' })])
    const first = (await screen.findByText('첫 댓글')).closest('li') as HTMLElement
    fireEvent.click(within(first).getByRole('button', { name: '답글' }))
    fireEvent.change(within(first).getByLabelText('답글 내용'), { target: { value: '고마워요' } })
    fireEvent.click(within(first).getByRole('button', { name: '답글 등록' }))

    await waitFor(() => expect(sent).toEqual({ body: '고마워요', parentId: 1, secret: false }))
    const replies = await within(first).findByRole('list', { name: '답글' })
    expect(within(replies).getByText('고마워요')).toBeInTheDocument()
    expect(within(first).queryByLabelText('답글 내용')).not.toBeInTheDocument()
  })

  it('봉한 쪽지에 다는 답글은 봉해서 남기기가 켜져 있고 끌 수 없다', async () => {
    loggedIn()
    renderSection([item({ id: 1, body: '비밀 질문', secret: true })])
    const root = (await screen.findByText('비밀 질문')).closest('li') as HTMLElement
    expect(within(root).getByText('봉한 쪽지')).toBeInTheDocument()
    fireEvent.click(within(root).getByRole('button', { name: '답글' }))
    const toggle = within(root).getByRole('checkbox')
    expect(toggle).toBeChecked()
    expect(toggle).toBeDisabled()
  })

  it('새 댓글을 봉해서 남기면 secret을 함께 보낸다', async () => {
    loggedIn()
    let sent: unknown = null
    server.use(
      http.post('/api/v1/posts/1/comments', async ({ request }) => {
        sent = await request.json()
        // 목록을 아직 받기 전에 등록이 끝나면 화면이 다시 읽으므로, 가짜 서버도 새 댓글을 기억한다
        stored.push(item({ id: 8, body: '주인만 보세요', secret: true }))
        return HttpResponse.json(stored[0], { status: 201 })
      }),
    )
    const stored: CommentItem[] = []
    renderSection(stored)
    fireEvent.change(await screen.findByLabelText('댓글 내용'), { target: { value: '주인만 보세요' } })
    fireEvent.click(screen.getByRole('checkbox', { name: /봉해서 남기기/ }))
    fireEvent.click(screen.getByRole('button', { name: '등록' }))
    await waitFor(() => expect(sent).toEqual({ body: '주인만 보세요', parentId: null, secret: true }))
    expect(await screen.findByText('주인만 보세요')).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: /봉해서 남기기/ })).not.toBeChecked()
  })

  it('답글이 남은 댓글을 지우면 지운 칸으로 바뀐다', async () => {
    loggedIn()
    server.use(http.delete('/api/v1/comments/1', () => new HttpResponse(null, { status: 204 })))
    renderSection([
      item({ id: 1, body: '지울 댓글', canDelete: true }),
      item({ id: 2, parentId: 1, body: '남는 답글' }),
    ])
    const root = (await screen.findByText('지울 댓글')).closest('li') as HTMLElement
    fireEvent.click(within(root).getAllByRole('button', { name: '삭제' })[0])
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: '삭제' }))
    expect(await screen.findByText('지운 댓글입니다')).toBeInTheDocument()
    expect(screen.getByText('남는 답글')).toBeInTheDocument()
  })

  it('답글을 다는 사이 원댓글이 지워졌으면 목록을 다시 읽고 답글 칸도 닫힌다', async () => {
    loggedIn()
    const stored = [item({ id: 1, body: '곧 지워질 댓글' })]
    server.use(
      http.post('/api/v1/posts/1/comments', () => {
        stored.splice(0, stored.length)
        return HttpResponse.json({ code: 'COMMENT_NOT_FOUND', message: '※ 댓글을 찾을 수 없습니다' }, { status: 404 })
      }),
    )
    renderSection(stored)
    const root = (await screen.findByText('곧 지워질 댓글')).closest('li') as HTMLElement
    fireEvent.click(within(root).getByRole('button', { name: '답글' }))
    fireEvent.change(within(root).getByLabelText('답글 내용'), { target: { value: '늦은 답글' } })
    fireEvent.click(within(root).getByRole('button', { name: '답글 등록' }))
    await waitFor(() => expect(screen.queryByText('곧 지워질 댓글')).not.toBeInTheDocument())
    expect(screen.queryByLabelText('답글 내용')).not.toBeInTheDocument()
  })

  it('답글 칸을 취소하면 키보드 자리가 답글 단추로 돌아간다', async () => {
    loggedIn()
    renderSection([item({ id: 1, body: '첫 댓글' })])
    const root = (await screen.findByText('첫 댓글')).closest('li') as HTMLElement
    const button = within(root).getByRole('button', { name: '답글' })
    fireEvent.click(button)
    expect(button).toHaveAttribute('aria-expanded', 'true')
    fireEvent.click(within(root).getByRole('button', { name: '취소' }))
    await waitFor(() => expect(button).toHaveFocus())
    expect(button).toHaveAttribute('aria-expanded', 'false')
  })
})
