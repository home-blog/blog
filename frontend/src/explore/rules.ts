// 글 목록·검색의 화면 규칙과 문구 (specs/004 T011). 서버 설정(application.yml의 explore.*)과
// docs/2-요구사항/상세/04-탐색.md `안내 문구` 표와 같게 둔다. 서버 검사가 마지막 방어선이다
/** 검색어 길이 (explore.search.keyword-min-length, keyword-max-length). 앞뒤 공백을 지운 뒤 코드 포인트로 센다 */
export const KEYWORD_MIN = 2
export const KEYWORD_MAX = 50

export const MESSAGES = {
  emptyList: '글이 없습니다',
  emptySearch: '검색 결과가 없습니다',
  keywordTooShort: `검색어를 ${KEYWORD_MIN}자 이상 입력해 주세요`,
  keywordTooLong: `검색어는 ${KEYWORD_MAX}자까지 입력할 수 있습니다`,
  /** 내 블로그에 글이 없을 때 (CF-10-8 본문. 상세/04 `안내 문구` 표에 아직 없다) */
  firstPost: '첫 글을 써 보세요',
} as const

/**
 * 주소의 ?page= 값을 읽는다. 1보다 작거나 숫자가 아니면 1, 아주 큰 숫자는 아주 큰 번호로 보내
 * 서버가 마지막 페이지로 바꾸게 한다 (서버와 같은 규칙, 004 D-2)
 */
export function pageFrom(value: string | null): number {
  if (!value || !/^\d+$/.test(value)) return 1
  const page = Number(value)
  if (!Number.isSafeInteger(page)) return Number.MAX_SAFE_INTEGER
  return page >= 1 ? page : 1
}

const dateFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** 작성일: 한국 시간의 날짜 (가안) */
export function formatDate(iso: string): string {
  return dateFormat.format(new Date(iso))
}

/** 글자 수 (코드 포인트, 이모지 하나 = 한 글자). 서버와 같은 방식 */
export function lengthOf(text: string): number {
  return Array.from(text).length
}

/** 앞뒤 공백(전각 공백 포함)을 지운 검색어. 짧으면 안내 문구 */
export function checkKeyword(raw: string): string | null {
  const length = lengthOf(raw.trim())
  if (length < KEYWORD_MIN) return MESSAGES.keywordTooShort
  if (length > KEYWORD_MAX) return MESSAGES.keywordTooLong
  return null
}

/** 50자를 넘는 입력은 받지 않는다 (004 D-1: A). 앞뒤 공백은 세지 않으므로 지운 뒤 50자까지 남긴다 */
export function limitKeyword(raw: string): string {
  if (lengthOf(raw.trim()) <= KEYWORD_MAX) return raw
  const leading = raw.length - raw.trimStart().length
  return raw.slice(0, leading) + Array.from(raw.trimStart()).slice(0, KEYWORD_MAX).join('')
}
