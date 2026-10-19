// 빨간 펜 교정지 (specs/014 US1): 교정부호 종류, 받아들이기·넘기기, 모두 받아들이기, 고칠 곳 없음
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import RedPenSheet from './RedPenSheet'
import type { Correction, Decision } from './redPenRules'

const BASE = '오늘은 할수있다. 됬다'
const CORRECTIONS: Correction[] = [
  { start: 4, end: 8, kind: 'spacing', original: '할수있다', replacement: '할 수 있다', reason: '의존 명사는 띄어 씁니다' },
  { start: 10, end: 12, kind: 'spelling', original: '됬다', replacement: '됐다', reason: '되었다를 줄이면 됐다' },
]

function renderSheet(decisions: Decision[], handlers: Partial<Record<'onDecide' | 'onAcceptAll' | 'onClose', () => void>> = {}) {
  const onDecide = vi.fn()
  render(
    <RedPenSheet
      base={BASE}
      corrections={CORRECTIONS}
      decisions={decisions}
      remaining={19}
      onDecide={handlers.onDecide ?? onDecide}
      onAcceptAll={handlers.onAcceptAll ?? vi.fn()}
      onClose={handlers.onClose ?? vi.fn()}
    />,
  )
  return onDecide
}

describe('빨간 펜 교정지', () => {
  it('고칠 곳마다 종류와 고친 말을 읽을 수 있는 교정부호가 있다', () => {
    renderSheet(['pending', 'pending'])
    expect(screen.getByRole('button', { name: '띄어쓰기 교정: 할수있다 → 할 수 있다' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '맞춤법 교정: 됬다 → 됐다' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('고칠 곳 2개 남음')
    expect(screen.getByRole('status')).toHaveTextContent('오늘 19번 남음')
  })

  it('처음 고칠 곳의 이유가 보이고 받아들이면 그 번호로 알린다', () => {
    const onDecide = renderSheet(['pending', 'pending'])
    expect(screen.getByText('의존 명사는 띄어 씁니다')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '받아들이기' }))
    expect(onDecide).toHaveBeenCalledWith(0, 'accepted')
  })

  it('다른 교정부호를 누르면 그 이유가 보이고 넘길 수 있다', () => {
    const onDecide = renderSheet(['pending', 'pending'])
    fireEvent.click(screen.getByRole('button', { name: /맞춤법/ }))
    expect(screen.getByText('되었다를 줄이면 됐다')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '넘기기' }))
    expect(onDecide).toHaveBeenCalledWith(1, 'skipped')
  })

  it('받아들인 곳은 고친 말로, 넘긴 곳은 원문으로 보이고 교정부호가 사라진다', () => {
    renderSheet(['accepted', 'skipped'])
    expect(screen.queryByRole('button', { name: /띄어쓰기|맞춤법/ })).toBeNull()
    expect(screen.getByRole('region', { name: '빨간 펜 교정지' })).toHaveTextContent('오늘은 할 수 있다. 됬다')
    expect(screen.getByRole('status')).toHaveTextContent('모두 정했습니다')
    expect(screen.queryByRole('button', { name: '모두 받아들이기' })).toBeNull()
  })

  it('모두 받아들이기', () => {
    const onAcceptAll = vi.fn()
    renderSheet(['pending', 'pending'], { onAcceptAll })
    fireEvent.click(screen.getByRole('button', { name: '모두 받아들이기' }))
    expect(onAcceptAll).toHaveBeenCalled()
  })

  it('고칠 곳이 없으면 그렇게 알린다', () => {
    render(
      <RedPenSheet base="깨끗한 글" corrections={[]} decisions={[]} remaining={5} onDecide={vi.fn()} onAcceptAll={vi.fn()} onClose={vi.fn()} />,
    )
    expect(screen.getByRole('status')).toHaveTextContent('고칠 곳이 없습니다')
  })
})
