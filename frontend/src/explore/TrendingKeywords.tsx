// 실시간 인기 검색어 (specs/015 US1): 최근 1시간 순위와 10분 전과의 변화(▲ ▼ NEW).
// 아직 모인 검색어가 없거나 불러오지 못하면 칸을 그리지 않는다(검색은 그대로 된다)
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { getTrending, type Trend } from './exploreApi'

const REFRESH_MS = 10 * 60 * 1000

function ChangeMark({ trend }: { trend: Trend }) {
  switch (trend.change) {
    case 'new':
      return <span className="trend-change trend-new">NEW</span>
    case 'up':
      return (
        <span className="trend-change trend-up" role="img" aria-label={`${trend.delta}칸 올라감`}>
          ▲{trend.delta}
        </span>
      )
    case 'down':
      return (
        <span className="trend-change trend-down" role="img" aria-label={`${trend.delta}칸 내려감`}>
          ▼{trend.delta}
        </span>
      )
    default:
      return (
        <span className="trend-change trend-same" role="img" aria-label="그대로">
          –
        </span>
      )
  }
}

export default function TrendingKeywords({ className = '' }: { className?: string }) {
  const [items, setItems] = useState<Trend[] | null>(null)

  useEffect(() => {
    let controller = new AbortController()
    const load = () => {
      controller.abort()
      controller = new AbortController()
      const signal = controller.signal
      getTrending(signal)
        .then((data) => setItems(data.items))
        .catch(() => {
          if (!signal.aborted) setItems((prev) => prev ?? [])
        })
    }
    load()
    // 순위는 10분마다 바뀐다. 창을 열어 둔 동안 다시 묻되, 보이지 않는 창에서는 묻지 않고 다시 보일 때 바로 묻는다
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') load()
    }, REFRESH_MS)
    const onVisible = () => {
      if (document.visibilityState === 'visible') load()
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', onVisible)
      controller.abort()
    }
  }, [])

  if (!items || items.length === 0) return null

  return (
    <section className={`trending ${className}`.trim()} aria-labelledby="trending-title">
      <h2 id="trending-title" className="home-section-title">
        <span className="ink-highlight">지금 많이 찾는 말</span>
      </h2>
      <p className="trending-note">최근 1시간, 10분마다 바뀝니다</p>
      <ol className="trend-list">
        {items.map((trend) => (
          <li key={trend.keyword} className="trend-item">
            <span className="trend-rank" aria-hidden="true">
              {trend.rank}
            </span>
            <Link to={`/search?${new URLSearchParams({ q: trend.keyword }).toString()}`} className="trend-keyword">
              {trend.keyword}
            </Link>
            <ChangeMark trend={trend} />
          </li>
        ))}
      </ol>
    </section>
  )
}
