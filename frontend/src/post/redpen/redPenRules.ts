// 빨간 펜 교정지의 규칙 (specs/014 US1): 서버가 준 위치는 교정을 받은 본문(base)의 위치다.
// 받아들일 때마다 base에서 처음부터 다시 만들어, 앞에서 글자 수가 늘거나 줄어도 뒤의 위치가 어긋나지 않는다 (S1-7)

export type CorrectionKind = 'spacing' | 'spelling' | 'style'

export interface Correction {
  start: number
  end: number
  kind: CorrectionKind
  original: string
  replacement: string
  reason: string
}

export type Decision = 'pending' | 'accepted' | 'skipped'

export type Segment =
  | { type: 'text'; text: string }
  | { type: 'mark'; index: number; correction: Correction; decision: Decision }

export const KIND_LABEL: Record<CorrectionKind, string> = {
  spacing: '띄어쓰기',
  spelling: '맞춤법',
  style: '다듬기',
}

/** 받아들인 곳만 고친 본문. corrections는 앞에서부터, 겹치지 않는다(서버가 맞춘다). */
export function applyDecisions(base: string, corrections: Correction[], decisions: Decision[]): string {
  let out = ''
  let at = 0
  corrections.forEach((c, i) => {
    out += base.slice(at, c.start)
    out += decisions[i] === 'accepted' ? c.replacement : c.original
    at = c.end
  })
  return out + base.slice(at)
}

/** 교정지에 그릴 조각: 본문 글자와 고칠 곳이 번갈아 온다. */
export function segmentsOf(base: string, corrections: Correction[], decisions: Decision[]): Segment[] {
  const segments: Segment[] = []
  let at = 0
  corrections.forEach((c, i) => {
    if (c.start > at) segments.push({ type: 'text', text: base.slice(at, c.start) })
    segments.push({ type: 'mark', index: i, correction: c, decision: decisions[i] ?? 'pending' })
    at = c.end
  })
  if (at < base.length) segments.push({ type: 'text', text: base.slice(at) })
  return segments
}

export function pendingCount(decisions: Decision[]): number {
  return decisions.filter((d) => d === 'pending').length
}

/** 띄어쓰기 교정부호: 띄어야 하면 ∨(띄움표), 붙여야 하면 ⁀(붙임표). */
export function spacingMark(c: Correction): '∨' | '⁀' {
  const spaces = (s: string) => (s.match(/\s/g) ?? []).length
  return spaces(c.replacement) < spaces(c.original) ? '⁀' : '∨'
}

/** 교정지를 연 뒤 쓰는 사람이 본문을 따로 고쳤나. 고쳤으면 위치가 맞지 않으므로 교정지를 닫는다. */
export function isStale(current: string, base: string, corrections: Correction[], decisions: Decision[]): boolean {
  return current !== applyDecisions(base, corrections, decisions)
}
