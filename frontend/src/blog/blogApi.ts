// 블로그·분류 요청 (specs/003 contracts/blog-post-api.md 1 ~ 8). 내 블로그는 번호를 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'

export type Visibility = 'public' | 'private'

export interface Blog {
  blogId: number
  name: string
  /** 비어 있으면 '' */
  intro: string
  /** 화면이 "블로그 설정" 버튼을 보일지만 정한다. 실제 권한은 서버가 다시 본다 */
  isOwner: boolean
}

export interface MyBlog {
  blogId: number
  name: string
  intro: string
}

export interface CategorySummary {
  categoryId: number
  name: string
  /** 보는 사람에 따라 다르다: 주인은 비공개 글까지, 방문자는 공개 분류의 공개 글만 */
  postCount: number
  isDefault: boolean
  visibility: Visibility
}

export function getBlog(blogId: number, signal?: AbortSignal): Promise<Blog> {
  return api<Blog>(`/api/blogs/${blogId}`, { signal })
}

export async function getCategories(blogId: number, signal?: AbortSignal): Promise<CategorySummary[]> {
  const res = await api<{ categories: CategorySummary[] }>(`/api/blogs/${blogId}/categories`, { signal })
  return res.categories
}

export function getMyBlog(signal?: AbortSignal): Promise<MyBlog> {
  return api<MyBlog>('/api/me/blog', { signal })
}
