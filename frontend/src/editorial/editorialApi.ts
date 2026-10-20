// 편집실 요청 (specs/018 contracts). 모두 관리자만 (서버가 403으로 막는다)
import { api, ApiError } from '../api/client'

export interface EditorialSummary {
  members: number
  membersToday: number
  publicPosts: number
  publicPostsToday: number
  boardPosts: number
  /** 글·글마당·댓글 신고를 모두 센 대기 신고 */
  pendingReports: number
  /** 댓글 신고 대기 (specs/022) */
  pendingCommentReports: number
  /** 지금 정지 중인 회원 (specs/022) */
  suspendedMembers: number
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

/** 댓글 검토 대기 (specs/022). hiddenBy: auto(신고가 모여 저절로) · editor(관리자) */
export interface PendingCommentRow extends ReasonCounts {
  commentId: number
  postId: number | null
  postTitle: string | null
  excerpt: string | null
  writerId: number | null
  writerNickname: string | null
  hidden: boolean
  hiddenBy: 'auto' | 'editor' | null
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

export interface CommentReview {
  commentId: number
  postId: number
  postTitle: string | null
  body: string
  writerNickname: string | null
  hidden: boolean
  hiddenBy: 'auto' | 'editor' | null
  reports: ReportView[]
}

/** 회원 한 줄 (specs/022 US2). suspendedUntil·suspendReason은 지금 정지 중일 때만 */
export interface MemberLine {
  memberId: number
  nickname: string
  blogId: number | null
  blogName: string | null
  role: 'member' | 'admin'
  joinedAt: string
  suspended: boolean
  suspendedUntil: string | null
  suspendReason: string | null
}

export interface MemberPage extends Pending<MemberLine> {
  /** 고를 수 있는 정지 기간(일) */
  suspendDays: number[]
}

export interface ActionRow {
  actionId: number
  targetKind: 'post' | 'board' | 'comment' | 'member'
  postId: number | null
  boardPostId: number | null
  commentId: number | null
  memberId: number | null
  /** 댓글이면 그 글의 제목, 회원이면 그때의 닉네임 */
  title: string
  action: 'taken_down' | 'dismissed' | 'removed' | 'unlocked' | 'hidden' | 'restored' | 'suspended' | 'unsuspended'
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

export const getPendingComments = (page: number, signal?: AbortSignal) =>
  api<Pending<PendingCommentRow>>(`/api/v1/editorial/reports/comments${pageQuery(page)}`, { signal })
export const getCommentReview = (commentId: number, signal?: AbortSignal) =>
  api<CommentReview>(`/api/v1/editorial/reports/comments/${commentId}`, { signal })
export const hideComment = (commentId: number, reason: string) =>
  api<void>(`/api/v1/editorial/comments/${commentId}/hide`, { method: 'POST', body: { reason } })
export const dismissComment = (commentId: number) =>
  api<void>(`/api/v1/editorial/comments/${commentId}/dismiss`, { method: 'POST' })
/** restored: 이번에 되돌렸는가 (가려져 있지 않았으면 false) */
export const restoreComment = (commentId: number) =>
  api<{ restored: boolean }>(`/api/v1/editorial/comments/${commentId}/restore`, { method: 'POST' })

export function getMembers(query: string, page: number, signal?: AbortSignal): Promise<MemberPage> {
  const params = new URLSearchParams()
  if (query.trim()) params.set('q', query.trim())
  if (page > 1) params.set('page', String(page))
  const qs = params.toString()
  return api<MemberPage>(`/api/v1/editorial/members${qs ? `?${qs}` : ''}`, { signal })
}
export const suspendMember = (memberId: number, days: number, reason: string) =>
  api<{ suspendedUntil: string }>(`/api/v1/editorial/members/${memberId}/suspend`, {
    method: 'POST',
    body: { days, reason },
  })
/** unsuspended: 이번에 풀었는가 (정지 중이 아니었으면 false) */
export const unsuspendMember = (memberId: number) =>
  api<{ unsuspended: boolean }>(`/api/v1/editorial/members/${memberId}/unsuspend`, { method: 'POST' })

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
  hidden: '가림',
  restored: '되돌림',
  suspended: '정지',
  unsuspended: '정지 풂',
}

export const TARGET_LABEL: Record<ActionRow['targetKind'], string> = {
  post: '블로그 글',
  board: '글마당',
  comment: '댓글',
  member: '회원',
}

/** 서버 설정 editorial.reason-max-length와 같다 */
export const REASON_MAX = 200

export function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}
