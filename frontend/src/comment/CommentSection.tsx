// 글 상세 아래 댓글 영역 (specs/005 US1, T021): 댓글 수, 오래된 순 목록, 입력칸
// - 내용은 글자로만 그린다 (white-space: pre-wrap, HTML로 넣지 않음, FR-030)
// - 탈퇴한 작성자는 "탈퇴한 사용자" (FR-007)
// - 로그인하지 않았으면 입력칸 대신 안내와 로그인 버튼. 로그인하면 이 글로 돌아온다 (FR-001)
// - 등록 중에는 버튼을 잠근다. 실패 문구는 서버 것 그대로 보여 준다
// - 지울 수 있는 댓글(canDelete)에만 삭제 버튼. 확인 창에서 취소하면 요청을 보내지 않는다 (US2, FR-004). 고치기 버튼은 없다 (FR-005)
// - 답글·비밀 댓글 (specs/019): 답글은 원댓글 아래 붉은 교정선을 따라 한 단계. 비밀 댓글은 "봉한 쪽지"(읽을 수 없으면
//   서버가 내용을 빼고 보내 봉인 도장만 보인다). 답글이 남은 댓글을 지우면 두 줄 그은 "지운 칸"으로 남는다
// - 댓글 신고 (specs/022): 남의 보이는 댓글에 신고 단추. 신고가 모여 저절로 가려지거나 편집실이 가린 댓글은
//   먹줄을 그은 원고지 칸("편집실이 가린 댓글입니다")으로 보이고, 쓴 사람·블로그 주인만 지울 수 있다
import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import ConfirmDialog from '../components/ConfirmDialog'
import ReportDialog from '../community/ReportDialog'
import {
  deleteComment,
  getComments,
  reportComment,
  writeComment,
  type CommentItem,
  type CommentReportResult,
  type WriteOptions,
} from './commentApi'
import { COMMENT_MAX_LENGTH, commentLength, commentMessages } from './rules'
import { afterDelete, afterHidden, insertReply, liveCount, threads } from './thread'
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
  const [secret, setSecret] = useState(false)
  const [replyTo, setReplyTo] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)
  // 목록 읽기마다 올린다. 늦게 온 옛 응답이 새 목록을 덮어쓰지 않게 마지막 읽기의 답만 쓴다
  const loadVersion = useRef(0)
  // 지금 화면의 글·로그인 상태. 등록을 기다리는 사이 로그인한 사람이 바뀌면 끝난 등록의 결과를 화면에 넣지 않는다
  const currentKey = useRef(key)
  const inputId = useId()
  const sectionRef = useRef<HTMLElement>(null)
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [reportNotice, setReportNotice] = useState<string | null>(null)

  // 블로그 관리의 댓글 관리에서 글 제목을 눌러 오면(/posts/:id#comments) 댓글 자리로 옮겨 간다 (specs/006 FR-024)
  useEffect(() => {
    if (location.hash === '#comments') sectionRef.current?.scrollIntoView()
  }, [location.hash])

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
  const count = comments ? liveCount(comments) : initialCount
  const length = commentLength(body)

  /** 쓰기 요청과 목록 고치기. 등록을 기다리는 사이 화면이 바뀌었으면 null (결과를 넣지 않는다) */
  async function send(text: string, options: WriteOptions): Promise<CommentItem | null> {
    const created = await writeComment(postId, text, options)
    // 등록은 끝났다. 목록이 있으면 제자리에 넣고, 아직 없거나 읽기에 실패했으면 다시 읽는다 (그 실패는 등록 실패가 아니다)
    if (currentKey.current !== key) return null
    if (comments) {
      setLoaded((prev) =>
        prev?.key === key && 'comments' in prev
          ? {
              ...prev,
              comments: created.parentId === null ? [...prev.comments, created] : insertReply(prev.comments, created),
            }
          : prev,
      )
    } else {
      load(key)
    }
    return created
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busyRef.current) return
    if (length === 0) return setError(commentMessages.empty)
    if (length > COMMENT_MAX_LENGTH) return setError(commentMessages.tooLong)
    busyRef.current = true
    setBusy(true)
    setError(null)
    try {
      if (await send(body, { secret })) {
        setBody('')
        setSecret(false)
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
    // 지우는 댓글에 열어 둔 답글 칸은 닫는다
    setReplyTo((current) => (current === commentId ? null : current))
    if (commentId === null || deletingId !== null) return
    setDeletingId(commentId)
    setDeleteError(null)
    try {
      await deleteComment(commentId)
      setLoaded((prev) =>
        prev && 'comments' in prev ? { ...prev, comments: afterDelete(prev.comments, commentId) } : prev,
      )
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : commentMessages.failed)
    } finally {
      setDeletingId(null)
    }
  }

  /** 신고가 받아들여졌다. 저절로 가려졌으면 그 자리에서 먹줄 칸으로 바꾸고, 열어 둔 답글 칸을 닫는다 */
  function reported(commentId: number, result: CommentReportResult) {
    if (!result.hidden) return
    setReplyTo((current) => (current === commentId ? null : current))
    setLoaded((prev) =>
      prev && 'comments' in prev ? { ...prev, comments: afterHidden(prev.comments, commentId) } : prev,
    )
  }

  /** 카드의 단추들. 답글 단추는 늘 원댓글에 단다 */
  function cardActions(rootId: number) {
    return {
      rootId,
      deleting: deletingId,
      onDelete: (id: number) => setDeleteTarget(id),
      onReply: () => setReplyTo((current) => (current === rootId ? null : rootId)),
      replying: replyTo === rootId,
      onReported: reported,
      onReportNotice: setReportNotice,
      // 그사이 지워지거나 가려진 댓글: 목록을 다시 읽어 지금 모습을 보여 준다
      onReportFailed: (err: unknown) => {
        if (err instanceof ApiError && err.status === 404) load(key)
      },
    }
  }

  /** 답글 칸을 닫고, 키보드 자리를 그 원댓글의 답글 단추로 돌려준다 */
  function closeReply(rootId: number) {
    setReplyTo(null)
    requestAnimationFrame(() =>
      sectionRef.current?.querySelector<HTMLButtonElement>(`[data-reply-root="${rootId}"]`)?.focus(),
    )
  }

  return (
    <section id="comments" ref={sectionRef} className="comments" aria-labelledby={`${inputId}-title`}>
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
      {reportNotice && (
        <p className="msg msg-ok comment-report-notice" role="status">
          {reportNotice}
        </p>
      )}
      {comments && comments.length > 0 && (
        <ol className="comment-list">
          {threads(comments).map(({ root, replies }) => (
            <li key={root.id} className="comment-thread">
              <CommentCard comment={root} {...cardActions(root.id)} />
              {replies.length > 0 && (
                <ol className="comment-replies" aria-label="답글">
                  {replies.map((reply) => (
                    <li key={reply.id}>
                      <CommentCard comment={reply} {...cardActions(root.id)} />
                    </li>
                  ))}
                </ol>
              )}
              {/* 지운 칸이 되었거나 보는 사람이 바뀌어 답글을 달 수 없으면 열려 있던 칸도 닫힌다 */}
              {replyTo === root.id && member && root.canReply && !root.deleted && (
                <ReplyForm
                  root={root}
                  onCancel={() => closeReply(root.id)}
                  onSend={async (text, wantSecret) => {
                    try {
                      const created = await send(text, { parentId: root.id, secret: wantSecret })
                      if (created) closeReply(root.id)
                    } catch (err) {
                      // 그사이 원댓글이 지워졌다: 목록을 다시 읽어 지금 모습을 보여 준다
                      if (err instanceof ApiError && err.status === 404) load(key)
                      throw err
                    }
                  }}
                />
              )}
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
          <label className="comment-seal-toggle">
            <input type="checkbox" checked={secret} onChange={(e) => setSecret(e.target.checked)} />
            봉해서 남기기 <span className="hint">(블로그 주인만 읽어요)</span>
          </label>
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
        <p>댓글을 삭제할까요? 답글이 남아 있으면 "지운 댓글" 자리만 남아요.</p>
      </ConfirmDialog>
    </section>
  )
}

interface CardProps {
  comment: CommentItem
  rootId: number
  deleting: number | null
  replying: boolean
  onDelete: (id: number) => void
  onReply: () => void
  onReported: (commentId: number, result: CommentReportResult) => void
  onReportNotice: (message: string) => void
  onReportFailed: (error: unknown) => void
}

/** 댓글 한 장: 지운 칸, 가린 칸, 봉한 쪽지, 보통 댓글 */
function CommentCard({
  comment,
  rootId,
  deleting,
  replying,
  onDelete,
  onReply,
  onReported,
  onReportNotice,
  onReportFailed,
}: CardProps) {
  const reply = comment.parentId !== null
  if (comment.deleted) {
    return (
      <article className="comment is-erased">
        <p className="comment-erased">{commentMessages.erased}</p>
      </article>
    )
  }
  if (comment.hidden) {
    return (
      <article className={reply ? 'comment is-reply is-inked' : 'comment is-inked'}>
        <p className="comment-inked">
          <span className="comment-ink-cells" aria-hidden="true">
            <span />
            <span />
            <span />
          </span>
          {commentMessages.inked}
          {comment.canDelete && (
            <button
              type="button"
              className="btn btn-quiet btn-small comment-inked-delete"
              disabled={deleting !== null}
              onClick={() => onDelete(comment.id)}
            >
              {deleting === comment.id ? '삭제하는 중' : '삭제'}
            </button>
          )}
        </p>
      </article>
    )
  }
  if (comment.sealed) {
    return (
      <article className={reply ? 'comment is-reply is-sealed' : 'comment is-sealed'}>
        <p className="comment-sealed">
          <span className="comment-seal" aria-hidden="true">
            봉
          </span>
          {commentMessages.sealed}
          <time className="hint" dateTime={comment.createdAt}>
            {formatTime(comment.createdAt)}
          </time>
        </p>
      </article>
    )
  }
  return (
    <article className={reply ? 'comment is-reply' : 'comment'}>
      <p className="comment-meta">
        <span className={comment.author.withdrawn ? 'comment-author is-withdrawn' : 'comment-author'}>
          {comment.author.withdrawn ? commentMessages.withdrawn : comment.author.nickname}
        </span>
        {comment.secret && <span className="comment-tag is-secret">봉한 쪽지</span>}
        <time className="hint" dateTime={comment.createdAt}>
          {formatTime(comment.createdAt)}
        </time>
        <span className="comment-actions">
          {comment.canReply && (
            <button
              type="button"
              className="btn btn-quiet btn-small"
              aria-expanded={replying}
              data-reply-root={rootId}
              onClick={onReply}
            >
              답글
            </button>
          )}
          {comment.canDelete && (
            <button
              type="button"
              className="btn btn-quiet btn-small"
              disabled={deleting !== null}
              onClick={() => onDelete(comment.id)}
            >
              {deleting === comment.id ? '삭제하는 중' : '삭제'}
            </button>
          )}
          {comment.canReport && (
            <ReportDialog<CommentReportResult>
              title="댓글 신고"
              send={(reason, detail) => reportComment(comment.id, reason, detail)}
              onReported={(result) => onReported(comment.id, result)}
              onDone={onReportNotice}
              onFailed={onReportFailed}
            />
          )}
        </span>
      </p>
      <p className="comment-body">{comment.body}</p>
    </article>
  )
}

interface ReplyFormProps {
  root: CommentItem
  onSend: (body: string, secret: boolean) => Promise<void>
  onCancel: () => void
}

/** 원댓글 아래에 여는 답글 입력칸. 비밀 댓글에 다는 답글은 늘 봉한다 (서버도 그렇게 저장한다) */
function ReplyForm({ root, onSend, onCancel }: ReplyFormProps) {
  const [body, setBody] = useState('')
  const [secret, setSecret] = useState(root.secret)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const inputId = useId()
  const length = commentLength(body)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    if (length === 0) return setError(commentMessages.empty)
    if (length > COMMENT_MAX_LENGTH) return setError(commentMessages.tooLong)
    setBusy(true)
    setError(null)
    try {
      await onSend(body, secret || root.secret)
    } catch (err) {
      setError(err instanceof ApiError ? (err.messageFor('body') ?? err.message) : commentMessages.failed)
      setBusy(false)
    }
  }

  return (
    <form className="comment-form comment-reply-form" onSubmit={submit} noValidate>
      <label htmlFor={inputId} className="visually-hidden">
        답글 내용
      </label>
      <textarea
        id={inputId}
        className="comment-input"
        value={body}
        rows={2}
        autoFocus
        placeholder="답글을 남겨 주세요"
        aria-invalid={error !== null}
        aria-describedby={`${inputId}-count${error ? ` ${inputId}-error` : ''}`}
        onChange={(e) => {
          setBody(e.target.value)
          setError(null)
        }}
      />
      <label className="comment-seal-toggle">
        <input
          type="checkbox"
          checked={secret || root.secret}
          disabled={root.secret}
          onChange={(e) => setSecret(e.target.checked)}
        />
        봉해서 남기기{' '}
        <span className="hint">
          {root.secret ? '(봉한 쪽지에 단 답글은 늘 봉해요)' : '(블로그 주인과 원댓글 쓴 사람만 읽어요)'}
        </span>
      </label>
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
        <button type="button" className="btn btn-quiet btn-small" onClick={onCancel}>
          취소
        </button>
        <button type="submit" className="btn btn-primary btn-small" disabled={busy}>
          {busy ? '등록하는 중' : '답글 등록'}
        </button>
      </div>
    </form>
  )
}
