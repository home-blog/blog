// 글 입력 규칙과 문구 (specs/003 contracts 10, FR-010, FR-011, FR-016, D-2).
// 최종 판단은 서버가 한다. 글자 수 최대값은 글쓰기 화면을 열 때 서버가 알려 준 값(limits)을 쓴다
import { countChars } from '../auth/rules'

export const postMessages = {
  titleRequired: '제목을 입력해 주세요',
  titleTooLong: (max: number) => `※ 제목은 ${max}자 이하로 입력해 주세요`,
  contentRequired: '본문을 입력해 주세요',
  contentTooLong: (max: number) => `※ 본문은 ${max.toLocaleString('ko-KR')}자 이하로 입력해 주세요`,
  categoryRequired: '※ 분류를 골라 주세요',
  topicRequired: '주제를 골라 주세요',
  failed: '※ 잠시 뒤 다시 시도해 주세요',
} as const

/** 제목은 앞뒤 공백을 지운 뒤 센다 (서버와 같게) */
export function titleLength(title: string): number {
  return countChars(title.trim())
}

/**
 * 본문은 저장하는 원문 그대로 센다: 마크다운 기호와 이미지 주소도 글자다 (D-2).
 * 줄바꿈은 서버처럼 \r\n을 한 글자로 맞춘 뒤 센다
 */
export function contentLength(content: string): number {
  return countChars(content.replace(/\r\n/g, '\n'))
}

/** 공백·줄바꿈만 있으면 비어 있는 것으로 본다 (D-2) */
export function isBlank(value: string): boolean {
  return value.trim() === ''
}
