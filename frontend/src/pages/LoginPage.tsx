// 로그인 화면 (specs/001 US2·US3, T030, T033)
import { Link, useLocation, useNavigate } from 'react-router'
import LoginForm from '../auth/LoginForm'
import './auth-layout.css'

interface LocationState {
  from?: string
  signedUp?: boolean
}

/** 로그인 뒤 갈 곳: 이전 화면, 로그인·가입 화면에서 바로 왔으면 첫 화면 (FR-025) */
function destination(state: LocationState | null): string {
  const from = state?.from
  if (!from || from.startsWith('/login') || from.startsWith('/signup')) return '/'
  return from
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LocationState | null

  return (
    <div className="auth-layout">
      <section className="auth-sheet hero" aria-hidden="true">
        <p className="hero-headline">다시 만나서 반갑습니다.</p>
      </section>

      <section className="auth-form-wrap" aria-labelledby="login-title">
        <h1 id="login-title" className="auth-title">로그인</h1>
        {state?.signedUp ? (
          <p className="auth-lead msg-ok" role="status">가입이 완료되었습니다. 로그인해 주세요</p>
        ) : (
          <p className="auth-lead">가입한 이메일과 비밀번호로 로그인합니다.</p>
        )}
        <LoginForm idPrefix="login" onLoggedIn={() => navigate(destination(state), { replace: true })} />
        <p className="auth-switch">
          아직 회원이 아니라면 <Link to="/signup">회원 가입</Link>
        </p>
      </section>
    </div>
  )
}
