// 공지사항 요청 (specs/012 contracts/notice-api.md)
import { api } from '../api/client'

export type NoticeKind = 'update' | 'notice'

export interface NoticeRow {
  noticeId: number
  kind: NoticeKind
  title: string
  releaseVersion: string | null
  pinned: boolean
  createdAt: string
}

export interface NoticePage {
  items: NoticeRow[]
  page: number
  totalPages: number
  totalCount: number
}

export interface NoticeDetail extends NoticeRow {
  content: string
  releaseUrl: string | null
  updatedAt: string | null
}

export interface NoticeInput {
  kind: NoticeKind
  title: string
  content: string
  releaseVersion: string | null
  pinned: boolean
}

export function getNotices(kind: NoticeKind | null, page: number, signal?: AbortSignal): Promise<NoticePage> {
  const query = new URLSearchParams()
  if (kind) query.set('kind', kind)
  if (page > 1) query.set('page', String(page))
  const qs = query.toString()
  return api<NoticePage>(`/api/v1/notices${qs ? `?${qs}` : ''}`, { signal })
}

export function getNotice(noticeId: number, signal?: AbortSignal): Promise<NoticeDetail> {
  return api<NoticeDetail>(`/api/v1/notices/${noticeId}`, { signal })
}

export function getLatestNoticeAt(signal?: AbortSignal): Promise<{ latestAt: string | null }> {
  return api<{ latestAt: string | null }>('/api/v1/notices/latest', { signal })
}

export function createNotice(input: NoticeInput): Promise<{ noticeId: number }> {
  return api<{ noticeId: number }>('/api/v1/notices', { method: 'POST', body: input })
}

export function updateNotice(noticeId: number, input: NoticeInput): Promise<{ noticeId: number }> {
  return api<{ noticeId: number }>(`/api/v1/notices/${noticeId}`, { method: 'PUT', body: input })
}

export function deleteNotice(noticeId: number): Promise<void> {
  return api<void>(`/api/v1/notices/${noticeId}`, { method: 'DELETE' })
}
