// 첫 화면 글마당 최근 글 (specs/017 US5, 요구사항 2절): 표 하나와 "전체 보기". 비어도 칸을 숨기지 않고 첫 글을 부른다 (S5-2)
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import BoardTable from './BoardTable'
import { getRecentBoardPosts, type BoardRow } from './boardApi'
import './board.css'

export default function BoardRecent() {
  const [posts, setPosts] = useState<BoardRow[] | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getRecentBoardPosts(controller.signal)
      .then((data) => setPosts(data.posts))
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true)
      })
    return () => controller.abort()
  }, [])

  // 못 읽으면 첫 화면의 다른 칸을 가리지 않게 조용히 숨긴다
  if (failed) return null

  return (
    <section className="board-recent" aria-labelledby="board-recent-title">
      <div className="board-recent-head">
        <h2 id="board-recent-title" className="home-section-title">
          <span className="ink-highlight">글마당</span>
        </h2>
        <Link to="/board" className="board-more">
          전체 보기 →
        </Link>
      </div>
      {posts === null ? (
        <p className="hint">불러오는 중</p>
      ) : posts.length === 0 ? (
        <p className="hint board-recent-empty">
          첫 이야기를 들려주세요. <Link to="/board/write">글쓰기</Link>
        </p>
      ) : (
        <BoardTable posts={posts} compact />
      )}
    </section>
  )
}
