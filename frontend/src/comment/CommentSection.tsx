// 글 상세 아래 댓글 영역 (specs/005 US1, T021): 댓글 수, 오래된 순 목록, 입력칸
// - 내용은 글자로만 그린다 (white-space: pre-wrap, HTML로 넣지 않음, FR-030)
// - 탈퇴한 작성자는 "탈퇴한 사용자" (FR-007)
// - 로그인하지 않았으면 입력칸 대신 안내와 로그인 버튼. 로그인하면 이 글로 돌아온다 (FR-001)
// - 등록 중에는 버튼을 잠근다. 실패 문구는 서버 것 그대로 보여 준다
// - 지울 수 있는 댓글(canDelete)에만 삭제 버튼. 확인 창에서 취소하면 요청을 보내지 않는다 (US2, FR-004). 고치기 버튼은 없다 (FR-005)
import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import ConfirmDialog from '../components/ConfirmDialog'
import { deleteComment, getComments, writeComment, type CommentItem } from './commentApi'
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
  // 목록 읽기마다 올린다. 늦게 온 옛 응답이 새 목록을 덮어쓰지 않게 마지막 읽기의 답만 쓴다
  const loadVersion = useRef(0)
  // 지금 화면의 글·로그인 상태. 등록을 기다리는 사이 로그인한 사람이 바뀌면 끝난 등록의 결과를 화면에 넣지 않는다
  const currentKey = useRef(key)
  const inputId = useId()
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const load = useCallback(
    (requestKey: string, signal?: AbortSignal) => {
      const version = ++loadVersion.current
      getComments(postId, signal)
        .then((list) => {
          if (version === loadVersion.current) setLoaded({ key: requestKey, comments: list.comments })
        })
        .catch((err: unknown) => {
          if (signal?.aborted || version !== loadVersion.current) return
          setLoaded({ key: requestKey, error: err instanceof ApiError ? err.message : commentMessages.failed })
        })
    },
    [postId],
  )

  // 그리기가 끝나자마자 지금 화면의 key를 적어 둔다 (그 뒤에 끝난 등록이 새 key를 보게)
  useLayoutEffect(() => {
    currentKey.current = key
  }, [key])

  // 로그인 상태가 바뀌면 다시 읽는다 (삭제할 수 있는 댓글이 달라진다)
  useEffect(() => {
    if (viewer === null) return
    const controller = new AbortController()
    load(key, controller.signal)
    return () => controller.abort()
  }, [key, viewer, load])

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
      // 등록은 끝났다. 목록이 있으면 맨 아래에 붙이고, 아직 없거나 읽기에 실패했으면 다시 읽는다 (그 실패는 등록 실패가 아니다)
      if (currentKey.current !== key) return
      setBody('')
      if (comments) {
        setLoaded((prev) => (prev?.key === key && 'comments' in prev ? { ...prev, comments: [...prev.comments, created] } : prev))
      } else {
        load(key)
      }
    } catch (err) {
      if (currentKey.current !== key) return
      setError(err instanceof ApiError ? (err.messageFor('body') ?? err.message) : commentMessages.failed)
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  async function remove() {
    const commentId = deleteTarget
    setDeleteTarget(null)
    if (commentId === null || deletingId !== null) return
    setDeletingId(commentId)
    setDeleteError(null)
    try {
      await deleteComment(commentId)
      setLoaded((prev) =>
        prev && 'comments' in prev ? { ...prev, comments: prev.comments.filter((c) => c.id !== commentId) } : prev,
      )
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : commentMessages.failed)
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section className="comments" aria-labelledby={`${inputId}-title`}>
      <h2 id={`${inputId}-title`} className="comments-title">
        댓글 <span className="comments-count">{count}</span>
      </h2>

      {current && 'error' in current && (
        <p className="msg msg-error comment-load-error" role="alert">
          {current.error}{' '}
          <button type="button" className="btn btn-quiet btn-small" onClick={() => load(key)}>
            다시 읽기
          </button>
        </p>
      )}
      {deleteError && (
        <p className="msg msg-error" role="alert">
          {deleteError}
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
                {comment.canDelete && (
                  <button
                    type="button"
                    className="btn btn-quiet btn-small comment-actions"
                    disabled={deletingId !== null}
                    onClick={() => setDeleteTarget(comment.id)}
                  >
                    {deletingId === comment.id ? '삭제하는 중' : '삭제'}
                  </button>
                )}
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

      <ConfirmDialog
        open={deleteTarget !== null}
        title="댓글 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setDeleteTarget(null)}
        onConfirm={remove}
      >
        <p>댓글을 삭제할까요?</p>
      </ConfirmDialog>
    </section>
  )
}
