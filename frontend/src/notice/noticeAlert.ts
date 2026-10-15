// 머리글 "공지"의 새 글 점 (specs/012 US3, research D-9)
// 이 기기의 브라우저 저장소에 "마지막으로 본 가장 새 글 시각"을 두고 서버의 가장 새 글 시각과 비교한다.
// 처음 오는 방문자는 점을 띄우지 않고 지금 시각을 본 것으로 적어 둔다(S3-3). 저장소를 쓸 수 없으면 점을 띄우지 않는다
import { createContext, useContext } from 'react'

export const SEEN_KEY = 'geulgyeol.notice-seen-at'

export interface NoticeAlertState {
  /** 마지막으로 본 뒤 새 글이 있는지 */
  hasNew: boolean
  /** 공지사항 목록을 봤을 때 부른다. 서버의 가장 새 글 시각을 본 것으로 적는다 */
  markSeen: () => void
}

export const NoticeAlertContext = createContext<NoticeAlertState>({ hasNew: false, markSeen: () => {} })

export function useNoticeAlert(): NoticeAlertState {
  return useContext(NoticeAlertContext)
}

export function readSeen(): string | null | undefined {
  try {
    return localStorage.getItem(SEEN_KEY)
  } catch {
    return undefined // 저장소를 쓸 수 없다
  }
}

export function writeSeen(value: string): void {
  try {
    localStorage.setItem(SEEN_KEY, value)
  } catch {
    // 개인 정보 보호 창 등: 점을 띄우지 않는 것으로 충분하다
  }
}

/** 새 글이 있는지: 본 적이 없거나(null) 저장소를 못 쓰면(undefined) 없음 */
export function isNewer(latestAt: string | null, seenAt: string | null | undefined): boolean {
  if (!latestAt || !seenAt) return false
  return Date.parse(latestAt) > Date.parse(seenAt)
}
