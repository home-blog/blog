// 내 정보 수정 (specs/002 US1, T018): 닉네임·소개. 이메일은 고칠 수 없다 (FR-005)
import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { countChars, INTRO_MAX, isValidNickname, messages } from '../auth/rules'
import { useAuth } from '../auth/useAuth'
import { updateProfile } from './accountApi'
import { useUnsavedChangesPrompt } from './useUnsavedChangesPrompt'

interface Props {
  nickname: string
  intro: string
  /** 저장에 성공하면 바뀐 값을 알린다 (내 정보 표시용) */
  onSaved: (nickname: string, intro: string) => void
}

interface FieldErrors {
  nickname?: string
  intro?: string
}

export default function ProfileForm({ nickname: savedNickname, intro: savedIntro, onSaved }: Props) {
  const { refresh } = useAuth()
  const [saved, setSaved] = useState({ nickname: savedNickname, intro: savedIntro })
  const [nickname, setNickname] = useState(savedNickname)
  const [intro, setIntro] = useState(savedIntro)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)

  // 원래 값으로 되돌린 경우도 "바뀐 것 없음"이다 (FR-009)
  const dirty = nickname !== saved.nickname || intro !== saved.intro
  useUnsavedChangesPrompt(dirty)
  const introLength = countChars(intro)

  function check(): FieldErrors {
    const found: FieldErrors = {}
    if (!isValidNickname(nickname)) found.nickname = messages.nickname
    if (introLength > INTRO_MAX) found.intro = messages.intro
    return found
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy || !dirty) return
    setDone(false)
    setFormError(null)
    const found = check()
    setErrors(found)
    if (found.nickname || found.intro) return

    setBusy(true)
    try {
      const res = await updateProfile(nickname.trim(), intro)
      setSaved({ nickname: res.nickname, intro: res.intro })
      setNickname(res.nickname)
      setIntro(res.intro)
      setDone(true)
      onSaved(res.nickname, res.intro)
      // 머리글의 닉네임을 새 값으로 (contracts 2의 5단계). 실패해도 저장은 끝났다
      refresh().catch((err: unknown) => console.error(err))
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        setErrors({ nickname: err.messageFor('nickname'), intro: err.messageFor('intro') })
      } else if (err instanceof ApiError) {
        setFormError(err.message)
      } else {
        setFormError('※ 잠시 뒤 다시 시도해 주세요')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={submit} noValidate aria-labelledby="profile-title">
      <div className="field">
        <label htmlFor="profile-nickname">닉네임</label>
        <input
          id="profile-nickname"
          autoComplete="nickname"
          value={nickname}
          onChange={(e) => {
            setNickname(e.target.value)
            setDone(false)
          }}
          aria-invalid={Boolean(errors.nickname)}
          aria-describedby={errors.nickname ? 'profile-nickname-error' : 'profile-nickname-hint'}
        />
        {errors.nickname ? (
          <p id="profile-nickname-error" className="msg msg-error" role="alert">
            {errors.nickname}
          </p>
        ) : (
          <p id="profile-nickname-hint" className="hint">
            {messages.nickname}
          </p>
        )}
      </div>

      <div className="field">
        <label htmlFor="profile-intro">소개</label>
        <textarea
          id="profile-intro"
          rows={3}
          value={intro}
          onChange={(e) => {
            setIntro(e.target.value)
            setDone(false)
          }}
          aria-invalid={Boolean(errors.intro) || introLength > INTRO_MAX}
          aria-describedby="profile-intro-count"
        />
        <p id="profile-intro-count" className={introLength > INTRO_MAX ? 'msg msg-error intro-count' : 'hint intro-count'}>
          {introLength} / {INTRO_MAX}자
        </p>
        {errors.intro && (
          <p className="msg msg-error" role="alert">
            {errors.intro}
          </p>
        )}
      </div>

      {formError && (
        <p className="msg msg-error form-error" role="alert">
          {formError}
        </p>
      )}
      {done && (
        <p className="msg msg-ok form-error" role="status">
          저장했습니다
        </p>
      )}

      <button type="submit" className="btn btn-primary btn-wide" disabled={busy || !dirty}>
        {busy ? '저장하는 중' : '저장'}
      </button>
    </form>
  )
}
