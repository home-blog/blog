import { describe, expect, it } from 'vitest'
import { localInputToIso, isoToLocalInput, publishAtLabel, savedAtLabel, scheduleError } from './draftRules'

// 2026-10-15 12:00 한국 시간 = 03:00 세계 시간
const NOW = new Date('2026-10-15T03:00:00Z')

describe('저장 시각 표시 (013 US3-1, 한국 시간)', () => {
  it('"임시 저장됨 14:32"', () => {
    expect(savedAtLabel('2026-10-15T05:32:10Z')).toBe('임시 저장됨 14:32')
  })

  it('자정 넘어도 두 자리', () => {
    expect(savedAtLabel('2026-10-14T15:05:00Z')).toBe('임시 저장됨 00:05')
  })
})

describe('예약 시각 표시', () => {
  it('"예약 10월 20일 09:00"', () => {
    expect(publishAtLabel('2026-10-20T00:00:00Z')).toBe('예약 10월 20일 09:00')
  })
})

describe('예약 칸(datetime-local, 한국 시간) ↔ 세계 시간', () => {
  it('한국 09:00은 세계 00:00', () => {
    expect(localInputToIso('2026-10-20T09:00')).toBe('2026-10-20T00:00:00.000Z')
  })

  it('되돌리면 같은 칸 값', () => {
    expect(isoToLocalInput('2026-10-20T00:00:00Z')).toBe('2026-10-20T09:00')
  })

  it('형식이 틀리면 null', () => {
    expect(localInputToIso('')).toBeNull()
    expect(localInputToIso('내일')).toBeNull()
  })
})

describe('예약 시각 검사 (013 S4-1: 1분 뒤 ~ 30일, 초는 버림)', () => {
  it('비었으면 고르라고 한다', () => {
    expect(scheduleError(null, NOW, 30)).toBe('※ 예약 시각을 골라 주세요')
  })

  it('지금과 지난 시각은 안 된다', () => {
    expect(scheduleError('2026-10-15T03:00:00.000Z', NOW, 30)).toBe('※ 예약 시각은 지금부터 1분 뒤부터 고를 수 있습니다')
    expect(scheduleError('2026-10-15T02:00:00.000Z', NOW, 30)).not.toBeNull()
  })

  it('1분 뒤와 30일 뒤는 된다', () => {
    expect(scheduleError('2026-10-15T03:01:00.000Z', NOW, 30)).toBeNull()
    expect(scheduleError('2026-11-14T03:00:00.000Z', NOW, 30)).toBeNull()
  })

  it('30일 1분 뒤는 안 된다', () => {
    expect(scheduleError('2026-11-14T03:01:00.000Z', NOW, 30)).toBe('※ 예약 시각은 지금부터 30일 안으로 골라 주세요')
  })
})
