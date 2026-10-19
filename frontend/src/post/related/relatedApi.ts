// 이 글과 비슷한 글 (specs/014 contracts 3). 누구나
import { api } from '../../api/client'
import type { FeedPost } from '../../explore/exploreApi'

export interface RelatedPosts {
  /** meaning: 뜻이 비슷한 글, tags: 태그·주제가 겹치는 글(AI가 꺼져 있거나 뜻 정보가 아직 없을 때) */
  basis: 'meaning' | 'tags'
  posts: FeedPost[]
}

export function getRelatedPosts(postId: number, signal?: AbortSignal): Promise<RelatedPosts> {
  return api<RelatedPosts>(`/api/v1/posts/${postId}/related`, { signal })
}
