// 구독·알림 요청 (specs/016 contracts). 받아 보기·우편함은 로그인, 그만 받기는 메일의 서명된 값으로 로그인 없이
import { api } from '../api/client'
import type { FeedPost } from '../explore/exploreApi'

export interface SubscriptionState {
  subscribed: boolean
  emailEnabled: boolean
  subscriberCount: number
}

/** 이미 받아 보면 메일 받기만 바꾼다 */
export function subscribe(blogId: number, emailEnabled: boolean): Promise<SubscriptionState> {
  return api<SubscriptionState>(`/api/v1/blogs/${blogId}/subscription`, { method: 'PUT', body: { emailEnabled } })
}

export function unsubscribe(blogId: number): Promise<SubscriptionState> {
  return api<SubscriptionState>(`/api/v1/blogs/${blogId}/subscription`, { method: 'DELETE' })
}

export interface MySubscription {
  blogId: number
  blogName: string
  nickname: string
  emailEnabled: boolean
  createdAt: string
}

export function getMySubscriptions(signal?: AbortSignal): Promise<{ items: MySubscription[] }> {
  return api<{ items: MySubscription[] }>('/api/v1/me/subscriptions', { signal })
}

export interface SubscribedFeed {
  totalCount: number
  page: number
  totalPages: number
  pageSize: number
  posts: FeedPost[]
}

export function getSubscribedFeed(page: number, signal?: AbortSignal): Promise<SubscribedFeed> {
  return api<SubscribedFeed>(`/api/v1/me/subscriptions/feed${page > 1 ? `?page=${page}` : ''}`, { signal })
}

/**
 * kind: post(받아 보는 블로그의 새 원고) · comment(내 글에 달린 댓글) · board(내 글마당 글에 달린 댓글)
 * · accepted(내 글마당 댓글이 채택됨, specs/017). available: 지금 그 글을 볼 수 있는가 (아니면 글 번호·제목은 null)
 * 블로그 글 소식은 postId, 글마당 소식은 boardPostId
 */
export interface Notice {
  notificationId: number
  kind: 'post' | 'comment' | 'board' | 'accepted'
  postId: number | null
  boardPostId: number | null
  postTitle: string | null
  actorName: string
  available: boolean
  read: boolean
  createdAt: string
}

export interface NoticePage {
  items: Notice[]
  page: number
  totalPages: number
  unreadCount: number
}

export function getNotifications(page: number, signal?: AbortSignal): Promise<NoticePage> {
  return api<NoticePage>(`/api/v1/me/notifications${page > 1 ? `?page=${page}` : ''}`, { signal })
}

export function getUnreadCount(signal?: AbortSignal): Promise<{ unreadCount: number }> {
  // 1분마다 묻는다. 로그인이 풀렸어도 머리글 전체가 로그아웃 처리하지 않게 알리지 않는다
  return api<{ unreadCount: number }>('/api/v1/me/notifications/unread-count', { signal, notifyUnauthenticated: false })
}

export function readNotification(id: number): Promise<void> {
  return api<void>(`/api/v1/me/notifications/${id}/read`, { method: 'POST' })
}

export function readAllNotifications(): Promise<void> {
  return api<void>('/api/v1/me/notifications/read-all', { method: 'POST' })
}

export function stopLetters(token: string): Promise<{ blogName: string | null }> {
  return api<{ blogName: string | null }>('/api/v1/newsletter/unsubscribe', { method: 'POST', body: { token } })
}
