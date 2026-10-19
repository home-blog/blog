// 글마당 화면 (specs/017): 목록 표·고르기, 글 읽기·댓글·채택 도장, 쓰기 규칙, 첫 화면·검색 칸
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { useAuth } from '../auth/useAuth'
import { server } from '../test/server'
import BoardEditorPage from './BoardEditorPage'
import BoardListPage from './BoardListPage'
import BoardPostPage from './BoardPostPage'
import BoardRecent from './BoardRecent'
import BoardSearchResults from './BoardSearchResults'
import type { BoardCommentList, BoardDetail, BoardPage, BoardRow } from './boardApi'
import { checkBoard } from './rules'

const TOPICS = ['여행', '음식', '취미', '운동', '개발']

function loggedIn(id = 3, nickname = '독자') {
  server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ member: { id, nickname, admin: false } })))
}

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

function row(overrides: Partial<BoardRow> = {}): BoardRow {
  return {
    boardPostId: 1,
    topic: '여행',
    kind: 'talk',
    title: '제주 렌터카 vs 대중교통 후기',
    writer: { id: 5, nickname: '파도', withdrawn: false },
    createdAt: '2026-10-19T03:00:00Z',
    views: 12,
    commentCount: 3,
    solved: false,
    ...overrides,
  }
}

function page(posts: BoardRow[], overrides: Partial<BoardPage> = {}): BoardPage {
  return {
    topics: TOPICS,
    topic: null,
    kind: null,
    unsolved: false,
    keyword: null,
    totalCount: posts.length,
    page: 1,
    totalPages: 1,
    pageSize: 10,
    posts,
    ...overrides,
  }
}

function detail(overrides: Partial<BoardDetail> = {}): BoardDetail {
  return {
    boardPostId: 1,
    topic: '개발',
    kind: 'question',
    title: 'NPE가 나요',
    content: '어디서 **null**이 오는지 모르겠어요',
    writer: { id: 3, nickname: '독자', withdrawn: false },
    createdAt: '2026-10-19T03:00:00Z',
    updatedAt: '2026-10-19T03:00:00Z',
    views: 4,
    commentCount: 1,
    acceptedCommentId: null,
    mine: true,
    canDelete: true,
    ...overrides,
  }
}

describe('글마당 목록', () => {
  it('표에 주제·제목·댓글 수·쓴 사람이 보이고, 물음 꼬리표와 해결 도장, 탈퇴한 회원을 보인다', async () => {
    server.use(
      http.get('/api/v1/board/posts', () =>
        HttpResponse.json(
          page([
            row(),
            row({ boardPostId: 2, kind: 'question', title: '김치찌개 비법', solved: true, commentCount: 0 }),
            row({ boardPostId: 3, title: '오래된 글', writer: { id: null, nickname: null, withdrawn: true } }),
          ]),
        ),
      ),
    )
    renderAt([{ path: '/board', element: <BoardListPage /> }])
    const table = await screen.findByRole('table')
    expect(within(table).getByRole('link', { name: '제주 렌터카 vs 대중교통 후기' })).toHaveAttribute(
      'href',
      '/board/1',
    )
    expect(within(table).getByText('물음')).toBeInTheDocument()
    expect(within(table).getByRole('img', { name: '해결된 물음' })).toBeInTheDocument()
    expect(within(table).getByText('탈퇴한 회원')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '여행' })).toHaveAttribute('href', '/board?topic=%EC%97%AC%ED%96%89')
  })

  it('고른 주제와 종류를 서버에 묻고, 글이 없으면 안내한다', async () => {
    const asked: string[] = []
    server.use(
      http.get('/api/v1/board/posts', ({ request }) => {
        asked.push(new URL(request.url).search)
        return HttpResponse.json(page([], { topic: '운동', kind: 'question', unsolved: true }))
      }),
    )
    renderAt([{ path: '/board', element: <BoardListPage /> }], '/board?topic=운동&kind=question&unsolved=true')
    expect(await screen.findByText(/글이 없습니다/)).toBeInTheDocument()
    expect(asked[0]).toContain('kind=question')
    expect(asked[0]).toContain('unsolved=true')
    expect(screen.getByRole('link', { name: '해결 전' })).toHaveAttribute('aria-current', 'page')
  })

  it('검색어가 붙어 오면 검색어로 묻고, 탭을 바꿔도 검색어를 남긴다', async () => {
    const asked: string[] = []
    server.use(
      http.get('/api/v1/board/posts', ({ request }) => {
        asked.push(new URL(request.url).searchParams.get('q') ?? '')
        return HttpResponse.json(page([row()], { keyword: '단풍' }))
      }),
    )
    renderAt([{ path: '/board', element: <BoardListPage /> }], '/board?q=단풍')
    await screen.findByRole('table')
    expect(screen.getByText('단풍')).toBeInTheDocument()
    expect(asked[0]).toBe('단풍')
    expect(screen.getByRole('link', { name: '여행' }).getAttribute('href')).toContain('q=%EB%8B%A8%ED%92%8D')
  })

  it('로그인하지 않고 글쓰기를 누르면 글쓰기 화면으로 가지 않는다', async () => {
    server.use(http.get('/api/v1/board/posts', () => HttpResponse.json(page([]))))
    const router = renderAt([
      { path: '/board', element: <BoardListPage /> },
      { path: '/board/write', element: <p>쓰기 화면</p> },
    ])
    fireEvent.click(await screen.findByRole('button', { name: /글쓰기/ }))
    expect(router.state.location.pathname).toBe('/board')
  })
})

