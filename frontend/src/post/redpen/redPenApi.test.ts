// 빨간 펜 요청 (specs/014 contracts 1): 화면의 동의 여부를 같이 보내 서버가 바깥 AI로 보내기 전에 확인한다
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { proofread } from './redPenApi'

describe('빨간 펜 요청', () => {
  it('본문과 함께 AI 보내기 동의 여부를 보낸다', async () => {
    let body: unknown = null
    server.use(
      http.post('/api/v1/me/proofread', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ corrections: [], remaining: 20, cached: false })
      }),
    )

    await proofread('됬다', false)
    expect(body).toEqual({ content: '됬다', aiConsent: false })

    await proofread('됬다', true)
    expect(body).toEqual({ content: '됬다', aiConsent: true })
  })
})
