// 편집실 (specs/018): /editorial?tab=posts|board|comments|members|history&page=. 관리자만 (서버가 다시 확인한다)
// 위에 운영 현황 숫자, 아래 탭: 검토 대기(블로그 글) · 글마당 · 댓글 · 회원 · 처리 기록 (댓글·회원은 specs/022).
// 줄을 펼치면 신고 자세히와 처리 도장. 처리 기록에서 내린 글은 잠금 풀기, 가린 댓글은 되돌리기
import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useAuth } from '../auth/useAuth'
import Pagination from '../explore/Pagination'
import { formatDate, pageFrom } from '../explore/rules'
import MembersPanel from './MembersPanel'
import ReviewPanel from './ReviewPanel'
import {
  ACTION_LABEL,
  getActions,
  getPendingBoard,
  getPendingComments,
  getPendingPosts,
  getSummary,
  messageOf,
  restoreComment,
  TARGET_LABEL,
  unlockPost,
  type ActionRow,
  type EditorialSummary,
  type Pending,
  type PendingBoardRow,
  type PendingCommentRow,
  type PendingPostRow,
} from './editorialApi'
import './editorial.css'

type Tab = 'posts' | 'board' | 'comments' | 'members' | 'history'
const TABS: { tab: Tab; label: string }[] = [
  { tab: 'posts', label: '검토 대기' },
  { tab: 'board', label: '글마당' },
  { tab: 'comments', label: '댓글' },
  { tab: 'members', label: '회원' },
  { tab: 'history', label: '처리 기록' },
]

function tabFrom(value: string | null): Tab {
  return TABS.some((item) => item.tab === value) ? (value as Tab) : 'posts'
}

function hrefFor(tab: Tab, page = 1): string {
  const query = new URLSearchParams()
  if (tab !== 'posts') query.set('tab', tab)
  if (page > 1) query.set('page', String(page))
  const qs = query.toString()
  return `/editorial${qs ? `?${qs}` : ''}`
}

type PendingRow = PendingPostRow | PendingBoardRow | PendingCommentRow
type Rows = Pending<PendingRow> | Pending<ActionRow>
type Loaded = { key: string; data: Rows } | { key: string; error: string }

const CARDS: { key: keyof EditorialSummary; label: string }[] = [
  { key: 'members', label: '회원' },
  { key: 'membersToday', label: '오늘 가입' },
  { key: 'publicPosts', label: '공개 글' },
  { key: 'publicPostsToday', label: '오늘 올라온 글' },
  { key: 'boardPosts', label: '글마당 글' },
  { key: 'pendingReports', label: '검토 대기 신고' },
  { key: 'pendingCommentReports', label: '댓글 신고 대기' },
  { key: 'suspendedMembers', label: '정지 중 회원' },
]

/** 줄의 번호와 제목·곁말 */
function rowIdOf(row: PendingRow): number {
  return 'commentId' in row ? row.commentId : 'postId' in row ? row.postId : row.boardPostId
}

function rowWhere(row: PendingRow): string {
  if ('commentId' in row) {
    const hidden = row.hidden ? (row.hiddenBy === 'auto' ? ' · 저절로 가림' : ' · 가림') : ''
    return `${row.writerNickname ?? '탈퇴한 사용자'}의 댓글 "${row.excerpt ?? ''}"${hidden}`
  }
  return 'blogName' in row ? (row.blogName ?? '') : (row.topic ?? '')
}

function reasonsOf(row: PendingRow): string {
  const parts: string[] = []
  if (row.spam) parts.push(`스팸 ${row.spam}`)
  if (row.abuse) parts.push(`욕설·혐오 ${row.abuse}`)
  if (row.adult) parts.push(`음란물 ${row.adult}`)
  if (row.other) parts.push(`기타 ${row.other}`)
  return parts.join(' · ')
}

