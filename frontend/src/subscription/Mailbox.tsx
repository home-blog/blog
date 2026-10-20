// 머리글 우편함 (specs/016 US2, D-9): 안 읽은 소식 수를 우표 모양 점으로(9개 넘으면 9+), 누르면 최근 소식 목록.
// 소식마다 톱니 우표 한 장: 새 원고는 "원고", 댓글은 "답장", 방명록 엽서는 "엽서"·"회신". 읽은 소식에는 소인이 찍힌다.
// 안 읽은 수는 창이 보일 때만 1분마다 다시 묻는다 (S2-5). 소식을 누르면 읽음으로 바꾸고 그 글로 간다
// 모양 (0.15.0): 항공우편 띠와 편지봉투 덮개 머리, 오늘 날짜 소인, 안 읽은 편지는 형광펜 제목과 봉랍 점,
// 읽는 순간 우표 위에 소인이 찍힌다("모두 읽음"이면 차례로). 전체·안 읽음으로 나눠 본다
import { useCallback, useEffect, useId, useRef, useState, type CSSProperties } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { formatDate } from '../explore/rules'
import {
  getNotifications,
  getUnreadCount,
  readAllNotifications,
  readNotification,
  type Notice,
} from './subscriptionApi'
import './subscription.css'

const POLL_MS = 60_000
/** 소식마다 우표 글자: 새 원고, 댓글은 답장, 내 댓글에 단 답글은 답글, 글마당 채택은 채택 */
const STAMP: Record<Notice['kind'], string> = {
  post: '원고',
  comment: '답장',
  board: '답장',
  accepted: '채택',
  editorial: '편집실',
  reply: '답글',
  guestbook: '엽서',
  gb_reply: '회신',
}

/** 소식마다 보낸 곳 한 줄 (편지 겉봉의 "보낸 사람") */
function fromLine(notice: Notice) {
  switch (notice.kind) {
    case 'post':
      return (
        <>
          <strong>{notice.actorName}</strong>에 새 원고
        </>
      )
    case 'editorial':
      return (
        <>
          <strong>편집실</strong>에서 신고된 내 글을 처리했어요
        </>
      )
    case 'reply':
      return (
        <>
          <strong>{notice.actorName}</strong>님이 내 댓글에 답글을 달았어요
        </>
      )
    case 'guestbook':
      return (
        <>
          <strong>{notice.actorName}</strong>님이 방명록에 엽서를 남겼어요
        </>
      )
    case 'gb_reply':
      return (
        <>
          <strong>{notice.actorName}</strong>님이 내 엽서에 답장했어요
        </>
      )
    case 'accepted':
      return (
        <>
          <strong>{notice.actorName}</strong>님이 내 댓글을 채택했어요
        </>
      )
    default:
      return (
        <>
          <strong>{notice.actorName}</strong>님이 {notice.kind === 'board' ? '글마당 글에 ' : ''}
          댓글을 남겼어요
        </>
      )
  }
}

/** 빈 우편함의 봉투: 올리면 덮개가 열리고 안에서 원고지 쪽지가 올라온다 (2026-10-20 사용자: 재미를) */
function EmptyEnvelope() {
  return (
    <svg className="empty-envelope" viewBox="0 -14 88 78" aria-hidden="true" focusable="false">
      {/* 열린 덮개 (처음엔 접혀 있다) */}
      <polygon className="env-flap-open" points="3,18 85,18 44,-10" />
      {/* 봉투 뒷면 */}
      <rect className="env-back" x="3" y="18" width="82" height="44" rx="3" />
      {/* 원고지 쪽지 */}
      <g className="env-letter">
        <rect x="14" y="22" width="60" height="36" rx="1.5" />
        <path d="M14 31h60M14 40h60M14 49h60M26 22v36M38 22v36M50 22v36M62 22v36" />
      </g>
      {/* 앞 주머니 */}
      <polygon className="env-front" points="3,62 3,26 44,46 85,26 85,62" />
      {/* 닫힌 덮개 */}
      <polygon className="env-flap" points="3,18 85,18 44,44" />
    </svg>
  )
}

