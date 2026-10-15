// 공지사항 쓰기·고치기 화면 (specs/012 US4): 주소의 번호가 숫자가 아니면 서버에 묻지 않는다
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { server } from '../test/server'
import NoticeEditorPage from './NoticeEditorPage'

function renderAt(url: string) {
  server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 1, nickname: '관리자', admin: true } })))
  const router = createMemoryRouter(
    [
      { path: '/notices/new', element: <NoticeEditorPage /> },
      { path: '/notices/:noticeId/edit', element: <NoticeEditorPage /> },
    ],
    { initialEntries: [url] },
  )
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  )
}

describe('공지사항 고치기 주소', () => {
  it.each(['/notices/abc/edit', '/notices/0/edit', '/notices/1.5/edit'])(
    '예외: %s는 서버에 묻지 않고 없는 공지로 보인다',
    async (url) => {
      const read = vi.fn()
      server.use(http.get('/api/v1/notices/:id', () => read()))
      renderAt(url)
      expect(await screen.findByText('※ 존재하지 않는 공지사항입니다')).toBeInTheDocument()
      expect(read).not.toHaveBeenCalled()
    },
  )

  it('정상: 숫자 번호면 그 글을 불러와 칸을 채운다', async () => {
    server.use(
      http.get('/api/v1/notices/7', () =>
        HttpResponse.json({
          noticeId: 7,
          kind: 'notice',
          title: '점검 안내',
          content: '본문',
          releaseVersion: null,
          releaseUrl: null,
          pinned: false,
          createdAt: '2026-10-15T09:00:00Z',
          updatedAt: null,
        }),
      ),
    )
    renderAt('/notices/7/edit')
    expect(await screen.findByDisplayValue('점검 안내')).toBeInTheDocument()
  })
})
