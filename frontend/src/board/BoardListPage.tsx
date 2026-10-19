// 글마당 목록 (specs/017 US1, US4): /board?topic=&kind=&unsolved=&page=. 누구나 본다
// 주제 탭과 종류 고르기는 주소에 남는다. 글쓰기는 로그인하지 않았으면 로그인 창을 띄운다
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { useLoginPrompt } from '../auth/loginPrompt'
import EmptySheet from '../components/EmptySheet'
import Pagination from '../explore/Pagination'
import { pageFrom } from '../explore/rules'
import BoardTable from './BoardTable'
import { boardQueryString, getBoardPosts, messageOf, type BoardKind, type BoardPage, type BoardQuery } from './boardApi'
import './board.css'

const KINDS: { kind: BoardKind | null; unsolved?: boolean; label: string }[] = [
  { kind: null, label: '모두' },
  { kind: 'talk', label: '이야기' },
  { kind: 'question', label: '물음' },
  { kind: 'question', unsolved: true, label: '해결 전' },
]

function kindFrom(value: string | null): BoardKind | null {
  return value === 'talk' || value === 'question' ? value : null
}

type Result = { key: string; data: BoardPage } | { key: string; error: string }

export default function BoardListPage() {
  const { member } = useAuth()
  const { open } = useLoginPrompt()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const topic = searchParams.get('topic')
  const kind = kindFrom(searchParams.get('kind'))
  const unsolved = kind === 'question' && searchParams.get('unsolved') === 'true'
  const page = pageFrom(searchParams.get('page'))
  const query: BoardQuery = { topic, kind, unsolved, page }
  const key = boardQueryString(query)
  const [result, setResult] = useState<Result | null>(null)
  const [topics, setTopics] = useState<string[]>([])

  useEffect(() => {
    const controller = new AbortController()
    const requestKey = boardQueryString({ topic, kind, unsolved, page })
    getBoardPosts({ topic, kind, unsolved, page }, controller.signal)
      .then((data) => {
        setResult({ key: requestKey, data })
        setTopics(data.topics)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setResult({ key: requestKey, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [topic, kind, unsolved, page])

  const current = result?.key === key ? result : null

  function write() {
    if (member) navigate('/board/write')
    else open('/board/write')
  }

  return (
    <div className="board-page">
      <div className="board-head">
        <div>
          <h1 className="board-heading">글마당</h1>
          <p className="hint">블로그 밖에서 주제별로 묻고 이야기하는 마당입니다.</p>
        </div>
        <button type="button" className="btn btn-primary btn-small" onClick={write}>
          <span aria-hidden="true">✎</span> 글쓰기
        </button>
      </div>
      <nav className="board-tabs" aria-label="주제">
        {[null, ...topics].map((name) => (
          <Link
            key={name ?? '전체'}
            to={`/board${boardQueryString({ topic: name, kind, unsolved })}`}
            className="board-tab"
            aria-current={name === topic ? 'page' : undefined}
          >
            {name ?? '전체'}
          </Link>
        ))}
      </nav>
      <nav className="board-kinds" aria-label="종류">
        {KINDS.map((item) => (
          <Link
            key={item.label}
            to={`/board${boardQueryString({ topic, kind: item.kind, unsolved: item.unsolved })}`}
            className="board-kind-link"
            aria-current={item.kind === kind && Boolean(item.unsolved) === unsolved ? 'page' : undefined}
          >
            {item.label}
          </Link>
        ))}
      </nav>
      {!current ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in current ? (
        <p className="msg msg-error" role="alert">
          {current.error}
        </p>
      ) : current.data.posts.length === 0 ? (
        <EmptySheet lines={['글이 아직', '없습니다']} message="글이 없습니다. 첫 이야기를 들려주세요." />
      ) : (
        <>
          <BoardTable posts={current.data.posts} />
          {current.data.totalPages > 1 && (
            <Pagination
              page={current.data.page}
              totalPages={current.data.totalPages}
              hrefFor={(p) => `/board${boardQueryString({ topic, kind, unsolved, page: p })}`}
            />
          )}
        </>
      )}
    </div>
  )
}
