// 글 상세 화면 (specs/003 US3, T029): 분류, 제목, 블로그 이름, 작성 시각(수정 시각은 있을 때만), 본문, 이전·다음 글
// - 누구나 본다. 볼 수 없는 글은 서버가 없는 글과 똑같이 답한다 (FR-026)
// - 본문은 MarkdownView로만 그린다 (D-1). 제목·이름은 React가 글자로 보여 준다
// - 로그인 상태가 바뀌면 다시 읽는다 (로그아웃한 뒤 비공개 글이 화면에 남지 않게)
import { lazy, Suspense, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import '../pages/auth-layout.css'
import { getPost, type PostDetail } from './postApi'
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
            {/* 수정·삭제는 US4에서 열린다 */}
            <span className="btn btn-outline btn-small" aria-disabled="true" title="다음 단계에서 열립니다">
              수정
            </span>
            <span className="btn btn-quiet btn-small" aria-disabled="true" title="다음 단계에서 열립니다">
              삭제
            </span>
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
        {/* 이 글의 분류로 좁힌 블로그 글 목록 (specs/004 T043, 주소 ?category=) */}
        <Link to={`/blog/${post.blogId}?category=${post.category.categoryId}`} className="btn btn-quiet btn-small">
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
    </article>
  )
}
