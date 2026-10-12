// 첫 화면 글 카드: 주제 · 원고지 매수, 제목, 본문 앞부분, 블로그(글쓴이) · 작성일 · 댓글·좋아요 수
// - 카드 어디를 눌러도 글로 간다(제목 링크가 카드 전체를 덮는다). 주제·블로그 링크는 그 위에 따로 눌린다
// - 마우스를 올리면 카드가 떠오르고 제목에 형광펜이 그어진다 (specs/007, 2026-10-12 사용자: 원고지 + 카드·하이라이트·애니메이션)
import { Link } from 'react-router'
import type { FeedPost } from './exploreApi'
import { formatDate } from './rules'

interface Props {
  post: FeedPost
}

export default function FeedItem({ post }: Props) {
  return (
    <li className="card feed-card">
      <span className="card-top">
        <Link to={`/?topic=${encodeURIComponent(post.topic)}`} className="feed-topic card-raise">
          {post.topic}
        </Link>
        <span className="manuscript-count">
          원고지 {post.sheets}매 · {post.readMinutes}분
        </span>
      </span>
      <h3 className="card-title">
        <Link to={`/posts/${post.postId}`} className="card-link">
          <span className="highlight">{post.title}</span>
        </Link>
      </h3>
      {post.preview && <p className="card-preview">{post.preview}</p>}
      <span className="card-foot">
        <span className="card-by">
          <Link to={`/blog/${post.blogId}`} className="post-row-blog card-raise">
            {post.blogName}
          </Link>
          <span className="feed-author">{post.nickname}</span>
        </span>
        <span className="feed-counts">
          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
          <span aria-label={`댓글 ${post.commentCount}개`}>댓글 {post.commentCount}</span>
          <span aria-label={`좋아요 ${post.likeCount}개`}>좋아요 {post.likeCount}</span>
        </span>
      </span>
    </li>
  )
}
