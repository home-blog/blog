// 글마당 표 (specs/017 US1, US5): 주제, 제목(+댓글 수), 쓴 사람, 날짜, 조회 수. 목록 화면과 첫 화면이 같이 쓴다
// 물음은 제목 앞에 꼬리표, 채택한 물음은 작은 "해결" 도장
import { Link } from 'react-router'
import Seal from '../components/Seal'
import { formatDate } from '../explore/rules'
import { type BoardRow, writerName } from './boardApi'

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
          {posts.map((post) => (
            <tr key={post.boardPostId}>
              <td className="board-topic">{post.topic}</td>
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
                {post.solved && <Seal text="해결" small label="해결된 물음" />}
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
