// 통계 /manage/stats (specs/006 US6, T043, FR-031, FR-032)
// - 기간 7일 / 30일 (처음 30일). 조회수·방문자 자리와 댓글 수 자리 두 개
// - 선 그래프 두 개 (T058, D-10: uPlot). 같은 숫자를 접힌 표로도 볼 수 있다
// - 유입 경로·시간대·기기 정보는 없다 (BM-06-8 확인 필요, 원본대로)
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import DailyLineChart, { type ChartSeries } from './charts/DailyLineChart'
import DailyTable from './DailyTable'
import { getStats, type DailyStat } from './manageApi'
import './manage-pages.css'

const PERIODS = [7, 30]
const DEFAULT_PERIOD = 30

function periodFrom(value: string | null): number {
  const days = Number(value)
  return PERIODS.includes(days) ? days : DEFAULT_PERIOD
}

export default function StatsPage() {
  const [params, setParams] = useSearchParams()
  const days = periodFrom(params.get('days'))
  const [daily, setDaily] = useState<{ days: number; rows: DailyStat[] } | null>(null)
  const [failure, setFailure] = useState<{ days: number; message: string } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getStats(days, controller.signal)
      .then((res) => {
        setDaily({ days: res.days, rows: res.daily })
        setFailure(null)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setFailure({ days, message: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      })
    return () => controller.abort()
  }, [days])

  const rows = daily?.days === days ? daily.rows : null
  // 오류도 그 기간의 것만 보인다. 다른 기간으로 바꾸면 지난 오류가 남지 않는다
  const error = failure?.days === days ? failure.message : null
  const chart = useMemo(() => {
    if (!rows) return null
    const views: ChartSeries[] = [
      { label: '조회수', values: rows.map((r) => r.views), colorToken: '--chart-1' },
      { label: '방문자', values: rows.map((r) => r.visitors), colorToken: '--chart-2' },
    ]
    const comments: ChartSeries[] = [{ label: '댓글', values: rows.map((r) => r.comments), colorToken: '--chart-1' }]
    return { dates: rows.map((r) => r.date), views, comments }
  }, [rows])

  return (
    <div className="manage">
      <header className="manage-head manage-head-row">
        <h1 className="manage-title">통계</h1>
        <div className="period-switch" role="group" aria-label="기간">
          {PERIODS.map((p) => (
            <button
              key={p}
              type="button"
              className={`btn btn-small ${p === days ? 'btn-primary' : 'btn-quiet'}`}
              aria-pressed={p === days}
              onClick={() => setParams(p === DEFAULT_PERIOD ? {} : { days: String(p) })}
            >
              {p}일
            </button>
          ))}
        </div>
      </header>

      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
      {!error && !rows && <p className="hint">불러오는 중</p>}
      {!error && rows && chart && (
        <div className="stats-panels">
          <section className="manage-panel" aria-labelledby="stats-views">
            <h2 id="stats-views" className="manage-panel-title">
              조회수·방문자
            </h2>
            <DailyLineChart dates={chart.dates} series={chart.views} label={`최근 ${days}일 조회수와 방문자 그래프`} />
            <details className="chart-numbers">
              <summary>숫자로 보기</summary>
              <DailyTable
                caption={`최근 ${days}일 조회수와 방문자`}
                rows={rows}
                columns={[
                  { label: '조회수', value: (r) => r.views },
                  { label: '방문자', value: (r) => r.visitors },
                ]}
              />
            </details>
          </section>
          <section className="manage-panel" aria-labelledby="stats-comments">
            <h2 id="stats-comments" className="manage-panel-title">
              댓글 수
            </h2>
            <DailyLineChart dates={chart.dates} series={chart.comments} label={`최근 ${days}일 댓글 수 그래프`} />
            <details className="chart-numbers">
              <summary>숫자로 보기</summary>
              <DailyTable
                caption={`최근 ${days}일 댓글 수`}
                rows={rows}
                columns={[{ label: '댓글', value: (r) => r.comments }]}
              />
            </details>
          </section>
        </div>
      )}
    </div>
  )
}
