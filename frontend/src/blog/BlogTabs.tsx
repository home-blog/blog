// 블로그 머리의 탭 (specs/021): 원고(글 목록) · 방명록. 지금 있는 곳은 원고지 칸이 채워진다
import { Link } from 'react-router'

interface Props {
  blogId: number
  current: 'posts' | 'guestbook'
}

export default function BlogTabs({ blogId, current }: Props) {
  return (
    <nav className="blog-tabs" aria-label="블로그 메뉴">
      <Link to={`/blog/${blogId}`} className="blog-tab" aria-current={current === 'posts' ? 'page' : undefined}>
        <span className="blog-tab-cell" aria-hidden="true">
          원
        </span>
        <span className="blog-tab-name">원고</span>
      </Link>
      <Link
        to={`/blog/${blogId}/guestbook`}
        className="blog-tab"
        aria-current={current === 'guestbook' ? 'page' : undefined}
      >
        <span className="blog-tab-cell" aria-hidden="true">
          엽
        </span>
        <span className="blog-tab-name">방명록</span>
      </Link>
    </nav>
  )
}
