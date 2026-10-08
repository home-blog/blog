// 검색 결과 (specs/004 US5, US6, T031, T034): "검색 결과 {N}건", 블로그 이름이 붙은 목록, 페이지 번호
// 주소: /search?q=입력 그대로&page=. 로그인 없이 본다. 짧은 검색어면 요청하지 않고 안내한다 (서버 검사가 마지막 방어선)
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { searchPosts, type SearchPage as SearchResultPage } from '../explore/exploreApi'
import Pagination from '../explore/Pagination'
import PostRow from '../explore/PostRow'
import { checkKeyword, MESSAGES, pageFrom } from '../explore/rules'
import '../blog/blog.css'
import '../explore/explore.css'

type Result = { key: string; data: SearchResultPage } | { key: string; error: string }

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const page = pageFrom(searchParams.get('page'))
  const problem = checkKeyword(q)
  const key = `${q}\n${page}`
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (problem) return
    const controller = new AbortController()
    const requestKey = `${q}\n${page}`
    searchPosts(q, page, controller.signal)
      .then((data) => {
        setResult({ key: requestKey, data })
        if (data.page !== page) {
          setSearchParams(
            (prev) => {
              const next = new URLSearchParams(prev)
              if (data.page > 1) next.set('page', String(data.page))
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
  }, [q, page, problem, setSearchParams])

  function hrefFor(target: number): string {
    const next = new URLSearchParams({ q })
    if (target > 1) next.set('page', String(target))
    return `/search?${next.toString()}`
  }

  const current = result?.key === key ? result : null
  // 서버가 페이지를 바꿔 준 직후(99 → 2)에는 바뀐 번호가 지금 주소의 번호와 같을 때만 앞 결과를 그대로 보여 준다
  const shown =
    current ??
    (result && 'data' in result && result.key.startsWith(`${q}\n`) && result.data.page === page ? result : null)

  return (
    <div className="search-page">
      <h1 className="search-title">검색</h1>
      {problem ? (
        <p className="msg msg-error" role="alert">
          {problem}
        </p>
      ) : !shown ? (
        <p className="hint">찾는 중</p>
      ) : 'error' in shown ? (
        <p className="msg msg-error" role="alert">
          {shown.error}
        </p>
      ) : (
        <>
          <p className="post-count">
            <strong className="search-keyword">{shown.data.keyword}</strong> 검색 결과 {shown.data.totalCount}건
          </p>
          {shown.data.totalCount === 0 ? (
            <p className="post-empty">{MESSAGES.emptySearch}</p>
          ) : (
            <>
              <ul className="post-list">
                {shown.data.results.map((post) => (
                  <PostRow key={post.postId} post={post} blogName={post.blogName} />
                ))}
              </ul>
              <Pagination page={shown.data.page} totalPages={shown.data.totalPages} hrefFor={hrefFor} />
            </>
          )}
        </>
      )}
    </div>
  )
}
