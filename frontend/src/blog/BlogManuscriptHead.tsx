// 블로그 머리 (v0.5.1 원고지 손길): 블로그 이름을 원고지에 한 자씩 적고, 지금까지 쓴 원고지 매수를 세어 올린다
// 이름은 화면 읽기 도구용 제목(h1)으로도 둔다. 원고지는 꾸밈이다
import { useEffect, useState } from 'react'
import ManuscriptSheet from '../pages/ManuscriptSheet'
import { getBlogManuscript, type BlogManuscript } from './blogApi'

const COLUMNS = 10
const MAX_ROWS = 3
const COUNT_MS = 1200

function nameLines(name: string): string[] {
  const chars = [...name]
  const lines: string[] = []
  for (let i = 0; i < chars.length && lines.length < MAX_ROWS; i += COLUMNS) {
    lines.push(chars.slice(i, i + COLUMNS).join(''))
  }
  return lines
}

function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

/** 0에서 target까지 세어 올린다. 움직임 줄이기 설정이거나 0이면 바로 target */
function useCountUp(target: number | null): number | null {
  const instant = target === null || target === 0 || prefersReducedMotion()
  const [animated, setAnimated] = useState(0)
  useEffect(() => {
    if (instant || target === null) return
    let frame = 0
    const start = performance.now()
    function step(now: number) {
      const progress = Math.min(1, (now - start) / COUNT_MS)
      setAnimated(Math.round(target! * (1 - Math.pow(1 - progress, 3))))
      if (progress < 1) frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frame)
  }, [target, instant])
  return instant ? target : animated
}

interface Props {
  blogId: number
  name: string
  /** 보는 사람이 바뀌면(로그인·로그아웃) 다시 센다: 주인은 비공개 글까지 센다 */
  viewer: string | number
}

export default function BlogManuscriptHead({ blogId, name, viewer }: Props) {
  const [total, setTotal] = useState<{ key: string; data: BlogManuscript } | null>(null)
  const key = `${blogId}:${viewer}`

  useEffect(() => {
    const controller = new AbortController()
    const requestKey = `${blogId}:${viewer}`
    getBlogManuscript(blogId, controller.signal)
      .then((data) => setTotal({ key: requestKey, data }))
      // 매수는 꾸밈이라 못 불러오면 보여 주지 않는다
      .catch(() => undefined)
    return () => controller.abort()
  }, [blogId, viewer])

  const shown = total?.key === key ? total.data : null
  const sheets = useCountUp(shown ? shown.sheets : null)
  const lines = nameLines(name)

  return (
    <div className="blog-title">
      <h1 className="visually-hidden">{name}</h1>
      {/* 칸 수는 가장 긴 줄 + 빈 칸 하나 (짧은 이름에 빈 칸이 줄줄이 남지 않게) */}
      <ManuscriptSheet
        lines={lines}
        rows={lines.length}
        columns={Math.min(COLUMNS, Math.max(0, ...lines.map((line) => [...line].length)) + 1)}
        small
      />
      {shown && (
        <p className="blog-sheets">
          <span>
            <span className="blog-sheets-number">{(sheets ?? 0).toLocaleString('ko-KR')}</span>매
          </span>
          <span className="blog-sheets-note hint">
            지금까지 쓴 원고지 · 글 {shown.postCount.toLocaleString('ko-KR')}편
          </span>
        </p>
      )}
    </div>
  )
}
