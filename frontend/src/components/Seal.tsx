// 도장(낙관) (v0.5.1 원고지 손길): 글 끝의 "글결", 저장 알림의 "탈고"·"퇴고"
// stampOnView면 화면에 들어올 때 한 번 쿵 찍힌다. 움직임 줄이기 설정이거나 관찰 도구가 없으면 처음부터 찍혀 있다
import { useEffect, useRef, useState } from 'react'
import './seal.css'

interface Props {
  /** 도장 글자 (2자 또는 4자) */
  text: string
  small?: boolean
  stampOnView?: boolean
  /** 화면 읽기 도구가 읽을 이름. 없으면 꾸밈으로 보고 읽지 않는다 */
  label?: string
}

function canAnimate(): boolean {
  return (
    typeof IntersectionObserver !== 'undefined' &&
    !window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  )
}

export default function Seal({ text, small, stampOnView, label }: Props) {
  const ref = useRef<HTMLSpanElement>(null)
  // 기다림: 아직 화면 밖이라 숨겨 둔다. 찍음: 쿵 하고 나타난다. 없음: 처음부터 보인다
  const [phase, setPhase] = useState<'waiting' | 'stamped' | 'still'>(() =>
    stampOnView && canAnimate() ? 'waiting' : 'still',
  )

  useEffect(() => {
    if (phase !== 'waiting' || !ref.current) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setPhase('stamped')
          observer.disconnect()
        }
      },
      { threshold: 0.6 },
    )
    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [phase])

  const chars = [...text]
  return (
    <span
      ref={ref}
      className={['seal', small && 'seal-small', chars.length > 2 && 'seal-four', `seal-${phase}`]
        .filter(Boolean)
        .join(' ')}
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
    >
      {chars.map((char, i) => (
        <span key={i}>{char}</span>
      ))}
    </span>
  )
}
