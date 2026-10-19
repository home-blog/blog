// 글마당 글 읽기 (specs/017 US1, US3, US4): /board/:boardPostId. 누구나 본다
// 본문은 블로그 글과 같은 마크다운 규칙. 아래에 댓글, 물음을 쓴 사람에게는 댓글마다 "채택" 단추, 채택한 댓글에는 붉은 도장
import { lazy, Suspense, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { useLoginPrompt } from '../auth/loginPrompt'
import ConfirmDialog from '../components/ConfirmDialog'
import ProofNotFound from '../components/ProofNotFound'
import Seal from '../components/Seal'
import { formatDate } from '../explore/rules'
import { ApiError } from '../api/client'
import {
  acceptComment,
  deleteBoardComment,
  deleteBoardPost,
  getBoardComments,
  getBoardPost,
  KIND_LABEL,
  messageOf,
  unacceptComment,
  writeBoardComment,
  writerName,
  type BoardCommentList,
  type BoardDetail,
} from './boardApi'
import '../post/post-detail.css'
import './board.css'

const MarkdownView = lazy(() => import('../post/MarkdownView'))
const COMMENT_MAX = 1000

type Loaded = { post: BoardDetail } | { error: string; notFound: boolean }
type Confirm = { type: 'post' } | { type: 'comment'; commentId: number } | null

export default function BoardPostPage() {
  const { boardPostId = '' } = useParams()
  const id = Number(boardPostId)
  const validId = /^\d+$/.test(boardPostId) && Number.isSafeInteger(id) && id > 0
  const { member } = useAuth()
  const { open } = useLoginPrompt()
  const navigate = useNavigate()
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [comments, setComments] = useState<BoardCommentList | null>(null)
  const [reload, setReload] = useState(0)
  const [draft, setDraft] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirm, setConfirm] = useState<Confirm>(null)

  useEffect(() => {
    if (!validId) return
    const controller = new AbortController()
    getBoardPost(id, controller.signal)
      .then((post) => setLoaded({ post }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoaded({ error: messageOf(err), notFound: err instanceof ApiError && err.status === 404 })
      })
    return () => controller.abort()
  }, [id, validId])

  useEffect(() => {
    if (!validId) return
    const controller = new AbortController()
    getBoardComments(id, controller.signal)
      .then(setComments)
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setError(messageOf(err))
      })
    return () => controller.abort()
  }, [id, validId, reload, member])

  if (!validId || (loaded && 'error' in loaded && loaded.notFound)) {
    return <ProofNotFound struck="쪽" inserted="글" particle="이" message="※ 존재하지 않는 글입니다" />
  }
  if (loaded && 'error' in loaded) {
    return (
      <p className="post-status msg msg-error" role="alert">
        {loaded.error}
      </p>
    )
  }
  if (!loaded) return <p className="post-status hint">불러오는 중</p>
  const { post } = loaded

  async function act(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      setReload((n) => n + 1)
    } catch (err) {
      setError(messageOf(err))
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!member) {
      open()
      return
    }
    const content = draft.trim()
    if (!content) {
      setError('※ 댓글 내용을 입력해 주세요')
      return
    }
    setBusy(true)
    await act(async () => {
      await writeBoardComment(post.boardPostId, content)
      setDraft('')
    })
    setBusy(false)
  }

  async function confirmed() {
    const target = confirm
    setConfirm(null)
    if (target?.type === 'post') {
      try {
        await deleteBoardPost(post.boardPostId)
        navigate('/board', { replace: true })
      } catch (err) {
        setError(messageOf(err))
      }
    } else if (target?.type === 'comment') {
      await act(() => deleteBoardComment(target.commentId))
    }
  }

  return (
    <article className="post board-post">
      <p className="board-back">
        <Link to="/board">← 글마당</Link>
      </p>
      <header className="post-head">
        <p className="board-meta">
          <span className="board-topic-tag">{post.topic}</span>
          <span className={post.kind === 'question' ? 'board-kind' : 'board-kind is-talk'}>
            {KIND_LABEL[post.kind]}
          </span>
          <span>{writerName(post.writer)}</span>
          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
          <span>조회 {post.views.toLocaleString('ko-KR')}</span>
        </p>
        <h1 className="post-title">{post.title}</h1>
      </header>
      <Suspense fallback={<p className="hint">불러오는 중</p>}>
        <MarkdownView source={post.content} />
      </Suspense>
      {(post.mine || post.canDelete) && (
        <p className="board-actions">
          {post.mine && (
            <Link to={`/board/${post.boardPostId}/edit`} className="btn btn-outline btn-small">
              수정
            </Link>
          )}
          <button type="button" className="btn btn-danger btn-small" onClick={() => setConfirm({ type: 'post' })}>
            삭제
          </button>
        </p>
      )}

      <section className="board-comments" aria-labelledby="board-comments-title">
        <h2 id="board-comments-title" className="board-comments-title">
          댓글 {comments ? comments.count : ''}
        </h2>
        {error && (
          <p className="msg msg-error" role="alert">
            {error}
          </p>
        )}
        {comments === null ? (
          !error && <p className="hint">불러오는 중</p>
        ) : comments.comments.length === 0 ? (
          <p className="hint">첫 댓글을 남겨 보세요</p>
        ) : (
          <ol className="board-comment-list">
            {comments.comments.map((comment) => (
              <li
                key={comment.boardCommentId}
                className={comment.accepted ? 'board-comment is-accepted' : 'board-comment'}
              >
                <p className="board-comment-meta">
                  <strong>{writerName(comment.writer)}</strong>
                  <time dateTime={comment.createdAt}>{formatDate(comment.createdAt)}</time>
                  {comment.accepted && <Seal text="채택" small stampOnView label="채택된 답" />}
                </p>
                <p className="board-comment-body">{comment.content}</p>
                <p className="board-comment-actions">
                  {comments.canAccept && !comment.mine && (
                    <button
                      type="button"
                      className="btn btn-quiet btn-small"
                      aria-pressed={comment.accepted}
                      onClick={() =>
                        act(() =>
                          comment.accepted
                            ? unacceptComment(post.boardPostId)
                            : acceptComment(post.boardPostId, comment.boardCommentId),
                        )
                      }
                    >
                      {comment.accepted ? '채택 거두기' : '채택'}
                    </button>
                  )}
                  {comment.canDelete && (
                    <button
                      type="button"
                      className="btn btn-quiet btn-small"
                      onClick={() => setConfirm({ type: 'comment', commentId: comment.boardCommentId })}
                    >
                      삭제
                    </button>
                  )}
                </p>
              </li>
            ))}
          </ol>
        )}
        <form className="board-comment-form" onSubmit={submit}>
          <label htmlFor="board-comment-input" className="visually-hidden">
            댓글
          </label>
          <textarea
            id="board-comment-input"
            value={draft}
            maxLength={COMMENT_MAX}
            rows={3}
            placeholder={member ? '댓글을 남겨 주세요' : '로그인하면 댓글을 남길 수 있어요'}
            onChange={(e) => setDraft(e.target.value)}
            onFocus={() => {
              if (!member) open()
            }}
          />
          <button type="submit" className="btn btn-primary btn-small" disabled={busy}>
            댓글 등록
          </button>
        </form>
      </section>

      <ConfirmDialog
        open={confirm !== null}
        title={confirm?.type === 'post' ? '글 삭제' : '댓글 삭제'}
        confirmLabel="삭제"
        danger
        onConfirm={confirmed}
        onCancel={() => setConfirm(null)}
      >
        <p>{confirm?.type === 'post' ? '글과 댓글을 모두 지웁니다. 삭제할까요?' : '이 댓글을 삭제할까요?'}</p>
      </ConfirmDialog>
    </article>
  )
}
