// 글 상세 아래 댓글 영역 (specs/005 US1, T021): 댓글 수, 오래된 순 목록, 입력칸
// - 내용은 글자로만 그린다 (white-space: pre-wrap, HTML로 넣지 않음, FR-030)
// - 탈퇴한 작성자는 "탈퇴한 사용자" (FR-007)
// - 로그인하지 않았으면 입력칸 대신 안내와 로그인 버튼. 로그인하면 이 글로 돌아온다 (FR-001)
// - 등록 중에는 버튼을 잠근다. 실패 문구는 서버 것 그대로 보여 준다
import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import { getComments, writeComment, type CommentItem } from './commentApi'
import { COMMENT_MAX_LENGTH, commentLength, commentMessages } from './rules'
import './comment.css'

type Loaded = { key: string; comments: CommentItem[] } | { key: string; error: string }

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}

interface Props {
  postId: number
  /** 글 상세가 알려 준 댓글 수. 목록을 받기 전에 보여 준다 */
  initialCount: number
}

export default function CommentSection({ postId, initialCount }: Props) {
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const location = useLocation()
  const viewer = member === undefined ? null : (member?.id ?? 'guest')
  const key = `${postId}:${viewer}`
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [body, setBody] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)
  const inputId = useId()

  // 로그인 상태가 바뀌면 다시 읽는다 (삭제할 수 있는 댓글이 달라진다)
  useEffect(() => {
    if (viewer === null) return
    const controller = new AbortController()
    const requestKey = `${postId}:${viewer}`
    getComments(postId, controller.signal)
      .then((list) => setLoaded({ key: requestKey, comments: list.comments }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoaded({ key: requestKey, error: err instanceof ApiError ? err.message : commentMessages.failed })
      })
    return () => controller.abort()
  }, [postId, viewer])

  const current = loaded?.key === key ? loaded : null
  const comments = current && 'comments' in current ? current.comments : null
  const count = comments ? comments.length : initialCount
  const length = commentLength(body)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busyRef.current) return
    if (length === 0) return setError(commentMessages.empty)
    if (length > COMMENT_MAX_LENGTH) return setError(commentMessages.tooLong)
    busyRef.current = true
    setBusy(true)
    setError(null)
    try {
      const created = await writeComment(postId, body)
      setLoaded((prev) => (prev && 'comments' in prev ? { ...prev, comments: [...prev.comments, created] } : prev))
      setBody('')
    } catch (err) {
      setError(err instanceof ApiError ? (err.messageFor('body') ?? err.message) : commentMessages.failed)
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  return (
    <section className="comments" aria-labelledby={`${inputId}-title`}>
      <h2 id={`${inputId}-title`} className="comments-title">
        댓글 <span className="comments-count">{count}</span>
      </h2>

      {current && 'error' in current && (
        <p className="msg msg-error" role="alert">
          {current.error}
        </p>
      )}
      {comments && comments.length > 0 && (
        <ol className="comment-list">
          {comments.map((comment) => (
            <li key={comment.id} className="comment">
              <p className="comment-meta">
                <span className={comment.author.withdrawn ? 'comment-author is-withdrawn' : 'comment-author'}>
                  {comment.author.withdrawn ? commentMessages.withdrawn : comment.author.nickname}
                </span>
                <time className="hint" dateTime={comment.createdAt}>
                  {formatTime(comment.createdAt)}
                </time>
              </p>
              <p className="comment-body">{comment.body}</p>
            </li>
          ))}
        </ol>
      )}

      {member ? (
        <form className="comment-form" onSubmit={submit} noValidate>
          <label htmlFor={inputId} className="visually-hidden">
            댓글 내용
          </label>
          <textarea
            id={inputId}
            className="comment-input"
            value={body}
            rows={3}
            placeholder="댓글을 남겨 주세요"
            aria-invalid={error !== null}
            aria-describedby={`${inputId}-count${error ? ` ${inputId}-error` : ''}`}
            onChange={(e) => {
              setBody(e.target.value)
              setError(null)
            }}
          />
          <div className="comment-form-foot">
            {error ? (
              <p id={`${inputId}-error`} className="msg msg-error" role="alert">
                {error}
              </p>
            ) : (
              <span />
            )}
            <span id={`${inputId}-count`} className={`comment-length${length > COMMENT_MAX_LENGTH ? ' is-over' : ''}`}>
              {length} / {COMMENT_MAX_LENGTH}
            </span>
            <button type="submit" className="btn btn-primary btn-small" disabled={busy}>
              {busy ? '등록하는 중' : '등록'}
            </button>
          </div>
        </form>
      ) : (
        member === null && (
          <div className="comment-login">
            <p className="hint">{commentMessages.loginRequired}</p>
            <button type="button" className="btn btn-outline btn-small" onClick={() => openLogin(location.pathname)}>
              로그인
            </button>
          </div>
        )
      )}
    </section>
  )
}
