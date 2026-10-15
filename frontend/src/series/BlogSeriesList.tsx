// 블로그 화면의 연재 목록 (specs/013 US2-5). 연재가 없으면 아무것도 보이지 않는다
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { getBlogSeries, type SeriesSummary } from './seriesApi'
import './series.css'

interface Props {
  blogId: number
  /** 보는 사람이 바뀌면 다시 센다: 글 수는 보이는 글로 센다 */
  viewer: string | number
}

export default function BlogSeriesList({ blogId, viewer }: Props) {
  const key = `${blogId}:${viewer}`
  const [state, setState] = useState<{ key: string; items: SeriesSummary[] } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const requestKey = `${blogId}:${viewer}`
    getBlogSeries(blogId, controller.signal)
      .then((items) => setState({ key: requestKey, items }))
      .catch(() => undefined)
    return () => controller.abort()
  }, [blogId, viewer])

  const items = state?.key === key ? state.items : []
  if (items.length === 0) return null
  return (
    <section className="blog-series" aria-labelledby="blog-series-title">
      <h2 id="blog-series-title" className="blog-series-title">
        연재
      </h2>
      <ul className="blog-series-list">
        {items.map((series) => (
          <li key={series.seriesId}>
            <Link to={`/series/${series.seriesId}`}>
              {series.name}
              <span className="hint">{series.postCount}화</span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
