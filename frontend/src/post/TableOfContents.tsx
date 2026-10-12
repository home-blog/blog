// 글 목차 (specs/007 US2): 그려진 본문의 소제목(h1~h3)을 찾아 목차를 만든다
// - 소제목이 2개 이상일 때만 보인다
// - 소제목마다 주소(#…)를 붙여, 목차를 누르면 이동하고 그 주소를 그대로 공유할 수 있다
// - 읽는 위치의 소제목을 목차에 표시한다
// 마크다운을 따로 해석하지 않고 그려진 결과를 읽으므로 본문과 어긋나지 않는다 (본문 도구는 늦게 불러오므로 바뀌는 것을 지켜본다)
import { useEffect, useState, type RefObject } from 'react'

interface Heading {
  id: string
  text: string
  level: number
}

const MIN_HEADINGS = 2

/** 소제목 글자로 주소 조각을 만든다. 한글은 그대로 두고 공백은 -로. 겹치면 -2, -3 … */
function slug(text: string, used: Map<string, number>): string {
  const base =
    text
      .trim()
      .toLowerCase()
      .replace(/[^\p{L}\p{N}\s-]/gu, '')
      .replace(/\s+/g, '-')
      .slice(0, 60) || 'section'
  const count = used.get(base) ?? 0
  used.set(base, count + 1)
  return count === 0 ? base : `${base}-${count + 1}`
}

function collect(container: HTMLElement): Heading[] {
  const used = new Map<string, number>()
  return Array.from(container.querySelectorAll<HTMLHeadingElement>('.markdown h1, .markdown h2, .markdown h3')).map(
    (el) => {
      const text = el.textContent ?? ''
      if (!el.id) el.id = slug(text, used)
      return { id: el.id, text, level: Number(el.tagName[1]) }
    },
  )
}

function sameHeadings(a: Heading[], b: Heading[]): boolean {
  return a.length === b.length && a.every((h, i) => h.id === b[i].id && h.text === b[i].text)
}

export default function TableOfContents({ container, source }: { container: RefObject<HTMLElement | null>; source: string }) {
  const [headings, setHeadings] = useState<Heading[]>([])
  const [active, setActive] = useState<string | null>(null)

  // 본문이 그려지거나 바뀌면 소제목을 다시 모은다
  useEffect(() => {
    const el = container.current
    if (!el) return
    let scrolledToHash = false
    const update = () => {
      const found = collect(el)
      setHeadings((prev) => (sameHeadings(prev, found) ? prev : found))
      // 주소에 #소제목이 있으면, 본문이 그려진 뒤 한 번 그리로 간다 (공유받은 주소로 들어온 경우)
      const hash = decodeURIComponent(window.location.hash.slice(1))
      if (!scrolledToHash && hash && found.some((h) => h.id === hash)) {
        scrolledToHash = true
        document.getElementById(hash)?.scrollIntoView()
      }
    }
    update()
    const observer = new MutationObserver(update)
    observer.observe(el, { childList: true, subtree: true })
    return () => observer.disconnect()
  }, [container, source])

  // 읽는 위치: 화면 위쪽 1/3 안에 들어온 마지막 소제목
  useEffect(() => {
    if (headings.length < MIN_HEADINGS) return
    const targets = headings.map((h) => document.getElementById(h.id)).filter((el): el is HTMLElement => el !== null)
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.filter((e) => e.isIntersecting).map((e) => e.target.id)
        if (visible.length > 0) setActive(visible[0])
      },
      { rootMargin: '0px 0px -66% 0px' },
    )
    targets.forEach((el) => observer.observe(el))
    return () => observer.disconnect()
  }, [headings])

  if (headings.length < MIN_HEADINGS) return null
  const top = Math.min(...headings.map((h) => h.level))

  return (
    <aside className="post-toc">
      <details className="post-toc-box" open>
        <summary>목차</summary>
        <ol>
          {headings.map((h) => (
            <li key={h.id} style={{ paddingLeft: `${(h.level - top) * 0.85}rem` }}>
              <a href={`#${encodeURIComponent(h.id)}`} aria-current={active === h.id ? 'location' : undefined}>
                {h.text}
              </a>
            </li>
          ))}
        </ol>
      </details>
    </aside>
  )
}
