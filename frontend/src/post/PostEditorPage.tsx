// 글쓰기 화면 (specs/003 US2, T024): 제목, 마크다운 본문, 분류, 주제, 공개 여부
// - 화면을 열 때 1회용 요청 번호(requestKey)를 한 번 만든다. 저장을 여러 번 눌러도 글은 하나 (FR-018, D-6)
// - 저장 중에는 버튼과 칸을 잠그고, 실패해도 입력한 내용은 그대로 둔다 (FR-015)
// - 저장하지 않고 나가면 묻는다 (FR-017)
// - 글자 수는 입력한 원문 그대로 센다 (D-2). 최종 판단은 서버가 한다
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { useUnsavedChangesPrompt } from '../account/useUnsavedChangesPrompt'
import { ApiError } from '../api/client'
import type { Visibility } from '../blog/blogApi'
import '../pages/auth-layout.css'
import { createPost, getPostForm, type PostForm, type PostInput } from './postApi'
import { contentLength, isBlank, postMessages, titleLength } from './rules'
import './post-editor.css'

type Field = 'title' | 'content' | 'categoryId' | 'topicId' | 'visibility'
type FieldErrors = Partial<Record<Field, string>>

const EMPTY: PostInput = { title: '', content: '', categoryId: null, topicId: null, visibility: 'public' }

