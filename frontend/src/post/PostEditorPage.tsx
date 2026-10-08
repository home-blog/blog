// 글쓰기·수정 화면 (specs/003 US2 T024, US4 T037, US5 T039): 제목, 마크다운 본문, 분류, 주제, 공개 여부
// - /write는 새 글, /write/:postId는 수정. 수정은 바뀐 것이 없으면 저장 버튼을 잠근다 (FR-020)
// - 새 글은 화면을 열 때 1회용 요청 번호(requestKey)를 한 번 만든다. 저장을 여러 번 눌러도 글은 하나 (FR-018, D-6)
// - 비공개 글을 공개로 바꿔 저장하면 먼저 묻고, 취소하면 요청을 보내지 않는다 (FR-033)
// - 저장 중에는 버튼과 칸을 잠그고, 실패해도 입력한 내용은 그대로 둔다 (FR-015)
// - 저장하지 않고 나가면 묻는다 (FR-017)
// - 글자 수는 입력한 원문 그대로 센다 (D-2). 최종 판단은 서버가 한다
// - 태그 0~5개 (specs/005 US5, TagInput). 서버의 tags / tags[n] 오류는 태그 칸에 보여 준다
// - 이미지 올리기 (specs/005 US4): 받은 주소를 본문의 커서 자리에 ![](주소)로 넣는다
import { useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { Navigate, useParams } from 'react-router'
import { useUnsavedChangesPrompt } from '../account/useUnsavedChangesPrompt'
import { ApiError } from '../api/client'
import ConfirmDialog from '../components/ConfirmDialog'
import ImageUploadButton from '../image/ImageUploadButton'
import type { Visibility } from '../blog/blogApi'
import '../pages/auth-layout.css'
import { createPost, getPostEdit, getPostForm, updatePost, type PostInput } from './postApi'
import { contentLength, isBlank, postMessages, titleLength } from './rules'
import TagInput from './TagInput'
import './post-editor.css'

type Field = 'title' | 'content' | 'categoryId' | 'topicId' | 'visibility' | 'tags'
type FieldErrors = Partial<Record<Field, string>> & { tagItems?: Record<number, string> }

const EMPTY: PostInput = { title: '', content: '', categoryId: null, topicId: null, visibility: 'public', tags: [] }

/** 서버 오류에서 tags[n] 칸의 문구를 번호별로 모은다 */
function tagItemErrors(err: ApiError): Record<number, string> | undefined {
  const items: Record<number, string> = {}
  for (const e of err.fieldErrors) {
    const match = /^tags\[(\d+)\]$/.exec(e.field)
    if (match) items[Number(match[1])] = e.message
  }
  return Object.keys(items).length > 0 ? items : undefined
}

/** 화면을 그리는 데 필요한 목록과 글자 수 제한 */
interface EditorOptions {
  categories: { categoryId: number; name: string }[]
  topics: { topicId: number; name: string }[]
  limits: { titleMaxLength: number; contentMaxLength: number }
}

export default function PostEditorPage() {
  const { postId: postIdParam } = useParams()
  const editingId = postIdParam === undefined ? null : Number(postIdParam)
  const [form, setForm] = useState<EditorOptions | null>(null)
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
  const [confirmPublic, setConfirmPublic] = useState(false)
  const contentRef = useRef<HTMLTextAreaElement>(null)
  /** 이미지를 넣은 뒤 커서를 둘 자리. 본문이 화면에 반영된 뒤에 옮긴다(여러 장을 이어 올려도 차례대로 들어가게) */
  const pendingCaret = useRef<number | null>(null)

  useLayoutEffect(() => {
    const area = contentRef.current
    if (area && pendingCaret.current !== null) {
      area.setSelectionRange(pendingCaret.current, pendingCaret.current)
      pendingCaret.current = null
    }
  }, [input.content])

  useEffect(() => {
    const controller = new AbortController()
    const load =
      editingId === null
        ? getPostForm(controller.signal).then((loaded) => ({
            options: loaded as EditorOptions,
            start: { ...EMPTY, categoryId: loaded.defaultCategoryId, topicId: loaded.defaultTopicId, visibility: loaded.defaultVisibility },
          }))
        : Promise.all([getPostEdit(editingId, controller.signal), getPostForm(controller.signal)]).then(([post, defaults]) => ({
            options: { categories: post.categories, topics: post.topics, limits: defaults.limits },
            start: {
              title: post.title,
              content: post.content,
              categoryId: post.categoryId,
              topicId: post.topicId,
              visibility: post.visibility,
              tags: post.tags,
            },
          }))
    load
      .then(({ options, start }) => {
        setForm(options)
        setInitial(start)
        setInput(start)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoadError(err instanceof ApiError ? err.message : postMessages.failed)
      })
    return () => controller.abort()
  }, [editingId])

  const dirty =
    savedPostId === null &&
    (input.title !== initial.title ||
      input.content !== initial.content ||
      input.categoryId !== initial.categoryId ||
      input.topicId !== initial.topicId ||
      input.visibility !== initial.visibility ||
      input.tags.join('\n') !== initial.tags.join('\n'))
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

  function submit(e: FormEvent) {
    e.preventDefault()
    if (sending.current || (editingId !== null && !dirty)) return
    setFormError(null)
    const found = check()
    setErrors(found)
    if (Object.keys(found).length > 0) return
    // 비공개 → 공개는 먼저 묻는다. 취소하면 요청을 보내지 않는다 (FR-033)
    if (editingId !== null && initial.visibility === 'private' && input.visibility === 'public') {
      setConfirmPublic(true)
      return
    }
    void save()
  }

  async function save() {
    if (sending.current) return
    sending.current = true
    setBusy(true)
    try {
      if (editingId === null) {
        const res = await createPost(input, requestKey)
        setSavedPostId(res.postId)
      } else {
        const res = await updatePost(editingId, input)
        setSavedPostId(res.postId)
      }
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        setErrors({
          title: err.messageFor('title'),
          content: err.messageFor('content'),
          categoryId: err.messageFor('categoryId'),
          topicId: err.messageFor('topicId'),
          visibility: err.messageFor('visibility'),
          tags: err.messageFor('tags'),
          tagItems: tagItemErrors(err),
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

  /** 본문의 커서 자리(없으면 끝)에 이미지 문법을 넣는다. 글과 붙지 않게 빈 줄로 띄워 문단 하나로 둔다 */
  function insertImage(url: string) {
    const area = contentRef.current
    if (!area) {
      setInput((prev) => ({ ...prev, content: `${prev.content}\n\n![](${url})\n\n` }))
      return
    }
    // 입력 칸의 지금 값(화면에 반영된 본문)과 커서로 새 본문과 넣은 뒤의 커서 자리를 함께 계산한다
    const current = area.value
    const before = current.slice(0, area.selectionStart)
    const after = current.slice(area.selectionEnd)
    const lead = before === '' || before.endsWith('\n\n') ? '' : before.endsWith('\n') ? '\n' : '\n\n'
    const tail = after.startsWith('\n\n') ? '' : after.startsWith('\n') ? '\n' : '\n\n'
    const inserted = `${before}${lead}![](${url})${tail}`
    pendingCaret.current = inserted.length
    setInput((prev) => ({ ...prev, content: `${inserted}${after}` }))
  }

  return (
    <form className="editor" onSubmit={submit} noValidate aria-labelledby="editor-title">
      <h1 id="editor-title" className="visually-hidden">
        {editingId === null ? '새 글 쓰기' : '글 고치기'}
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
        <div className="editor-content-head">
          <label htmlFor="post-content">
            본문 <span className="hint">마크다운으로 씁니다</span>
          </label>
          <ImageUploadButton postId={editingId} disabled={busy} onUploaded={insertImage} />
        </div>
        <textarea
          ref={contentRef}
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

      <TagInput
        tags={input.tags}
        disabled={busy}
        serverError={errors.tags}
        serverTagErrors={errors.tagItems}
        onChange={(tags) => {
          update('tags', tags)
          setErrors((prev) => ({ ...prev, tags: undefined, tagItems: undefined }))
        }}
      />

      {formError && (
        <p className="msg msg-error form-error" role="alert">
          {formError}
        </p>
      )}

      <div className="editor-actions">
        <button type="submit" className="btn btn-primary" disabled={busy || (editingId !== null && !dirty)}>
          {busy ? '저장하는 중' : '저장'}
        </button>
      </div>

      <ConfirmDialog
        open={confirmPublic}
        title="공개로 바꿀까요?"
        confirmLabel="공개로 저장"
        onCancel={() => setConfirmPublic(false)}
        onConfirm={() => {
          setConfirmPublic(false)
          void save()
        }}
      >
        <p>공개로 바꾸면 누구나 볼 수 있습니다</p>
      </ConfirmDialog>
    </form>
  )
}
