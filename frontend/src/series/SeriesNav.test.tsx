import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { SeriesFooter, SeriesHead } from './SeriesNav'
import type { PostSeries } from './seriesApi'

const series: PostSeries = {
  seriesId: 3,
  name: '제주 한 달 살기',
  episode: 2,
  total: 3,
  previous: { postId: 41, title: '첫날' },
  next: { postId: 45, title: '셋째 날' },
  posts: [
    { postId: 41, title: '첫날', episode: 1 },
    { postId: 43, title: '둘째 날', episode: 2 },
    { postId: 45, title: '셋째 날', episode: 3 },
  ],
}

function show(element: React.ReactElement) {
  const router = createMemoryRouter([{ path: '/', element }])
  return render(<RouterProvider router={router} />)
}

describe('글 읽기의 연재 표시 (013 US2-4)', () => {
  it('머리에 "연재 이름 N화"와 연재 링크', () => {
    show(<SeriesHead series={series} />)
    expect(screen.getByText('제주 한 달 살기')).toHaveAttribute('href', '/series/3')
    expect(screen.getByText('2화')).toBeInTheDocument()
    expect(screen.getByText('/ 3화')).toBeInTheDocument()
  })

  it('바닥에 이전 화·다음 화 링크', () => {
    show(<SeriesFooter series={series} currentPostId={43} />)
    expect(screen.getByRole('link', { name: /이전 화.*첫날/ })).toHaveAttribute('href', '/posts/41')
    expect(screen.getByRole('link', { name: /다음 화.*셋째 날/ })).toHaveAttribute('href', '/posts/45')
  })

  it('첫 화에는 이전 화가, 끝 화에는 다음 화가 없다', () => {
    show(<SeriesFooter series={{ ...series, episode: 1, previous: null }} currentPostId={41} />)
    expect(screen.queryByRole('link', { name: /이전 화/ })).toBeNull()
    expect(screen.getByRole('link', { name: /다음 화/ })).toBeInTheDocument()
  })

  it('전체 목록은 접혀 있다가 펼치면 지금 화를 표시한다', async () => {
    show(<SeriesFooter series={series} currentPostId={43} />)
    expect(screen.queryByRole('link', { name: /1화 첫날/ })).toBeNull()
    await userEvent.click(screen.getByRole('button', { name: '연재 전체 보기 (3화)' }))
    expect(screen.getByRole('link', { name: /1화 첫날/ })).toHaveAttribute('href', '/posts/41')
    expect(screen.getByText(/둘째 날/).closest('li')).toHaveAttribute('aria-current', 'page')
  })
})
