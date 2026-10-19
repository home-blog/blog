// 첫 화면 맨 위 카드: 오늘의 이슈(specs/015 US2)는 받은 순서대로 1위를 크게, 2·3위를 작게
import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { FeedPost } from './exploreApi'
import FeaturedPost from './FeaturedPost'

function card(postId: number, title: string, thumbnail: string | null = null): FeedPost {
  return {
    postId,
    blogId: postId,
    blogName: `블로그 ${postId}`,
    nickname: '글쓴이',
    title,
    topic: '여행',
    categoryId: 1,
    categoryName: '미분류',
    createdAt: '2026-10-19T03:00:00Z',
    preview: `${title} 미리보기`,
    commentCount: 0,
    likeCount: 0,
    sheets: 2,
    readMinutes: 1,
    thumbnail,
  }
}

function renderWith(element: React.ReactElement) {
  const router = createMemoryRouter([{ path: '/', element }])
  return render(<RouterProvider router={router} />)
}

describe('첫 화면 맨 위 카드', () => {
  it('오늘의 이슈는 사진이 없어도 1위를 크게, 2·3위에 순위 칸을 붙인다', () => {
    renderWith(
      <FeaturedPost
        posts={[card(1, '가장 많이 읽힌 글'), card(2, '두 번째', '/img/2.png'), card(3, '세 번째')]}
        label="오늘의 이슈"
        ranked
      />,
    )
    const section = screen.getByRole('region', { name: '오늘의 이슈' })
    expect(within(section).getByRole('heading', { name: '가장 많이 읽힌 글' })).toBeInTheDocument()
    expect(within(section).getByText('1위')).toBeInTheDocument()
    expect(within(section).getByText('2위')).toBeInTheDocument()
    expect(within(section).getByText('3위')).toBeInTheDocument()
  })

  it('이번 주 인기 글은 사진이 있는 글을 크게 놓는다', () => {
    renderWith(<FeaturedPost posts={[card(1, '사진 없는 1위'), card(2, '사진 있는 2위', '/img/2.png')]} />)
    const section = screen.getByRole('region', { name: '이번 주 눈여겨볼 글' })
    expect(within(section).getByRole('heading', { name: '사진 있는 2위' })).toBeInTheDocument()
    expect(within(section).queryByText('1위')).toBeNull()
  })
})
