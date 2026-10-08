// 글 목록·검색의 화면 규칙과 문구 (specs/004 T011). 서버 설정(application.yml의 explore.*)과
// docs/2-요구사항/상세/04-탐색.md `안내 문구` 표와 같게 둔다. 서버 검사가 마지막 방어선이다
export const MESSAGES = {
  emptyList: '글이 없습니다',
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
