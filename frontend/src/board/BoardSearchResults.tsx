// 검색 결과의 글마당 칸 (specs/017 US5, S5-1): 블로그 글 결과 아래에 따로. 검색어가 비면 묻지 않는다
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import BoardTable from './BoardTable'
import { boardQueryString, getBoardPosts, type BoardPage } from './boardApi'
import './board.css'

type Result = { key: string; data: BoardPage } | { key: string; failed: true }

export default function BoardSearchResults({ keyword }: { keyword: string }) {
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (!keyword.trim()) return
    const controller = new AbortController()
    getBoardPosts({ q: keyword }, controller.signal)
      .then((data) => setResult({ key: keyword, data }))
      .catch(() => {
        if (!controller.signal.aborted) setResult({ key: keyword, failed: true })
      })
    return () => controller.abort()
  }, [keyword])

  const current = result?.key === keyword ? result : null
  if (!keyword.trim() || !current || 'failed' in current) return null

  return (
    <section className="board-search" aria-labelledby="board-search-title">
      <h2 id="board-search-title" className="board-search-title">
        글마당 {current.data.totalCount}건
      </h2>
      {current.data.posts.length === 0 ? (
        <p className="hint">글마당에는 찾는 글이 없어요.</p>
      ) : (
        <>
          <BoardTable posts={current.data.posts} compact />
          {current.data.totalPages > 1 && (
            <p className="board-more-line">
              <Link to={`/board${boardQueryString({ q: keyword })}`} className="board-more">
                글마당에서 더 보기 →
              </Link>
            </p>
          )}
        </>
      )}
    </section>
  )
}
