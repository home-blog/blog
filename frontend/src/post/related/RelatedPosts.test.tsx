// 이 글과 비슷한 글 (specs/014 US2): 카드, 고른 방법 표시, 없으면 칸을 그리지 않음
import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { FeedPost } from '../../explore/exploreApi'
import { server } from '../../test/server'
import RelatedPostsView from './RelatedPosts'
import type { RelatedPosts } from './relatedApi'

function card(postId: number, title: string): FeedPost {
  return {
    postId,
    blogId: 7,
    blogName: '바다 일기',
    nickname: '파도',
    title,
    topic: '여행',
    categoryId: 1,
    categoryName: '미분류',
    createdAt: '2026-10-12T03:00:00Z',
    preview: `${title} 미리보기`,
    commentCount: 0,
    likeCount: 0,
    sheets: 3,
    readMinutes: 1,
    thumbnail: null,
  }
}

function renderFor(postId: number, body: RelatedPosts) {
  server.use(http.get(`/api/v1/posts/${postId}/related`, () => HttpResponse.json(body)))
  const router = createMemoryRouter([{ path: '/', element: <RelatedPostsView postId={postId} /> }])
  return render(<RouterProvider router={router} />)
}

describe('이 글과 비슷한 글', () => {
  it('뜻으로 고른 글은 카드와 "뜻이 비슷한 글"을 보여 준다', async () => {
    renderFor(1, { basis: 'meaning', posts: [card(2, '제주 버스 여행 팁'), card(3, '제주 오름 산책')] })
    const section = await screen.findByRole('region', { name: /이 글과 비슷한 글/ })
    expect(within(section).getByText('뜻이 비슷한 글')).toBeInTheDocument()
    expect(within(section).getAllByRole('listitem')).toHaveLength(2)
    expect(within(section).getByRole('link', { name: '제주 버스 여행 팁' })).toHaveAttribute('href', '/posts/2')
  })

  it('태그로 대신 고른 글은 "태그가 겹치는 글"이라고 적는다', async () => {
    renderFor(4, { basis: 'tags', posts: [card(5, '태그 친구')] })
    expect(await screen.findByText('태그가 겹치는 글')).toBeInTheDocument()
  })

  it('비슷한 글이 없으면 칸을 그리지 않는다', async () => {
    renderFor(6, { basis: 'tags', posts: [] })
    // 응답을 기다린 뒤에도 칸이 없다
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(screen.queryByRole('region', { name: /이 글과 비슷한 글/ })).toBeNull()
  })
})
