// 글 요청 (specs/003 contracts/blog-post-api.md 9 ~ 14). 블로그 번호는 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'
import type { Visibility } from '../blog/blogApi'

export interface CategoryOption {
  categoryId: number
  name: string
}

export interface TopicOption {
  topicId: number
  name: string
}

export interface PostForm {
  categories: CategoryOption[]
  topics: TopicOption[]
  /** 마지막에 쓴 글의 분류, 글이 없으면 미분류 */
  defaultCategoryId: number
  /** 마지막에 쓴 글의 주제, 글이 없으면 null (사용자가 고른다) */
  defaultTopicId: number | null
  defaultVisibility: Visibility
  limits: { titleMaxLength: number; contentMaxLength: number }
}

export interface PostInput {
  title: string
  content: string
  categoryId: number | null
  topicId: number | null
  visibility: Visibility
}

export function getPostForm(signal?: AbortSignal): Promise<PostForm> {
  return api<PostForm>('/api/me/blog/post-form', { signal })
}

/** 같은 requestKey로 다시 보내면 서버가 새 글을 만들지 않고 처음 글 번호를 돌려준다 (D-6) */
export function createPost(input: PostInput, requestKey: string): Promise<{ postId: number }> {
  return api<{ postId: number }>('/api/posts', { method: 'POST', body: { ...input, requestKey } })
}
