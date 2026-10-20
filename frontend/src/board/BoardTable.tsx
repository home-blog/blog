// 글마당 표 (specs/017 US1, US5): 주제, 제목(+댓글 수), 쓴 사람, 날짜, 조회 수. 목록 화면과 첫 화면이 같이 쓴다
// 물음은 제목 앞에 꼬리표, 채택한 물음은 작은 "해결" 도장(화면에 들어올 때 쿵 찍힌다)
// 줄은 차례로 떠오르고, 주제는 주제마다 다른 색 칩 (2026-10-20 사용자: 애니메이션·하이라이트)
import type { CSSProperties } from 'react'
import { Link } from 'react-router'
import Seal from '../components/Seal'
import { formatDate } from '../explore/rules'
import { type BoardRow, writerName } from './boardApi'

/** 주제마다 분류 색 하나 (index.css의 --category-1..6). 모르는 주제는 기본 초록 */
const TOPIC_COLOR: Record<string, number> = { 여행: 2, 음식: 3, 취미: 5, 운동: 6, 개발: 4 }

export default function BoardTable({ posts, compact }: { posts: BoardRow[]; compact?: boolean }) {
  return (
    <div className="board-table-wrap">
      <table className={compact ? 'board-table is-compact' : 'board-table'}>
        <thead>
          <tr>
            <th scope="col">주제</th>
            <th scope="col">제목</th>
            <th scope="col">쓴 사람</th>
            <th scope="col">날짜</th>
            {!compact && <th scope="col">조회</th>}
          </tr>
        </thead>
        <tbody>
          {posts.map((post, index) => (
            <tr key={post.boardPostId} className="board-row" style={{ '--i': index } as CSSProperties}>
              <td className="board-topic">
                <span
                  className="board-topic-chip"
                  style={{ '--topic': `var(--category-${TOPIC_COLOR[post.topic] ?? 1})` } as CSSProperties}
                >
                  {post.topic}
                </span>
              </td>
              <td className="board-title-cell">
                {post.kind === 'question' && <span className="board-kind">물음</span>}
                <Link to={`/board/${post.boardPostId}`} className="board-title-link">
                  {post.title}
                </Link>
                {post.commentCount > 0 && (
                  <span className="board-comment-count">
                    <span className="visually-hidden">댓글 </span>
                    {post.commentCount}
                  </span>
                )}
                {post.solved && <Seal text="해결" small stampOnView label="해결된 물음" />}
              </td>
              <td className="board-writer">{writerName(post.writer)}</td>
              <td className="board-date">
                <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
              </td>
              {!compact && <td className="board-views">{post.views.toLocaleString('ko-KR')}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
