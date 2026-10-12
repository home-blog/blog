// 글 목록·검색 요청 (specs/004 contracts/explore-api.md 1, 2). 둘 다 로그인 없이 부르는 읽기 요청이다
import { api } from '../api/client'
import type { Visibility } from '../blog/blogApi'

/** 목록 한 줄 (contracts `목록 한 줄의 모양`). 제목·미리보기는 글자 그대로 보여 준다 */
export interface PostSummary {
  postId: number
  title: string
  categoryId: number
  categoryName: string
  /** ISO 8601 (시간대 포함) */
  createdAt: string
  /** 마크다운 기호를 걷어 낸 본문 앞부분 (서버가 만든다) */
  preview: string
  /** 주인에게만 'private'가 올 수 있다 */
  visibility: Visibility
  /** 원고지 매수·읽는 시간(분) (specs/007 US3). 블로그 글 목록에만 온다 */
  sheets?: number
  readMinutes?: number
  /** 카드 썸네일 주소: 대표 이미지, 없으면 본문 첫 이미지 (specs/008). 이미지가 없으면 null */
  thumbnail?: string | null
}

export interface PostListPage {
  blogId: number
  isOwner: boolean
  /** 실제로 적용한 분류 (고르지 않았으면 null) */
  categoryId: number | null
  totalCount: number
  /** 서버가 범위 안으로 바꾼 페이지 번호 (1부터) */
  page: number
  totalPages: number
  pageSize: number
  posts: PostSummary[]
}

export function getBlogPosts(
  blogId: number,
  options: { page?: number; categoryId?: number | string | null } = {},
  signal?: AbortSignal,
): Promise<PostListPage> {
  const params = new URLSearchParams()
  if (options.page && options.page > 1) params.set('page', String(options.page))
  if (options.categoryId != null && options.categoryId !== '') params.set('categoryId', String(options.categoryId))
  const query = params.toString()
  return api<PostListPage>(`/api/v1/blogs/${blogId}/posts${query ? `?${query}` : ''}`, { signal })
}

/** 검색 결과 한 줄. 항상 공개 글이라 visibility가 없다 */
export interface SearchResult {
  postId: number
  blogId: number
  blogName: string
  title: string
  categoryId: number
  categoryName: string
  createdAt: string
  preview: string
  /** 카드 썸네일 주소 (없으면 null) */
  thumbnail: string | null
}

export interface SearchPage {
  /** 실제로 찾은 값 (앞뒤 공백을 지운 값) */
  keyword: string
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  results: SearchResult[]
}

export function searchPosts(q: string, page: number, signal?: AbortSignal): Promise<SearchPage> {
  const params = new URLSearchParams({ q })
  if (page > 1) params.set('page', String(page))
  return api<SearchPage>(`/api/v1/search/posts?${params.toString()}`, { signal })
}

/** 첫 화면 글 모음 한 줄: 여러 블로그의 공개 글 (GET /api/v1/posts, /api/posts/popular) */
export interface FeedPost {
  postId: number
  blogId: number
  blogName: string
  nickname: string
  title: string
  /** 주제 이름 (여행, 음식, 취미, 운동, 개발) */
  topic: string
  categoryId: number
  categoryName: string
  createdAt: string
  preview: string
  commentCount: number
  likeCount: number
  /** 원고지 매수·읽는 시간(분) (specs/007 US3) */
  sheets: number
  readMinutes: number
  /** 카드 썸네일 주소: 대표 이미지, 없으면 본문 첫 이미지 (specs/008). 이미지가 없으면 null */
  thumbnail: string | null
}

export interface FeedPage {
  /** 고른 주제 (모든 주제면 null) */
  topic: string | null
  /** 주제 탭에 쓸 이름들 (정해진 순서) */
  topics: string[]
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  posts: FeedPost[]
}

export interface PopularPosts {
  topic: string | null
  /** 인기 글을 고른 기간(일) */
  days: number
  posts: FeedPost[]
}

function topicParams(topic: string | null, page = 1): string {
  const params = new URLSearchParams()
  if (topic) params.set('topic', topic)
  if (page > 1) params.set('page', String(page))
  const query = params.toString()
  return query ? `?${query}` : ''
}

export function getFeed(topic: string | null, page: number, signal?: AbortSignal): Promise<FeedPage> {
  return api<FeedPage>(`/api/v1/posts${topicParams(topic, page)}`, { signal })
}

export function getPopular(topic: string | null, signal?: AbortSignal): Promise<PopularPosts> {
  return api<PopularPosts>(`/api/v1/posts/popular${topicParams(topic)}`, { signal })
}
