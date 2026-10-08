// 회원 탈퇴 (specs/002 US3, T036, FR-022 ~ FR-027)
// - 되돌릴 수 없다는 것, 지워지는 것, 남는 것을 먼저 보여 준다. 안내를 확인(체크)해야 버튼이 눌린다
// - 누르면 한 번 더 묻는다. 취소하면 요청을 보내지 않는다
// - 성공하면 서버가 이미 로그아웃시켰으므로 화면만 로그아웃 상태로 바꾸고 첫 화면으로 간다
import { useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { withdraw } from './accountApi'
import '../pages/auth-layout.css'
import '../pages/mypage.css'

export default function WithdrawalPage() {
  const { signedOut } = useAuth()
  const navigate = useNavigate()
  const [agreed, setAgreed] = useState(false)
  const [password, setPassword] = useState('')
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)

  const ready = agreed && password !== '' && !busy

  function askConfirm(e: FormEvent) {
    e.preventDefault()
    if (!ready) return
    setPasswordError(null)
    setFormError(null)
    dialogRef.current?.showModal()
  }

  async function confirmWithdraw() {
    dialogRef.current?.close()
    setBusy(true)
    try {
      const res = await withdraw(password, agreed)
      signedOut()
      navigate('/', { replace: true, state: { withdrawn: res.message } })
    } catch (err) {
      setPassword('')
      if (err instanceof ApiError && err.messageFor('password')) {
        setPasswordError(err.messageFor('password') ?? null)
      } else if (err instanceof ApiError) {
        setFormError(err.message) // 잠금(423)은 남은 시간이 든 서버 문구 그대로
      } else {
        setFormError('※ 잠시 뒤 다시 시도해 주세요')
      }
      passwordRef.current?.focus()
      setBusy(false)
    }
  }

  return (
    <div className="mypage">
      <header className="mypage-head">
        <p className="mypage-back">
          <Link to="/mypage">← 마이페이지</Link>
        </p>
        <h1 className="auth-title">회원 탈퇴</h1>
        <p className="auth-lead">탈퇴하면 되돌릴 수 없습니다. 아래 내용을 확인해 주세요.</p>
      </header>

      <section className="mypage-card withdrawal-notice" aria-labelledby="notice-title">
        <h2 id="notice-title" className="mypage-card-title">탈퇴하면</h2>
        <div className="withdrawal-lists">
          <div>
            <h3 className="withdrawal-subtitle">지워지는 것</h3>
            <ul>
              <li>내 블로그와 블로그의 모든 글</li>
              <li>내 블로그의 분류</li>
              <li>내 블로그 글에 달린 댓글과 좋아요</li>
              <li>내가 누른 좋아요</li>
              <li>내 계정 정보(이메일, 닉네임, 소개)</li>
            </ul>
          </div>
          <div>
            <h3 className="withdrawal-subtitle">남는 것</h3>
            <ul>
              <li>다른 사람 글에 내가 단 댓글 — 작성자는 "탈퇴한 사용자"로 보입니다</li>
            </ul>
          </div>
        </div>
        <p className="hint">탈퇴한 뒤 같은 이메일로 다시 가입할 수 있지만, 지워진 블로그와 글은 돌아오지 않습니다.</p>
      </section>

      <section className="mypage-card" aria-label="탈퇴하기">
        <form className="auth-form" onSubmit={askConfirm} noValidate>
          <label className="withdrawal-agree">
            <input type="checkbox" checked={agreed} disabled={busy} onChange={(e) => setAgreed(e.target.checked)} />
            위 내용을 확인했고, 되돌릴 수 없다는 것을 이해했습니다
          </label>
          <div className="field">
            <label htmlFor="withdrawal-password">비밀번호</label>
            <input
              id="withdrawal-password"
              ref={passwordRef}
              type="password"
              autoComplete="current-password"
              value={password}
              disabled={busy}
              onChange={(e) => {
                setPassword(e.target.value)
                // 고친 칸의 오류는 지운다. 다시 낼 때 서버가 새로 판단한다
                setPasswordError(null)
                setFormError(null)
              }}
              aria-invalid={Boolean(passwordError)}
              aria-describedby={passwordError ? 'withdrawal-password-error' : undefined}
            />
            {passwordError && (
              <p id="withdrawal-password-error" className="msg msg-error" role="alert">
                {passwordError}
              </p>
            )}
          </div>
          {formError && (
            <p className="msg msg-error form-error" role="alert">
              {formError}
            </p>
          )}
          <button type="submit" className="btn btn-danger btn-wide" disabled={!ready}>
            {busy ? '탈퇴하는 중' : '탈퇴하기'}
          </button>
        </form>
      </section>

      <dialog ref={dialogRef} className="confirm-dialog" aria-labelledby="confirm-title">
        <h2 id="confirm-title" className="confirm-title">정말 탈퇴하시겠습니까?</h2>
        <p>이 작업은 되돌릴 수 없습니다.</p>
        <div className="confirm-actions">
          <button type="button" className="btn btn-quiet" onClick={() => dialogRef.current?.close()}>
            취소
          </button>
          <button type="button" className="btn btn-danger" onClick={confirmWithdraw}>
            탈퇴하기
          </button>
        </div>
      </dialog>
    </div>
  )
}
