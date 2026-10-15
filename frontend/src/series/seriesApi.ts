// 연재 요청 (specs/013 contracts 2). 읽기는 누구나, 쓰기는 주인만(내 블로그는 서버가 세션으로 정한다)
import { api } from '../api/client'
import type { Visibility } from '../blog/blogApi'

export interface SeriesSummary {
  seriesId: number
  name: string
  description: string
  postCount: number
}

export interface SeriesEpisode {
  postId: number
  title: string
  episode: number
  createdAt: string
  /** 주인에게만 온다 */
  visibility?: Visibility
}

export interface SeriesDetail {
  seriesId: number
  blogId: number
  name: string
  description: string
  posts: SeriesEpisode[]
}

export interface PostSeries {
  seriesId: number
  name: string
  episode: number
  total: number
  previous: { postId: number; title: string } | null
  next: { postId: number; title: string } | null
  posts: { postId: number; title: string; episode: number }[]
}

export async function getBlogSeries(blogId: number, signal?: AbortSignal): Promise<SeriesSummary[]> {
  const res = await api<{ items: SeriesSummary[] }>(`/api/v1/blogs/${blogId}/series`, { signal })
  return res.items
}

export function getSeries(seriesId: number, signal?: AbortSignal): Promise<SeriesDetail> {
  return api<SeriesDetail>(`/api/v1/series/${seriesId}`, { signal })
}

export async function getPostSeries(postId: number, signal?: AbortSignal): Promise<PostSeries | null> {
  const res = await api<{ series: PostSeries | null }>(`/api/v1/posts/${postId}/series`, { signal })
  return res.series
}

export function createSeries(name: string, description: string): Promise<SeriesDetail> {
  return api<SeriesDetail>('/api/v1/me/blog/series', { method: 'POST', body: { name: name.trim(), description: description.trim() } })
}

export function updateSeries(seriesId: number, name: string, description: string): Promise<SeriesDetail> {
  return api<SeriesDetail>(`/api/v1/me/blog/series/${seriesId}`, {
    method: 'PUT',
    body: { name: name.trim(), description: description.trim() },
  })
}

export function deleteSeries(seriesId: number): Promise<void> {
  return api<void>(`/api/v1/me/blog/series/${seriesId}`, { method: 'DELETE' })
}

export function addPostToSeries(seriesId: number, postId: number): Promise<void> {
  return api<void>(`/api/v1/me/blog/series/${seriesId}/posts/${postId}`, { method: 'PUT' })
}

export function removePostFromSeries(seriesId: number, postId: number): Promise<void> {
  return api<void>(`/api/v1/me/blog/series/${seriesId}/posts/${postId}`, { method: 'DELETE' })
}

export function reorderSeries(seriesId: number, postIds: number[]): Promise<SeriesDetail> {
  return api<SeriesDetail>(`/api/v1/me/blog/series/${seriesId}/order`, { method: 'PUT', body: { postIds } })
}
