// 공지사항 쓰기·고치기 (specs/012 US4): /notices/new, /notices/:noticeId/edit. 관리자만 (서버가 다시 확인한다)
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import ProofNotFound from '../components/ProofNotFound'
import { createNotice, getNotice, updateNotice, type NoticeInput, type NoticeKind } from './noticeApi'
import { checkNotice, KIND_LABEL, NOTICE_CONTENT_MAX, NOTICE_TITLE_MAX, noticeMessages, type NoticeErrors } from './rules'
import { countChars } from '../auth/rules'
import '../pages/auth-layout.css'
import './notice.css'

const EMPTY: NoticeInput = { kind: 'notice', title: '', content: '', releaseVersion: '', pinned: false }
const FIELDS = ['kind', 'title', 'content', 'releaseVersion'] as const

export default function NoticeEditorPage() {
  const { noticeId } = useParams()
  const editing = noticeId !== undefined
  const id = Number(noticeId)
  /** 고치기 주소의 번호가 숫자가 아니면(/notices/abc/edit) 서버에 묻지 않고 "없는 공지"로 본다 (읽기 화면과 같다) */
  const validId = !editing || (/^\d+$/.test(noticeId) && Number.isSafeInteger(id) && id > 0)
  const { member } = useAuth()
  const navigate = useNavigate()
  const [input, setInput] = useState<NoticeInput>(EMPTY)
  const [loaded, setLoaded] = useState(!editing)
  const [errors, setErrors] = useState<NoticeErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!editing || !validId) return
    const controller = new AbortController()
    getNotice(id, controller.signal)
      .then((notice) => {
        setInput({
          kind: notice.kind,
          title: notice.title,
          content: notice.content,
          releaseVersion: notice.releaseVersion ?? '',
          pinned: notice.pinned,
        })
        setLoaded(true)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setFormError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [editing, id, validId])

  if (!validId) {
    return <ProofNotFound struck="쪽" inserted="공지" particle="가" message="※ 존재하지 않는 공지사항입니다" />
  }

  if (member && !member.admin) {
    return (
      <p className="post-status msg msg-error" role="alert">
        {noticeMessages.forbidden}
      </p>
    )
  }

  function set<K extends keyof NoticeInput>(key: K, value: NoticeInput[K]) {
    setInput((prev) => ({ ...prev, [key]: value }))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy) return
    setFormError(null)
    const { errors: found, value } = checkNotice(input)
    setErrors(found)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    try {
      const saved = editing ? await updateNotice(id, value) : await createNotice(value)
      navigate(`/notices/${saved.noticeId}`, { replace: editing })
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        const next: NoticeErrors = {}
        for (const field of FIELDS) {
          const message = err.messageFor(field)
          if (message) next[field] = message
        }
        setErrors(next)
      } else {
        setFormError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="notice-editor">
      <h1 className="search-title">{editing ? '공지사항 고치기' : '공지사항 쓰기'}</h1>
      {formError && (
        <p className="msg msg-error" role="alert">
          {formError}
        </p>
      )}
      {!loaded ? (
        !formError && <p className="hint">불러오는 중</p>
      ) : (
        <form className="auth-form" onSubmit={submit} noValidate>
          <fieldset className="field notice-kind" disabled={busy}>
            <legend>종류</legend>
            {(['update', 'notice'] as NoticeKind[]).map((kind) => (
              <label key={kind}>
                <input
                  type="radio"
                  name="notice-kind"
                  value={kind}
                  checked={input.kind === kind}
                  onChange={() => set('kind', kind)}
                />
                {KIND_LABEL[kind]}
              </label>
            ))}
            {errors.kind && <p className="msg msg-error">{errors.kind}</p>}
          </fieldset>

          {input.kind === 'update' && (
            <div className="field">
              <label htmlFor="notice-version">릴리스 번호</label>
              <input
                id="notice-version"
                inputMode="decimal"
                placeholder="0.6.0"
                value={input.releaseVersion ?? ''}
                onChange={(e) => set('releaseVersion', e.target.value)}
                aria-invalid={Boolean(errors.releaseVersion)}
                aria-describedby={errors.releaseVersion ? 'notice-version-error' : undefined}
              />
              {errors.releaseVersion && (
                <p id="notice-version-error" className="msg msg-error">
                  {errors.releaseVersion}
                </p>
              )}
            </div>
          )}

          <div className="field">
            <label htmlFor="notice-title">제목</label>
            <input
              id="notice-title"
              value={input.title}
              onChange={(e) => set('title', e.target.value)}
              aria-invalid={Boolean(errors.title)}
              aria-describedby={errors.title ? 'notice-title-error' : undefined}
            />
            <span className="hint notice-count">
              {countChars(input.title.trim())} / {NOTICE_TITLE_MAX}
            </span>
            {errors.title && (
              <p id="notice-title-error" className="msg msg-error">
                {errors.title}
              </p>
            )}
          </div>

          <div className="field">
            <label htmlFor="notice-content">본문 (마크다운)</label>
            <textarea
              id="notice-content"
              rows={14}
              value={input.content}
              onChange={(e) => set('content', e.target.value)}
              aria-invalid={Boolean(errors.content)}
              aria-describedby={errors.content ? 'notice-content-error' : undefined}
            />
            <span className="hint notice-count">
              {countChars(input.content).toLocaleString('ko-KR')} / {NOTICE_CONTENT_MAX.toLocaleString('ko-KR')}
            </span>
            {errors.content && (
              <p id="notice-content-error" className="msg msg-error">
                {errors.content}
              </p>
            )}
          </div>

          <label className="notice-pinned">
            <input type="checkbox" checked={input.pinned} onChange={(e) => set('pinned', e.target.checked)} />
            목록 맨 위에 고정
          </label>

          <p className="notice-actions">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? '저장하는 중' : editing ? '고치기' : '게시'}
            </button>
            <Link to={editing ? `/notices/${noticeId}` : '/notices'} className="btn btn-outline">
              취소
            </Link>
          </p>
        </form>
      )}
    </div>
  )
}
