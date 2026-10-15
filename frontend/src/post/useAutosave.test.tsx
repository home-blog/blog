import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAutosave } from './useAutosave'

describe('자동 저장 (013 US3-1: 바뀐 것이 있을 때만, 간격마다)', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('바뀐 것이 없으면 저장하지 않는다', () => {
    const save = vi.fn(() => Promise.resolve())
    renderHook(() => useAutosave({ snapshot: 'a', savedSnapshot: 'a', enabled: true, intervalSeconds: 30, save }))
    act(() => {
      vi.advanceTimersByTime(60_000)
    })
    expect(save).not.toHaveBeenCalled()
  })

  it('바뀌면 간격이 지난 뒤 한 번 저장한다', () => {
    const save = vi.fn(() => Promise.resolve())
    renderHook(() => useAutosave({ snapshot: 'b', savedSnapshot: 'a', enabled: true, intervalSeconds: 30, save }))
    act(() => {
      vi.advanceTimersByTime(29_000)
    })
    expect(save).not.toHaveBeenCalled()
    act(() => {
      vi.advanceTimersByTime(1_000)
    })
    expect(save).toHaveBeenCalledTimes(1)
  })

  it('저장 중이거나 꺼져 있으면 저장하지 않는다', () => {
    const save = vi.fn(() => Promise.resolve())
    renderHook(() => useAutosave({ snapshot: 'b', savedSnapshot: 'a', enabled: false, intervalSeconds: 30, save }))
    act(() => {
      vi.advanceTimersByTime(60_000)
    })
    expect(save).not.toHaveBeenCalled()
  })

  it('저장이 끝나기 전에 다음 차례가 와도 겹쳐 보내지 않는다', () => {
    let finish: () => void = () => undefined
    const save = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)))
    renderHook(() => useAutosave({ snapshot: 'b', savedSnapshot: 'a', enabled: true, intervalSeconds: 30, save }))
    act(() => {
      vi.advanceTimersByTime(90_000)
    })
    expect(save).toHaveBeenCalledTimes(1)
    act(() => finish())
  })
})
