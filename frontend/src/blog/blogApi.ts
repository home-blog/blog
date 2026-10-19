// 블로그·분류 요청 (specs/003 contracts/blog-post-api.md 1 ~ 8). 내 블로그는 번호를 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'
import type { ActivityDay } from './activity'

export type Visibility = 'public' | 'private'

export interface Blog {
  blogId: number
  name: string
  /** 비어 있으면 '' */
  intro: string
  /** 화면이 "블로그 설정" 버튼을 보일지만 정한다. 실제 권한은 서버가 다시 본다 */
  isOwner: boolean
  /** 받아 보는 사람 수와 내가 받아 보는지 (specs/016 US1) */
  subscriberCount: number
  subscribedByMe: boolean
  /** 내가 새 원고를 메일로도 받는가 */
  subscriptionEmail: boolean
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
  /** 분류 색 번호 (specs/006). 색 값은 manage/categoryColors.ts */
  colorIndex: number
}

export function getBlog(blogId: number, signal?: AbortSignal): Promise<Blog> {
  return api<Blog>(`/api/v1/blogs/${blogId}`, { signal })
}

/** 블로그 머리의 "지금까지 쓴 원고지" (v0.5.1). 보이는 글만 센다: 방문자는 공개 글, 주인은 비공개까지 */
export interface BlogManuscript {
  blogId: number
  postCount: number
  sheets: number
}

export function getBlogManuscript(blogId: number, signal?: AbortSignal): Promise<BlogManuscript> {
  return api<BlogManuscript>(`/api/v1/blogs/${blogId}/manuscript`, { signal })
}

/** 글쓰기 잔디 (specs/013 US1). 공개 분류의 공개 글을 쓴 날만 담는다 */
export interface BlogActivity {
  blogId: number
  from: string
  to: string
  days: ActivityDay[]
  totalPosts: number
  totalSheets: number
}

export function getBlogActivity(blogId: number, signal?: AbortSignal): Promise<BlogActivity> {
  return api<BlogActivity>(`/api/v1/blogs/${blogId}/activity`, { signal })
}

export async function getCategories(blogId: number, signal?: AbortSignal): Promise<CategorySummary[]> {
  const res = await api<{ categories: CategorySummary[] }>(`/api/v1/blogs/${blogId}/categories`, { signal })
  return res.categories
}

export function getMyBlog(signal?: AbortSignal): Promise<MyBlog> {
  return api<MyBlog>('/api/v1/me/blog', { signal })
}

/** 내 블로그 이름·소개 고치기 (contracts 4). 앞뒤 공백은 서버가 지운다 */
export function updateMyBlog(name: string, intro: string): Promise<MyBlog> {
  return api<MyBlog>('/api/v1/me/blog', { method: 'PUT', body: { name, intro } })
}

// 분류 관리 (contracts 5 ~ 8): 내 블로그는 세션으로 정한다
export function createCategory(name: string, visibility: Visibility): Promise<CategorySummary> {
  return api<CategorySummary>('/api/v1/me/blog/categories', { method: 'POST', body: { name, visibility } })
}

/** 보낸 칸만 바꾼다 */
export function updateCategory(categoryId: number, change: { name?: string; visibility?: Visibility }): Promise<CategorySummary> {
  return api<CategorySummary>(`/api/v1/me/blog/categories/${categoryId}`, { method: 'PATCH', body: change })
}

/** 내 분류 번호 전체를 위에서부터 새 순서로 */
export async function reorderCategories(categoryIds: number[]): Promise<CategorySummary[]> {
  const res = await api<{ categories: CategorySummary[] }>('/api/v1/me/blog/categories/order', { method: 'PUT', body: { categoryIds } })
  return res.categories
}

export function deleteCategory(categoryId: number): Promise<void> {
  return api<void>(`/api/v1/me/blog/categories/${categoryId}`, { method: 'DELETE' })
}
