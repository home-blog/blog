// 마이페이지 (specs/002 US1, T017): 내 정보 보기, 내 정보 수정, 비밀번호 변경·탈퇴로 가는 길
// 주소에 회원 번호가 없다. 서버가 세션의 회원만 돌려준다 (FR-002)
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import { getAccount, type Account } from '../account/accountApi'
import ProfileForm from '../account/ProfileForm'
import './auth-layout.css'
import './mypage.css'

function joinedDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

export default function MyPage() {
  const [account, setAccount] = useState<Account | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getAccount(controller.signal)
      .then(setAccount)
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        // 401이면 client.ts가 로그인 창을 띄운다
        setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  if (error) {
    return (
      <p className="mypage-status msg msg-error" role="alert">
        {error}
      </p>
    )
  }
  if (!account) {
    return <p className="mypage-status hint">불러오는 중</p>
  }

  return (
    <div className="mypage">
      <header className="mypage-head">
        <h1 className="auth-title">마이페이지</h1>
        <p className="auth-lead">내 정보를 보고 닉네임과 소개를 고칩니다.</p>
      </header>

      <section className="mypage-card" aria-labelledby="info-title">
        <h2 id="info-title" className="mypage-card-title">내 정보</h2>
        <dl className="mypage-info">
          <div>
            <dt>이메일</dt>
            <dd>{account.email}</dd>
          </div>
          <div>
            <dt>닉네임</dt>
            <dd>{account.nickname}</dd>
          </div>
          <div>
            <dt>소개</dt>
            <dd className="mypage-intro">{account.intro || <span className="hint">아직 소개가 없습니다</span>}</dd>
          </div>
          <div>
            <dt>가입일</dt>
            <dd>{joinedDate(account.joinedAt)}</dd>
          </div>
        </dl>
        {account.blog && (
          <Link to={`/blog/${account.blog.id}`} className="btn btn-outline btn-small mypage-blog">
            내 블로그
          </Link>
        )}
      </section>

      <section className="mypage-card" aria-labelledby="profile-title">
        <h2 id="profile-title" className="mypage-card-title">내 정보 수정</h2>
        <p className="hint mypage-note">이메일은 바꿀 수 없습니다.</p>
        <ProfileForm
          nickname={account.nickname}
          intro={account.intro}
          onSaved={(nickname, intro) => setAccount({ ...account, nickname, intro })}
        />
      </section>

      <section className="mypage-card" aria-labelledby="security-title">
        <h2 id="security-title" className="mypage-card-title">계정</h2>
        <ul className="mypage-links">
          <li>
            <Link to="/mypage/password" className="mypage-link">
              비밀번호 변경
            </Link>
          </li>
          <li>
            <span className="mypage-soon">회원 탈퇴</span> <span className="hint">다음 단계에서 열립니다</span>
          </li>
        </ul>
      </section>
    </div>
  )
}