describe('글마당 글 읽기', () => {
  function comments(overrides: Partial<BoardCommentList> = {}): BoardCommentList {
    return {
      count: 1,
      acceptedCommentId: null,
      canAccept: true,
      comments: [
        {
          boardCommentId: 9,
          writer: { id: 5, nickname: '파도', withdrawn: false },
          content: 'null 확인을 하세요',
          createdAt: '2026-10-19T04:00:00Z',
          accepted: false,
          mine: false,
          canDelete: false,
        },
      ],
      ...overrides,
    }
  }

  it('물음을 쓴 사람은 남의 댓글을 채택하고, 채택하면 도장이 찍힌다', async () => {
    loggedIn()
    let accepted = false
    server.use(
      http.get('/api/v1/board/posts/1', () => HttpResponse.json(detail())),
      http.get('/api/v1/board/posts/1/comments', () =>
        HttpResponse.json(
          accepted
            ? comments({
                acceptedCommentId: 9,
                comments: [{ ...comments().comments[0], accepted: true }],
              })
            : comments(),
        ),
      ),
      http.put('/api/v1/board/posts/1/accepted', () => {
        accepted = true
        return HttpResponse.json({ acceptedCommentId: 9 })
      }),
    )
    renderAt([{ path: '/board/:boardPostId', element: <BoardPostPage /> }], '/board/1')
    expect(await screen.findByRole('heading', { name: 'NPE가 나요' })).toBeInTheDocument()
    await screen.findByText('독자님')
    fireEvent.click(await screen.findByRole('button', { name: '채택' }))
    expect(await screen.findByRole('img', { name: '채택된 답' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '채택 거두기' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('댓글을 달면 다시 읽고, 서버가 거절하면 문구를 보인다', async () => {
    loggedIn(7, '나그네')
    let list = comments({ count: 0, comments: [], canAccept: false })
    server.use(
      http.get('/api/v1/board/posts/1', () => HttpResponse.json(detail({ mine: false, canDelete: false }))),
      http.get('/api/v1/board/posts/1/comments', () => HttpResponse.json(list)),
      http.post('/api/v1/board/posts/1/comments', async ({ request }) => {
        const body = (await request.json()) as { content: string }
        if (body.content === '또') {
          return HttpResponse.json(
            { code: 'BOARD_COMMENT_TOO_FAST', message: '※ 잠시 뒤에 다시 등록해 주세요' },
            { status: 429 },
          )
        }
        list = comments({
          canAccept: false,
          comments: [{ ...comments().comments[0], content: body.content, mine: true }],
        })
        return HttpResponse.json(list.comments[0], { status: 201 })
      }),
    )
    renderAt([{ path: '/board/:boardPostId', element: <BoardPostPage /> }], '/board/1')
    expect(await screen.findByText('첫 댓글을 남겨 보세요')).toBeInTheDocument()
    await screen.findByText('나그네님')
    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('댓글'), { target: { value: '좋은 질문이에요' } })
    fireEvent.click(screen.getByRole('button', { name: '댓글 등록' }))
    expect(await screen.findByText('좋은 질문이에요')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '채택' })).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('댓글'), { target: { value: '또' } })
    fireEvent.click(screen.getByRole('button', { name: '댓글 등록' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('잠시 뒤에 다시 등록해 주세요')
  })

  it('없는 글은 교정부호 404를 보인다', async () => {
    server.use(
      http.get('/api/v1/board/posts/99', () =>
        HttpResponse.json({ code: 'BOARD_POST_NOT_FOUND', message: '※ 존재하지 않는 글입니다' }, { status: 404 }),
      ),
      http.get('/api/v1/board/posts/99/comments', () =>
        HttpResponse.json({ code: 'BOARD_POST_NOT_FOUND', message: '※ 존재하지 않는 글입니다' }, { status: 404 }),
      ),
    )
    renderAt([{ path: '/board/:boardPostId', element: <BoardPostPage /> }], '/board/99')
    expect(await screen.findByText(/존재하지 않는 글입니다/)).toBeInTheDocument()
  })
})

describe('글마당 쓰기', () => {
  it('규칙: 주제·제목·본문이 비면 칸마다, 경계값 100자 제목은 된다', () => {
    expect(checkBoard({ topic: '', kind: 'talk', title: ' ', content: '' })).toEqual({
      topic: '※ 주제를 골라 주세요',
      title: '제목을 입력해 주세요',
      content: '본문을 입력해 주세요',
    })
    expect(checkBoard({ topic: '여행', kind: 'talk', title: '가'.repeat(100), content: '본문' })).toEqual({})
    expect(checkBoard({ topic: '여행', kind: 'talk', title: '가'.repeat(101), content: '본문' }).title).toContain(
      '100자',
    )
  })

  it('주제를 고르고 물음으로 올리면 그 글로 간다', async () => {
    loggedIn()
    const sent: unknown[] = []
    server.use(
      http.get('/api/v1/board/posts', () => HttpResponse.json(page([]))),
      http.post('/api/v1/board/posts', async ({ request }) => {
        sent.push(await request.json())
        return HttpResponse.json({ boardPostId: 42 }, { status: 201 })
      }),
    )
    const router = renderAt([
      { path: '/board/write', element: <BoardEditorPage /> },
      { path: '/board/:boardPostId', element: <p>글 화면</p> },
    ])
    await screen.findByRole('option', { name: '개발' })
    fireEvent.change(screen.getByLabelText('주제'), { target: { value: '개발' } })
    fireEvent.click(screen.getByLabelText('물음'))
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: '  스프링 질문  ' } })
    fireEvent.change(screen.getByLabelText('본문 (마크다운)'), { target: { value: '본문입니다' } })
    fireEvent.click(screen.getByRole('button', { name: '올리기' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/board/42'))
    expect(sent[0]).toEqual({ topic: '개발', kind: 'question', title: '스프링 질문', content: '본문입니다' })
  })
})

describe('첫 화면과 검색', () => {
  it('첫 화면 글마당이 비면 첫 이야기를 부른다', async () => {
    server.use(http.get('/api/v1/board/posts/recent', () => HttpResponse.json({ posts: [] })))
    renderAt([{ path: '/', element: <BoardRecent /> }])
    expect(await screen.findByText(/첫 이야기를 들려주세요/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '전체 보기 →' })).toHaveAttribute('href', '/board')
  })

  it('검색 결과에 글마당 칸을 따로 보인다', async () => {
    server.use(
      http.get('/api/v1/board/posts', ({ request }) => {
        expect(new URL(request.url).searchParams.get('q')).toBe('단풍 명소')
        return HttpResponse.json(page([row({ title: '단풍 명소 추천' })], { keyword: '단풍 명소' }))
      }),
    )
    renderAt([{ path: '/search', element: <BoardSearchResults keyword="단풍 명소" /> }])
    expect(await screen.findByRole('heading', { name: '글마당 1건' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '단풍 명소 추천' })).toBeInTheDocument()
  })
})
