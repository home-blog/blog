// 목록 한 줄 (specs/004 FR-005, FR-015): 분류, 작성일, 제목, 본문 앞부분. 검색 결과면 블로그 이름도
// 제목과 미리보기는 글자로만 보인다 (React 기본 이스케이프)
import { Link } from 'react-router'
import { formatDate } from './rules'
import type { PostSummary } from './exploreApi'

interface Props {
  post: Pick<PostSummary, 'postId' | 'title' | 'categoryName' | 'createdAt' | 'preview'> & {
    visibility?: PostSummary['visibility']
  }
  blogName?: string
}

export default function PostRow({ post, blogName }: Props) {
  return (
    <li className="post-row">
      <Link to={`/posts/${post.postId}`} className="post-row-link">
        <span className="post-row-meta">
          {blogName && <span className="post-row-blog">{blogName}</span>}
          <span className="post-row-category">{post.categoryName}</span>
          <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
          {post.visibility === 'private' && <span className="blog-badge">비공개</span>}
        </span>
        <span className="post-row-title">{post.title}</span>
        {post.preview && <span className="post-row-preview">{post.preview}</span>}
      </Link>
    </li>
  )
}
