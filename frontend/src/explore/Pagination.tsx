// 페이지 번호 (specs/004 FR-003): 현재 페이지 강조, 첫 페이지에서 `이전`·마지막 페이지에서 `다음`을 누를 수 없다
import { Link } from 'react-router'

interface Props {
  page: number
  totalPages: number
  /** 그 페이지로 가는 주소 */
  hrefFor: (page: number) => string
}

/** 한 번에 보여 줄 번호 수. 많으면 현재 페이지 둘레만 보인다 */
const WINDOW = 10

export default function Pagination({ page, totalPages, hrefFor }: Props) {
  const start = Math.max(1, Math.min(page - Math.floor(WINDOW / 2), totalPages - WINDOW + 1))
  const end = Math.min(totalPages, start + WINDOW - 1)
  const numbers = Array.from({ length: end - start + 1 }, (_, i) => start + i)

  return (
    <nav className="pagination" aria-label="페이지">
      {page > 1 ? (
        <Link className="pagination-step" to={hrefFor(page - 1)}>
          이전
        </Link>
      ) : (
        <span className="pagination-step" aria-disabled="true">
          이전
        </span>
      )}
      <ol>
        {numbers.map((n) => (
          <li key={n}>
            {n === page ? (
              <span className="pagination-number" aria-current="page">
                {n}
              </span>
            ) : (
              <Link className="pagination-number" to={hrefFor(n)} aria-label={`${n}페이지`}>
                {n}
              </Link>
            )}
          </li>
        ))}
      </ol>
      {page < totalPages ? (
        <Link className="pagination-step" to={hrefFor(page + 1)}>
          다음
        </Link>
      ) : (
        <span className="pagination-step" aria-disabled="true">
          다음
        </span>
      )}
    </nav>
  )
}
