// 첫 화면 맨 위 (specs/008): 이번 주 인기 글 가운데 사진이 있는 첫 글(없으면 1위)을 크게, 옆에 다음 인기 글 둘을 작게.
// 오늘의 이슈(specs/015 US2)가 있으면 그 순서 그대로 1위를 크게, 2·3위를 작게 놓고 "오늘의 이슈" 도장을 찍는다.
// 넓은 화면을 꽉 채운다 (2026-10-12 사용자: 양옆 공백이 답답하다). 좁은 화면에서는 작은 카드가 아래로
// 카드 어디를 눌러도 글로 가고, 주제·블로그 링크는 따로 눌린다 (FeedItem과 같은 방식)
import { Link } from 'react-router'
import type { FeedPost } from './exploreApi'
import { formatDate } from './rules'
import Thumbnail from './Thumbnail'

interface Props {
  posts: FeedPost[]
  /** 맨 위 띠 이름 (기본: 이번 주 눈여겨볼 글) */
  label?: string
  /** 참이면 사진이 있는 글을 앞으로 당기지 않고 받은 순서(순위) 그대로 놓는다 */
  ranked?: boolean
}

/** 크게 보여 줄 글: 사진이 있는 인기 글을 먼저. 옆 작은 카드는 나머지에서 사진 있는 글 먼저 두 개 */
function pick(posts: FeedPost[], ranked: boolean): { main?: FeedPost; side: FeedPost[] } {
  if (ranked) return { main: posts[0], side: posts.slice(1, 3) }
  const main = posts.find((post) => post.thumbnail) ?? posts[0]
  const rest = posts.filter((post) => post !== main)
  const side = [...rest.filter((post) => post.thumbnail), ...rest.filter((post) => !post.thumbnail)].slice(0, 2)
  return { main, side }
}

export default function FeaturedPost({ posts, label = '이번 주 눈여겨볼 글', ranked = false }: Props) {
  const { main, side } = pick(posts, ranked)
  if (!main) return null
  return (
    <section
      className={['feature', ranked && 'feature-ranked', side.length === 0 && 'feature-solo'].filter(Boolean).join(' ')}
      aria-label={label}
    >
      <MainCard post={main} label={label} rank={ranked ? 1 : undefined} />
      {side.length > 0 && (
        <ul className="feature-side">
          {side.map((post, index) => (
            <li key={post.postId} className="card feature-mini">
              {ranked && (
                <span className="issue-rank">
                  <span aria-hidden="true">{index + 2}</span>
                  <span className="visually-hidden">{index + 2}위</span>
                </span>
              )}
              <Thumbnail src={post.thumbnail} text={post.preview || post.title} topic={post.topic} variant="row" />
              <span className="feature-mini-body">
                <Link to={`/?topic=${encodeURIComponent(post.topic)}`} className="feed-topic card-raise">
                  {post.topic}
                </Link>
                <Link to={`/posts/${post.postId}`} className="card-link feature-mini-title">
                  <span className="highlight">{post.title}</span>
                </Link>
                <span className="feature-mini-by">
                  {post.blogName} · 원고지 {post.sheets}매
                </span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function MainCard({ post, label, rank }: { post: FeedPost; label: string; rank?: number }) {
  return (
    <article className="card feature-card" aria-labelledby={`feature-${post.postId}`}>
      <Thumbnail src={post.thumbnail} text={post.preview || post.title} topic={post.topic} variant="feature" />
      <div className="feature-body">
        <span className="card-top">
          {rank ? (
            <span className="feature-label issue-stamp">
              {label} <span className="issue-rank-text">{rank}위</span>
            </span>
          ) : (
            <span className="feature-label">{label}</span>
          )}
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
