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

// 분류 관리 (contracts 5 ~ 8): 내 블로그는 세션으로 정한다
export function createCategory(name: string, visibility: Visibility): Promise<CategorySummary> {
  return api<CategorySummary>('/api/me/blog/categories', { method: 'POST', body: { name, visibility } })
}

/** 보낸 칸만 바꾼다 */
export function updateCategory(categoryId: number, change: { name?: string; visibility?: Visibility }): Promise<CategorySummary> {
  return api<CategorySummary>(`/api/me/blog/categories/${categoryId}`, { method: 'PATCH', body: change })
}

/** 내 분류 번호 전체를 위에서부터 새 순서로 */
export async function reorderCategories(categoryIds: number[]): Promise<CategorySummary[]> {
  const res = await api<{ categories: CategorySummary[] }>('/api/me/blog/categories/order', { method: 'PUT', body: { categoryIds } })
  return res.categories
}

export function deleteCategory(categoryId: number): Promise<void> {
  return api<void>(`/api/me/blog/categories/${categoryId}`, { method: 'DELETE' })
}
