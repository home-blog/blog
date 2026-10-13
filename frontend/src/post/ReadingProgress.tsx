// 읽기 진행을 원고지 칸으로 (v0.5.1 원고지 손길): 화면 맨 위 20칸이 읽은 만큼 한 칸씩 먹으로 찬다
// 글 본문(target)의 처음부터 끝까지를 기준으로 센다. 꾸밈이라 화면 읽기 도구에는 읽히지 않는다
// 글 화면의 등장 효과(transform)가 위치 기준을 바꾸지 않게 body 바로 아래에 그린다
import { useEffect, useState, type RefObject } from 'react'
import { createPortal } from 'react-dom'
import './reading-progress.css'

const CELLS = 20

interface Props {
  target: RefObject<HTMLElement | null>
}

export default function ReadingProgress({ target }: Props) {
  const [filled, setFilled] = useState(0)

  useEffect(() => {
    let frame = 0
    function measure() {
      frame = 0
      const element = target.current
      if (!element) return
      const rect = element.getBoundingClientRect()
      const scrollable = rect.height - window.innerHeight
      const ratio = scrollable <= 0 ? (rect.bottom <= window.innerHeight ? 1 : 0) : -rect.top / scrollable
      setFilled(Math.round(Math.min(1, Math.max(0, ratio)) * CELLS))
    }
    function schedule() {
      if (!frame) frame = requestAnimationFrame(measure)
    }
    measure()
    window.addEventListener('scroll', schedule, { passive: true })
    window.addEventListener('resize', schedule)
    return () => {
      window.removeEventListener('scroll', schedule)
      window.removeEventListener('resize', schedule)
      if (frame) cancelAnimationFrame(frame)
    }
  }, [target])

  return createPortal(
    <div className="reading-progress" aria-hidden="true">
      {Array.from({ length: CELLS }, (_, i) => (
        <span key={i} className={i < filled ? 'on' : undefined} />
      ))}
    </div>,
    document.body,
  )
}
