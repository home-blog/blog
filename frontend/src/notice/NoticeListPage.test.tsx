// 공지사항 목록 화면 (specs/012 US1): 사용자가 보는 화면 기준
import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { server } from '../test/server'
import NoticeAlertProvider from './NoticeAlertProvider'
import NoticeListPage from './NoticeListPage'
import type { NoticePage } from './noticeApi'

const PAGE: NoticePage = {
  items: [
    { noticeId: 2, kind: 'update', title: '공지사항이 생겼어요', releaseVersion: '0.6.0', pinned: true, createdAt: '2026-10-15T09:00:00Z' },
    { noticeId: 1, kind: 'notice', title: '서버 점검 안내', releaseVersion: null, pinned: false, createdAt: '2026-10-14T09:00:00Z' },
  ],
  page: 1,
  totalPages: 1,
  totalCount: 2,
}

let requestedKind: string | null = null

function renderAt(url: string, admin: boolean | null = null) {
  server.use(
    http.get('/api/v1/notices', ({ request }) => {
      requestedKind = new URL(request.url).searchParams.get('kind')
      return HttpResponse.json(PAGE)
    }),
    http.get('/api/v1/notices/latest', () => HttpResponse.json({ latestAt: PAGE.items[0].createdAt })),
  )
  if (admin !== null) {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id: 1, nickname: '글쓴이', admin } })))
  }
  const router = createMemoryRouter([{ path: '/notices', element: <NoticeListPage /> }], { initialEntries: [url] })
  render(
    <AuthProvider>
      <NoticeAlertProvider>
        <RouterProvider router={router} />
      </NoticeAlertProvider>
    </AuthProvider>,
  )
}

beforeEach(() => {
  requestedKind = null
  localStorage.clear()
})

describe('공지사항 목록', () => {
  it('S1-1 로그인하지 않아도 목록이 보이고, 꼬리표에 글자가 있다', async () => {
    renderAt('/notices')
    const rows = await screen.findAllByRole('listitem')
    expect(rows).toHaveLength(2)
    expect(within(rows[0]).getByText('업데이트')).toBeInTheDocument()
    expect(within(rows[0]).getByText('고정')).toBeInTheDocument()
    expect(within(rows[0]).getByText('v0.6.0')).toBeInTheDocument()
    expect(within(rows[1]).getByText('공지')).toBeInTheDocument()
    expect(within(rows[1]).queryByText('고정')).toBeNull()
  })

  it('S1-2 탭은 주소의 kind를 따르고 지금 탭이 표시된다', async () => {
    renderAt('/notices?kind=update')
    await screen.findAllByRole('listitem')
    expect(requestedKind).toBe('update')
    expect(screen.getByRole('link', { name: '업데이트' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: '전체' })).not.toHaveAttribute('aria-current')
  })

  it('S1-5 모르는 탭 값은 전체로', async () => {
    renderAt('/notices?kind=xyz')
    await screen.findAllByRole('listitem')
    expect(requestedKind).toBeNull()
    expect(screen.getByRole('link', { name: '전체' })).toHaveAttribute('aria-current', 'page')
  })

  it.each([
    ['로그인하지 않은 방문자', null],
    ['관리자가 아닌 회원', false],
  ])('%s에게는 글쓰기 버튼이 없다', async (_, admin) => {
    renderAt('/notices', admin)
    await screen.findAllByRole('listitem')
    await new Promise((resolve) => setTimeout(resolve, 20)) // 로그인 상태 답을 기다린다
    expect(screen.queryByRole('link', { name: '글쓰기' })).toBeNull()
  })

  it('관리자에게는 글쓰기 버튼', async () => {
    renderAt('/notices', true)
    expect(await screen.findByRole('link', { name: '글쓰기' })).toHaveAttribute('href', '/notices/new')
  })

  it('글이 없으면 안내 문구', async () => {
    server.use(http.get('/api/v1/notices', () => HttpResponse.json({ items: [], page: 1, totalPages: 1, totalCount: 0 })))
    const router = createMemoryRouter([{ path: '/notices', element: <NoticeListPage /> }], { initialEntries: ['/notices'] })
    server.use(http.get('/api/v1/notices/latest', () => HttpResponse.json({ latestAt: null })))
    render(
      <AuthProvider>
        <NoticeAlertProvider>
          <RouterProvider router={router} />
        </NoticeAlertProvider>
      </AuthProvider>,
    )
    expect(await screen.findByText('아직 공지사항이 없습니다')).toBeInTheDocument()
  })
})
