// 방명록 요청 (specs/021 contracts 1 ~ 4). 쓴 사람은 서버가 세션으로 정한다
import { api } from '../api/client'

export type StampName = 'pine' | 'wave' | 'moon' | 'plum' | 'crane'

export interface EntryAuthor {
  /** 탈퇴했거나 봉한 엽서면 null */
  id: number | null
  nickname: string | null
  withdrawn: boolean
}

/** 봉한 엽서(sealed)는 body가 '', author가 모두 빈 값, reply가 null이다 */
export interface GuestbookEntry {
  entryId: number
  author: EntryAuthor
  body: string
  stamp: StampName
  secret: boolean
  sealed: boolean
  createdAt: string
  reply: string | null
  repliedAt: string | null
  /** 화면이 단추를 보일지만 정한다. 실제 권한은 서버가 다시 본다 */
  canDelete: boolean
  canReply: boolean
}

export interface GuestbookPage {
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  /** 로그인했고 블로그 주인이 아니다 */
  canWrite: boolean
  bodyMaxLength: number
  stamps: StampName[]
  items: GuestbookEntry[]
}

export function getGuestbook(blogId: number, page: number, signal?: AbortSignal): Promise<GuestbookPage> {
  return api<GuestbookPage>(`/api/v1/blogs/${blogId}/guestbook${page > 1 ? `?page=${page}` : ''}`, { signal })
}

export function writeEntry(blogId: number, body: string, stamp: StampName, secret: boolean): Promise<GuestbookEntry> {
  return api<GuestbookEntry>(`/api/v1/blogs/${blogId}/guestbook`, { method: 'POST', body: { body, stamp, secret } })
}

export function deleteEntry(entryId: number): Promise<void> {
  return api<void>(`/api/v1/guestbook/${entryId}`, { method: 'DELETE' })
}

export function replyEntry(entryId: number, body: string): Promise<GuestbookEntry> {
  return api<GuestbookEntry>(`/api/v1/guestbook/${entryId}/reply`, { method: 'PUT', body: { body } })
}

export function deleteReply(entryId: number): Promise<void> {
  return api<void>(`/api/v1/guestbook/${entryId}/reply`, { method: 'DELETE' })
}
