// 머리글의 "내 블로그": 내 블로그 번호를 물어 그 블로그 화면으로 보낸다 (specs/003 contracts 3)
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router'
import { ApiError } from '../api/client'
import { getMyBlog } from './blogApi'

export default function MyBlogRedirect() {
  const [blogId, setBlogId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getMyBlog(controller.signal)
      .then((blog) => setBlogId(blog.blogId))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  if (blogId !== null) return <Navigate to={`/blog/${blogId}`} replace />
  if (error) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {error}
      </p>
    )
  }
  return <p className="blog-status hint">불러오는 중</p>
}
