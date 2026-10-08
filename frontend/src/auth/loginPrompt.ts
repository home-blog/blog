// "로그인 창 띄우기" 상태 (specs/001 US4). LoginModal이 값을 채운다.
import { createContext, useContext } from 'react'

export interface LoginPrompt {
  /** 로그인 창을 띄운다. 로그인하면 returnTo(없으면 지금 화면)로 간다 */
  open: (returnTo?: string) => void
}

export const LoginPromptContext = createContext<LoginPrompt>({ open: () => {} })

export function useLoginPrompt(): LoginPrompt {
  return useContext(LoginPromptContext)
}
