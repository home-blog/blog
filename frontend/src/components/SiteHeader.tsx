// 모든 화면 위의 머리글: 서비스 이름, 로그인 상태, 로그인·가입·로그아웃
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'
import './site-header.css'

/** 로그인해야 볼 수 있는 화면. 여기서 로그아웃하면 첫 화면으로 보낸다 (FR-031). 기능이 생기면 더한다. */
const MEMBER_ONLY_PREFIXES = ['/manage', '/write', '/me']

export default function SiteHeader() {
  const { member, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [logoutError, setLogoutError] = useState(false)

  async function handleLogout() {
    setLogoutError(false)
    try {
      await logout()
    } catch {
      setLogoutError(true) // 서버의 로그인 상태가 남아 있다
      return
    }
    if (MEMBER_ONLY_PREFIXES.some((p) => location.pathname.startsWith(p))) navigate('/', { replace: true })
  }

  return (
    <header className="site-header">
      <Link to="/" className="site-brand">
        MyBlog
      </Link>
      <nav className="site-nav" aria-label="계정">
        {member === undefined ? null : member ? (
          <>
            {logoutError && (
              <span className="msg msg-error" role="alert">
                로그아웃하지 못했습니다. 다시 눌러 주세요
              </span>
            )}
            <span className="site-member">{member.nickname}님</span>
            <button type="button" className="btn btn-quiet btn-small" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" state={{ from: location.pathname }} className="site-link">
              로그인
            </Link>
            <Link to="/signup" className="btn btn-primary btn-small">
              회원 가입
            </Link>
          </>
        )}
      </nav>
    </header>
  )
}