export default function EditorialPage() {
  const { member } = useAuth()
  const [searchParams] = useSearchParams()
  const tab = tabFrom(searchParams.get('tab'))
  const page = pageFrom(searchParams.get('page'))
  const key = `${tab}\n${page}`
  const [summary, setSummary] = useState<EditorialSummary | null>(null)
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [openId, setOpenId] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const admin = Boolean(member?.admin)

  useEffect(() => {
    if (!admin) return
    const controller = new AbortController()
    getSummary(controller.signal)
      .then(setSummary)
      .catch(() => {
        // 숫자를 못 읽어도 검토는 할 수 있다
      })
    return () => controller.abort()
  }, [admin, reload])

  useEffect(() => {
    if (!admin || tab === 'members') return
    const controller = new AbortController()
    const requestKey = `${tab}\n${page}`
    const load: Promise<Rows> =
      tab === 'posts'
        ? getPendingPosts(page, controller.signal)
        : tab === 'board'
          ? getPendingBoard(page, controller.signal)
          : tab === 'comments'
            ? getPendingComments(page, controller.signal)
            : getActions(page, controller.signal)
    load
      .then((data) => setLoaded({ key: requestKey, data }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setLoaded({ key: requestKey, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [admin, tab, page, reload])

  const done = useCallback((message: string) => {
    setActionError(null)
    setNotice(message)
    setOpenId(null)
    setReload((n) => n + 1)
  }, [])

  if (member === undefined) return null
  if (!admin) {
    return (
      <p className="post-status msg msg-error" role="alert">
        편집실은 관리자만 들어올 수 있습니다.
      </p>
    )
  }

  const current = loaded?.key === key ? loaded : null

  async function restore(commentId: number) {
    try {
      const { restored } = await restoreComment(commentId)
      done(restored ? '댓글을 되돌렸습니다. 다시 보여요' : '이미 보이는 댓글입니다')
    } catch (err) {
      setNotice(null)
      setActionError(messageOf(err))
    }
  }

  async function unlock(postId: number) {
    try {
      const { unlocked } = await unlockPost(postId)
      done(unlocked ? '잠금을 풀었습니다. 글 주인이 다시 공개할 수 있어요' : '이미 잠금이 풀린 글입니다')
    } catch (err) {
      setNotice(null)
      setActionError(messageOf(err))
    }
  }

  return (
    <div className="editorial-page">
      <h1 className="editorial-title">편집실</h1>
      <p className="hint">
        신고가 들어온 원고와 댓글을 살펴보고 내리거나 가리거나 반려합니다. 신고한 사람은 보이지 않습니다.
      </p>
      {summary && (
        <ul className="editorial-cards" aria-label="운영 현황">
          {/* 예전 서버가 모르는 숫자는 빼고 보인다 */}
          {CARDS.filter((card) => typeof summary[card.key] === 'number').map((card) => (
            <li
              key={card.key}
              className={
                (card.key === 'pendingReports' || card.key === 'pendingCommentReports') && summary[card.key] > 0
                  ? 'is-alert'
                  : ''
              }
            >
              <span className="editorial-card-label">{card.label}</span>
              <strong className="editorial-card-value">{summary[card.key].toLocaleString('ko-KR')}</strong>
            </li>
          ))}
        </ul>
      )}
      <nav className="editorial-tabs" aria-label="편집실">
        {TABS.map((item) => (
          <Link
            key={item.tab}
            to={hrefFor(item.tab)}
            className="editorial-tab"
            aria-current={item.tab === tab ? 'page' : undefined}
            onClick={() => setOpenId(null)}
          >
            {item.label}
          </Link>
        ))}
      </nav>
      {notice && (
        <p className="msg msg-ok" role="status">
          {notice}
        </p>
      )}
      {actionError && (
        <p className="msg msg-error" role="alert">
          {actionError}
        </p>
      )}
      {tab === 'members' ? (
        <MembersPanel onDone={done} reload={reload} />
      ) : !current ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in current ? (
        <p className="msg msg-error" role="alert">
          {current.error}
        </p>
      ) : current.data.items.length === 0 ? (
        <p className="editorial-empty">
          {tab === 'history' ? '아직 처리한 신고가 없습니다.' : '검토할 신고가 없습니다.'}
        </p>
      ) : (
        <>
          <ol className="editorial-list">
            {tab === 'history'
              ? (current.data.items as ActionRow[]).map((row) => (
                  <li key={row.actionId} className="editorial-row">
                    <span className={`editorial-stamp is-${row.action}`}>{ACTION_LABEL[row.action]}</span>
                    <span className="editorial-row-main">
                      <strong>{row.title}</strong>
                      <span className="hint">
                        {TARGET_LABEL[row.targetKind]}
                        {row.targetKind !== 'member' && ` · 신고 ${row.reportCount}건`} · {formatDate(row.createdAt)}
                        {row.reason && ` · 사유: ${row.reason}`}
                      </span>
                    </span>
                    {row.action === 'taken_down' && row.postId !== null && (
                      <button type="button" className="btn btn-quiet btn-small" onClick={() => unlock(row.postId!)}>
                        잠금 풀기
                      </button>
                    )}
                    {row.action === 'hidden' && row.commentId !== null && (
                      <button type="button" className="btn btn-quiet btn-small" onClick={() => restore(row.commentId!)}>
                        되돌리기
                      </button>
                    )}
                  </li>
                ))
              : (current.data.items as PendingRow[]).map((row) => {
                  const id = rowIdOf(row)
                  const expanded = openId === id
                  return (
                    <li key={id} className={expanded ? 'editorial-row is-open' : 'editorial-row'}>
                      <button
                        type="button"
                        className="editorial-row-toggle"
                        aria-expanded={expanded}
                        onClick={() => setOpenId(expanded ? null : id)}
                      >
                        <span className="editorial-count" aria-label={`신고 ${row.reportCount}건`}>
                          {row.reportCount}
                        </span>
                        <span className="editorial-row-main">
                          <strong>{('commentId' in row ? row.postTitle : row.title) ?? '(지워진 글)'}</strong>
                          <span className="hint">
                            {rowWhere(row)} · {reasonsOf(row)} · 마지막 신고 {formatDate(row.lastAt)}
                          </span>
                        </span>
                      </button>
                      {expanded && (
                        <ReviewPanel
                          kind={tab === 'board' ? 'board' : tab === 'comments' ? 'comment' : 'post'}
                          id={id}
                          onDone={done}
                        />
                      )}
                    </li>
                  )
                })}
          </ol>
          {current.data.totalPages > 1 && (
            <Pagination
              page={current.data.page}
              totalPages={current.data.totalPages}
              hrefFor={(p) => hrefFor(tab, p)}
            />
          )}
        </>
      )}
    </div>
  )
}