export default function PostEditorPage() {
  const [form, setForm] = useState<PostForm | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [initial, setInitial] = useState<PostInput>(EMPTY)
  const [input, setInput] = useState<PostInput>(EMPTY)
  const [requestKey] = useState(() => crypto.randomUUID())
  const [errors, setErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [savedPostId, setSavedPostId] = useState<number | null>(null)
  // 화면이 다시 그려지기 전에 빠르게 여러 번 눌러도 요청은 하나만 보낸다 (busy는 다음 그리기부터 보인다)
  const sending = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    getPostForm(controller.signal)
      .then((loaded) => {
        const start: PostInput = {
          ...EMPTY,
          categoryId: loaded.defaultCategoryId,
          topicId: loaded.defaultTopicId,
          visibility: loaded.defaultVisibility,
        }
        setForm(loaded)
        setInitial(start)
        setInput(start)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoadError(err instanceof ApiError ? err.message : postMessages.failed)
      })
    return () => controller.abort()
  }, [])

  const dirty =
    savedPostId === null &&
    (input.title !== initial.title ||
      input.content !== initial.content ||
      input.categoryId !== initial.categoryId ||
      input.topicId !== initial.topicId ||
      input.visibility !== initial.visibility)
  useUnsavedChangesPrompt(dirty)

  if (savedPostId !== null) return <Navigate to={`/posts/${savedPostId}`} replace />
  if (loadError) {
    return (
      <p className="editor-status msg msg-error" role="alert">
        {loadError}
      </p>
    )
  }
  if (!form) return <p className="editor-status hint">불러오는 중</p>

  const { titleMaxLength, contentMaxLength } = form.limits
  const titleCount = titleLength(input.title)
  const contentCount = contentLength(input.content)

  function check(): FieldErrors {
    const found: FieldErrors = {}
    if (isBlank(input.title)) found.title = postMessages.titleRequired
    else if (titleCount > titleMaxLength) found.title = postMessages.titleTooLong(titleMaxLength)
    if (isBlank(input.content)) found.content = postMessages.contentRequired
    else if (contentCount > contentMaxLength) found.content = postMessages.contentTooLong(contentMaxLength)
    if (input.categoryId === null) found.categoryId = postMessages.categoryRequired
    if (input.topicId === null) found.topicId = postMessages.topicRequired
    return found
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (sending.current) return
    setFormError(null)
    const found = check()
    setErrors(found)
    if (Object.keys(found).length > 0) return

    sending.current = true
    setBusy(true)
    try {
      const res = await createPost(input, requestKey)
      setSavedPostId(res.postId)
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        setErrors({
          title: err.messageFor('title'),
          content: err.messageFor('content'),
          categoryId: err.messageFor('categoryId'),
          topicId: err.messageFor('topicId'),
          visibility: err.messageFor('visibility'),
        })
      } else {
        // 401이면 client.ts가 로그인 창을 띄운다. 입력한 내용은 그대로 둔다
        setFormError(err instanceof ApiError ? err.message : postMessages.failed)
      }
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  function update<K extends keyof PostInput>(key: K, value: PostInput[K]) {
    setInput((prev) => ({ ...prev, [key]: value }))
  }

  return (
    <form className="editor" onSubmit={submit} noValidate aria-labelledby="editor-title">
      <h1 id="editor-title" className="visually-hidden">
        새 글 쓰기
      </h1>

      <div className="editor-meta">
        <div className="field">
          <label htmlFor="post-category">분류</label>
          <select
            id="post-category"
            value={input.categoryId ?? ''}
            disabled={busy}
            aria-invalid={errors.categoryId ? true : undefined}
            aria-describedby={errors.categoryId ? 'post-category-error' : undefined}
            onChange={(e) => update('categoryId', e.target.value === '' ? null : Number(e.target.value))}
          >
            {form.categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.name}
              </option>
            ))}
          </select>
          {errors.categoryId && (
            <p id="post-category-error" className="msg msg-error">
              {errors.categoryId}
            </p>
          )}
        </div>

        <div className="field">
          <label htmlFor="post-topic">주제</label>
          <select
            id="post-topic"
            value={input.topicId ?? ''}
            disabled={busy}
            aria-invalid={errors.topicId ? true : undefined}
            aria-describedby={errors.topicId ? 'post-topic-error' : undefined}
            onChange={(e) => update('topicId', e.target.value === '' ? null : Number(e.target.value))}
          >
            <option value="">주제를 고르세요</option>
            {form.topics.map((t) => (
              <option key={t.topicId} value={t.topicId}>
                {t.name}
              </option>
            ))}
          </select>
          {errors.topicId && (
            <p id="post-topic-error" className="msg msg-error">
              {errors.topicId}
            </p>
          )}
        </div>

        <fieldset className="field editor-visibility" disabled={busy}>
          <legend>공개 여부</legend>
          {(['public', 'private'] as Visibility[]).map((value) => (
            <label key={value}>
              <input
                type="radio"
                name="visibility"
                value={value}
                checked={input.visibility === value}
                onChange={() => update('visibility', value)}
              />
              {value === 'public' ? '공개' : '비공개'}
            </label>
          ))}
          {errors.visibility && <p className="msg msg-error">{errors.visibility}</p>}
        </fieldset>
      </div>

      <div className="field editor-title-field">
        <label htmlFor="post-title" className="visually-hidden">
          제목
        </label>
        <input
          id="post-title"
          className="editor-title-input"
          placeholder="제목"
          value={input.title}
          readOnly={busy}
          aria-invalid={errors.title ? true : undefined}
          aria-describedby="post-title-count post-title-error"
          onChange={(e) => update('title', e.target.value)}
        />
        <div className="editor-field-foot">
          {errors.title ? (
            <p id="post-title-error" className="msg msg-error">
              {errors.title}
            </p>
          ) : (
            <span />
          )}
          <span id="post-title-count" className={`editor-count${titleCount > titleMaxLength ? ' is-over' : ''}`}>
            {titleCount} / {titleMaxLength}자
          </span>
        </div>
      </div>

      <div className="field editor-content-field">
        <label htmlFor="post-content">
          본문 <span className="hint">마크다운으로 씁니다</span>
        </label>
        <textarea
          id="post-content"
          className="editor-content-input"
          value={input.content}
          readOnly={busy}
          rows={18}
          aria-invalid={errors.content ? true : undefined}
          aria-describedby="post-content-count post-content-error"
          onChange={(e) => update('content', e.target.value)}
        />
        <div className="editor-field-foot">
          {errors.content ? (
            <p id="post-content-error" className="msg msg-error">
              {errors.content}
            </p>
          ) : (
            <span className="hint">마크다운 기호와 이미지 주소도 글자 수에 들어갑니다</span>
          )}
          <span id="post-content-count" className={`editor-count${contentCount > contentMaxLength ? ' is-over' : ''}`}>
            {contentCount.toLocaleString('ko-KR')} / {contentMaxLength.toLocaleString('ko-KR')}자
          </span>
        </div>
      </div>

      {formError && (
        <p className="msg msg-error form-error" role="alert">
          {formError}
        </p>
      )}

      <div className="editor-actions">
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? '저장하는 중' : '저장'}
        </button>
      </div>
    </form>
  )
}
