// 댓글 요청 (specs/005 contracts/community-api.md 1 ~ 3). 작성자는 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'
import type { ReportReason } from '../community/communityApi'

export interface CommentAuthor {
  /** 탈퇴했으면 null */
  id: number | null
  /** 탈퇴했으면 null. 화면은 "탈퇴한 사용자"로 보여 준다 */
  nickname: string | null
  withdrawn: boolean
}

export interface CommentItem {
  id: number
  /** 지운 칸·봉한 쪽지면 id·nickname이 null이고 withdrawn은 거짓 */
  author: CommentAuthor
  /** 입력한 글자 그대로. HTML로 넣지 않고 글자로만 그린다 (FR-030). 지운 칸·봉한 쪽지면 빈 글자 */
  body: string
  createdAt: string
  /** 삭제 버튼을 보일지만 정한다. 실제 권한은 서버가 다시 본다 */
  canDelete: boolean
  /** 답글이면 원댓글 번호 (specs/019). 답글은 한 단계 */
  parentId: number | null
  /** 비밀 댓글(봉한 쪽지) */
  secret: boolean
  /** 읽을 수 없는 비밀 댓글: 서버가 내용·쓴 사람을 빼고 보냈다 */
  sealed: boolean
  /** 답글이 남아 줄만 남은 지운 칸 */
  deleted: boolean
  /** 답글 단추를 보일지 (답글이면 그 원댓글에 단다) */
  canReply: boolean
  /** 편집실이 가린 댓글 (specs/022): 서버가 내용·쓴 사람을 빼고 보냈다. 답글을 달 수 없다 */
  hidden: boolean
  /** 신고 단추를 보일지 (로그인했고, 내 댓글이 아니고, 보이는 댓글) */
  canReport: boolean
}

export interface CommentList {
  /** 지운 칸·가린 댓글을 뺀 수 */
  count: number
  /** 원댓글이 오래된 순, 각 원댓글 바로 뒤에 그 답글이 오래된 순 */
  comments: CommentItem[]
}

export interface WriteOptions {
  /** 답글을 달 댓글 번호 */
  parentId?: number
  /** 봉해서 남기기 */
  secret?: boolean
}

export function getComments(postId: number, signal?: AbortSignal): Promise<CommentList> {
  return api<CommentList>(`/api/v1/posts/${postId}/comments`, { signal })
}

export function writeComment(postId: number, body: string, options: WriteOptions = {}): Promise<CommentItem> {
  return api<CommentItem>(`/api/v1/posts/${postId}/comments`, {
    method: 'POST',
    body: { body, parentId: options.parentId ?? null, secret: options.secret ?? false },
  })
}

/** 작성자나 그 글의 블로그 주인만. 아니면 서버가 없는 댓글과 같은 404로 답한다 */
export function deleteComment(commentId: number): Promise<void> {
  return api<void>(`/api/v1/comments/${commentId}`, { method: 'DELETE' })
}

export interface CommentReportResult {
  message: string
  /** 이 신고로 대기 신고가 기준에 닿아 저절로 가려졌다 */
  hidden: boolean
}

/** 댓글 신고 (specs/022 contracts 1). detail은 OTHER일 때만 보낸다 */
export function reportComment(commentId: number, reason: ReportReason, detail: string): Promise<CommentReportResult> {
  return api<CommentReportResult>(`/api/v1/comments/${commentId}/reports`, {
    method: 'POST',
    body: reason === 'OTHER' ? { reason, detail } : { reason },
  })
}
