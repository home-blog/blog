// 첫 화면 글 모음 한 줄: 주제 · 블로그(글쓴이) · 작성일, 제목, 본문 앞부분, 댓글·좋아요 수
// 제목은 글로, 블로그 이름은 그 블로그로 간다 (링크 안에 링크를 넣지 않게 나눠 둔다)
import { Link } from 'react-router'
import type { FeedPost } from './exploreApi'
import { formatDate } from './rules'

interface Props {
  post: FeedPost
}

export default function FeedItem({ post }: Props) {
  return (
    <li className="post-row feed-item">
      <span className="post-row-meta">
        <Link to={`/?topic=${encodeURIComponent(post.topic)}`} className="feed-topic">
          {post.topic}
        </Link>
        <Link to={`/blog/${post.blogId}`} className="post-row-blog">
          {post.blogName}
        </Link>
        <span className="feed-author">{post.nickname}</span>
        <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
      </span>
      <Link to={`/posts/${post.postId}`} className="feed-link">
        <span className="post-row-title">{post.title}</span>
        {post.preview && <span className="post-row-preview">{post.preview}</span>}
      </Link>
      <span className="feed-counts">
        <span>댓글 {post.commentCount}</span>
        <span>좋아요 {post.likeCount}</span>
      </span>
    </li>
  )
}
