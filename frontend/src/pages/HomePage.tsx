// 첫 화면: 가운데 큰 제목 하나와 주 동작(다크 알약) + 보조 동작(밑줄 링크) — docs/DESIGN.md Hero Headline
// 블로그 글 목록·검색은 specs/004(블로그 화면, /search), 주제별 화면은 개인 기능이라 아직 명세가 없다.
import { Link, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'
import './home.css'

export default function HomePage() {
  const { member } = useAuth()
  // 탈퇴한 직후에는 서버가 준 "탈퇴가 완료되었습니다"를 보여 준다 (specs/002 FR-027)
  const withdrawn = (useLocation().state as { withdrawn?: string } | null)?.withdrawn
  return (
    <div className="home">
      {withdrawn && (
        <p className="home-notice msg msg-ok" role="status">
          {withdrawn}
        </p>
      )}
      <section className="hero">
        <h1 className="hero-headline">{member ? `${member.nickname}님, 어서 오세요.` : '오늘은 무엇을 쓸까요.'}</h1>
        {member ? (
          <>
            <p className="hero-lead">{member.nickname}의 블로그에 오늘의 글을 남겨 보세요.</p>
            <p className="hero-actions">
              <Link to="/write" className="btn btn-primary">
                글쓰기
              </Link>
              <Link to="/me/blog" className="text-link">
                내 블로그 보기
              </Link>
            </p>
          </>
        ) : (
          <>
            <p className="hero-lead">가입하면 내 블로그가 바로 생깁니다.</p>
            <p className="hero-actions">
              <Link to="/signup" className="btn btn-primary">
                회원 가입
              </Link>
              <Link to="/login" className="text-link">
                로그인
              </Link>
            </p>
          </>
        )}
      </section>
    </div>
  )
}
