// 글 목록·검색 규칙 (specs/004 D-1, D-2)
import { describe, expect, it } from 'vitest'
import { checkKeyword, KEYWORD_MAX, limitKeyword, MESSAGES, pageFrom } from './rules'

describe('주소의 page 값', () => {
  it.each([
    ['정상', '3', 3],
    ['경계: 1', '1', 1],
    ['예외: 0은 1로', '0', 1],
    ['예외: 음수는 1로', '-2', 1],
    ['예외: 숫자가 아니면 1로', 'abc', 1],
    ['예외: 소수는 1로', '1.5', 1],
    ['예외: 없으면 1로', null, 1],
  ])('%s', (_, value, expected) => {
    expect(pageFrom(value)).toBe(expected)
  })

  it('아주 큰 숫자는 아주 큰 번호로 보내 서버가 마지막 페이지로 바꾸게 한다', () => {
    expect(pageFrom('99999999999999999999')).toBe(Number.MAX_SAFE_INTEGER)
  })
})

describe('검색어 (앞뒤 공백을 지우고 2~50자)', () => {
  it('정상', () => {
    expect(checkKeyword('여행')).toBeNull()
  })

  it('경계: 2자와 50자는 되고 1자와 51자는 안 된다', () => {
    expect(checkKeyword('가나')).toBeNull()
    expect(checkKeyword('가'.repeat(KEYWORD_MAX))).toBeNull()
    expect(checkKeyword('가')).toBe(MESSAGES.keywordTooShort)
    expect(checkKeyword('가'.repeat(KEYWORD_MAX + 1))).toBe(MESSAGES.keywordTooLong)
  })

  it('앞뒤 공백(전각 공백 포함)은 세지 않는다', () => {
    expect(checkKeyword('  가  ')).toBe(MESSAGES.keywordTooShort)
    expect(checkKeyword('　여행　')).toBeNull()
  })

  it('이모지는 한 글자로 센다', () => {
    expect(checkKeyword('😀')).toBe(MESSAGES.keywordTooShort)
    expect(checkKeyword('😀😀')).toBeNull()
  })
})

describe('검색어 입력을 50자로 자르기', () => {
  it('50자 이하는 그대로 둔다', () => {
    expect(limitKeyword('가'.repeat(KEYWORD_MAX))).toBe('가'.repeat(KEYWORD_MAX))
  })

  it('넘치면 앞 공백은 두고 글자만 50자로 자른다', () => {
    expect(limitKeyword('  ' + '가'.repeat(KEYWORD_MAX + 5))).toBe('  ' + '가'.repeat(KEYWORD_MAX))
  })

  it('이모지를 반으로 자르지 않는다', () => {
    const cut = limitKeyword('😀'.repeat(KEYWORD_MAX + 1))
    expect(Array.from(cut)).toHaveLength(KEYWORD_MAX)
    expect(cut).toBe('😀'.repeat(KEYWORD_MAX))
  })
})
