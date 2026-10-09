// 가입 화면 (specs/001 US1, T025): 인증번호 받기 → 확인 → 이메일 잠금 → 비밀번호 → 가입하기
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { api, ApiError } from '../api/client'
import {
  checkPassword,
  isValidEmail,
  isValidNickname,
  isValidPassword,
  messages,
} from '../auth/rules'
import './auth-layout.css'

type Field = 'nickname' | 'email' | 'code' | 'password' | 'passwordConfirm'
type Phase = 'idle' | 'sent' | 'verified'

const FIELD_ORDER: Field[] = ['nickname', 'email', 'code', 'password', 'passwordConfirm']

/** 서버 오류 code를 어느 칸 아래에 보여 줄지 */
const FIELD_OF_CODE: Record<string, Field> = {
  INVALID_EMAIL: 'email',
  EMAIL_ALREADY_REGISTERED: 'email',
  RESEND_TOO_SOON: 'email',
  RESEND_DAILY_LIMIT: 'email',
  MAIL_SEND_FAILED: 'email',
  EMAIL_NOT_VERIFIED: 'email',
  INVALID_NICKNAME: 'nickname',
  NICKNAME_ALREADY_USED: 'nickname',
  CODE_MISMATCH: 'code',
  CODE_EXPIRED: 'code',
  CODE_ATTEMPTS_EXCEEDED: 'code',
}

interface Props {
  onSignedUp: () => void
}

