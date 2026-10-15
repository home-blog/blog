import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import BlogActivity from './BlogActivity'

const activity = {
  blogId: 7,
  from: '2026-10-11',
  to: '2026-10-15',
  days: [
    { date: '2026-10-12', postCount: 2, sheets: 18 },
    { date: '2026-10-15', postCount: 1, sheets: 3 },
  ],
  totalPosts: 3,
  totalSheets: 21,
}

describe('글쓰기 잔디 키보드 (013 US1, PR #72 리뷰)', () => {
  it('잔디에 초점을 두면 오늘을 읽어 주고, 화살표로 날짜를 옮긴다', async () => {
    server.use(http.get('/api/v1/blogs/7/activity', () => HttpResponse.json(activity)))
    render(<BlogActivity blogId={7} />)
    const grid = await screen.findByRole('group', { name: /지난 1년 글 3개 · 원고지 21매/ })
    expect(screen.getByRole('status')).toHaveTextContent('지난 1년 글 3개 · 원고지 21매')

    await userEvent.tab()
    expect(grid).toHaveFocus()
    expect(screen.getByRole('status')).toHaveTextContent('10월 15일 · 글 1개 · 원고지 3매')

    // 위쪽 화살표는 하루 전, 왼쪽 화살표는 한 주 전
    await userEvent.keyboard('{ArrowUp}{ArrowUp}{ArrowUp}')
    expect(screen.getByRole('status')).toHaveTextContent('10월 12일 · 글 2개 · 원고지 18매')
    await userEvent.keyboard('{ArrowDown}')
    expect(screen.getByRole('status')).toHaveTextContent('10월 13일 · 글 없음')
    await userEvent.keyboard('{Home}')
    expect(screen.getByRole('status')).toHaveTextContent('10월 11일 · 글 없음')
    await userEvent.keyboard('{ArrowUp}')
    expect(screen.getByRole('status')).toHaveTextContent('10월 11일 · 글 없음') // 첫날 앞으로는 가지 않는다
    await userEvent.keyboard('{End}')
    expect(screen.getByRole('status')).toHaveTextContent('10월 15일 · 글 1개 · 원고지 3매')
  })
})
