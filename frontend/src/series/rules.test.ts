import { describe, expect, it } from 'vitest'
import { SERIES_DESCRIPTION_MAX, SERIES_NAME_MAX, moveItem, seriesInputError } from './rules'

describe('연재 이름·소개 (013 S2-1: 이름 1~30자, 소개 0~200자, 앞뒤 공백 제외, 이모지 한 글자)', () => {
  it('1자와 30자는 된다', () => {
    expect(seriesInputError('가', '')).toBeNull()
    expect(seriesInputError('나'.repeat(SERIES_NAME_MAX), '')).toBeNull()
    expect(seriesInputError('😀'.repeat(SERIES_NAME_MAX), '')).toBeNull()
  })

  it('비었거나 공백만이면 거절', () => {
    expect(seriesInputError('', '')).toBe('※ 연재 이름을 입력해 주세요')
    expect(seriesInputError('   ', '')).toBe('※ 연재 이름을 입력해 주세요')
  })

  it('31자는 거절', () => {
    expect(seriesInputError('다'.repeat(SERIES_NAME_MAX + 1), '')).toBe('※ 연재 이름은 30자까지 쓸 수 있습니다')
  })

  it('소개 200자는 되고 201자는 거절', () => {
    expect(seriesInputError('연재', '라'.repeat(SERIES_DESCRIPTION_MAX))).toBeNull()
    expect(seriesInputError('연재', '라'.repeat(SERIES_DESCRIPTION_MAX + 1))).toBe('※ 연재 소개는 200자까지 쓸 수 있습니다')
  })
})

describe('순서 옮기기', () => {
  it('한 칸 위·아래로 바꾼다', () => {
    expect(moveItem([1, 2, 3], 1, -1)).toEqual([2, 1, 3])
    expect(moveItem([1, 2, 3], 1, 1)).toEqual([1, 3, 2])
  })

  it('끝을 넘으면 그대로 (새 배열)', () => {
    const list = [1, 2, 3]
    expect(moveItem(list, 0, -1)).toEqual([1, 2, 3])
    expect(moveItem(list, 2, 1)).toEqual([1, 2, 3])
    expect(moveItem(list, 0, 1)).not.toBe(list)
  })
})
