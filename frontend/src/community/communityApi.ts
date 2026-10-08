// 좋아요·신고 요청 (specs/005 contracts/community-api.md 4 ~ 6)
import { api } from '../api/client'

export interface LikeState {
  likeCount: number
  likedByMe: boolean
}

/** 이미 눌렀으면 그대로 같은 답 */
export function likePost(postId: number): Promise<LikeState> {
  return api<LikeState>(`/api/posts/${postId}/like`, { method: 'PUT' })
}

/** 누르지 않았어도 같은 답 */
export function unlikePost(postId: number): Promise<LikeState> {
  return api<LikeState>(`/api/posts/${postId}/like`, { method: 'DELETE' })
}

export type ReportReason = 'SPAM' | 'ABUSE' | 'ADULT' | 'OTHER'

/** detail은 OTHER일 때만 저장된다 (다른 사유면 서버가 버린다) */
export function reportPost(postId: number, reason: ReportReason, detail: string): Promise<{ message: string }> {
  return api<{ message: string }>(`/api/posts/${postId}/reports`, {
    method: 'POST',
    body: reason === 'OTHER' ? { reason, detail } : { reason },
  })
}
