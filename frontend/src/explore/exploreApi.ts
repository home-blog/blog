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
  return api<PostListPage>(`/api/blogs/${blogId}/posts${query ? `?${query}` : ''}`, { signal })
}
