// 블로그 화면 (specs/003 US1, T016): 이름, 소개, 분류 목록과 글 개수
// 누구나 본다. 주인에게만 비공개 분류와 "블로그 설정" 버튼이 보인다 (실제 권한은 서버가 다시 본다)
// 글 목록 자리는 specs/004가 채운다
import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { ApiError } from '../api/client'
import { getBlog, getCategories, type Blog, type CategorySummary } from '../blog/blogApi'
import './auth-layout.css'
import '../blog/blog.css'

/** 어느 블로그의 결과인지 함께 둔다: 주소의 번호가 바뀌면 이전 결과를 보여 주지 않는다 */
type Result = { id: number; blog: Blog; categories: CategorySummary[] } | { id: number; error: string }

export default function BlogHomePage() {
  const { blogId } = useParams()
  const id = Number(blogId)
  const validId = Number.isInteger(id) && id > 0
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (!validId) return
    const controller = new AbortController()
    Promise.all([getBlog(id, controller.signal), getCategories(id, controller.signal)])
      .then(([blog, categories]) => setResult({ id, blog, categories }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({ id, error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      })
    return () => controller.abort()
  }, [id, validId])

  const current = result?.id === id ? result : null
  if (!validId || (current && 'error' in current)) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {current && 'error' in current ? current.error : '※ 존재하지 않는 블로그입니다'}
      </p>
    )
  }
  if (!current) {
    return <p className="blog-status hint">불러오는 중</p>
  }

  const { blog, categories } = current
  return (
    <div className="blog-home">
      <header className="blog-head">
        <h1 className="blog-name">{blog.name}</h1>
        {blog.intro ? <p className="blog-intro">{blog.intro}</p> : <p className="blog-intro hint">아직 소개가 없습니다</p>}
        {blog.isOwner && (
          <p className="blog-owner-actions">
            {/* 블로그 설정 화면은 US7에서 열린다 */}
            <span className="btn btn-outline btn-small" aria-disabled="true" title="다음 단계에서 열립니다">
              블로그 설정
            </span>
          </p>
        )}
      </header>

      <div className="blog-body">
        <nav className="blog-categories" aria-labelledby="category-title">
          <h2 id="category-title" className="blog-side-title">분류</h2>
          <ul>
            {categories.map((category) => (
              <li key={category.categoryId} className="blog-category">
                <span className="blog-category-name">{category.name}</span>
                {category.visibility === 'private' && <span className="blog-badge">비공개</span>}
                <span className="blog-category-count" aria-label={`글 ${category.postCount}개`}>
                  {category.postCount}
                </span>
              </li>
            ))}
          </ul>
        </nav>

        <section className="blog-posts" aria-labelledby="posts-title">
          <h2 id="posts-title" className="blog-side-title">글</h2>
          <p className="hint">글 목록은 다음 단계에서 열립니다.</p>
        </section>
      </div>
    </div>
  )
}
