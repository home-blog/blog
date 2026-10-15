// 새 공지 점 (specs/012 US3, S3-1 ~ S3-3)
import { act, render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { server } from '../test/server'
import NoticeAlertProvider from './NoticeAlertProvider'
import { isNewer, SEEN_KEY, useNoticeAlert } from './noticeAlert'

const LATEST = '2026-10-15T09:00:00Z'

function Probe() {
  const { hasNew, markSeen } = useNoticeAlert()
  return (
    <button type="button" onClick={markSeen}>
      {hasNew ? '새 글 있음' : '새 글 없음'}
    </button>
  )
}

function renderWithLatest(latestAt: string | null) {
  server.use(http.get('/api/v1/notices/latest', () => HttpResponse.json({ latestAt })))
  render(
    <NoticeAlertProvider>
      <Probe />
    </NoticeAlertProvider>,
  )
}

beforeEach(() => localStorage.clear())

describe('새 공지 점', () => {
  it('S3-3 처음 온 기기에는 점이 없고, 지금까지의 글을 본 것으로 적어 둔다', async () => {
    renderWithLatest(LATEST)
    await act(() => Promise.resolve())
    expect(await screen.findByText('새 글 없음')).toBeInTheDocument()
    await vi_waitForStorage()
    expect(localStorage.getItem(SEEN_KEY)).toBe(LATEST)
  })

  it('S3-1 마지막으로 본 뒤 새 글이 있으면 점', async () => {
    localStorage.setItem(SEEN_KEY, '2026-10-14T00:00:00Z')
    renderWithLatest(LATEST)
    expect(await screen.findByText('새 글 있음')).toBeInTheDocument()
  })

  it('경계: 본 시각과 같으면 점이 없다', async () => {
    localStorage.setItem(SEEN_KEY, LATEST)
    renderWithLatest(LATEST)
    await act(() => Promise.resolve())
    expect(screen.getByText('새 글 없음')).toBeInTheDocument()
  })

  it('S3-2 목록을 보면(markSeen) 점이 사라지고 본 시각이 바뀐다', async () => {
    localStorage.setItem(SEEN_KEY, '2026-10-14T00:00:00Z')
    renderWithLatest(LATEST)
    const button = await screen.findByText('새 글 있음')
    act(() => button.click())
    expect(screen.getByText('새 글 없음')).toBeInTheDocument()
    expect(localStorage.getItem(SEEN_KEY)).toBe(LATEST)
  })
})

describe('isNewer', () => {
  it.each([
    [LATEST, '2026-10-14T00:00:00Z', true],
    [LATEST, LATEST, false],
    [null, LATEST, false],
    [LATEST, null, false],
    [LATEST, undefined, false],
  ])('%s, %s → %s', (latest, seen, expected) => expect(isNewer(latest, seen)).toBe(expected))
})

/** 저장소 쓰기는 서버 답을 받은 뒤라 한 번 더 기다린다 */
async function vi_waitForStorage() {
  for (let i = 0; i < 20 && localStorage.getItem(SEEN_KEY) === null; i++) {
    await act(() => new Promise((resolve) => setTimeout(resolve, 5)))
  }
}
