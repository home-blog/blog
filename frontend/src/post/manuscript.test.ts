// 원고지 매수·읽는 시간 (specs/007 US3): 마크다운 기호를 뺀 글자 ÷ 200매, ÷ 500분, 올림, 최소 1
import { describe, expect, it } from 'vitest'
import { measureManuscript } from './manuscript'

describe('원고지 매수', () => {
  it('경계: 200자는 1매, 201자는 2매', () => {
    expect(measureManuscript('가'.repeat(200)).sheets).toBe(1)
    expect(measureManuscript('가'.repeat(201)).sheets).toBe(2)
  })

  it('경계: 500자는 1분, 501자는 2분', () => {
    expect(measureManuscript('가'.repeat(500)).readMinutes).toBe(1)
    expect(measureManuscript('가'.repeat(501)).readMinutes).toBe(2)
  })

  it('예외: 빈 본문도 최소 1매·1분', () => {
    expect(measureManuscript('')).toEqual({ chars: 0, sheets: 1, readMinutes: 1 })
  })

  it('마크다운 기호는 세지 않는다', () => {
    expect(measureManuscript('# 제목').chars).toBe(2)
    expect(measureManuscript('**굵게** 와 _기울임_').chars).toBe('굵게 와 기울임'.length)
    expect(measureManuscript('[글결](https://example.com)').chars).toBe(2)
    expect(measureManuscript('![사진](a.png)').chars).toBe(2)
    expect(measureManuscript('- 하나\n- 둘').chars).toBe('하나 둘'.length)
  })

  it('단어 안의 밑줄(snake_case)은 그대로 센다 (서버와 같은 규칙)', () => {
    expect(measureManuscript('snake_case_name').chars).toBe('snake_case_name'.length)
  })

  it('이모지는 한 글자', () => {
    expect(measureManuscript('😀😀').chars).toBe(2)
  })
})
