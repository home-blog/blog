// 로그인 창 (specs/001 US4, T034, FR-033)
// - 서버가 401 UNAUTHENTICATED를 주면(client.ts의 "로그인 필요" 신호) 하려던 화면을 기억하고 로그인 창을 띄운다.
// - 로그인하면 기억한 화면으로 간다. 회원 전용 화면은 RequireLogin이 같은 창을 띄운다.
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { onUnauthenticated } from '../api/client'
import LoginForm from './LoginForm'
import { LoginPromptContext } from './loginPrompt'
import './login-modal.css'

export function LoginModalProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const [returnTo, setReturnTo] = useState<string | null>(null)
  const dialogRef = useRef<HTMLDialogElement>(null)
  const here = location.pathname + location.search

  const open = useCallback((target?: string) => setReturnTo(target ?? here), [here])

  // 어떤 요청이든 401을 받으면 로그인 창 (로그인 화면에서는 띄우지 않는다)
  useEffect(
    () =>
      onUnauthenticated(() => {
        if (!location.pathname.startsWith('/login')) open()
      }),
    [open, location.pathname],
  )

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (returnTo !== null && !dialog.open) dialog.showModal()
    if (returnTo === null && dialog.open) dialog.close()
  }, [returnTo])

  const close = useCallback(() => setReturnTo(null), [])
  const value = useMemo(() => ({ open }), [open])

  return (
    <LoginPromptContext.Provider value={value}>
      {children}
      <dialog ref={dialogRef} className="login-modal" aria-labelledby="login-modal-title" onClose={close}>
        {returnTo !== null && (
          <div className="login-modal-body">
            <h2 id="login-modal-title" className="auth-title">로그인이 필요합니다</h2>
            <p className="auth-lead">로그인하면 보던 화면으로 돌아갑니다.</p>
            <LoginForm
              idPrefix="login-modal"
              onLoggedIn={() => {
                const target = returnTo
                close()
                if (target !== here) navigate(target)
              }}
            />
            <div className="login-modal-actions">
              <Link to="/signup" onClick={close}>
                회원 가입
              </Link>
              <button type="button" className="btn btn-quiet btn-small" onClick={close}>
                닫기
              </button>
            </div>
          </div>
        )}
      </dialog>
    </LoginPromptContext.Provider>
  )
}
