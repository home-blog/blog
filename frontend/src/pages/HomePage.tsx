// 첫 화면. 지금은 로그인 상태만 보여 준다. 블로그 글 목록·검색은 specs/004(블로그 화면, /search),
// 주제별 화면은 개인 기능이라 아직 명세가 없다.
import { Link, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'
import ManuscriptSheet from './ManuscriptSheet'
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
      <ManuscriptSheet lines={['', member ? `${member.nickname}님,` : '오늘은', member ? '어서 오세요.' : '무엇을 쓸까요.']} rows={4} />
      {member ? (
        <p className="home-lead">
          <strong>{member.nickname}의 블로그</strong>가 만들어져 있습니다. 글쓰기와 블로그 관리는 다음 단계에서 열립니다.
        </p>
      ) : (
        <p className="home-lead">
          가입하면 내 블로그가 바로 생깁니다. <Link to="/signup">회원 가입</Link> 또는 <Link to="/login">로그인</Link>
        </p>
      )}
    </div>
  )
}
