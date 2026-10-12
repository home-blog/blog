// 첫 화면 맨 위의 큰 카드 (specs/008): 이번 주 인기 글 가운데 사진이 있는 첫 글(없으면 1위)을 크게
// 카드 어디를 눌러도 글로 가고, 주제·블로그 링크는 따로 눌린다 (FeedItem과 같은 방식)
import { Link } from 'react-router'
import type { FeedPost } from './exploreApi'
import { formatDate } from './rules'
import Thumbnail from './Thumbnail'

interface Props {
  posts: FeedPost[]
}

/** 크게 보여 줄 글: 사진이 있는 인기 글을 먼저 */
function pickFeatured(posts: FeedPost[]): FeedPost | undefined {
  return posts.find((post) => post.thumbnail) ?? posts[0]
}

export default function FeaturedPost({ posts }: Props) {
  const post = pickFeatured(posts)
  if (!post) return null
  return (
    <article className="card feature-card" aria-labelledby={`feature-${post.postId}`}>
      <Thumbnail src={post.thumbnail} text={post.preview || post.title} topic={post.topic} variant="feature" />
      <div className="feature-body">
        <span className="card-top">
          <span className="feature-label">이번 주 눈여겨볼 글</span>
          <Link to={`/?topic=${encodeURIComponent(post.topic)}`} className="feed-topic card-raise">
            {post.topic}
          </Link>
        </span>
        <h2 id={`feature-${post.postId}`} className="feature-title">
          <Link to={`/posts/${post.postId}`} className="card-link">
            <span className="highlight">{post.title}</span>
          </Link>
        </h2>
        {post.preview && <p className="feature-preview">{post.preview}</p>}
        <span className="card-foot">
          <span className="card-by">
            <Link to={`/blog/${post.blogId}`} className="post-row-blog card-raise">
              {post.blogName}
            </Link>
            <span className="feed-author">{post.nickname}</span>
          </span>
          <span className="feed-counts">
            <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
            <span className="manuscript-count">
              원고지 {post.sheets}매 · {post.readMinutes}분
            </span>
          </span>
        </span>
      </div>
    </article>
  )
}
