// 누를 때마다 먹 한 방울 (2026-10-20 사용자: "하나하나 클릭할 때마다 재미를"): 단추·링크·탭을 누른 자리에
// 먹 동그라미가 번지고 가운데에 형광펜 점이 잠깐 남는다. 화면 어디에 하나만 둔다(Layout). 움직임 줄이기 설정이면 하지 않는다
import { useEffect } from 'react'
import './ink-touch.css'

const TARGET = 'a[href], button:not(:disabled), [role="tab"], [role="button"], summary, label[for]'
/** 끝나는 이벤트를 못 받아도 남지 않게 (탭이 숨겨지면 animationend가 오지 않는다) */
const LIFETIME_MS = 900

export default function InkTouch() {
  useEffect(() => {
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    function onDown(event: PointerEvent) {
      // 페이지를 연 뒤 설정을 바꿔도 따르도록 누를 때마다 본다
      if (reduce?.matches) return
      if (event.button !== 0 || !(event.target instanceof Element) || !event.target.closest(TARGET)) return
      const drop = document.createElement('span')
      drop.className = 'ink-touch'
      drop.setAttribute('aria-hidden', 'true')
      drop.style.left = `${event.clientX}px`
      drop.style.top = `${event.clientY}px`
      document.body.appendChild(drop)
      const remove = () => drop.remove()
      drop.addEventListener('animationend', remove, { once: true })
      window.setTimeout(remove, LIFETIME_MS)
    }
    document.addEventListener('pointerdown', onDown, { passive: true })
    return () => document.removeEventListener('pointerdown', onDown)
  }, [])
  return null
}
