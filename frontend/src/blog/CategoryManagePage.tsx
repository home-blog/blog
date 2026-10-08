// 분류 관리 화면 /manage/categories (specs/003 US6, T046). 내 블로그는 서버가 세션으로 정한다
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import '../pages/auth-layout.css'
import { getCategories, getMyBlog, type CategorySummary, type MyBlog } from './blogApi'
import './blog.css'
import CategoryManager from './CategoryManager'
import './manage.css'

export default function CategoryManagePage() {
  const [blog, setBlog] = useState<MyBlog | null>(null)
  const [categories, setCategories] = useState<CategorySummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getMyBlog(controller.signal)
      .then(async (mine) => {
        const list = await getCategories(mine.blogId, controller.signal)
        setBlog(mine)
        setCategories(list)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  if (error) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {error}
      </p>
    )
  }
  if (!blog || !categories) return <p className="blog-status hint">불러오는 중</p>

  return (
    <div className="manage">
      <header className="manage-head">
        <h1 className="manage-title">분류 관리</h1>
        <p className="hint">
          비공개 분류와 그 분류의 글은 나만 봅니다. 글이 있는 분류와 미분류는 지울 수 없습니다.{' '}
          <Link to={`/blog/${blog.blogId}`}>내 블로그로</Link>
        </p>
      </header>
      <CategoryManager categories={categories} onChange={setCategories} />
    </div>
  )
}
