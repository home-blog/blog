// 가입·로그인 입력 규칙 (specs/001 FR-005 ~ FR-008). 정상값·경계값·예외값을 함께 본다
import { describe, expect, it } from 'vitest'
import { checkPassword, countChars, isValidEmail, isValidNickname, isValidPassword } from './rules'

describe('닉네임 (한글·영문·숫자 2~10자)', () => {
  it.each(['글결', '재웅123', 'abcdefghij'])('정상: %s', (v) => {
    expect(isValidNickname(v)).toBe(true)
  })

  it('경계: 2자와 10자는 되고 1자와 11자는 안 된다', () => {
    expect(isValidNickname('가나')).toBe(true)
    expect(isValidNickname('가'.repeat(10))).toBe(true)
    expect(isValidNickname('가')).toBe(false)
    expect(isValidNickname('가'.repeat(11))).toBe(false)
  })

  it.each(['글 결', '글결!', '', '   ', 'ㄱㄴ'])('예외: %j', (v) => {
    expect(isValidNickname(v)).toBe(false)
  })

  it('앞뒤 공백은 지우고 본다', () => {
    expect(isValidNickname('  글결  ')).toBe(true)
  })
})

describe('이메일', () => {
  it('정상: 보통 주소', () => {
    expect(isValidEmail('writer@example.com')).toBe(true)
  })

  it.each(['writer', 'writer@', '@example.com', 'writer@example', 'a b@example.com'])('예외: %s', (v) => {
    expect(isValidEmail(v)).toBe(false)
  })

  it('경계: 255자까지는 되고 256자는 안 된다', () => {
    const domain = '@example.com'
    expect(isValidEmail('a'.repeat(255 - domain.length) + domain)).toBe(true)
    expect(isValidEmail('a'.repeat(256 - domain.length) + domain)).toBe(false)
  })
})

describe('비밀번호 (영문·숫자·특수문자를 모두 넣어 8~20자)', () => {
  it('정상', () => {
    expect(isValidPassword('abc123!@')).toBe(true)
  })

  it('경계: 8자와 20자는 되고 7자와 21자는 안 된다', () => {
    expect(isValidPassword('ab1!' + 'a'.repeat(4))).toBe(true)
    expect(isValidPassword('ab1!' + 'a'.repeat(16))).toBe(true)
    expect(isValidPassword('ab1!' + 'a'.repeat(3))).toBe(false)
    expect(isValidPassword('ab1!' + 'a'.repeat(17))).toBe(false)
  })

  it.each([
    ['영문이 없다', '12345678!'],
    ['숫자가 없다', 'abcdefgh!'],
    ['특수문자가 없다', 'abcd1234'],
    ['허용하지 않는 기호가 있다', 'abcd123!~'],
    ['공백이 있다', 'abcd 123!'],
  ])('예외: %s', (_, v) => {
    expect(isValidPassword(v)).toBe(false)
  })

  it('규칙마다 충족 여부를 따로 알려 준다', () => {
    const checks = checkPassword('abcdefgh')
    expect(checks.map((c) => c.met)).toEqual([true, true, false, false, true])
  })

  it('빈 값은 "공백이나 다른 기호 없이"도 충족하지 않은 것으로 본다', () => {
    expect(checkPassword('').every((c) => !c.met)).toBe(true)
  })
})

describe('글자 수', () => {
  it('이모지 하나를 한 글자로 센다 (서버와 같은 기준)', () => {
    expect(countChars('글결😀')).toBe(3)
  })
})
