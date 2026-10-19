// 글마당 요청 (specs/017 contracts/board-api.md)
import { api, ApiError } from '../api/client'

export type BoardKind = 'talk' | 'question'

/** 탈퇴한 회원이면 id·nickname이 null이고 withdrawn이 참 */
export interface BoardWriter {
  id: number | null
  nickname: string | null
  withdrawn: boolean
}

export interface BoardRow {
  boardPostId: number
  topic: string
  kind: BoardKind
  title: string
  writer: BoardWriter
  createdAt: string
  views: number
  commentCount: number
  /** 채택한 댓글이 있는 물음 */
  solved: boolean
}

export interface BoardPage {
  topics: string[]
  topic: string | null
  kind: BoardKind | null
  unsolved: boolean
  keyword: string | null
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  posts: BoardRow[]
}

export interface BoardDetail {
  boardPostId: number
  topic: string
  kind: BoardKind
  title: string
  content: string
  writer: BoardWriter
  createdAt: string
  updatedAt: string
  views: number
  commentCount: number
  acceptedCommentId: number | null
  mine: boolean
  canDelete: boolean
}

export interface BoardComment {
  boardCommentId: number
  writer: BoardWriter
  content: string
  createdAt: string
  accepted: boolean
  mine: boolean
  canDelete: boolean
}

export interface BoardCommentList {
  count: number
  acceptedCommentId: number | null
  /** 보는 사람이 이 물음을 쓴 사람인가 */
  canAccept: boolean
  comments: BoardComment[]
}

export interface BoardInput {
  topic: string
  kind: BoardKind
  title: string
  content: string
}

export interface BoardQuery {
  topic?: string | null
  kind?: BoardKind | null
  unsolved?: boolean
  q?: string | null
  page?: number
}

export function boardQueryString(query: BoardQuery): string {
  const params = new URLSearchParams()
  if (query.topic) params.set('topic', query.topic)
  if (query.kind) params.set('kind', query.kind)
  if (query.unsolved) params.set('unsolved', 'true')
  if (query.q) params.set('q', query.q)
  if (query.page && query.page > 1) params.set('page', String(query.page))
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

export function getBoardPosts(query: BoardQuery, signal?: AbortSignal): Promise<BoardPage> {
  return api<BoardPage>(`/api/v1/board/posts${boardQueryString(query)}`, { signal })
}

export function getRecentBoardPosts(signal?: AbortSignal): Promise<{ posts: BoardRow[] }> {
  return api<{ posts: BoardRow[] }>('/api/v1/board/posts/recent', { signal })
}

export function getBoardPost(id: number, signal?: AbortSignal): Promise<BoardDetail> {
  return api<BoardDetail>(`/api/v1/board/posts/${id}`, { signal })
}

export function createBoardPost(input: BoardInput): Promise<{ boardPostId: number }> {
  return api<{ boardPostId: number }>('/api/v1/board/posts', { method: 'POST', body: input })
}

export function updateBoardPost(id: number, input: BoardInput): Promise<{ boardPostId: number }> {
  return api<{ boardPostId: number }>(`/api/v1/board/posts/${id}`, { method: 'PUT', body: input })
}

export function deleteBoardPost(id: number): Promise<void> {
  return api<void>(`/api/v1/board/posts/${id}`, { method: 'DELETE' })
}

export function getBoardComments(id: number, signal?: AbortSignal): Promise<BoardCommentList> {
  return api<BoardCommentList>(`/api/v1/board/posts/${id}/comments`, { signal })
}

export function writeBoardComment(id: number, content: string): Promise<BoardComment> {
  return api<BoardComment>(`/api/v1/board/posts/${id}/comments`, { method: 'POST', body: { content } })
}

export function deleteBoardComment(commentId: number): Promise<void> {
  return api<void>(`/api/v1/board/comments/${commentId}`, { method: 'DELETE' })
}

export function acceptComment(id: number, commentId: number): Promise<{ acceptedCommentId: number | null }> {
  return api<{ acceptedCommentId: number | null }>(`/api/v1/board/posts/${id}/accepted`, {
    method: 'PUT',
    body: { commentId },
  })
}

export function unacceptComment(id: number): Promise<{ acceptedCommentId: number | null }> {
  return api<{ acceptedCommentId: number | null }>(`/api/v1/board/posts/${id}/accepted`, { method: 'DELETE' })
}

export const KIND_LABEL: Record<BoardKind, string> = { talk: '이야기', question: '물음' }

/** 탈퇴한 회원은 "탈퇴한 회원" */
export function writerName(writer: BoardWriter): string {
  return writer.withdrawn || !writer.nickname ? '탈퇴한 회원' : writer.nickname
}

export function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}
