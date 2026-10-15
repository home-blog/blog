// 공지사항 입력 규칙과 문구 (specs/012 contracts 4). 숫자는 서버 설정(notice.*)과 같다. 마지막 판단은 서버가 한다
import { countChars } from '../auth/rules'
import type { NoticeInput, NoticeKind } from './noticeApi'

export const NOTICE_TITLE_MAX = 100
export const NOTICE_CONTENT_MAX = 10_000

/** 0.6.0 모양, 자리마다 1~4자리 (research D-6, 배포 작업이 받는 번호와 같다) */
const RELEASE_VERSION = /^\d{1,4}\.\d{1,4}\.\d{1,4}$/

export const KIND_LABEL: Record<NoticeKind, string> = { update: '업데이트', notice: '공지' }

export const noticeMessages = {
  titleRequired: '제목을 입력해 주세요',
  titleTooLong: `※ 제목은 ${NOTICE_TITLE_MAX}자 이하로 입력해 주세요`,
  contentRequired: '본문을 입력해 주세요',
  contentTooLong: `※ 본문은 ${NOTICE_CONTENT_MAX.toLocaleString('ko-KR')}자 이하로 입력해 주세요`,
  versionRequired: '※ 업데이트에는 릴리스 번호를 적어 주세요 (예: 0.6.0)',
  versionInvalid: '※ 릴리스 번호는 0.6.0 모양으로 적어 주세요',
  empty: '아직 공지사항이 없습니다',
  forbidden: '※ 권한이 없습니다',
} as const

export type NoticeErrors = Partial<Record<'title' | 'content' | 'releaseVersion' | 'kind', string>>

/** 탭 값: 모르는 값이면 전체(null) (S1-5, 서버와 같다) */
export function kindFrom(value: string | null): NoticeKind | null {
  return value === 'update' || value === 'notice' ? value : null
}

export function isReleaseVersion(value: string): boolean {
  return RELEASE_VERSION.test(value)
}

/** 화면에서 먼저 보는 검사. 서버로 보낼 값도 함께 다듬는다 (공지는 번호를 비운다) */
export function checkNotice(input: NoticeInput): { errors: NoticeErrors; value: NoticeInput } {
  const errors: NoticeErrors = {}
  const title = input.title.trim()
  const titleLength = countChars(title)
  if (titleLength === 0) errors.title = noticeMessages.titleRequired
  else if (titleLength > NOTICE_TITLE_MAX) errors.title = noticeMessages.titleTooLong

  const content = input.content.replace(/\r\n/g, '\n')
  if (content.trim() === '') errors.content = noticeMessages.contentRequired
  else if (countChars(content) > NOTICE_CONTENT_MAX) errors.content = noticeMessages.contentTooLong

  let releaseVersion: string | null = null
  if (input.kind === 'update') {
    releaseVersion = (input.releaseVersion ?? '').trim()
    if (releaseVersion === '') errors.releaseVersion = noticeMessages.versionRequired
    else if (!isReleaseVersion(releaseVersion)) errors.releaseVersion = noticeMessages.versionInvalid
  }
  return { errors, value: { ...input, title, content, releaseVersion } }
}
