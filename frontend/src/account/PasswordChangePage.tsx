// 비밀번호 변경 (specs/002 US2, T028, FR-013 ~ FR-020)
// - 세 칸을 모두 채워야 버튼이 눌린다. 새 비밀번호 규칙은 가입 화면과 같이 칸 아래에 바로 보여 준다
// - 현재 비밀번호가 틀리면 400이라 로그인 창이 뜨지 않는다. 잠금은 서버 문구(D-3)를 그대로 보여 준다
// - 바꾸면 이 기기는 로그인이 유지되고 다른 기기는 모두 로그아웃된다. AI 비서 열쇠도 모두 끊긴다 (specs/020)
// - 소셜로만 가입해 비밀번호가 없으면 "비밀번호 만들기": 현재 비밀번호 칸이 없다 (specs/010 US4-4, D-4)
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import { checkPassword, isValidPassword, messages } from '../auth/rules'
import { changePassword, getSocialAccounts } from './accountApi'
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
  // undefined: 확인 중. 확인하지 못하면 비밀번호가 있는 것으로 본다 (서버가 마지막에 판단한다)
  const [hasPassword, setHasPassword] = useState<boolean | undefined>(undefined)
  const [created, setCreated] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getSocialAccounts(controller.signal)
      .then((res) => setHasPassword(res.hasPassword))
      .catch(() => {
        if (!controller.signal.aborted) setHasPassword(true)
      })
    return () => controller.abort()
  }, [])

  const creating = hasPassword === false
  const checks = checkPassword(next)
  const filled = (creating || current !== '') && next !== '' && confirm !== ''

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
      await changePassword(creating ? '' : current, next, confirm)
      setCurrent('')
      setNext('')
      setConfirm('')
      setDone(true)
      setCreated(creating) // 완료 문구는 이번 요청의 모드를 따른다
      if (creating) setHasPassword(true) // 다음부터는 현재 비밀번호를 묻는다
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
            // 고친 칸의 오류는 지운다. 다시 낼 때 서버가 새로 판단한다
            setErrors((prev) => ({ ...prev, [id]: undefined }))
            setFormError(null)
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
                <span aria-hidden="true" className="rule-mark">
                  {rule.met ? '✓' : '·'}
                </span>
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
        <h1 className="auth-title">{creating ? '비밀번호 만들기' : '비밀번호 변경'}</h1>
        <p className="auth-lead">
          {creating
            ? '소셜 계정으로 가입해 비밀번호가 없습니다. 만들면 이메일과 비밀번호로도 로그인할 수 있습니다(AI 비서 열쇠는 모두 끊깁니다).'
            : '바꾸면 이 기기는 로그인이 유지되고, 다른 기기에서는 모두 로그아웃됩니다. AI 비서 열쇠도 모두 끊깁니다.'}
        </p>
      </header>

      {hasPassword === undefined ? (
        <p className="mypage-status hint">불러오는 중</p>
      ) : (
        <section className="mypage-card" aria-label={creating ? '비밀번호 만들기' : '비밀번호 변경'}>
          <form className="auth-form" onSubmit={submit} noValidate>
            {!creating && field('currentPassword', '현재 비밀번호', current, setCurrent, 'current-password')}
            {field('newPassword', '새 비밀번호', next, setNext, 'new-password')}
            {field('newPasswordConfirm', '새 비밀번호 확인', confirm, setConfirm, 'new-password')}

            {formError && (
              <p className="msg msg-error form-error" role="alert">
                {formError}
              </p>
            )}
            {done && (
              <p className="msg msg-ok form-error" role="status">
                {created ? '비밀번호를 만들었습니다' : '비밀번호를 변경했습니다'}
              </p>
            )}

            <button type="submit" className="btn btn-primary btn-wide" disabled={busy || !filled}>
              {busy ? (creating ? '만드는 중' : '바꾸는 중') : creating ? '비밀번호 만들기' : '비밀번호 변경'}
            </button>
          </form>
        </section>
      )}
    </div>
  )
}
