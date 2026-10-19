import { describe, expect, it } from 'vitest'
import {
  applyDecisions,
  isStale,
  KIND_LABEL,
  pendingCount,
  segmentsOf,
  spacingMark,
  type Correction,
  type Decision,
} from './redPenRules'

// 서버가 준 위치는 교정을 받은 본문(base)의 위치다 (specs/014 contracts 1)
const BASE = '오늘은 할수있다. 됬다'
const CORRECTIONS: Correction[] = [
  { start: 4, end: 8, kind: 'spacing', original: '할수있다', replacement: '할 수 있다', reason: '띄어 씁니다' },
  { start: 10, end: 12, kind: 'spelling', original: '됬다', replacement: '됐다', reason: '맞춤법' },
]

describe('받아들이기 (S1-7)', () => {
  it('받아들인 곳만 바꾼다', () => {
    expect(applyDecisions(BASE, CORRECTIONS, ['accepted', 'skipped'])).toBe('오늘은 할 수 있다. 됬다')
  })

  it('앞을 받아들여 글자 수가 늘어도 뒤의 위치가 어긋나지 않는다', () => {
    expect(applyDecisions(BASE, CORRECTIONS, ['accepted', 'accepted'])).toBe('오늘은 할 수 있다. 됐다')
  })

  it('뒤만 받아들여도 된다', () => {
    expect(applyDecisions(BASE, CORRECTIONS, ['pending', 'accepted'])).toBe('오늘은 할수있다. 됐다')
  })

  it('하나도 받아들이지 않으면 그대로', () => {
    expect(applyDecisions(BASE, CORRECTIONS, ['skipped', 'pending'])).toBe(BASE)
  })
})

describe('교정지 조각', () => {
  it('글자와 고칠 곳을 차례로 나눈다', () => {
    const decisions: Decision[] = ['pending', 'accepted']
    const segments = segmentsOf(BASE, CORRECTIONS, decisions)
    expect(segments.map((s) => (s.type === 'text' ? s.text : `[${s.index}]`))).toEqual([
      '오늘은 ',
      '[0]',
      '. ',
      '[1]',
    ])
    expect(segments[3]).toMatchObject({ type: 'mark', decision: 'accepted' })
  })

  it('고칠 곳이 없으면 본문 한 조각', () => {
    expect(segmentsOf('글', [], [])).toEqual([{ type: 'text', text: '글' }])
  })
})

describe('남은 수와 표시', () => {
  it('아직 정하지 않은 것만 센다', () => {
    expect(pendingCount(['pending', 'accepted', 'skipped', 'pending'])).toBe(2)
  })

  it('띄어야 하면 ∨, 붙여야 하면 ⁀', () => {
    expect(spacingMark({ ...CORRECTIONS[0] })).toBe('∨')
    expect(spacingMark({ ...CORRECTIONS[0], original: '할 수 있 다', replacement: '할 수 있다' })).toBe('⁀')
  })

  it('종류 이름', () => {
    expect(KIND_LABEL.spacing).toBe('띄어쓰기')
    expect(KIND_LABEL.spelling).toBe('맞춤법')
    expect(KIND_LABEL.style).toBe('다듬기')
  })
})

describe('본문이 바뀌었나', () => {
  it('받아들인 결과와 지금 본문이 같으면 그대로', () => {
    const decisions: Decision[] = ['accepted', 'pending']
    expect(isStale('오늘은 할 수 있다. 됬다', BASE, CORRECTIONS, decisions)).toBe(false)
  })

  it('쓰는 사람이 따로 고쳤으면 교정지를 닫아야 한다', () => {
    expect(isStale(BASE + '!', BASE, CORRECTIONS, ['pending', 'pending'])).toBe(true)
  })
})
