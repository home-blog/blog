// 숫자를 0에서 target까지 세어 올린다 (v0.5.1 원고지 손길: 블로그 매수, 첫 화면 분량)
// 움직임 줄이기 설정이거나 0이면 바로 target. target이 아직 없으면 null
import { useEffect, useState } from 'react'

const COUNT_MS = 1200

function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

export function useCountUp(target: number | null): number | null {
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
