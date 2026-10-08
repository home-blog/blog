// 댓글 관리 /manage/comments (specs/006 US4·US5, T029, T036, FR-023 ~ FR-030)
// - 열면 읽음 처리를 먼저 보내고(contracts 5-1), 받은 이전 시각으로 목록을 읽어 그 뒤에 남이 단 댓글에 NEW를 붙인다
//   새 댓글 수는 0으로 바꾼다. 쪽을 넘겨도 같은 기준을 쓴다 (다시 읽음 처리하지 않음)
// - 댓글 내용은 글자 그대로 (태그로 그리지 않음). 탈퇴한 작성자는 "탈퇴한 사용자"
// - 삭제는 확인 창에서 확인했을 때만 005의 DELETE /api/comments/{commentId}
import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { deleteComment } from '../comment/commentApi'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../explore/Pagination'
import '../explore/explore.css'
import { pageFrom } from '../explore/rules'
import { getManageComments, markCommentsRead, NEVER_READ, type ManageComment, type ManageCommentPage } from './manageApi'
import { useNewCommentCount } from './newCommentCount'
import './manage-pages.css'

const timeFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  dateStyle: 'medium',
  timeStyle: 'short',
})

function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}

export default function ManageCommentsPage() {
  const { setCount } = useNewCommentCount()
  const [params, setParams] = useSearchParams()
  const page = pageFrom(params.get('page'))

  /** 읽음 처리로 받은 NEW 기준. 받기 전에는 null */
  const [newSince, setNewSince] = useState<string | null>(null)
  /** key: 이 결과를 받은 쪽. 쪽이 바뀌면 새 결과가 올 때까지 이전 목록을 보이지 않는다 */
  const [loaded, setLoaded] = useState<{ page: number; result: ManageCommentPage } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [deleting, setDeleting] = useState<ManageComment | null>(null)
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)

  // 읽음 처리 요청은 화면 하나에 한 번만 보낸다. 두 번 보내면 두 번째의 "이전 시각"이 첫 번째 시각이 되어 NEW가 사라진다
  const readMark = useRef<ReturnType<typeof markCommentsRead> | null>(null)

  // 화면을 열 때 한 번: 읽음 처리 먼저 (FR-029, research R-4)
  useEffect(() => {
    let cancelled = false
    readMark.current ??= markCommentsRead()
    readMark.current
      .then((mark) => {
        if (cancelled) return
        setNewSince(mark.previousReadAt ?? NEVER_READ)
        setCount(0)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(messageOf(err))
      })
    return () => {
      cancelled = true
    }
    // 읽음 처리는 화면을 열 때 한 번 보낸다 (setCount는 로그인한 회원이 바뀔 때만 바뀐다)
  }, [setCount])

  useEffect(() => {
    if (newSince === null) return
    const controller = new AbortController()
    getManageComments(page, newSince, controller.signal)
      .then((res) => {
        setLoaded({ page, result: res })
        setError(null)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setError(messageOf(err))
      })
    return () => controller.abort()
  }, [page, newSince, reload])

  const result = loaded?.page === page ? loaded.result : null

  async function remove(comment: ManageComment) {
    setDeleting(null)
    setMessage(null)
    try {
      await deleteComment(comment.commentId)
      setMessage({ text: '댓글을 삭제했습니다', error: false })
      if (result && result.items.length === 1 && page > 1) {
        // 이 쪽의 마지막 댓글을 지웠으면 앞 쪽으로 (빈 쪽에는 쪽 번호가 없어 돌아갈 수 없다)
        setParams({ page: String(page - 1) })
      } else {
        setReload((n) => n + 1)
      }
    } catch (err) {
      setMessage({ text: messageOf(err), error: true })
    }
  }

  function hrefFor(target: number): string {
    return `?page=${target}`
  }

  const totalPages = result ? Math.max(1, Math.ceil(result.totalCount / result.pageSize)) : 1

  return (
    <div className="manage">
      <header className="manage-head">
        <h1 className="manage-title">댓글 관리</h1>
        <p className="hint">내 블로그 글에 달린 모든 댓글입니다. 블로그 주인은 누가 쓴 댓글이든 지울 수 있습니다.</p>
      </header>

      <p className={`msg ${message?.error ? 'msg-error' : 'msg-ok'} manage-message`} role="status" aria-live="polite">
        {message?.text}
      </p>

      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
      {!error && !result && <p className="hint">불러오는 중</p>}
      {!error && result && result.items.length === 0 && (
        <div className="manage-empty">
          <p>아직 달린 댓글이 없습니다</p>
        </div>
      )}
      {!error && result && result.items.length > 0 && (
        <>
          <ul className="manage-list">
            {result.items.map((comment) => (
              <li key={comment.commentId} className="manage-row">
                <div className="manage-row-main">
                  <span className="manage-row-author">
                    {comment.author.withdrawn ? '탈퇴한 사용자' : comment.author.nickname}
                  </span>
                  {comment.isNew && <span className="new-mark">NEW</span>}
                  <span className="manage-comment-preview">{comment.preview}</span>
                </div>
                <p className="manage-row-meta">
                  <time dateTime={comment.createdAt}>{timeFormat.format(new Date(comment.createdAt))}</time>
                  <Link to={`/posts/${comment.post.postId}#comments`} className="manage-row-link">
                    {comment.post.title}
                  </Link>
                </p>
                <div className="manage-row-actions">
                  <button type="button" className="btn btn-quiet btn-small" onClick={() => setDeleting(comment)}>
                    삭제
                  </button>
                </div>
              </li>
            ))}
          </ul>
          {totalPages > 1 && <Pagination page={Math.min(page, totalPages)} totalPages={totalPages} hrefFor={hrefFor} />}
        </>
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="댓글 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && void remove(deleting)}
      >
        <p>댓글을 삭제할까요?</p>
      </ConfirmDialog>
    </div>
  )
}
