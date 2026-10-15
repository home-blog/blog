// 공지사항 입력 규칙 (specs/012 S4-2 ~ S4-4): 정상·경계·예외값
import { describe, expect, it } from 'vitest'
import type { NoticeInput } from './noticeApi'
import { checkNotice, isReleaseVersion, kindFrom, NOTICE_CONTENT_MAX, NOTICE_TITLE_MAX, noticeMessages } from './rules'

const base: NoticeInput = { kind: 'notice', title: '점검 안내', content: '본문', releaseVersion: null, pinned: false }

describe('릴리스 번호 모양', () => {
  it.each(['0.6.0', '0.10.12', '1234.0.0'])('정상: %s', (v) => expect(isReleaseVersion(v)).toBe(true))
  it.each(['v0.6.0', '0.6', '0.6.0.1', 'abc', '12345.0.0', ' 0.6.0', ''])('예외: %j', (v) =>
    expect(isReleaseVersion(v)).toBe(false),
  )
})

describe('입력 검사', () => {
  it('정상: 공지는 번호를 비워 보낸다', () => {
    const { errors, value } = checkNotice({ ...base, releaseVersion: '0.6.0' })
    expect(errors).toEqual({})
    expect(value.releaseVersion).toBeNull()
  })

  it('정상: 업데이트는 번호 앞뒤 공백을 지워 보낸다', () => {
    const { errors, value } = checkNotice({ ...base, kind: 'update', releaseVersion: ' 0.6.0 ' })
    expect(errors).toEqual({})
    expect(value.releaseVersion).toBe('0.6.0')
  })

  it('경계: 제목 100자는 되고 101자는 안 된다 (이모지는 한 글자)', () => {
    expect(checkNotice({ ...base, title: '가'.repeat(NOTICE_TITLE_MAX - 1) + '😀' }).errors.title).toBeUndefined()
    expect(checkNotice({ ...base, title: '가'.repeat(NOTICE_TITLE_MAX + 1) }).errors.title).toBe(
      noticeMessages.titleTooLong,
    )
  })

  it('경계: 본문 10,000자는 되고 10,001자는 안 된다', () => {
    expect(checkNotice({ ...base, content: '가'.repeat(NOTICE_CONTENT_MAX) }).errors.content).toBeUndefined()
    expect(checkNotice({ ...base, content: '가'.repeat(NOTICE_CONTENT_MAX + 1) }).errors.content).toBe(
      noticeMessages.contentTooLong,
    )
  })

  it('예외: 공백만 있는 제목·본문', () => {
    const { errors } = checkNotice({ ...base, title: '   ', content: '\n  ' })
    expect(errors.title).toBe(noticeMessages.titleRequired)
    expect(errors.content).toBe(noticeMessages.contentRequired)
  })

  it('예외: 업데이트인데 번호가 없거나 모양이 틀리다', () => {
    expect(checkNotice({ ...base, kind: 'update', releaseVersion: '' }).errors.releaseVersion).toBe(
      noticeMessages.versionRequired,
    )
    expect(checkNotice({ ...base, kind: 'update', releaseVersion: 'v0.6' }).errors.releaseVersion).toBe(
      noticeMessages.versionInvalid,
    )
  })
})

describe('탭 값', () => {
  it.each([
    ['update', 'update'],
    ['notice', 'notice'],
    ['xyz', null],
    [null, null],
    ['UPDATE', null],
  ])('%s → %s', (value, expected) => expect(kindFrom(value)).toBe(expected))
})
