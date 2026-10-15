// 모든 화면 테스트 앞에 한 번 돈다: DOM 검사 도구(toBeInTheDocument 등)와 가짜 서버(MSW)
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

// 테스트가 정하지 않은 요청이 나가면 실패시킨다 (모르는 서버 호출을 놓치지 않게)
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
})
afterAll(() => server.close())
