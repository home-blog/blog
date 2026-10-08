// 댓글 요청 (specs/005 contracts/community-api.md 1 ~ 3). 작성자는 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'

export interface CommentAuthor {
  /** 탈퇴했으면 null */
  id: number | null
  /** 탈퇴했으면 null. 화면은 "탈퇴한 사용자"로 보여 준다 */
  nickname: string | null
  withdrawn: boolean
}

export interface CommentItem {
  id: number
  author: CommentAuthor
  /** 입력한 글자 그대로. HTML로 넣지 않고 글자로만 그린다 (FR-030) */
  body: string
  createdAt: string
  /** 삭제 버튼을 보일지만 정한다. 실제 권한은 서버가 다시 본다 */
  canDelete: boolean
}

export interface CommentList {
  count: number
  /** 오래된 댓글이 위 */
  comments: CommentItem[]
}

export function getComments(postId: number, signal?: AbortSignal): Promise<CommentList> {
  return api<CommentList>(`/api/posts/${postId}/comments`, { signal })
}

export function writeComment(postId: number, body: string): Promise<CommentItem> {
  return api<CommentItem>(`/api/posts/${postId}/comments`, { method: 'POST', body: { body } })
}
