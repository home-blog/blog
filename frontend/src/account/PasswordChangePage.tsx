// 비밀번호 변경 (specs/002 US2, T028, FR-013 ~ FR-020)
// - 세 칸을 모두 채워야 버튼이 눌린다. 새 비밀번호 규칙은 가입 화면과 같이 칸 아래에 바로 보여 준다
// - 현재 비밀번호가 틀리면 400이라 로그인 창이 뜨지 않는다. 잠금은 서버 문구(D-3)를 그대로 보여 준다
// - 바꾸면 이 기기는 로그인이 유지되고 다른 기기는 모두 로그아웃된다
import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import { checkPassword, isValidPassword, messages } from '../auth/rules'
import { changePassword } from './accountApi'
import '../pages/auth-layout.css'
import '../pages/mypage.css'

type Field = 'currentPassword' | 'newPassword' | 'newPasswordConfirm'
type FieldErrors = Partial<Record<Field, string>>

export default function PasswordChangePage() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)
  const currentRef = useRef<HTMLInputElement>(null)

  const checks = checkPassword(next)
  const filled = current !== '' && next !== '' && confirm !== ''

  function check(): FieldErrors {
    const found: FieldErrors = {}
    if (!isValidPassword(next)) found.newPassword = messages.password
    if (next !== confirm) found.newPasswordConfirm = messages.passwordConfirm
    return found
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy || !filled) return
    setDone(false)
    setFormError(null)
    const found = check()
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setBusy(true)
    try {
      await changePassword(current, next, confirm)
      setCurrent('')
      setNext('')
      setConfirm('')
      setDone(true)
    } catch (err) {
      setCurrent('')
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        setErrors({
          currentPassword: err.messageFor('currentPassword'),
          newPassword: err.messageFor('newPassword'),
          newPasswordConfirm: err.messageFor('newPasswordConfirm'),
        })
      } else if (err instanceof ApiError) {
        setFormError(err.message) // 잠금(423)은 남은 시간이 든 서버 문구 그대로
      } else {
        setFormError('※ 잠시 뒤 다시 시도해 주세요')
      }
      currentRef.current?.focus()
    } finally {
      setBusy(false)
    }
  }

  function field(id: Field, label: string, value: string, set: (v: string) => void, autoComplete: string) {
    const error = errors[id]
    return (
      <div className="field">
        <label htmlFor={`pw-${id}`}>{label}</label>
        <input
          id={`pw-${id}`}
          ref={id === 'currentPassword' ? currentRef : undefined}
          type="password"
          autoComplete={autoComplete}
          value={value}
          disabled={busy}
          onChange={(e) => {
            set(e.target.value)
            setDone(false)
          }}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `pw-${id}-error` : id === 'newPassword' ? 'pw-rules' : undefined}
        />
        {error && (
          <p id={`pw-${id}-error`} className="msg msg-error" role="alert">
            {error}
          </p>
        )}
        {id === 'newPassword' && (
          <ul id="pw-rules" className="rules" aria-label="비밀번호 규칙">
            {checks.map((rule) => (
              <li key={rule.label} className={rule.met ? 'rule rule-met' : 'rule'}>
                <span aria-hidden="true" className="rule-mark">{rule.met ? '✓' : '·'}</span>
                {rule.label}
                <span className="visually-hidden">{rule.met ? ' 충족' : ' 아직'}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    )
  }

  return (
    <div className="mypage">
      <header className="mypage-head">
        <p className="mypage-back">
          <Link to="/mypage">← 마이페이지</Link>
        </p>
        <h1 className="auth-title">비밀번호 변경</h1>
        <p className="auth-lead">바꾸면 이 기기는 로그인이 유지되고, 다른 기기에서는 모두 로그아웃됩니다.</p>
      </header>

      <section className="mypage-card" aria-label="비밀번호 변경">
        <form className="auth-form" onSubmit={submit} noValidate>
          {field('currentPassword', '현재 비밀번호', current, setCurrent, 'current-password')}
          {field('newPassword', '새 비밀번호', next, setNext, 'new-password')}
          {field('newPasswordConfirm', '새 비밀번호 확인', confirm, setConfirm, 'new-password')}

          {formError && (
            <p className="msg msg-error form-error" role="alert">
              {formError}
            </p>
          )}
          {done && (
            <p className="msg msg-ok form-error" role="status">
              비밀번호를 변경했습니다
            </p>
          )}

          <button type="submit" className="btn btn-primary btn-wide" disabled={busy || !filled}>
            {busy ? '바꾸는 중' : '비밀번호 변경'}
          </button>
        </form>
      </section>
    </div>
  )
}
