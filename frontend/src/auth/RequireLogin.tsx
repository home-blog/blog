// 회원 전용 화면 감싸개 (specs/001 US4, T034). 로그인하지 않았으면 로그인 창을 띄우고 내용은 보여 주지 않는다.
import { useEffect, type ReactNode } from 'react'
import { useLoginPrompt } from './loginPrompt'
import { useAuth } from './useAuth'

export default function RequireLogin({ children }: { children: ReactNode }) {
  const { member } = useAuth()
  const { open } = useLoginPrompt()

  useEffect(() => {
    if (member === null) open()
  }, [member, open])

  if (!member) {
    return member === undefined ? null : <p className="require-login">로그인한 회원만 볼 수 있는 화면입니다.</p>
  }
  return <>{children}</>
}
