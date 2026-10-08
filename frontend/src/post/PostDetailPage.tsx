// 글 상세 화면 (specs/003 US3, T029): 분류, 제목, 블로그 이름, 작성 시각(수정 시각은 있을 때만), 본문, 이전·다음 글
// - 누구나 본다. 볼 수 없는 글은 서버가 없는 글과 똑같이 답한다 (FR-026)
// - 본문은 MarkdownView로만 그린다 (D-1). 제목·이름은 React가 글자로 보여 준다
// - 로그인 상태가 바뀌면 다시 읽는다 (로그아웃한 뒤 비공개 글이 화면에 남지 않게)
// - 주인에게만 수정·삭제. 삭제는 확인 창을 거치고, 끝나면 내 블로그로 간다 (FR-021, FR-022)
import { lazy, Suspense, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import ConfirmDialog from '../components/ConfirmDialog'
import '../pages/auth-layout.css'
import { deletePost, getPost, type PostDetail } from './postApi'
import './post-detail.css'

const MarkdownView = lazy(() => import('./MarkdownView'))

type Result = { key: string; post: PostDetail } | { key: string; error: string }

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'long', timeStyle: 'short' })
}

export default function PostDetailPage() {
  const { postId } = useParams()
  const id = Number(postId)
  const validId = Number.isInteger(id) && id > 0
  const { member } = useAuth()
  const viewer = member === undefined ? null : (member?.id ?? 'guest')
  const key = `${id}:${viewer}`
  const [result, setResult] = useState<Result | null>(null)
  const navigate = useNavigate()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    if (!validId || viewer === null) return
    const controller = new AbortController()
    const requestKey = `${id}:${viewer}`
    getPost(id, controller.signal)
      .then((post) => setResult({ key: requestKey, post }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({ key: requestKey, error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      })
    return () => controller.abort()
  }, [id, validId, viewer])

  const current = result?.key === key ? result : null
  if (!validId || (current && 'error' in current)) {
    return (
      <p className="post-status msg msg-error" role="alert">
        {current && 'error' in current ? current.error : '존재하지 않는 글입니다'}
      </p>
    )
  }
  if (!current) return <p className="post-status hint">불러오는 중</p>

  const { post } = current

  async function remove() {
    setConfirmDelete(false)
    setDeleting(true)
    setDeleteError(null)
    try {
      await deletePost(post.postId)
      navigate(`/blog/${post.blogId}`, { replace: true })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      setDeleting(false)
    }
  }
  return (
    <article className="post">
      <header className="post-head">
        <p className="post-meta-top">
          <Link to={`/blog/${post.blogId}`} className="post-blog">
            {post.blogName}
          </Link>
          <span aria-hidden="true"> · </span>
          <span className="post-category">{post.category.name}</span>
          <span className="post-topic">{post.topic.name}</span>
          {post.isOwner && post.visibility === 'private' && <span className="blog-badge">비공개 글</span>}
          {post.isOwner && post.category.visibility === 'private' && <span className="blog-badge">비공개 분류</span>}
        </p>
        <h1 className="post-title">{post.title}</h1>
        <p className="post-times hint">
          <time dateTime={post.createdAt}>{formatTime(post.createdAt)}</time>
          {post.updatedAt && (
            <>
              {' '}
              · 수정 <time dateTime={post.updatedAt}>{formatTime(post.updatedAt)}</time>
            </>
          )}
        </p>
        {post.isOwner && (
          <p className="post-owner-actions">
            <Link to={`/write/${post.postId}`} className="btn btn-outline btn-small">
              수정
            </Link>
            <button type="button" className="btn btn-quiet btn-small" disabled={deleting} onClick={() => setConfirmDelete(true)}>
              {deleting ? '삭제하는 중' : '삭제'}
            </button>
          </p>
        )}
        {deleteError && (
          <p className="msg msg-error" role="alert">
            {deleteError}
          </p>
        )}
      </header>

      <Suspense fallback={<div className="post-content-raw">{post.content}</div>}>
        <MarkdownView source={post.content} />
      </Suspense>

      <nav className="post-nav" aria-label="이전·다음 글">
        {post.prevPostId !== null ? (
          <Link to={`/posts/${post.prevPostId}`} className="btn btn-outline btn-small">
            ← 이전 글
          </Link>
        ) : (
          <span />
        )}
        {/* 분류별 글 목록 주소는 specs/004가 정하면 바꾼다 */}
        <Link to={`/blog/${post.blogId}`} className="btn btn-quiet btn-small">
          목록으로
        </Link>
        {post.nextPostId !== null ? (
          <Link to={`/posts/${post.nextPostId}`} className="btn btn-outline btn-small">
            다음 글 →
          </Link>
        ) : (
          <span />
        )}
      </nav>

      {/* 취소하면 요청을 보내지 않는다 (FR-021) */}
      <ConfirmDialog
        open={confirmDelete}
        title="글 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setConfirmDelete(false)}
        onConfirm={remove}
      >
        <p>삭제하면 되돌릴 수 없습니다. 삭제할까요?</p>
      </ConfirmDialog>
    </article>
  )
}
