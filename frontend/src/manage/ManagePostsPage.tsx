// 글 관리 /manage/posts (specs/006 US2, T021, FR-012 ~ FR-017)
// - 비공개 포함 내 글을 최신순 10개씩. 공개 여부·분류로 거른다 (주소의 ?visibility=&category=&page=에 남아 새로고침해도 그대로)
// - 보기 → 글 상세, 수정 → 글 고치기, 삭제 → 확인 창. 취소하면 요청을 보내지 않는다 (003의 DELETE /api/posts/{postId})
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { getCategories, type CategorySummary } from '../blog/blogApi'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../explore/Pagination'
import '../blog/blog.css'
import '../explore/explore.css'
import { formatDate, pageFrom } from '../explore/rules'
import { deletePost } from '../post/postApi'
import { getManagePosts, type ManagePost, type ManagePostPage, type VisibilityFilter } from './manageApi'
import { useManageBlog } from './manageBlog'
import './manage-pages.css'

const VISIBILITY_OPTIONS: { value: VisibilityFilter; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: 'public', label: '공개' },
  { value: 'private', label: '비공개' },
]

function visibilityFrom(value: string | null): VisibilityFilter {
  return value === 'public' || value === 'private' ? value : 'all'
}

function categoryFrom(value: string | null): number | null {
  return value && /^\d+$/.test(value) ? Number(value) : null
}

function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}

export default function ManagePostsPage() {
  const { header } = useManageBlog()
  const [params, setParams] = useSearchParams()
  const visibility = visibilityFrom(params.get('visibility'))
  const categoryId = categoryFrom(params.get('category'))
  const page = pageFrom(params.get('page'))

  const [categories, setCategories] = useState<CategorySummary[]>([])
  const [result, setResult] = useState<ManagePostPage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [deleting, setDeleting] = useState<ManagePost | null>(null)
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getCategories(header.blogId, controller.signal)
      .then(setCategories)
      .catch(() => {
        // 분류 목록이 없어도 글 목록은 볼 수 있다 (거르기 칸만 비어 있음)
      })
    return () => controller.abort()
  }, [header.blogId])

  useEffect(() => {
    const controller = new AbortController()
    getManagePosts({ visibility, categoryId, page }, controller.signal)
      .then((res) => {
        setResult(res)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setError(messageOf(err))
      })
    return () => controller.abort()
  }, [visibility, categoryId, page, reload])

  /** 거르기를 바꾸면 1쪽부터 */
  function filter(change: { visibility?: VisibilityFilter; category?: number | null }) {
    const next = new URLSearchParams(params)
    const nextVisibility = change.visibility ?? visibility
    const nextCategory = change.category === undefined ? categoryId : change.category
    if (nextVisibility === 'all') next.delete('visibility')
    else next.set('visibility', nextVisibility)
    if (nextCategory === null) next.delete('category')
    else next.set('category', String(nextCategory))
    next.delete('page')
    setParams(next)
  }

  function hrefFor(target: number): string {
    const next = new URLSearchParams(params)
    next.set('page', String(target))
    return `?${next}`
  }

  async function remove(post: ManagePost) {
    setDeleting(null)
    setMessage(null)
    try {
      await deletePost(post.postId)
      setMessage({ text: '글을 삭제했습니다', error: false })
      setReload((n) => n + 1)
    } catch (err) {
      setMessage({ text: messageOf(err), error: true })
    }
  }

  const totalPages = result ? Math.max(1, Math.ceil(result.totalCount / result.pageSize)) : 1

  return (
    <div className="manage">
      <header className="manage-head manage-head-row">
        <h1 className="manage-title">글 관리</h1>
        <Link to="/write" className="btn btn-primary btn-small">
          글쓰기
        </Link>
      </header>

      <div className="manage-filters">
        <label>
          공개 여부
          <select value={visibility} onChange={(e) => filter({ visibility: e.target.value as VisibilityFilter })}>
            {VISIBILITY_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          분류
          <select
            value={categoryId ?? ''}
            onChange={(e) => filter({ category: e.target.value === '' ? null : Number(e.target.value) })}
          >
            <option value="">전체</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      <p className={`msg ${message?.error ? 'msg-error' : 'msg-ok'} manage-message`} role="status" aria-live="polite">
        {message?.text}
      </p>

      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
      {!error && !result && <p className="hint">불러오는 중</p>}
      {!error && result && !result.hasAnyPost && (
        <div className="manage-empty">
          <p>아직 쓴 글이 없습니다</p>
          <Link to="/write" className="btn btn-primary btn-small">
            글쓰기
          </Link>
        </div>
      )}
      {!error && result && result.hasAnyPost && result.items.length === 0 && (
        <div className="manage-empty">
          <p>글이 없습니다</p>
        </div>
      )}
      {!error && result && result.items.length > 0 && (
        <>
          <ul className="manage-list">
            {result.items.map((post) => (
              <li key={post.postId} className="manage-row">
                <div className="manage-row-main">
                  <Link to={`/posts/${post.postId}`} className="manage-row-title">
                    {post.title}
                  </Link>
                </div>
                <p className="manage-row-meta">
                  <span>{post.category.name}</span>
                  <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
                  {post.visibility === 'private' ? <span className="blog-badge">비공개</span> : <span>공개</span>}
                  <span>조회 {post.views.toLocaleString('ko-KR')}</span>
                  <span>댓글 {post.commentCount.toLocaleString('ko-KR')}</span>
                </p>
                <div className="manage-row-actions">
                  <Link to={`/posts/${post.postId}`} className="btn btn-quiet btn-small">
                    보기
                  </Link>
                  <Link to={`/write/${post.postId}`} className="btn btn-quiet btn-small">
                    수정
                  </Link>
                  <button type="button" className="btn btn-quiet btn-small" onClick={() => setDeleting(post)}>
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
        title="글 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && void remove(deleting)}
      >
        <p>삭제하면 되돌릴 수 없습니다. 삭제할까요?</p>
      </ConfirmDialog>
    </div>
  )
}
