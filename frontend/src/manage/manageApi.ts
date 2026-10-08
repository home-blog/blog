// 블로그 관리 요청 (specs/006 contracts/manage-api.md). 내 블로그는 번호를 보내지 않는다: 서버가 세션으로 정한다
// 분류 관리·블로그 설정은 003의 주소(blog/blogApi.ts)를 그대로 쓴다
import { api } from '../api/client'

/** 관리 화면 머리 정보 (contracts 1) */
export interface ManageHeader {
  blogId: number
  name: string
  /** 비어 있으면 '' */
  intro: string
  /** "내 블로그 보기"가 갈 화면 주소 */
  blogPath: string
}

export function getManageHeader(signal?: AbortSignal): Promise<ManageHeader> {
  return api<ManageHeader>('/api/manage/blog', { signal })
}

export type VisibilityFilter = 'all' | 'public' | 'private'

/** 글 관리 한 줄 (contracts 3-1) */
export interface ManagePost {
  postId: number
  title: string
  category: { categoryId: number; name: string }
  createdAt: string
  visibility: 'public' | 'private'
  views: number
  commentCount: number
}

export interface ManagePostPage {
  items: ManagePost[]
  page: number
  pageSize: number
  totalCount: number
  /** 거르기 전 내 글이 하나라도 있는지 */
  hasAnyPost: boolean
}

/** 내 글 목록, 비공개 포함 (contracts 3-1). page는 1부터 */
export function getManagePosts(
  query: { visibility: VisibilityFilter; categoryId: number | null; page: number },
  signal?: AbortSignal,
): Promise<ManagePostPage> {
  const params = new URLSearchParams({ visibility: query.visibility, page: String(query.page) })
  if (query.categoryId !== null) params.set('categoryId', String(query.categoryId))
  return api<ManagePostPage>(`/api/manage/posts?${params}`, { signal })
}
