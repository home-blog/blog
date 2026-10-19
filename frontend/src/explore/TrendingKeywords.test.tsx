// 지금 많이 찾는 말 (specs/015 US1): 순위·변화 표시, 검색 링크, 비었거나 실패하면 칸을 그리지 않음
import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import type { Trend } from './exploreApi'
import TrendingKeywords from './TrendingKeywords'

function renderWith(response: Parameters<typeof HttpResponse.json>[0], status = 200) {
  server.use(http.get('/api/v1/search/trending', () => HttpResponse.json(response, { status })))
  const router = createMemoryRouter([{ path: '/', element: <TrendingKeywords /> }])
  return render(<RouterProvider router={router} />)
}

describe('지금 많이 찾는 말', () => {
  it('순위와 변화(▲ ▼ NEW)를 보여 주고 검색어는 검색 결과로 이어진다', async () => {
    const items: Trend[] = [
      { rank: 1, keyword: '제주 여행', change: 'up', delta: 2 },
      { rank: 2, keyword: 'spring boot', change: 'down', delta: 1 },
      { rank: 3, keyword: '김장', change: 'new', delta: 0 },
      { rank: 4, keyword: '러닝', change: 'same', delta: 0 },
    ]
    renderWith({ items })
    const section = await screen.findByRole('region', { name: '지금 많이 찾는 말' })
    const rows = within(section).getAllByRole('listitem')
    expect(rows).toHaveLength(4)
    expect(within(rows[0]).getByLabelText('2칸 올라감')).toHaveTextContent('▲2')
    expect(within(rows[1]).getByLabelText('1칸 내려감')).toHaveTextContent('▼1')
    expect(within(rows[2]).getByText('NEW')).toBeInTheDocument()
    expect(within(rows[3]).getByLabelText('그대로')).toBeInTheDocument()
    expect(within(section).getByRole('link', { name: '제주 여행' })).toHaveAttribute(
      'href',
      '/search?q=%EC%A0%9C%EC%A3%BC+%EC%97%AC%ED%96%89',
    )
  })

  it('모인 검색어가 없으면 칸을 그리지 않는다', async () => {
    const { container } = renderWith({ items: [] })
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(container.querySelector('.trending')).toBeNull()
  })

  it('불러오지 못해도 화면을 깨뜨리지 않고 칸만 그리지 않는다', async () => {
    const { container } = renderWith({ code: 'INTERNAL', message: '잠시 뒤' }, 500)
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(container.querySelector('.trending')).toBeNull()
  })
})
