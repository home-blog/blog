// 누를 때마다 먹 한 방울 (2026-10-20): 단추·링크를 누른 자리에만 생기고, 저절로 사라지고, 움직임 줄이기면 생기지 않는다
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import InkTouch from './InkTouch'

function drops() {
  return document.querySelectorAll('.ink-touch')
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  document.querySelectorAll('.ink-touch').forEach((drop) => drop.remove())
})

describe('먹 한 방울', () => {
  it('단추를 누른 자리에 생기고 잠시 뒤 사라진다. 글자나 끈 단추에는 생기지 않는다', () => {
    vi.useFakeTimers()
    render(
      <>
        <InkTouch />
        <button type="button">누르기</button>
        <button type="button" disabled>
          꺼짐
        </button>
        <p>그냥 글</p>
      </>,
    )
    fireEvent.pointerDown(screen.getByText('그냥 글'), { button: 0, clientX: 5, clientY: 5 })
    fireEvent.pointerDown(screen.getByText('꺼짐'), { button: 0, clientX: 5, clientY: 5 })
    expect(drops()).toHaveLength(0)

    fireEvent.pointerDown(screen.getByText('누르기'), { button: 0, clientX: 40, clientY: 70 })
    expect(drops()).toHaveLength(1)
    const drop = drops()[0] as HTMLElement
    expect(drop.style.left).toBe('40px')
    expect(drop.style.top).toBe('70px')
    expect(drop).toHaveAttribute('aria-hidden', 'true')
    // 오른쪽 단추는 세지 않는다
    fireEvent.pointerDown(screen.getByText('누르기'), { button: 2, clientX: 40, clientY: 70 })
    expect(drops()).toHaveLength(1)

    act(() => vi.advanceTimersByTime(1000))
    expect(drops()).toHaveLength(0)
  })

  it('움직임 줄이기 설정이면 생기지 않는다', () => {
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: query.includes('reduce'), media: query }))
    render(
      <>
        <InkTouch />
        <a href="/board">글마당</a>
      </>,
    )
    fireEvent.pointerDown(screen.getByText('글마당'), { button: 0, clientX: 1, clientY: 1 })
    expect(drops()).toHaveLength(0)
  })
})
