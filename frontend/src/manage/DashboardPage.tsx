// 대시보드 /manage (specs/006 US7, T048, FR-006 ~ FR-011)
// - 오늘·어제·누적 조회수와 방문자, 새 댓글 수(있으면 강조, "댓글 보기"), 최근 30일 그래프(T058, 숫자는 접힌 표), 인기 글, 최근 글
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import '../blog/blog.css'
import { formatDate } from '../explore/rules'
import DailyLineChart, { type ChartSeries } from './charts/DailyLineChart'
import DailyTable from './DailyTable'
import { getDashboard, type Count, type Dashboard } from './manageApi'
import { useNewCommentCount } from './newCommentCount'
import './manage-pages.css'

function CountCard({ label, count }: { label: string; count: Count }) {
  return (
    <section className="count-card" aria-label={label}>
      <h2 className="count-card-title">{label}</h2>
      <dl className="count-card-numbers">
        <div>
          <dt>오늘</dt>
          <dd>{count.today.toLocaleString('ko-KR')}</dd>
        </div>
        <div>
          <dt>어제</dt>
          <dd>{count.yesterday.toLocaleString('ko-KR')}</dd>
        </div>
        <div>
          <dt>누적</dt>
          <dd>{count.total.toLocaleString('ko-KR')}</dd>
        </div>
      </dl>
    </section>
  )
}

export default function DashboardPage() {
  const { setCount } = useNewCommentCount()
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getDashboard(controller.signal)
      .then((res) => {
        setDashboard(res)
        setCount(res.newCommentCount)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [setCount])

  const chart = useMemo(() => {
    if (!dashboard) return null
    const series: ChartSeries[] = [
      { label: '조회수', values: dashboard.chart.map((d) => d.views), colorToken: '--category-1' },
      { label: '방문자', values: dashboard.chart.map((d) => d.visitors), colorToken: '--category-2' },
    ]
    return { dates: dashboard.chart.map((d) => d.date), series }
  }, [dashboard])

  if (error) {
    return (
      <p className="msg msg-error" role="alert">
        {error}
      </p>
    )
  }
  if (!dashboard || !chart) return <p className="hint">불러오는 중</p>

  const noPosts = dashboard.recentPosts.length === 0

  return (
    <div className="manage">
      <header className="manage-head">
        <h1 className="manage-title">대시보드</h1>
      </header>

      <div className="count-cards">
        <CountCard label="조회수" count={dashboard.views} />
        <CountCard label="방문자" count={dashboard.visitors} />
        <section className={`count-card${dashboard.newCommentCount > 0 ? ' is-new' : ''}`} aria-label="새 댓글">
          <h2 className="count-card-title">새 댓글</h2>
          <p className="count-card-big">{dashboard.newCommentCount.toLocaleString('ko-KR')}</p>
          <Link to="/manage/comments" className="btn btn-quiet btn-small">
            댓글 보기
          </Link>
        </section>
      </div>

      <section className="manage-panel" aria-labelledby="dash-chart">
        <div className="manage-panel-head">
          <h2 id="dash-chart" className="manage-panel-title">
            최근 30일
          </h2>
          <Link to="/manage/stats" className="manage-row-link">
            통계 더 보기
          </Link>
        </div>
        <DailyLineChart dates={chart.dates} series={chart.series} label="최근 30일 조회수와 방문자 그래프" />
        <details className="chart-numbers">
          <summary>숫자로 보기</summary>
          <DailyTable
            caption="최근 30일 조회수와 방문자"
            rows={dashboard.chart}
            columns={[
              { label: '조회수', value: (r) => r.views },
              { label: '방문자', value: (r) => r.visitors },
            ]}
          />
        </details>
      </section>

      <div className="dash-lists">
        <section className="manage-panel" aria-labelledby="dash-popular">
          <h2 id="dash-popular" className="manage-panel-title">
            인기 글 <span className="hint">최근 7일</span>
          </h2>
          {noPosts ? (
            <p className="hint">아직 쓴 글이 없습니다</p>
          ) : dashboard.popularPosts.length === 0 ? (
            <p className="hint">최근 7일 동안 읽힌 공개 글이 없습니다</p>
          ) : (
            <ol className="dash-list">
              {dashboard.popularPosts.map((post) => (
                <li key={post.postId}>
                  <Link to={`/posts/${post.postId}`} className="manage-row-title">
                    {post.title}
                  </Link>
                  <span className="hint">조회 {post.views.toLocaleString('ko-KR')}</span>
                </li>
              ))}
            </ol>
          )}
        </section>
        <section className="manage-panel" aria-labelledby="dash-recent">
          <h2 id="dash-recent" className="manage-panel-title">
            최근 글
          </h2>
          {noPosts ? (
            <p className="hint">아직 쓴 글이 없습니다</p>
          ) : (
            <ul className="dash-list">
              {dashboard.recentPosts.map((post) => (
                <li key={post.postId}>
                  <Link to={`/posts/${post.postId}`} className="manage-row-title">
                    {post.title}
                  </Link>
                  {post.visibility === 'private' && <span className="blog-badge">비공개</span>}
                  <time className="hint" dateTime={post.createdAt}>
                    {formatDate(post.createdAt)}
                  </time>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
