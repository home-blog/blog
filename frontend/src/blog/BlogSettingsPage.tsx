// 블로그 설정 화면 /manage/blog (specs/003 US7, T050): 이름과 소개. 006 블로그 관리 화면 틀 안의 `설정` (specs/006 T038)
// - 바뀐 것이 없으면 저장을 잠근다. 저장하면 블로그 화면에 바로 새 값이 보인다 (캐시 없음, SC-010)
// - 블로그를 지우는 버튼은 없다 (FR-007)
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useUnsavedChangesPrompt } from '../account/useUnsavedChangesPrompt'
import { ApiError } from '../api/client'
import { countChars } from '../auth/rules'
import '../pages/auth-layout.css'
import '../pages/mypage.css'
import { useManageBlog } from '../manage/manageBlog'
import { getMyBlog, updateMyBlog, type MyBlog } from './blogApi'
import './blog.css'
import './manage.css'
import { BLOG_INTRO_MAX, BLOG_NAME_MAX, blogNameLength } from './rules'

interface FieldErrors {
  name?: string
  intro?: string
}

export default function BlogSettingsPage() {
  const { refresh } = useManageBlog()
  const [saved, setSaved] = useState<MyBlog | null>(null)
  const [name, setName] = useState('')
  const [intro, setIntro] = useState('')
  const [loadError, setLoadError] = useState<string | null>(null)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getMyBlog(controller.signal)
      .then((blog) => {
        setSaved(blog)
        setName(blog.name)
        setIntro(blog.intro)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoadError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  const dirty = saved !== null && (name !== saved.name || intro !== saved.intro)
  useUnsavedChangesPrompt(dirty)

  if (loadError) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {loadError}
      </p>
    )
  }
  if (!saved) return <p className="blog-status hint">불러오는 중</p>

  const nameLength = blogNameLength(name)
  const introLength = countChars(intro.trim())

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy || !dirty) return
    setDone(false)
    setFormError(null)
    const found: FieldErrors = {}
    if (nameLength === 0) found.name = '※ 블로그 이름을 입력해 주세요'
    else if (nameLength > BLOG_NAME_MAX) found.name = `※ 블로그 이름은 ${BLOG_NAME_MAX}자 이하로 입력해 주세요`
    if (introLength > BLOG_INTRO_MAX) found.intro = `※ 소개는 ${BLOG_INTRO_MAX}자 이하로 입력해 주세요`
    setErrors(found)
    if (found.name || found.intro) return

    setBusy(true)
    try {
      const res = await updateMyBlog(name, intro)
      setSaved(res)
      setName(res.name)
      setIntro(res.intro)
      setDone(true)
      // 관리 화면 위쪽 이름을 바로 바꾼다 (specs/006 T038, FR-040). 실패해도 저장은 끝났다
      refresh().catch(() => {})
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        setErrors({ name: err.messageFor('name'), intro: err.messageFor('intro') })
      } else {
        setFormError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="manage">
      <header className="manage-head">
        <h1 className="manage-title">블로그 설정</h1>
        <p className="hint">
          이름과 소개는 블로그와 글 화면에 바로 보입니다. <Link to={`/blog/${saved.blogId}`}>내 블로그로</Link> ·{' '}
          <Link to="/manage/categories">분류 관리</Link>
        </p>
      </header>

      <form className="auth-form category-manager" onSubmit={submit} noValidate>
        <div className="field">
          <label htmlFor="blog-name">블로그 이름</label>
          <input
            id="blog-name"
            value={name}
            readOnly={busy}
            aria-invalid={errors.name ? true : undefined}
            aria-describedby="blog-name-help"
            onChange={(e) => setName(e.target.value)}
          />
          <p id="blog-name-help" className={errors.name ? 'msg msg-error' : 'hint'}>
            {errors.name ?? `${nameLength} / ${BLOG_NAME_MAX}자`}
          </p>
        </div>
        <div className="field">
          <label htmlFor="blog-intro">소개</label>
          <textarea
            id="blog-intro"
            value={intro}
            rows={4}
            readOnly={busy}
            aria-invalid={errors.intro ? true : undefined}
            aria-describedby="blog-intro-help"
            onChange={(e) => setIntro(e.target.value)}
          />
          <p id="blog-intro-help" className={errors.intro ? 'msg msg-error' : 'hint'}>
            {errors.intro ?? `${introLength} / ${BLOG_INTRO_MAX}자 (비워도 됩니다)`}
          </p>
        </div>
        {formError && (
          <p className="msg msg-error form-error" role="alert">
            {formError}
          </p>
        )}
        {done && (
          <p className="msg msg-ok" role="status">
            저장했습니다
          </p>
        )}
        <button type="submit" className="btn btn-primary" disabled={busy || !dirty}>
          {busy ? '저장하는 중' : '저장'}
        </button>
      </form>
    </div>
  )
}
