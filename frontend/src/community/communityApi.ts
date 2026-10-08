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
