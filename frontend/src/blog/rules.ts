// 블로그·분류 입력 규칙 (specs/003 FR-004, FR-005, FR-036). 숫자는 서버 application.yml의 blog.*, category.* 기본값과 같게 둔다
// 최종 판단은 서버가 한다
import { countChars } from '../auth/rules'

export const CATEGORY_NAME_MAX = 20
export const BLOG_NAME_MAX = 30
export const BLOG_INTRO_MAX = 200

/** 앞뒤 공백을 지운 뒤 센다 (서버와 같게) */
export function categoryNameLength(name: string): number {
  return countChars(name.trim())
}

export function blogNameLength(name: string): number {
  return countChars(name.trim())
}
