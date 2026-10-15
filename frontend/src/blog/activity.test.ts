import { describe, expect, it } from 'vitest'
import { activityLevel, activityWeeks, dayLabel } from './activity'

describe('잔디 진하기 (013 D-14: 0 / 1 / 2 / 3~4 / 5 이상)', () => {
  it.each([
    [0, 0],
    [1, 1],
    [2, 2],
    [3, 3],
    [4, 3],
    [5, 4],
    [12, 4],
  ])('글 %i개는 %i단계', (count, level) => {
    expect(activityLevel(count)).toBe(level)
  })

  it('음수·숫자가 아닌 값은 0단계', () => {
    expect(activityLevel(-1)).toBe(0)
    expect(activityLevel(Number.NaN)).toBe(0)
  })
})

describe('칸 설명 글자', () => {
  it('글이 있는 날', () => {
    expect(dayLabel({ date: '2026-10-12', postCount: 2, sheets: 18 })).toBe('10월 12일 · 글 2개 · 원고지 18매')
  })

  it('글이 없는 날', () => {
    expect(dayLabel({ date: '2026-01-05', postCount: 0, sheets: 0 })).toBe('1월 5일 · 글 없음')
  })
})

describe('주 단위로 칸 놓기 (일요일 시작)', () => {
  it('첫 주는 시작 요일 앞을 비우고, 모든 날을 한 번씩 담는다', () => {
    // 2026-10-15는 목요일
    const weeks = activityWeeks('2026-10-11', '2026-10-15', [{ date: '2026-10-12', postCount: 2, sheets: 18 }])
    expect(weeks).toHaveLength(1)
    expect(weeks[0]).toHaveLength(7)
    expect(weeks[0][0]?.date).toBe('2026-10-11') // 일요일
    expect(weeks[0][1]).toEqual({ date: '2026-10-12', postCount: 2, sheets: 18 })
    expect(weeks[0][4]?.date).toBe('2026-10-15')
    expect(weeks[0][5]).toBeNull() // 오늘 뒤는 빈 칸
    expect(weeks[0][6]).toBeNull()
  })

  it('수요일에 시작하면 그 주 일~화는 빈 칸', () => {
    const weeks = activityWeeks('2026-10-14', '2026-10-20', [])
    expect(weeks).toHaveLength(2)
    expect(weeks[0].slice(0, 3)).toEqual([null, null, null])
    expect(weeks[0][3]).toEqual({ date: '2026-10-14', postCount: 0, sheets: 0 })
    expect(weeks[1][0]?.date).toBe('2026-10-18')
    expect(weeks[1][2]?.date).toBe('2026-10-20')
  })

  it('365일이면 53주 안에 모든 날이 들어간다', () => {
    const weeks = activityWeeks('2025-10-16', '2026-10-15', [])
    const dates = weeks.flat().filter((day) => day !== null)
    expect(dates).toHaveLength(365)
    expect(weeks.length).toBeLessThanOrEqual(53)
    expect(dates[0]?.date).toBe('2025-10-16')
    expect(dates[364]?.date).toBe('2026-10-15')
  })
})
