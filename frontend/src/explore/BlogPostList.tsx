// 블로그 화면의 글 목록 (specs/004 US1, US2, T017): "{N}개의 글", 최신순 10개, 페이지 번호
// 페이지는 주소의 ?page=에 담아 새로고침·뒤로 가기에도 남는다. 서버가 범위 안으로 바꾼 번호(99 → 3)는 주소에도 반영한다
// 무엇이 보이는지(비공개 글)는 서버가 정한다. 화면은 받은 대로 그린다
import { useEffect, useState } from 'react'
import { useLocation, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { getBlogPosts, type PostListPage } from './exploreApi'
import Pagination from './Pagination'
import PostRow from './PostRow'
import { MESSAGES, pageFrom } from './rules'
import './explore.css'

interface Props {
  blogId: number
  /** 보는 사람 (로그인 상태가 바뀌면 다시 읽는다) */
  viewer: string | number
}

type Result = { key: string; list: PostListPage } | { key: string; error: string }

export default function BlogPostList({ blogId, viewer }: Props) {
  const [searchParams, setSearchParams] = useSearchParams()
  const { pathname } = useLocation()
  const page = pageFrom(searchParams.get('page'))
  const key = `${blogId}:${viewer}:${page}`
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const requestKey = `${blogId}:${viewer}:${page}`
    getBlogPosts(blogId, { page }, controller.signal)
      .then((list) => {
        setResult({ key: requestKey, list })
        if (list.page !== page) {
          // 서버가 바꾼 페이지를 주소에 맞춘다 (기록을 늘리지 않음)
          setSearchParams(
            (prev) => {
              const next = new URLSearchParams(prev)
              if (list.page > 1) next.set('page', String(list.page))
              else next.delete('page')
              return next
            },
            { replace: true },
          )
        }
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({ key: requestKey, error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      })
    return () => controller.abort()
  }, [blogId, viewer, page, setSearchParams])

  function hrefFor(target: number): string {
    const next = new URLSearchParams(searchParams)
    if (target > 1) next.set('page', String(target))
    else next.delete('page')
    const query = next.toString()
    return query ? `${pathname}?${query}` : pathname
  }

  const current = result?.key === key ? result : null
  // 서버가 페이지를 바꿔 준 직후에는 주소보다 결과가 먼저 온다: 같은 블로그·사람의 결과면 그대로 보여 준다
  const shown = current ?? (result && 'list' in result && result.key.startsWith(`${blogId}:${viewer}:`) ? result : null)

  return (
    <section className="blog-posts" aria-labelledby="posts-title">
      <h2 id="posts-title" className="blog-side-title">
        글
      </h2>
      {!shown ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in shown ? (
        <p className="msg msg-error" role="alert">
          {shown.error}
        </p>
      ) : (
        <>
          <p className="post-count">{shown.list.totalCount}개의 글</p>
          {shown.list.totalCount === 0 ? (
            <p className="post-empty">{MESSAGES.emptyList}</p>
          ) : (
            <>
              <ul className="post-list">
                {shown.list.posts.map((post) => (
                  <PostRow key={post.postId} post={post} />
                ))}
              </ul>
              <Pagination page={shown.list.page} totalPages={shown.list.totalPages} hrefFor={hrefFor} />
            </>
          )}
        </>
      )}
    </section>
  )
}
