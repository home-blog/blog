// 글마당 쓰기 규칙 (specs/017 S2-1). 서버 board.* 설정과 같은 숫자. 화면에서 먼저 보고, 서버 검사가 마지막 방어선
import { countChars } from '../auth/rules'
import type { BoardInput } from './boardApi'

export const BOARD_TITLE_MAX = 100
export const BOARD_CONTENT_MAX = 10_000
export const BOARD_FIELDS = ['topic', 'kind', 'title', 'content'] as const
export type BoardErrors = Partial<Record<(typeof BOARD_FIELDS)[number], string>>

/** 화면에서 먼저 보는 규칙 (서버 검사가 마지막 방어선) */
export function checkBoard(input: BoardInput): BoardErrors {
  const errors: BoardErrors = {}
  if (!input.topic) errors.topic = '※ 주제를 골라 주세요'
  const title = input.title.trim()
  if (!title) errors.title = '제목을 입력해 주세요'
  else if (countChars(title) > BOARD_TITLE_MAX) errors.title = `※ 제목은 ${BOARD_TITLE_MAX}자 이하로 입력해 주세요`
  if (!input.content.trim()) errors.content = '본문을 입력해 주세요'
  else if (countChars(input.content) > BOARD_CONTENT_MAX)
    errors.content = `※ 본문은 ${BOARD_CONTENT_MAX.toLocaleString('ko-KR')}자 이하로 입력해 주세요`
  return errors
}