export default function SignupPage({ onSignedUp }: Props) {
  const [nickname, setNickname] = useState('')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')

  const [phase, setPhase] = useState<Phase>('idle')
  // 인증번호를 받을 때 서버가 준 증표. 확인·이메일 변경·가입 때 다시 보낸다 (내가 시작한 인증임을 증명)
  const [verificationToken, setVerificationToken] = useState('')
  const [expiresAt, setExpiresAt] = useState<number | null>(null)
  const [now, setNow] = useState(() => Date.now())
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({})
  const [notices, setNotices] = useState<Partial<Record<Field, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState<'send' | 'confirm' | 'cancel' | 'signup' | null>(null)

  const nicknameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const codeRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const passwordConfirmRef = useRef<HTMLInputElement>(null)

  /** 칸으로 커서를 옮긴다. 화면이 바뀐 다음에 옮기도록 한 박자 늦춘다. */
  function focusField(field: Field) {
    const refs = { nickname: nicknameRef, email: emailRef, code: codeRef, password: passwordRef, passwordConfirm: passwordConfirmRef }
    window.setTimeout(() => refs[field].current?.focus(), 0)
  }

  // 인증번호 남은 시간
  useEffect(() => {
    if (phase !== 'sent' || expiresAt === null) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [phase, expiresAt])

  const remaining = phase === 'sent' && expiresAt !== null ? Math.max(0, Math.ceil((expiresAt - now) / 1000)) : null
  const codeExpired = remaining === 0

  const blogName = `${nickname.trim() || '○○'}의 블로그`
  const passwordChecks = checkPassword(password)

  function setFieldError(field: Field, message?: string) {
    setErrors((prev) => ({ ...prev, [field]: message }))
  }

  function showApiError(err: unknown, fallbackField: Field) {
    if (!(err instanceof ApiError)) {
      setFormError('※ 잠시 뒤 다시 시도해 주세요')
      return
    }
    if (err.fieldErrors.length > 0) {
      const next: Partial<Record<Field, string>> = {}
      for (const fe of err.fieldErrors) next[fe.field as Field] = fe.message
      setErrors((prev) => ({ ...prev, ...next }))
      return
    }
    setFieldError(FIELD_OF_CODE[err.code] ?? fallbackField, err.message)
  }

  async function sendCode() {
    const next: Partial<Record<Field, string>> = {
      nickname: isValidNickname(nickname) ? undefined : messages.nickname,
      email: isValidEmail(email) ? undefined : messages.email,
    }
    setErrors((prev) => ({ ...prev, ...next, code: undefined }))
    setNotices({})
    setFormError(null)
    if (next.nickname || next.email) {
      focusField(next.nickname ? 'nickname' : 'email')
      return
    }
    setBusy('send')
    try {
      const res = await api<{ message: string; expiresInSeconds: number; verificationToken: string }>('/api/auth/email-verifications', {
        method: 'POST',
        body: { nickname: nickname.trim(), email: email.trim() },
      })
      setPhase('sent')
      setVerificationToken(res.verificationToken)
      setCode('')
      setNow(Date.now())
      setExpiresAt(Date.now() + res.expiresInSeconds * 1000)
      setNotices({ email: res.message })
      focusField('code')
    } catch (err) {
      showApiError(err, 'email')
    } finally {
      setBusy(null)
    }
  }

  async function confirmCode() {
    setFieldError('code', undefined)
    if (codeExpired) {
      setFieldError('code', messages.codeExpired)
      return
    }
    setBusy('confirm')
    try {
      const res = await api<{ message: string }>('/api/auth/email-verifications/confirm', {
        method: 'POST',
        body: { email: email.trim(), code: code.trim(), verificationToken },
      })
      setPhase('verified')
      setExpiresAt(null)
      setNotices({ email: res.message })
      focusField('password')
    } catch (err) {
      showApiError(err, 'code')
      if (err instanceof ApiError && (err.code === 'CODE_EXPIRED' || err.code === 'CODE_ATTEMPTS_EXCEEDED')) {
        setExpiresAt(null)
        setPhase('idle')
      }
    } finally {
      setBusy(null)
    }
  }

  async function changeEmail() {
    setBusy('cancel')
    try {
      await api('/api/auth/email-verifications/cancel', { method: 'POST', body: { email: email.trim(), verificationToken } })
    } catch {
      // 지우지 못해도 화면은 처음 상태로 돌린다. 서버의 인증 표시는 시간이 지나면 사라진다
    } finally {
      setBusy(null)
      setPhase('idle')
      setVerificationToken('')
      setExpiresAt(null)
      setCode('')
      setNotices({})
      setErrors((prev) => ({ ...prev, email: undefined, code: undefined }))
      focusField('email')
    }
  }

  function failValidation(next: Partial<Record<Field, string>>) {
    setErrors(next)
    // 어긴 칸이 있으면 비밀번호 두 칸을 비우고, 맨 위의 어긴 칸으로 커서를 옮긴다 (FR-009)
    setPassword('')
    setPasswordConfirm('')
    const first = FIELD_ORDER.find((f) => next[f])
    if (first) focusField(first)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy) return
    setFormError(null)

    const next: Partial<Record<Field, string>> = {}
    if (!isValidNickname(nickname)) next.nickname = messages.nickname
    if (!isValidEmail(email)) next.email = messages.email
    else if (phase !== 'verified') next.email = messages.verifyFirst
    if (!isValidPassword(password)) next.password = messages.password
    if (password !== passwordConfirm || passwordConfirm === '') next.passwordConfirm = messages.passwordConfirm
    if (Object.values(next).some(Boolean)) {
      failValidation(next)
      return
    }

    setBusy('signup')
    try {
      await api('/api/auth/signup', {
        method: 'POST',
        body: { nickname: nickname.trim(), email: email.trim(), password, passwordConfirm, verificationToken },
      })
      onSignedUp()
    } catch (err) {
      if (err instanceof ApiError && err.code === 'EMAIL_NOT_VERIFIED') {
        // 인증은 했는데 서버의 인증 표시가 사라졌다면 30분이 지난 것 (VERIFICATION_EXPIRED)
        setPhase('idle')
        setNotices({})
        failValidation({ email: messages.verificationExpired })
      } else if (err instanceof ApiError && err.fieldErrors.length > 0) {
        const next2: Partial<Record<Field, string>> = {}
        for (const fe of err.fieldErrors) next2[fe.field as Field] = fe.message
        failValidation(next2)
      } else {
        showApiError(err, 'email')
      }
    } finally {
      setBusy(null)
    }
  }

  const describedBy = (field: Field, extra?: string) =>
    [errors[field] || notices[field] ? `${field}-msg` : null, extra].filter(Boolean).join(' ') || undefined

  const message = (field: Field) =>
    errors[field] ? (
      <p id={`${field}-msg`} className="msg msg-error">{errors[field]}</p>
    ) : notices[field] ? (
      <p id={`${field}-msg`} className="msg msg-ok">{notices[field]}</p>
    ) : null

  const timer =
    remaining !== null && remaining > 0
      ? `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}`
      : null

  return (
    <div className="auth-layout">
      <section className="auth-sheet hero">
        <p className="hero-headline" aria-hidden="true">내 블로그를 시작합니다.</p>
        <p className="hero-lead">
          가입하면 <strong>{blogName}</strong>와 기본 분류 ‘미분류’가 바로 만들어집니다.
        </p>
      </section>

      <section className="auth-form-wrap" aria-labelledby="signup-title">
        <h1 id="signup-title" className="auth-title">회원 가입</h1>
        <p className="auth-lead">이메일 인증을 마치면 바로 가입할 수 있습니다.</p>

        <form className="auth-form" onSubmit={submit} noValidate aria-live="polite">
          <div className="field">
            <label htmlFor="nickname">닉네임</label>
            <input
              id="nickname"
              ref={nicknameRef}
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              autoComplete="nickname"
              maxLength={20}
              aria-invalid={Boolean(errors.nickname)}
              aria-describedby={describedBy('nickname', 'nickname-hint')}
            />
            <p id="nickname-hint" className="hint">한글, 영문, 숫자 2~10자</p>
            {message('nickname')}
          </div>

          <div className="field">
            <label htmlFor="email">이메일</label>
            <div className="field-row">
              <input
                id="email"
                ref={emailRef}
                type="email"
                inputMode="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                readOnly={phase === 'verified'}
                aria-invalid={Boolean(errors.email)}
                aria-describedby={describedBy('email')}
              />
              {phase === 'verified' ? (
                <button type="button" className="btn btn-quiet" onClick={changeEmail} disabled={busy !== null}>
                  이메일 변경
                </button>
              ) : (
                <button type="button" className="btn btn-outline" onClick={sendCode} disabled={busy !== null}>
                  {busy === 'send' ? '보내는 중' : phase === 'sent' ? '다시 받기' : '인증번호 받기'}
                </button>
              )}
            </div>
            {message('email')}
          </div>

          {phase === 'sent' && (
            <div className="field">
              <label htmlFor="code">인증번호</label>
              <div className="field-row">
                <div className="code-input">
                  <input
                    id="code"
                    ref={codeRef}
                    value={code}
                    onChange={(e) => setCode(e.target.value.toUpperCase())}
                    autoComplete="one-time-code"
                    maxLength={6}
                    spellCheck={false}
                    aria-invalid={Boolean(errors.code)}
                    aria-describedby={describedBy('code', timer ? 'code-timer' : undefined)}
                  />
                  {timer && (
                    <span id="code-timer" className="code-timer" aria-label={`남은 시간 ${timer}`}>
                      {timer}
                    </span>
                  )}
                </div>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={confirmCode}
                  disabled={busy !== null || code.trim().length === 0}
                >
                  {busy === 'confirm' ? '확인하는 중' : '확인'}
                </button>
              </div>
              {codeExpired && !errors.code ? (
                <p id="code-msg" className="msg msg-error">{messages.codeExpired}</p>
              ) : (
                message('code')
              )}
            </div>
          )}

          <div className="field">
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              ref={passwordRef}
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              maxLength={40}
              aria-invalid={Boolean(errors.password)}
              aria-describedby={describedBy('password', 'password-rules')}
            />
            <ul id="password-rules" className="rules" aria-label="비밀번호 규칙">
              {passwordChecks.map((rule) => (
                <li key={rule.label} className={rule.met ? 'rule rule-met' : 'rule'}>
                  <span aria-hidden="true" className="rule-mark">{rule.met ? '✓' : '·'}</span>
                  {rule.label}
                  <span className="visually-hidden">{rule.met ? ' 충족' : ' 아직'}</span>
                </li>
              ))}
            </ul>
            {message('password')}
          </div>

          <div className="field">
            <label htmlFor="passwordConfirm">비밀번호 확인</label>
            <input
              id="passwordConfirm"
              ref={passwordConfirmRef}
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              autoComplete="new-password"
              maxLength={40}
              aria-invalid={Boolean(errors.passwordConfirm)}
              aria-describedby={describedBy('passwordConfirm')}
            />
            {message('passwordConfirm')}
          </div>

          {formError && <p className="msg msg-error form-error">{formError}</p>}

          <button type="submit" className="btn btn-primary btn-wide" disabled={busy !== null}>
            {busy === 'signup' ? '가입하는 중' : '가입하기'}
          </button>
          <p className="auth-switch">
            이미 회원이라면 <Link to="/login">로그인</Link>
          </p>
        </form>
      </section>
    </div>
  )
}
