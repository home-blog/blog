// 편집실 요청 (specs/018 contracts). 모두 관리자만 (서버가 403으로 막는다)
import { api, ApiError } from '../api/client'

export interface EditorialSummary {
  members: number
  membersToday: number
  publicPosts: number
  publicPostsToday: number
  boardPosts: number
  pendingReports: number
}

interface ReasonCounts {
  reportCount: number
  spam: number
  abuse: number
  adult: number
  other: number
  firstAt: string
  lastAt: string
}

export interface PendingPostRow extends ReasonCounts {
  postId: number
  blogId: number | null
  blogName: string | null
  title: string | null
  visibility: 'public' | 'private' | null
  locked: boolean
}

export interface PendingBoardRow extends ReasonCounts {
  boardPostId: number
  topic: string | null
  title: string | null
}

export interface Pending<T> {
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  items: T[]
}

export interface ReportView {
  reason: 'SPAM' | 'ABUSE' | 'ADULT' | 'OTHER'
  detail: string | null
  createdAt: string
}

export interface PostReview {
  postId: number
  blogName: string
  title: string
  excerpt: string
  visibility: 'public' | 'private'
  locked: boolean
  lockReason: string | null
  reports: ReportView[]
}

export interface BoardReview {
  boardPostId: number
  topic: string
  title: string
  excerpt: string
  reports: ReportView[]
}

export interface ActionRow {
  actionId: number
  targetKind: 'post' | 'board'
  postId: number | null
  boardPostId: number | null
  title: string
  action: 'taken_down' | 'dismissed' | 'removed' | 'unlocked'
  reason: string | null
  reportCount: number
  createdAt: string
}

const pageQuery = (page: number) => (page > 1 ? `?page=${page}` : '')

export const getSummary = (signal?: AbortSignal) => api<EditorialSummary>('/api/v1/editorial/summary', { signal })
export const getPendingPosts = (page: number, signal?: AbortSignal) =>
  api<Pending<PendingPostRow>>(`/api/v1/editorial/reports/posts${pageQuery(page)}`, { signal })
export const getPendingBoard = (page: number, signal?: AbortSignal) =>
  api<Pending<PendingBoardRow>>(`/api/v1/editorial/reports/board${pageQuery(page)}`, { signal })
export const getActions = (page: number, signal?: AbortSignal) =>
  api<Pending<ActionRow>>(`/api/v1/editorial/actions${pageQuery(page)}`, { signal })
export const getPostReview = (postId: number, signal?: AbortSignal) =>
  api<PostReview>(`/api/v1/editorial/reports/posts/${postId}`, { signal })
export const getBoardReview = (boardPostId: number, signal?: AbortSignal) =>
  api<BoardReview>(`/api/v1/editorial/reports/board/${boardPostId}`, { signal })

export const takeDown = (postId: number, reason: string) =>
  api<void>(`/api/v1/editorial/posts/${postId}/take-down`, { method: 'POST', body: { reason } })
export const dismissPost = (postId: number) =>
  api<void>(`/api/v1/editorial/posts/${postId}/dismiss`, { method: 'POST' })
/** unlocked: 이번에 풀었는가 (이미 풀려 있었으면 false) */
export const unlockPost = (postId: number) =>
  api<{ unlocked: boolean }>(`/api/v1/editorial/posts/${postId}/unlock`, { method: 'POST' })
export const removeBoardPost = (boardPostId: number, reason: string) =>
  api<void>(`/api/v1/editorial/board/${boardPostId}/remove`, { method: 'POST', body: { reason } })
export const dismissBoardPost = (boardPostId: number) =>
  api<void>(`/api/v1/editorial/board/${boardPostId}/dismiss`, { method: 'POST' })

export const REASON_LABEL: Record<ReportView['reason'], string> = {
  SPAM: '스팸',
  ABUSE: '욕설·혐오',
  ADULT: '음란물',
  OTHER: '기타',
}

export const ACTION_LABEL: Record<ActionRow['action'], string> = {
  taken_down: '내림',
  dismissed: '반려',
  removed: '지움',
  unlocked: '잠금 풂',
}

/** 서버 설정 editorial.reason-max-length와 같다 */
export const REASON_MAX = 200

export function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}
