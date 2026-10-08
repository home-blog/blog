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
  /** 0~5개. 보낸 목록이 새 전체 목록이다 (specs/005 contracts 8) */
  tags: string[]
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
  /** 댓글 수 (specs/005 contracts 7) */
  commentCount: number
  /** 좋아요 수 */
  likeCount: number
  /** 로그인한 회원이 좋아요를 눌렀나 (로그인하지 않았으면 false) */
  likedByMe: boolean
  /** 태그 (소문자, 이름순). 글 아래에 보여 준다 */
  tags: string[]
}

export function getPost(postId: number, signal?: AbortSignal): Promise<PostDetail> {
  return api<PostDetail>(`/api/posts/${postId}`, { signal })
}

export interface PostEdit {
  postId: number
  title: string
  content: string
  categoryId: number
  topicId: number
  visibility: Visibility
  tags: string[]
  categories: CategoryOption[]
  topics: TopicOption[]
}

export interface PostUpdated {
  postId: number
  /** 바뀐 것이 없으면 false이고 아무것도 저장하지 않았다 */
  changed: boolean
  updatedAt: string | null
}

/** 남의 글이면 서버가 404로 답한다 */
export function getPostEdit(postId: number, signal?: AbortSignal): Promise<PostEdit> {
  return api<PostEdit>(`/api/posts/${postId}/edit`, { signal })
}

export function updatePost(postId: number, input: PostInput): Promise<PostUpdated> {
  return api<PostUpdated>(`/api/posts/${postId}`, { method: 'PUT', body: input })
}

export function deletePost(postId: number): Promise<void> {
  return api<void>(`/api/posts/${postId}`, { method: 'DELETE' })
}

/** 같은 태그의 공개 글 한 줄. 검색 결과(specs/004)와 같은 모양 */
export interface TagPost {
  postId: number
  blogId: number
  blogName: string
  title: string
  categoryId: number
  categoryName: string
  createdAt: string
  preview: string
}

export interface TagPostsPage {
  /** 서버가 다듬은 이름 (소문자) */
  tag: string
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  posts: TagPost[]
}

/** 누가 봐도 공개 분류의 공개 글만 온다 (specs/005 contracts 9). 없는 태그면 빈 목록 */
export function getTagPosts(tag: string, page: number, signal?: AbortSignal): Promise<TagPostsPage> {
  const query = page > 1 ? `?page=${page}` : ''
  return api<TagPostsPage>(`/api/tags/${encodeURIComponent(tag)}/posts${query}`, { signal })
}
