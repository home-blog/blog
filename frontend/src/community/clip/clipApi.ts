// 문장 수집·필사 노트 요청 (specs/015 contracts 3). 모두 로그인이 필요하다
import { api } from '../../api/client'

/** 원문 글. postId는 글이 지워졌으면 null, available은 지금 그 글을 볼 수 있는가 */
export interface ClipPost {
  postId: number | null
  title: string
  blogName: string
  available: boolean
}

export interface Clip {
  clipId: number
  text: string
  /** ISO 8601 */
  createdAt: string
  post: ClipPost
}

export interface ClipPage {
  items: Clip[]
  page: number
  totalPages: number
  totalCount: number
}

/** 화면과 서버가 같이 쓰는 한 문장의 길이 (community.clip.max-length) */
export const CLIP_MAX_LENGTH = 300

/** 이미 모은 문장이면 서버가 앞의 것을 돌려준다 */
export function collectSentence(postId: number, text: string): Promise<Clip> {
  return api<Clip>('/api/v1/me/clips', { method: 'POST', body: { postId, text } })
}

export function getClips(page: number, signal?: AbortSignal): Promise<ClipPage> {
  return api<ClipPage>(`/api/v1/me/clips${page > 1 ? `?page=${page}` : ''}`, { signal })
}

export function deleteClip(clipId: number): Promise<void> {
  return api<void>(`/api/v1/me/clips/${clipId}`, { method: 'DELETE' })
}

/** 글 주인만: 이 글에서 문장을 모은 사람 수 */
export function getCollectors(postId: number, signal?: AbortSignal): Promise<{ collectors: number }> {
  return api<{ collectors: number }>(`/api/v1/posts/${postId}/clips/collectors`, { signal })
}

/** 고른 글자를 서버가 받는 모양으로: 앞뒤 공백을 지우고 줄바꿈·여러 칸 공백을 한 칸으로 */
export function tidySelection(text: string): string {
  return text.trim().replace(/\s+/g, ' ')
}
