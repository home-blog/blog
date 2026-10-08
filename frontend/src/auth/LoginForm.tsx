// 로그인 입력 (로그인 화면과 로그인 창이 함께 쓴다)
import { useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from './useAuth'

interface Props {
  onLoggedIn: () => void
  /** 같은 화면에 두 개가 있어도 칸 id가 겹치지 않게 */
  idPrefix: string
}

export default function LoginForm({ onLoggedIn, idPrefix }: Props) {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const emailRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy) return
    setError(null)
    if (!email.trim() || !password) {
      setError('이메일과 비밀번호를 입력해 주세요')
      ;(email.trim() ? passwordRef : emailRef).current?.focus()
      return
    }
    setBusy(true)
    try {
      await login(email.trim(), password)
      onLoggedIn()
    } catch (err) {
      setPassword('')
      if (err instanceof ApiError && err.code === 'ACCOUNT_LOCKED' && err.retryAfterSeconds !== undefined) {
        // {N}은 남은 시간을 올림한 분 (T033)
        const minutes = Math.max(1, Math.ceil(err.retryAfterSeconds / 60))
        setError(err.message.replace(/\d+분 뒤에/, `${minutes}분 뒤에`))
      } else if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('※ 잠시 뒤 다시 시도해 주세요')
      }
      passwordRef.current?.focus()
    } finally {
      setBusy(false)
    }
  }

  const errorId = `${idPrefix}-error`
  return (
    <form className="auth-form" onSubmit={submit} noValidate>
      <div className="field">
        <label htmlFor={`${idPrefix}-email`}>이메일</label>
        <input
          id={`${idPrefix}-email`}
          ref={emailRef}
          type="email"
          inputMode="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
        />
      </div>
      <div className="field">
        <label htmlFor={`${idPrefix}-password`}>비밀번호</label>
        <input
          id={`${idPrefix}-password`}
          ref={passwordRef}
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
        />
      </div>

      {error && (
        <p id={errorId} className="msg msg-error" role="alert">
          {error}
        </p>
      )}

      <button type="submit" className="btn btn-primary btn-wide" disabled={busy}>
        {busy ? '로그인하는 중' : '로그인'}
      </button>
    </form>
  )
}
