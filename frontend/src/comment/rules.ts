// 댓글 입력 규칙과 문구 (specs/005 contracts 2, FR-002). 숫자는 서버 설정(community.comment.*)과 같다.
// 최종 판단은 서버가 한다. 화면 검사는 보조다
import { countChars } from '../auth/rules'

export const COMMENT_MAX_LENGTH = 500

export const commentMessages = {
  empty: '※ 댓글 내용을 입력해 주세요',
  tooLong: `※ 댓글은 ${COMMENT_MAX_LENGTH}자 이하로 입력해 주세요`,
  loginRequired: '로그인한 회원만 댓글을 쓸 수 있습니다',
  withdrawn: '탈퇴한 사용자',
  failed: '※ 잠시 뒤 다시 시도해 주세요',
} as const

/** 서버처럼 줄바꿈을 \n으로 맞추고 앞뒤 공백을 지운 뒤 센다 (이모지 하나 = 한 글자) */
export function commentLength(body: string): number {
  return countChars(body.replace(/\r\n?/g, '\n').trim())
}
