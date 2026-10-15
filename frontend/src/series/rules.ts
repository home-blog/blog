// 연재 입력 규칙 (specs/013 S2-1). 숫자는 서버 application.yml의 series.* 기본값과 같게 둔다. 최종 판단은 서버가 한다
import { countChars } from '../auth/rules'

export const SERIES_NAME_MAX = 30
export const SERIES_DESCRIPTION_MAX = 200

/** 앞뒤 공백을 지운 뒤 센다 (서버와 같게). 문제가 없으면 null */
export function seriesInputError(name: string, description: string): string | null {
  const nameLength = countChars(name.trim())
  if (nameLength === 0) return '※ 연재 이름을 입력해 주세요'
  if (nameLength > SERIES_NAME_MAX) return `※ 연재 이름은 ${SERIES_NAME_MAX}자까지 쓸 수 있습니다`
  if (countChars(description.trim()) > SERIES_DESCRIPTION_MAX) return `※ 연재 소개는 ${SERIES_DESCRIPTION_MAX}자까지 쓸 수 있습니다`
  return null
}

/** index의 항목을 delta(-1 위, +1 아래)만큼 옮긴 새 배열. 끝을 넘으면 그대로 */
export function moveItem<T>(list: T[], index: number, delta: number): T[] {
  const next = [...list]
  const target = index + delta
  if (target < 0 || target >= next.length) return next
  ;[next[index], next[target]] = [next[target], next[index]]
  return next
}
