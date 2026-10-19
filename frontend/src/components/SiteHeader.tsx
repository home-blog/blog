// 모든 화면 위의 머리글: 마크와 서비스 이름, 글 검색, 공지사항(새 글 점), 로그인 상태, 로그인·가입·로그아웃
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'
import SearchBox from '../explore/SearchBox'
import { useNewCommentCount } from '../manage/newCommentCount'
import { useNoticeAlert } from '../notice/noticeAlert'
import Mailbox from '../subscription/Mailbox'
import ThemeToggle from '../theme/ThemeToggle'
import BrandMark from './BrandMark'
import UserMenu from './UserMenu'
import './site-header.css'

/** 로그인해야 볼 수 있는 화면. 여기서 로그아웃하면 첫 화면으로 보낸다 (FR-031). 기능이 생기면 더한다. */
const MEMBER_ONLY_PREFIXES = ['/manage', '/write', '/mypage', '/me/']

export default function SiteHeader() {
  const { member, logout } = useAuth()
  const { count: newCommentCount } = useNewCommentCount()
  const { hasNew: hasNewNotice } = useNoticeAlert()
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
        <BrandMark />
        글결
      </Link>
      {/* 글 검색은 누구나 (specs/004 US5) */}
      <SearchBox />
      <nav className="site-nav" aria-label="계정">
        {/* 글마당 (specs/017) */}
        <Link to="/board" className="header-notice">
          글마당
        </Link>
        <Link to="/notices" className="header-notice">
          공지
          {hasNewNotice && (
            <>
              <span className="header-notice-dot" aria-hidden="true" />
              <span className="visually-hidden">새 글 있음</span>
            </>
          )}
        </Link>
        <ThemeToggle />
        {member === undefined ? null : member ? (
          <>
            {logoutError && (
              <span className="msg msg-error" role="alert">
                로그아웃하지 못했습니다. 다시 눌러 주세요
              </span>
            )}
            {/* 우편함 (specs/016 US2) */}
            <Mailbox />
            <Link to="/write" className="header-write">
              <span aria-hidden="true">✎</span> 글쓰기
            </Link>
            {/* 내 블로그·블로그 관리·마이페이지·로그아웃은 한 메뉴로 (블로그 관리와 마이페이지는 따로 둔다: specs/006 FR-004) */}
            <UserMenu
              nickname={member.nickname}
              admin={member.admin}
              newCommentCount={newCommentCount}
              onLogout={handleLogout}
            />
          </>
        ) : (
          <>
            <Link to="/login" state={{ from: location.pathname }} className="header-login">
              로그인
            </Link>
            <Link to="/signup" className="header-write">
              회원 가입
            </Link>
          </>
        )}
      </nav>
    </header>
  )
}
