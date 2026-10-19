// 머리글 우편함 (specs/016 US2, D-9): 안 읽은 소식 수를 우표 모양 점으로(9개 넘으면 9+), 누르면 최근 소식 목록.
// 소식마다 우표 한 장: 새 원고는 "원고", 댓글은 "답장". 읽은 소식에는 소인이 찍힌다.
// 안 읽은 수는 창이 보일 때만 1분마다 다시 묻는다 (S2-5). 소식을 누르면 읽음으로 바꾸고 그 글로 간다
import { useCallback, useEffect, useId, useRef, useState } from 'react'
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
/** 소식마다 우표 글자: 새 원고, 댓글은 답장, 글마당 채택은 채택 */
const STAMP: Record<Notice['kind'], string> = { post: '원고', comment: '답장', board: '답장', accepted: '채택' }

export default function Mailbox() {
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState<Notice[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const panelId = useId()
  const wrapRef = useRef<HTMLDivElement>(null)
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
        setItems(
          (list) => list?.map((n) => (n.notificationId === notice.notificationId ? { ...n, read: true } : n)) ?? null,
        )
        setUnread((n) => Math.max(0, n - 1))
      } catch {
        // 읽음 표시는 다음에 다시 할 수 있다
      }
    }
    const target =
      notice.boardPostId !== null
        ? `/board/${notice.boardPostId}`
        : notice.postId !== null
          ? `/posts/${notice.postId}`
          : null
    if (notice.available && target) {
      setOpen(false)
      navigate(target)
    }
  }

  async function readAll() {
    try {
      await readAllNotifications()
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
          <span className="mailbox-badge" aria-hidden="true">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>
      {open && (
        <div id={panelId} className="mailbox-panel" role="dialog" aria-label="우편함">
          <div className="mailbox-head">
            <strong>우편함</strong>
            <button type="button" className="btn btn-quiet btn-small" onClick={readAll} disabled={unread === 0}>
              모두 읽음
            </button>
          </div>
          {error && (
            <p className="msg msg-error" role="alert">
              {error}
            </p>
          )}
          {items === null ? (
            !error && <p className="hint">불러오는 중</p>
          ) : items.length === 0 ? (
            <p className="hint mailbox-empty">
              아직 온 소식이 없어요. 마음에 드는 블로그를 <Link to="/subscriptions">받아 보면</Link> 새 원고가 여기로
              옵니다.
            </p>
          ) : (
            <ul className="mailbox-list">
              {items.map((notice) => (
                <li key={notice.notificationId}>
                  <button
                    type="button"
                    className={notice.read ? 'mailbox-item is-read' : 'mailbox-item'}
                    onClick={() => openNotice(notice)}
                  >
                    <span className={`stamp stamp-${notice.kind}`} aria-hidden="true">
                      {STAMP[notice.kind]}
                    </span>
                    <span className="mailbox-text">
                      <span className="mailbox-line">
                        {notice.kind === 'post' ? (
                          <>
                            <strong>{notice.actorName}</strong>에 새 원고
                          </>
                        ) : notice.kind === 'accepted' ? (
                          <>
                            <strong>{notice.actorName}</strong>님이 내 댓글을 채택했어요
                          </>
                        ) : (
                          <>
                            <strong>{notice.actorName}</strong>님이 {notice.kind === 'board' ? '글마당 글에 ' : ''}
                            댓글을 남겼어요
                          </>
                        )}
                        {!notice.read && <span className="visually-hidden"> (안 읽음)</span>}
                      </span>
                      <span className="mailbox-title">{notice.available ? notice.postTitle : '볼 수 없는 글'}</span>
                      <time className="mailbox-time" dateTime={notice.createdAt}>
                        {formatDate(notice.createdAt)}
                      </time>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <p className="mailbox-foot">
            <Link to="/subscriptions" onClick={() => setOpen(false)}>
              받아 보는 글 모아 보기
            </Link>
          </p>
        </div>
      )}
    </div>
  )
}
