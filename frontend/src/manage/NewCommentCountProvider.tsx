// 새 댓글 수 (specs/006 T036, FR-028, BM-05-7): 로그인한 회원이 화면을 열면 한 번 묻는다. 계속 다시 묻지 않는다
// 종 모양 알림 목록·메일·푸시는 만들지 않는다 (BM-05-7 확인 필요, 원본대로 숫자까지)
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useAuth } from '../auth/useAuth'
import { getNewCommentCount } from './manageApi'
import { NewCommentCountContext } from './newCommentCount'

export default function NewCommentCountProvider({ children }: { children: ReactNode }) {
  const { member } = useAuth()
  const memberId = member?.id ?? null
  const [state, setState] = useState<{ memberId: number; count: number } | null>(null)

  useEffect(() => {
    if (memberId === null) return
    const controller = new AbortController()
    getNewCommentCount(controller.signal)
      .then((count) => setState({ memberId, count }))
      .catch(() => {
        // 숫자를 못 받아도 화면은 그대로 쓴다 (숫자만 안 보임)
      })
    return () => controller.abort()
  }, [memberId])

  const setCount = useCallback(
    (count: number) => {
      if (memberId !== null) setState({ memberId, count })
    },
    [memberId],
  )

  const value = useMemo(
    () => ({
      // 로그아웃했거나 다른 회원으로 바뀌었으면 이전 숫자를 보이지 않는다
      count: state !== null && state.memberId === memberId ? state.count : null,
      setCount,
    }),
    [state, memberId, setCount],
  )

  return <NewCommentCountContext.Provider value={value}>{children}</NewCommentCountContext.Provider>
}
