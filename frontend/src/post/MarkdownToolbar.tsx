// 글쓰기 서식 도구 막대 (specs/007 US7): 고른 글자를 마크다운 기호로 감싸거나 줄 앞에 기호를 붙인다
// - 브라우저의 "글자 넣기"로 바꿔 되돌리기(Cmd/Ctrl+Z)가 그대로 된다. 안 되는 브라우저는 값을 직접 바꾼다
// - 단축키: Cmd/Ctrl+B 굵게, Cmd/Ctrl+I 기울임 (본문 칸에서)
import { useLayoutEffect, useRef, type ReactNode, type RefObject } from 'react'

interface Props {
  target: RefObject<HTMLTextAreaElement | null>
  disabled?: boolean
  /** 브라우저가 글자 넣기를 못 할 때 새 본문을 넘긴다 */
  onFallback: (value: string) => void
  /** 이미지 올리기 버튼 (본문의 커서 자리에 넣는 일은 그 버튼이 한다) */
  imageButton: ReactNode
}

interface Edit {
  /** 바꿀 구간과 새 글자 */
  from: number
  to: number
  text: string
  /** 바꾼 뒤 고를 구간 (새 글자 안에서의 위치) */
  select: [number, number]
}

function lineRange(value: string, start: number, end: number): [number, number] {
  const from = value.lastIndexOf('\n', start - 1) + 1
  const next = value.indexOf('\n', end)
  return [from, next === -1 ? value.length : next]
}

function wrap(value: string, start: number, end: number, mark: string, placeholder: string): Edit {
  const chosen = value.slice(start, end) || placeholder
  return { from: start, to: end, text: `${mark}${chosen}${mark}`, select: [mark.length, mark.length + chosen.length] }
}

function prefixLines(value: string, start: number, end: number, prefix: (line: string) => string): Edit {
  const [from, to] = lineRange(value, start, end)
  const text = value.slice(from, to).split('\n').map(prefix).join('\n')
  return { from, to, text, select: [text.length, text.length] }
}

function heading(value: string, start: number, end: number, level: number): Edit {
  return prefixLines(value, start, end, (line) => `${'#'.repeat(level)} ${line.replace(/^#{1,6}\s*/, '')}`)
}

function code(value: string, start: number, end: number): Edit {
  const chosen = value.slice(start, end)
  if (chosen.includes('\n') || chosen === '') {
    const body = chosen || '코드'
    const text = `\`\`\`\n${body}\n\`\`\``
    return { from: start, to: end, text, select: [4, 4 + body.length] }
  }
  return wrap(value, start, end, '`', '코드')
}

function link(value: string, start: number, end: number): Edit {
  const label = value.slice(start, end) || '링크 글자'
  const url = 'https://'
  const text = `[${label}](${url})`
  return { from: start, to: end, text, select: [label.length + 3, label.length + 3 + url.length] }
}

type Action = { label: string; title: string; edit: (v: string, s: number, e: number) => Edit; className?: string }

const ACTIONS: (Action | 'sep')[] = [
  { label: 'H1', title: '큰 제목', edit: (v, s, e) => heading(v, s, e, 1), className: 'is-heading' },
  { label: 'H2', title: '제목', edit: (v, s, e) => heading(v, s, e, 2), className: 'is-heading' },
  { label: 'H3', title: '소제목', edit: (v, s, e) => heading(v, s, e, 3), className: 'is-heading' },
  { label: 'H4', title: '작은 제목', edit: (v, s, e) => heading(v, s, e, 4), className: 'is-heading' },
  'sep',
  { label: 'B', title: '굵게 (Ctrl/⌘+B)', edit: (v, s, e) => wrap(v, s, e, '**', '굵은 글'), className: 'is-bold' },
  {
    label: 'I',
    title: '기울임 (Ctrl/⌘+I)',
    edit: (v, s, e) => wrap(v, s, e, '_', '기울인 글'),
    className: 'is-italic',
  },
  { label: 'S', title: '취소선', edit: (v, s, e) => wrap(v, s, e, '~~', '지운 글'), className: 'is-strike' },
  'sep',
  { label: '❝', title: '인용', edit: (v, s, e) => prefixLines(v, s, e, (line) => `> ${line}`) },
  { label: '•', title: '목록', edit: (v, s, e) => prefixLines(v, s, e, (line) => `- ${line}`) },
  { label: '🔗', title: '링크', edit: link },
  { label: '</>', title: '코드', edit: code },
]

export default function MarkdownToolbar({ target, disabled, onFallback, imageButton }: Props) {
  const pendingSelection = useRef<[number, number] | null>(null)

  // 값을 직접 바꾼 경우(글자 넣기를 못 하는 브라우저) 다시 그려진 뒤에 고를 구간을 맞춘다
  useLayoutEffect(() => {
    const area = target.current
    if (area && pendingSelection.current) {
      area.setSelectionRange(...pendingSelection.current)
      pendingSelection.current = null
    }
  })

  function apply(make: Action['edit']) {
    const area = target.current
    if (!area || disabled) return
    const { from, to, text, select } = make(area.value, area.selectionStart, area.selectionEnd)
    const range: [number, number] = [from + select[0], from + select[1]]
    area.focus()
    area.setSelectionRange(from, to)
    const inserted = document.execCommand?.('insertText', false, text)
    if (inserted) {
      area.setSelectionRange(...range)
    } else {
      pendingSelection.current = range
      onFallback(area.value.slice(0, from) + text + area.value.slice(to))
    }
  }

  // 본문 칸의 단축키
  useLayoutEffect(() => {
    const area = target.current
    if (!area) return
    const onKey = (e: KeyboardEvent) => {
      if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey) return
      const key = e.key.toLowerCase()
      if (key === 'b' || key === 'i') {
        e.preventDefault()
        apply(
          key === 'b' ? (v, s, en) => wrap(v, s, en, '**', '굵은 글') : (v, s, en) => wrap(v, s, en, '_', '기울인 글'),
        )
      }
    }
    area.addEventListener('keydown', onKey)
    return () => area.removeEventListener('keydown', onKey)
  })

  return (
    <div className="md-toolbar" role="toolbar" aria-label="서식">
      {ACTIONS.map((action, i) =>
        action === 'sep' ? (
          <span key={`sep-${i}`} className="md-toolbar-sep" aria-hidden="true" />
        ) : (
          <button
            key={action.title}
            type="button"
            className={`md-tool ${action.className ?? ''}`}
            title={action.title}
            aria-label={action.title}
            disabled={disabled}
            // 버튼을 눌러도 본문 칸의 고른 구간이 풀리지 않게 한다
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => apply(action.edit)}
          >
            {action.label}
          </button>
        ),
      )}
      <span className="md-toolbar-sep" aria-hidden="true" />
      {imageButton}
    </div>
  )
}
