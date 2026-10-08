// 저장하지 않은 내용 확인 (specs/002 FR-011, SC-009, research R-5)
// - 화면 안 이동(링크, 뒤로 가기): useBlocker로 막고 확인 창으로 묻는다
// - 탭 닫기·새로고침: beforeunload로 브라우저가 묻는다 (문구는 브라우저 것)
import { useEffect } from 'react'
import { useBlocker } from 'react-router'
import { messages } from '../auth/rules'

export function useUnsavedChangesPrompt(dirty: boolean): void {
  const blocker = useBlocker(({ currentLocation, nextLocation }) => dirty && currentLocation.pathname !== nextLocation.pathname)

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    if (window.confirm(messages.unsavedChanges)) blocker.proceed()
    else blocker.reset()
  }, [blocker])

  useEffect(() => {
    if (!dirty) return
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault()
    }
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [dirty])
}
