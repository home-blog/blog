// 날짜별 선 그래프 (specs/006 T058, D-10 A: 가벼운 그래프 도구 uPlot 1.6.32를 골랐다)
// - 마우스를 올리면(휴대폰은 누르면) 아래 범례에 그날 숫자가 나온다
// - 화면 폭이 바뀌면 다시 맞춘다 (NF-03). 색은 index.css 토큰을 읽어 쓴다
// - 같은 숫자를 표로도 볼 수 있게 아래에 접힌 표를 둔다 (화면 낭독기, 정확한 숫자)
import { useEffect, useRef } from 'react'
import uPlot from 'uplot'
import 'uplot/dist/uPlot.min.css'
import './charts.css'

export interface ChartSeries {
  label: string
  values: number[]
  /** index.css 토큰 이름 (예: --accent) */
  colorToken: string
}

interface Props {
  /** 한국 날짜 YYYY-MM-DD, 오래된 날부터 */
  dates: string[]
  series: ChartSeries[]
  /** 그래프 이름 (화면 낭독기) */
  label: string
  height?: number
}

/** 2026-10-08 → 10.8 */
function shortDate(date: string): string {
  const [, month, day] = date.split('-')
  return `${Number(month)}.${Number(day)}`
}

function token(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#2c6a4d'
}

export default function DailyLineChart({ dates, series, label, height = 220 }: Props) {
  const box = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = box.current
    if (!el) return
    const xs = dates.map((_, i) => i)
    const grid = { stroke: token('--grid'), width: 1, dash: [3, 3] }
    const ink = token('--ink-soft')
    const options: uPlot.Options = {
      width: el.clientWidth,
      height,
      scales: { x: { time: false } },
      cursor: { drag: { x: false, y: false } },
      legend: { live: true },
      axes: [
        {
          stroke: ink,
          grid,
          values: (_u, splits) => splits.map((i) => (Number.isInteger(i) && dates[i] ? shortDate(dates[i]) : '')),
        },
        { stroke: ink, grid, size: 44, values: (_u, splits) => splits.map((v) => (Number.isInteger(v) ? String(v) : '')) },
      ],
      series: [
        { label: '날짜', value: (_u, i) => (i == null || !dates[i] ? '' : shortDate(dates[i])) },
        ...series.map((s) => ({
          label: s.label,
          stroke: token(s.colorToken),
          width: 2,
          points: { size: 5 },
          value: (_u: uPlot, v: number | null) => (v == null ? '' : v.toLocaleString('ko-KR')),
        })),
      ],
    }
    const plot = new uPlot(options, [xs, ...series.map((s) => s.values)], el)
    const resize = new ResizeObserver(() => plot.setSize({ width: el.clientWidth, height }))
    resize.observe(el)
    return () => {
      resize.disconnect()
      plot.destroy()
    }
  }, [dates, series, height])

  return <div ref={box} className="daily-chart" role="img" aria-label={label} />
}
