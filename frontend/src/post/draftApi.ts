// 임시 저장·예약 요청 (specs/013 contracts 3, 4). 모두 주인만, 남의 것은 404
import { api } from '../api/client'
import type { Visibility } from '../blog/blogApi'
import type { PostInput } from './postApi'

export interface DraftSaved {
  draftId: number
  updatedAt: string
  publishAt: string | null
}

export interface DraftItem {
  draftId: number
  title: string
  updatedAt: string
  publishAt: string | null
}

export interface DraftList {
  items: DraftItem[]
  count: number
  maxCount: number
}

export interface Draft {
  draftId: number
  title: string
  content: string
  categoryId: number | null
  topicId: number | null
  seriesId: number | null
  visibility: Visibility
  tags: string[]
  updatedAt: string
  publishAt: string | null
}

export type DraftInput = PostInput & { seriesId: number | null }

export function getDrafts(signal?: AbortSignal): Promise<DraftList> {
  return api<DraftList>('/api/v1/me/drafts', { signal })
}

export function getDraft(draftId: number, signal?: AbortSignal): Promise<Draft> {
  return api<Draft>(`/api/v1/me/drafts/${draftId}`, { signal })
}

export function createDraft(input: DraftInput): Promise<DraftSaved> {
  return api<DraftSaved>('/api/v1/me/drafts', { method: 'POST', body: input })
}

export function updateDraft(draftId: number, input: DraftInput): Promise<DraftSaved> {
  return api<DraftSaved>(`/api/v1/me/drafts/${draftId}`, { method: 'PUT', body: input })
}

export function deleteDraft(draftId: number): Promise<void> {
  return api<void>(`/api/v1/me/drafts/${draftId}`, { method: 'DELETE' })
}

export function publishDraft(draftId: number, input: DraftInput): Promise<{ postId: number }> {
  return api<{ postId: number }>(`/api/v1/me/drafts/${draftId}/publish`, { method: 'POST', body: input })
}

export function scheduleDraft(draftId: number, input: DraftInput, publishAt: string): Promise<DraftSaved> {
  return api<DraftSaved>(`/api/v1/me/drafts/${draftId}/schedule`, { method: 'PUT', body: { ...input, publishAt } })
}

export function unscheduleDraft(draftId: number): Promise<DraftSaved> {
  return api<DraftSaved>(`/api/v1/me/drafts/${draftId}/schedule`, { method: 'DELETE' })
}
