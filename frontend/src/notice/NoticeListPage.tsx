// 공지사항 목록 (specs/012 US1): /notices?kind=&page=. 누구나 본다
// 탭(전체·업데이트·공지)은 주소에 남는다. 고정 글이 먼저, 그 안에서 새 글이 먼저 (서버)
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import EmptySheet from '../components/EmptySheet'
import Pagination from '../explore/Pagination'
import { formatDate, pageFrom } from '../explore/rules'
import NoticeTag from './NoticeTag'
import { useNoticeAlert } from './noticeAlert'
import { getNotices, type NoticeKind, type NoticePage } from './noticeApi'
import { kindFrom, noticeMessages } from './rules'
import '../explore/explore.css'
import './notice.css'

const TABS: { kind: NoticeKind | null; label: string }[] = [
  { kind: null, label: '전체' },
  { kind: 'update', label: '업데이트' },
  { kind: 'notice', label: '공지' },
]

function hrefFor(kind: NoticeKind | null, page: number): string {
  const query = new URLSearchParams()
  if (kind) query.set('kind', kind)
  if (page > 1) query.set('page', String(page))
  const qs = query.toString()
  return `/notices${qs ? `?${qs}` : ''}`
}

type Result = { key: string; data: NoticePage } | { key: string; error: string }

export default function NoticeListPage() {
  const { member } = useAuth()
  const { markSeen } = useNoticeAlert()
  const [searchParams, setSearchParams] = useSearchParams()
  const kind = kindFrom(searchParams.get('kind'))
  const page = pageFrom(searchParams.get('page'))
  const key = `${kind}\n${page}`
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => markSeen(), [markSeen])

  useEffect(() => {
    const controller = new AbortController()
    const requestKey = `${kind}\n${page}`
    getNotices(kind, page, controller.signal)
      .then((data) => {
        setResult({ key: requestKey, data })
        // 없는 쪽이면 서버가 마지막 쪽을 준다. 주소도 맞춘다
        if (data.page !== page) {
          const next = new URLSearchParams()
          if (kind) next.set('kind', kind)
          if (data.page > 1) next.set('page', String(data.page))
          setSearchParams(next, { replace: true })
        }
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({ key: requestKey, error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      })
    return () => controller.abort()
  }, [kind, page, setSearchParams])

  const current = result?.key === key ? result : null

  return (
    <div className="search-page notice-page">
      <div className="notice-head">
        <h1 className="search-title">공지사항</h1>
        {member?.admin && (
          <Link to="/notices/new" className="btn btn-primary btn-small">
            글쓰기
          </Link>
        )}
      </div>
      <nav className="notice-tabs" aria-label="공지사항 종류">
        {TABS.map((tab) => (
          <Link
            key={tab.label}
            to={hrefFor(tab.kind, 1)}
            className="notice-tab"
            aria-current={tab.kind === kind ? 'page' : undefined}
          >
            {tab.label}
          </Link>
        ))}
      </nav>
      {!current ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in current ? (
        <p className="msg msg-error" role="alert">
          {current.error}
        </p>
      ) : current.data.items.length === 0 ? (
        <EmptySheet lines={['공지사항이', '아직 없습니다']} message={noticeMessages.empty} />
      ) : (
        <>
          <ol className="notice-list">
            {current.data.items.map((notice) => (
              <li key={notice.noticeId} className={`card notice-row${notice.pinned ? ' is-pinned' : ''}`}>
                <Link to={`/notices/${notice.noticeId}`} className="notice-row-link">
                  <span className="notice-row-meta">
                    {notice.pinned && <span className="notice-pin">고정</span>}
                    <NoticeTag kind={notice.kind} />
                    {notice.releaseVersion && <span className="notice-version">v{notice.releaseVersion}</span>}
                    <time dateTime={notice.createdAt}>{formatDate(notice.createdAt)}</time>
                  </span>
                  <span className="notice-row-title">{notice.title}</span>
                </Link>
              </li>
            ))}
          </ol>
          {current.data.totalPages > 1 && (
            <Pagination page={current.data.page} totalPages={current.data.totalPages} hrefFor={(p) => hrefFor(kind, p)} />
          )}
        </>
      )}
    </div>
  )
}
