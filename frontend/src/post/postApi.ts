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

export interface PostDetail {
  postId: number
  blogId: number
  blogName: string
  category: { categoryId: number; name: string; visibility: Visibility }
  topic: { topicId: number; name: string }
  title: string
  /** 마크다운 원문. MarkdownView로만 그린다 */
  content: string
  visibility: Visibility
  createdAt: string
  /** 수정한 적이 없으면 null */
  updatedAt: string | null
  /** 같은 블로그의 공개 분류의 공개 글 중 바로 앞에 쓴 글. 없으면 null */
  prevPostId: number | null
  /** 바로 뒤에 쓴 글. 없으면 null */
  nextPostId: number | null
  /** 화면이 수정·삭제 버튼을 보일지만 정한다. 실제 권한은 서버가 다시 본다 */
  isOwner: boolean
}

export function getPost(postId: number, signal?: AbortSignal): Promise<PostDetail> {
  return api<PostDetail>(`/api/posts/${postId}`, { signal })
}
