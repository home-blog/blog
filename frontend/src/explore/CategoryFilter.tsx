// 분류로 좁혀 보기 (specs/004 US4, T025): 글 목록 위 알약 줄의 분류를 누르면 그 분류의 글만 보인다
// 분류 목록은 003의 GET /api/blogs/{blogId}/categories를 쓴다. 방문자에게는 비공개 분류가 빠져 온다
// 고른 분류는 주소 ?category=에 담고 페이지는 1로 돌린다. `전체`로 선택을 푼다
import { Link, useLocation } from 'react-router'
import type { CategorySummary } from '../blog/blogApi'

interface Props {
  categories: CategorySummary[]
  /** 주소의 ?category= 값 (고르지 않았으면 null) */
  selected: string | null
}

export default function CategoryFilter({ categories, selected }: Props) {
  const { pathname } = useLocation()

  return (
    <nav className="blog-categories" aria-labelledby="category-title">
      <h2 id="category-title" className="visually-hidden">
        분류
      </h2>
      <ul className="category-pills">
        <li>
          <Link to={pathname} className="pill" aria-current={selected === null ? 'page' : undefined}>
            전체
          </Link>
        </li>
        {categories.map((category) => (
          <li key={category.categoryId}>
            <Link
              to={`${pathname}?category=${category.categoryId}`}
              className="pill"
              aria-current={selected === String(category.categoryId) ? 'page' : undefined}
              aria-label={`${category.name}${category.visibility === 'private' ? ' (비공개)' : ''}, 글 ${category.postCount}개`}
            >
              <span className="pill-name">{category.name}</span>
              {category.visibility === 'private' && <span aria-hidden="true">비공개</span>}
              <span className="pill-count" aria-hidden="true">
                {category.postCount}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  )
}
