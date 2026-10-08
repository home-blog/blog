// 태그 규칙과 문구 (specs/005 contracts 8, FR-013, FR-014, D-10). 숫자는 서버 설정(community.tag.*)과 같다.
// 화면 검사는 보조다. 서버가 저장할 때 같은 규칙으로 다시 본다
import { countChars } from '../auth/rules'

export const TAG_MAX_PER_POST = 5
export const TAG_MAX_LENGTH = 15

export const tagMessages = {
  tooMany: `※ 태그는 ${TAG_MAX_PER_POST}개까지 붙일 수 있습니다`,
  invalid: `※ 태그는 공백과 쉼표 없이 1~${TAG_MAX_LENGTH}자로 입력해 주세요`,
  duplicated: '※ 이미 붙인 태그입니다',
  lowercase: '태그는 소문자로 보입니다',
} as const

/** 서버와 같게 다듬는다: 앞뒤 공백 → 앞의 # 지우기 → 1~15자, 공백·쉼표 없음 → 소문자. 맞지 않으면 null */
export function normalizeTag(raw: string): string | null {
  const name = raw.trim().replace(/^#+/, '')
  const length = countChars(name)
  if (length < 1 || length > TAG_MAX_LENGTH || /[\s,]/u.test(name)) return null
  return name.toLowerCase()
}