/** 소인 날짜: 10.19 */
function postmarkDate(iso: string) {
  const date = new Date(iso)
  return `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

export default function Mailbox() {
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState<Notice[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [onlyUnread, setOnlyUnread] = useState(false)
  // 머리의 오늘 날짜 소인. 열 때 정한다 (그리는 동안 시계를 읽지 않게)
  const [today, setToday] = useState('')
  // 이번에 연 사이에 읽은 소식: 그 우표에만 소인이 쾅 찍히는 움직임을 준다 (처음부터 읽은 소식은 조용히)
  const [stamped, setStamped] = useState<ReadonlySet<number>>(new Set())
  const panelId = useId()
  const wrapRef = useRef<HTMLDivElement>(null)
  // "안 읽음"에서 편지를 읽으면 그 단추가 목록에서 빠진다: 키보드 자리를 이 단추로 돌려준다
  const unreadFilterRef = useRef<HTMLButtonElement>(null)
  const navigate = useNavigate()

  const refreshCount = useCallback((signal?: AbortSignal) => {
    getUnreadCount(signal)
      .then((data) => setUnread(data.unreadCount))
      .catch(() => {
        // 수는 꾸밈이다. 못 받아도 조용히 넘어간다
      })
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    refreshCount(controller.signal)
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') refreshCount()
    }, POLL_MS)
    const onVisible = () => {
      if (document.visibilityState === 'visible') refreshCount()
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      controller.abort()
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [refreshCount])

  // 바깥을 누르거나 Esc면 닫는다
  useEffect(() => {
    if (!open) return
    const onDown = (event: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(event.target as Node)) setOpen(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  async function toggle() {
    if (open) {
      setOpen(false)
      return
    }
    setOpen(true)
    setError(null)
    setStamped(new Set())
    setToday(postmarkDate(new Date().toISOString()))
    try {
      const page = await getNotifications(1)
      setItems(page.items)
      setUnread(page.unreadCount)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    }
  }

  async function openNotice(notice: Notice) {
    if (!notice.read) {
      try {
        await readNotification(notice.notificationId)
        setStamped((set) => new Set(set).add(notice.notificationId))
        setItems(
          (list) => list?.map((n) => (n.notificationId === notice.notificationId ? { ...n, read: true } : n)) ?? null,
        )
        setUnread((n) => Math.max(0, n - 1))
      } catch {
        // 읽음 표시는 다음에 다시 할 수 있다
      }
    }
    const target =
      notice.blogId != null && (notice.kind === 'guestbook' || notice.kind === 'gb_reply')
        ? `/blog/${notice.blogId}/guestbook`
        : notice.boardPostId !== null
          ? `/board/${notice.boardPostId}`
          : notice.postId !== null
            ? `/posts/${notice.postId}${notice.kind === 'reply' ? '#comments' : ''}`
            : null
    if (notice.available && target) {
      setOpen(false)
      navigate(target)
    } else if (onlyUnread && !notice.read) {
      unreadFilterRef.current?.focus()
    }
  }

  async function readAll() {
    try {
      await readAllNotifications()
      setStamped(new Set(items?.filter((n) => !n.read).map((n) => n.notificationId) ?? []))
      setItems((list) => list?.map((n) => ({ ...n, read: true })) ?? null)
      setUnread(0)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    }
  }

  return (
    <div className="mailbox" ref={wrapRef}>
      <button
        type="button"
        className="mailbox-button"
        aria-expanded={open}
        aria-controls={panelId}
        aria-label={unread > 0 ? `우편함, 안 읽은 소식 ${unread}개` : '우편함'}
        onClick={toggle}
      >
        <span aria-hidden="true">✉</span>
        {unread > 0 && (
          // 수가 바뀌면 key가 바뀌어 배지가 한 번 통통 튄다 (새 편지가 왔다)
          <span key={unread} className="mailbox-badge" aria-hidden="true">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>
      {open && (
        <div id={panelId} className="mailbox-panel" role="dialog" aria-label="우편함">
          <div className="mailbox-flap">
            <div className="mailbox-heading">
              <span className="mailbox-kicker" aria-hidden="true">
                글결 우체국
              </span>
              <strong className="mailbox-name">우편함</strong>
              <span className="mailbox-count">
                {unread > 0 ? (
                  <>
                    안 읽은 편지 <b>{unread}</b>통
                  </>
                ) : (
                  '모두 읽었어요'
                )}
              </span>
            </div>
            <span className="postmark-today" aria-hidden="true">
              <span>{today}</span>
              <small>글결</small>
            </span>
          </div>
          <div className="mailbox-tools">
            <div className="mailbox-filter" role="group" aria-label="소식 고르기">
              <button type="button" aria-pressed={!onlyUnread} onClick={() => setOnlyUnread(false)}>
                전체
              </button>
              <button type="button" ref={unreadFilterRef} aria-pressed={onlyUnread} onClick={() => setOnlyUnread(true)}>
                안 읽음
              </button>
            </div>
            <button
              type="button"
              className="btn btn-quiet btn-small"
              onClick={readAll}
              disabled={unread === 0 || items === null}
            >
              모두 읽음
            </button>
          </div>
          {error && (
            <p className="msg msg-error" role="alert">
              {error}
            </p>
          )}
          {items === null ? (
            !error && <p className="hint mailbox-loading">편지를 꺼내는 중</p>
          ) : items.length === 0 ? (
            <div className="mailbox-empty">
              <EmptyEnvelope />
              <p className="hint">
                아직 온 소식이 없어요. 마음에 드는 블로그를 <Link to="/subscriptions">받아 보면</Link> 새 원고가 여기로
                옵니다.
              </p>
            </div>
          ) : onlyUnread && items.every((n) => n.read) ? (
            <div className="mailbox-empty">
              <span className="postmark-done" aria-hidden="true">
                다 읽음
              </span>
              <p className="hint">최근 편지 중에는 안 읽은 편지가 없어요.</p>
            </div>
          ) : (
            <ul className="mailbox-list">
              {items
                .filter((n) => !onlyUnread || !n.read)
                .map((notice, index) => (
                  <li key={notice.notificationId} style={{ '--i': index } as CSSProperties}>
                    <button
                      type="button"
                      className={notice.read ? 'letter is-read' : 'letter'}
                      onClick={() => openNotice(notice)}
                    >
                      <span className={`stamp stamp-${notice.kind}`} aria-hidden="true">
                        <span className="stamp-face">
                          <span className="stamp-tiny">글결</span>
                          <span className="stamp-word">{STAMP[notice.kind]}</span>
                        </span>
                      </span>
                      {notice.read && (
                        <span
                          className={stamped.has(notice.notificationId) ? 'postmark is-stamping' : 'postmark'}
                          aria-hidden="true"
                        >
                          {postmarkDate(notice.createdAt)}
                        </span>
                      )}
                      <span className="letter-text">
                        <span className="letter-from">
                          {fromLine(notice)}
                          {!notice.read && <span className="visually-hidden"> (안 읽음)</span>}
                        </span>
                        <span className="letter-subject">
                          <span className="letter-subject-ink">
                            {notice.kind === 'guestbook' || notice.kind === 'gb_reply'
                              ? `${notice.postTitle ?? ''} 방명록`
                              : notice.available || notice.kind === 'editorial'
                                ? notice.postTitle
                                : '볼 수 없는 글'}
                          </span>
                        </span>
                        {notice.note && <span className="letter-note">사유: {notice.note}</span>}
                        <time className="letter-time" dateTime={notice.createdAt}>
                          {formatDate(notice.createdAt)}
                        </time>
                      </span>
                      {!notice.read && <span className="letter-seal" aria-hidden="true" />}
                    </button>
                  </li>
                ))}
            </ul>
          )}
          <p className="mailbox-foot">
            <Link to="/subscriptions" onClick={() => setOpen(false)}>
              받아 보는 글 모아 보기 →
            </Link>
          </p>
        </div>
      )}
    </div>
  )
}
