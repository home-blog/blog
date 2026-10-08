// 같은 태그의 공개 글 (specs/005 US5, T046): /tags/:tagName?page=. 누구나 본다
// 누가 봐도 공개 분류의 공개 글만 최신순으로 온다 (서버, SC-008). 글이 없으면 "글이 없습니다" (상세/04)
import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import Pagination from '../explore/Pagination'
import PostRow from '../explore/PostRow'
import { MESSAGES, pageFrom } from '../explore/rules'
import { getTagPosts, type TagPostsPage as TagPostsData } from '../post/postApi'
import '../blog/blog.css'
import '../explore/explore.css'

type Result = { key: string; data: TagPostsData } | { key: string; error: string }

export default function TagPostsPage() {
  const { tagName = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = pageFrom(searchParams.get('page'))
  const key = `${tagName}\n${page}`
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const requestKey = `${tagName}\n${page}`
    getTagPosts(tagName, page, controller.signal)
      .then((data) => {
        setResult({ key: requestKey, data })
        // 없는 페이지면 서버가 마지막 페이지를 준다. 주소도 맞춘다
        if (data.page !== page) {
          setSearchParams(data.page > 1 ? { page: String(data.page) } : {}, { replace: true })
        }
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({ key: requestKey, error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      })
    return () => controller.abort()
  }, [tagName, page, setSearchParams])

  const current = result?.key === key ? result : null
  const hrefFor = (target: number) => `/tags/${encodeURIComponent(tagName)}${target > 1 ? `?page=${target}` : ''}`

  return (
    <div className="search-page">
      <h1 className="search-title">#{current && 'data' in current ? current.data.tag : tagName}</h1>
      {!current ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in current ? (
        <p className="msg msg-error" role="alert">
          {current.error}
        </p>
      ) : (
        <>
          <p className="post-count">글 {current.data.totalCount}개</p>
          {current.data.totalCount === 0 ? (
            <p className="post-empty">{MESSAGES.emptyList}</p>
          ) : (
            <>
              <ul className="post-list">
                {current.data.posts.map((post) => (
                  <PostRow key={post.postId} post={post} blogName={post.blogName} />
                ))}
              </ul>
              <Pagination page={current.data.page} totalPages={current.data.totalPages} hrefFor={hrefFor} />
            </>
          )}
        </>
      )}
    </div>
  )
}
