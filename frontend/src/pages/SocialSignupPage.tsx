// 소셜 가입 (specs/010 US2, contracts 3·4): 회사 화면을 다녀온 처음 사용자. 가입 화면을 소셜 모드로 쓴다
// 가입이 끝나면 바로 로그인된 상태로 첫 화면에 간다 (D-3)
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError, resetCsrfToken } from '../api/client'
import { getPending, type SocialPending } from '../auth/socialApi'
import { useAuth } from '../auth/useAuth'
import SignupPage from './SignupPage'
import './auth-layout.css'

export default function SocialSignupPage() {
  const navigate = useNavigate()
  const { refresh } = useAuth()
  const [pending, setPending] = useState<SocialPending | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getPending(controller.signal)
      .then(setPending)
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  async function signedUp() {
    resetCsrfToken() // 로그인으로 세션이 바뀌었다
    await refresh().catch(() => undefined)
    navigate('/', { replace: true })
  }

  if (error) {
    return (
      <div className="auth-layout auth-layout-single">
        <section className="auth-form-wrap">
          <h1 className="auth-title">소셜 계정으로 가입</h1>
          <p className="msg msg-error" role="alert">
            {error}
          </p>
          <p className="auth-switch">
            <Link to="/login">로그인 화면으로</Link>
          </p>
        </section>
      </div>
    )
  }
  if (!pending) return <p className="hint auth-loading">불러오는 중</p>
  return <SignupPage social={pending} onSignedUp={signedUp} />
}
