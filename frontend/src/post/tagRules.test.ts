// 태그 다듬기 (specs/005 FR-013, D-10): 앞뒤 공백 → 앞의 # → 소문자 → 1~15자, 공백·쉼표 없음
import { describe, expect, it } from 'vitest'
import { normalizeTag, TAG_MAX_LENGTH } from './tagRules'

describe('태그', () => {
  it('정상: 앞의 #과 공백을 지우고 소문자로', () => {
    expect(normalizeTag('  #Spring ')).toBe('spring')
    expect(normalizeTag('##여행')).toBe('여행')
  })

  it('경계: 15자는 되고 16자는 안 된다', () => {
    expect(normalizeTag('가'.repeat(TAG_MAX_LENGTH))).toBe('가'.repeat(TAG_MAX_LENGTH))
    expect(normalizeTag('가'.repeat(TAG_MAX_LENGTH + 1))).toBeNull()
  })

  it.each(['', '#', '   ', '두 단어', '쉼표,있음'])('예외: %j', (v) => {
    expect(normalizeTag(v)).toBeNull()
  })
})
