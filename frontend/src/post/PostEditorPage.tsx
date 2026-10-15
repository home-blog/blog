// 글쓰기·수정 화면 (specs/003 US2 T024, US4 T037, US5 T039): 제목, 마크다운 본문, 분류, 주제, 공개 여부
// - /write는 새 글, /write/:postId는 수정. 수정은 바뀐 것이 없으면 저장 버튼을 잠근다 (FR-020)
// - 새 글은 화면을 열 때 1회용 요청 번호(requestKey)를 한 번 만든다. 저장을 여러 번 눌러도 글은 하나 (FR-018, D-6)
// - 비공개 글을 공개로 바꿔 저장하면 먼저 묻고, 취소하면 요청을 보내지 않는다 (FR-033)
// - 저장 중에는 버튼과 칸을 잠그고, 실패해도 입력한 내용은 그대로 둔다 (FR-015)
// - 저장하지 않고 나가면 묻는다 (FR-017)
// - 글자 수는 입력한 원문 그대로 센다 (D-2). 최종 판단은 서버가 한다
// - 태그 0~5개 (specs/005 US5, TagInput). 서버의 tags / tags[n] 오류는 태그 칸에 보여 준다
// - 이미지 올리기 (specs/005 US4): 받은 주소를 본문의 커서 자리에 ![](주소)로 넣는다
// - 나란히 미리보기 (specs/007 US7): 넓은 화면은 왼쪽에 쓰고 오른쪽 원고지 종이에 바로 보인다(스크롤도 같이). 좁으면 쓰기/미리보기 탭
// - 서식 도구 막대(MarkdownToolbar)와 원고지 매수 실시간 표시
// - 본문에 이미지가 있으면 대표 이미지(카드 썸네일) 고르기 (specs/008, CoverPicker)
// - 새 글은 임시 저장(specs/013 US3): 바뀐 것이 있으면 writing.draftAutosaveSeconds(30초)마다 저절로, "임시 저장" 버튼으로도.
//   /write?draft=번호로 이어 쓴다. 임시 저장 글을 발행하면 보통 글이 되고 임시 저장 글은 사라진다
// - 새 글은 예약 발행(US4): 시각을 고르면 그 시각에 올라간다. 예약을 취소하면 임시 저장 글로 남는다
import { useEffect, useLayoutEffect, useRef, useState, type FormEvent } from 'react'
import { Navigate, useParams, useSearchParams } from 'react-router'
import { useUnsavedChangesPrompt } from '../account/useUnsavedChangesPrompt'
import { ApiError } from '../api/client'
import ConfirmDialog from '../components/ConfirmDialog'
import ImageUploadButton from '../image/ImageUploadButton'
import type { Visibility } from '../blog/blogApi'
import '../pages/auth-layout.css'
import { addPostToSeries, getPostSeries, removePostFromSeries } from '../series/seriesApi'
import {
  createDraft,
  getDraft,
  publishDraft,
  scheduleDraft,
  unscheduleDraft,
  updateDraft,
  type DraftInput,
  type DraftSaved,
} from './draftApi'
import { isoToLocalInput, localInputToIso, publishAtLabel, savedAtLabel, scheduleError } from './draftRules'
import { createPost, getPostEdit, getPostForm, updatePost, type PostForm, type PostInput, type SavedState } from './postApi'
import { contentLength, isBlank, postMessages, titleLength } from './rules'
import EditorPreview from './EditorPreview'
import CoverPicker from './CoverPicker'
import MarkdownToolbar from './MarkdownToolbar'
import TagInput from './TagInput'
import { useAutosave } from './useAutosave'
import './post-detail.css'
import './post-editor.css'

type Field = 'title' | 'content' | 'categoryId' | 'topicId' | 'visibility' | 'tags'
type FieldErrors = Partial<Record<Field, string>> & { tagItems?: Record<number, string> }

const EMPTY: PostInput = { title: '', content: '', categoryId: null, topicId: null, visibility: 'public', tags: [] }

/** 넓은 화면에서 미리보기를 펼쳐 둘지 (브라우저에 기억) */
const PREVIEW_KEY = 'geulgyeol-editor-preview'

function savedPreviewOpen(): boolean {
  try {
    return localStorage.getItem(PREVIEW_KEY) !== 'off'
  } catch {
    return true
  }
}

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
  series: { seriesId: number; name: string }[]
}

const DEFAULT_WRITING: PostForm['writing'] = { draftAutosaveSeconds: 30, draftMaxCount: 50, scheduleMaxDays: 30 }

function snapshotOf(input: PostInput, seriesId: number | null): string {
  return JSON.stringify([input, seriesId])
}

export default function PostEditorPage() {
  const { postId: postIdParam } = useParams()
  const editingId = postIdParam === undefined ? null : Number(postIdParam)
  const [form, setForm] = useState<EditorOptions | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [initial, setInitial] = useState<PostInput>(EMPTY)
  const [input, setInput] = useState<PostInput>(EMPTY)
  // 연재 (specs/013 US2): 글과 따로 저장한다. 저장한 뒤 바뀌었으면 연재에 넣거나 뺀다
  const [initialSeriesId, setInitialSeriesId] = useState<number | null>(null)
  const [seriesId, setSeriesId] = useState<number | null>(null)
  // 임시 저장·예약 (specs/013 US3, US4). 새 글만. 처음 연 주소의 ?draft=번호를 이어 쓴다
  const [searchParams, setSearchParams] = useSearchParams()
  const [loadDraftId] = useState(() => {
    const value = editingId === null ? Number(searchParams.get('draft')) : Number.NaN
    return Number.isInteger(value) && value > 0 ? value : null
  })
  const draftRef = useRef<number | null>(loadDraftId)
  const draftChain = useRef<Promise<unknown>>(Promise.resolve())
  const [writing, setWriting] = useState(DEFAULT_WRITING)
  const [savedSnapshot, setSavedSnapshot] = useState<string | null>(null)
  const [savedAt, setSavedAt] = useState<string | null>(null)
  const [publishAt, setPublishAt] = useState<string | null>(null)
  const [draftMessage, setDraftMessage] = useState<{ text: string; error: boolean } | null>(null)
  const [scheduling, setScheduling] = useState(false)
  const [scheduleValue, setScheduleValue] = useState('')
  const [scheduleProblem, setScheduleProblem] = useState<string | null>(null)
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
  // 나란히 미리보기 (specs/007 US7): 넓은 화면에서 펼칠지, 좁은 화면에서 지금 보는 쪽
  const [previewOpen, setPreviewOpen] = useState(savedPreviewOpen)
  const [mode, setMode] = useState<'write' | 'preview'>('write')
  const previewRef = useRef<HTMLElement>(null)

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
        ? Promise.all([
            getPostForm(controller.signal),
            loadDraftId === null ? Promise.resolve(null) : getDraft(loadDraftId, controller.signal),
          ]).then(([loaded, draft]) => {
            setWriting(loaded.writing ?? DEFAULT_WRITING)
            setSavedAt(draft?.updatedAt ?? null)
            setPublishAt(draft?.publishAt ?? null)
            return {
              options: loaded as EditorOptions,
              seriesId: draft?.seriesId ?? null,
              start: draft
                ? {
                    title: draft.title,
                    content: draft.content,
                    categoryId: draft.categoryId ?? loaded.defaultCategoryId,
                    topicId: draft.topicId ?? loaded.defaultTopicId,
                    visibility: draft.visibility,
                    tags: draft.tags,
                  }
                : {
                    ...EMPTY,
                    categoryId: loaded.defaultCategoryId,
                    topicId: loaded.defaultTopicId,
                    visibility: loaded.defaultVisibility,
                  },
            }
          })
        : Promise.all([
            getPostEdit(editingId, controller.signal),
            getPostForm(controller.signal),
            getPostSeries(editingId, controller.signal),
          ]).then(
            ([post, defaults, series]) => ({
              options: { categories: post.categories, topics: post.topics, limits: defaults.limits, series: defaults.series },
              seriesId: series?.seriesId ?? null,
              start: {
                title: post.title,
                content: post.content,
                categoryId: post.categoryId,
                topicId: post.topicId,
                visibility: post.visibility,
                tags: post.tags,
              },
            }),
          )
    load
      .then(({ options, start, seriesId: startSeriesId }) => {
        setForm(options)
        setInitial(start)
        setInput(start)
        setInitialSeriesId(startSeriesId)
        setSeriesId(startSeriesId)
        setSavedSnapshot(snapshotOf(start, startSeriesId))
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoadError(err instanceof ApiError ? err.message : postMessages.failed)
      })
    return () => controller.abort()
  }, [editingId, loadDraftId])

  const dirty =
    savedPostId === null &&
    (input.title !== initial.title ||
      input.content !== initial.content ||
      input.categoryId !== initial.categoryId ||
      input.topicId !== initial.topicId ||
      input.visibility !== initial.visibility ||
      input.tags.join('\n') !== initial.tags.join('\n') ||
      seriesId !== initialSeriesId)
  const snapshot = snapshotOf(input, seriesId)
  // 새 글은 마지막 임시 저장과 비교한다 (임시 저장한 뒤에는 나가도 묻지 않는다)
  const unsaved = editingId === null ? savedPostId === null && savedSnapshot !== null && snapshot !== savedSnapshot : dirty
  useUnsavedChangesPrompt(unsaved)

  /** 임시 저장. 저장이 겹치지 않게 차례로 보낸다(처음 저장이 끝나야 번호가 생긴다). 임시 저장 글 번호를 돌려준다 */
  function saveDraft(): Promise<DraftSaved> {
    const body: DraftInput = { ...input, seriesId }
    const snap = snapshot
    const next = draftChain.current.then(async () => {
      const id = draftRef.current
      const res = id === null ? await createDraft(body) : await updateDraft(id, body)
      if (id === null) {
        draftRef.current = res.draftId
        setSearchParams({ draft: String(res.draftId) }, { replace: true })
      }
      setSavedSnapshot(snap)
      setSavedAt(res.updatedAt)
      setPublishAt(res.publishAt)
      return res
    })
    draftChain.current = next.catch(() => undefined)
    return next
  }

  useAutosave({
    snapshot,
    savedSnapshot: savedSnapshot ?? snapshot,
    enabled: editingId === null && form !== null && !busy && savedPostId === null,
    intervalSeconds: writing.draftAutosaveSeconds,
    save: () =>
      saveDraft().then(
        () => setDraftMessage(null),
        (err: unknown) => {
          setDraftMessage({ text: err instanceof ApiError ? err.message : '※ 임시 저장하지 못했습니다', error: true })
        },
      ),
  })

  if (savedPostId !== null) {
    // 글 화면이 "탈고"(새 글)·"퇴고"(고친 글) 도장 알림을 한 번 띄운다 (v0.5.1)
    const state: SavedState = { saved: editingId === null ? 'created' : 'edited', postId: savedPostId }
    return <Navigate to={`/posts/${savedPostId}`} replace state={state} />
  }
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
      if (editingId === null && draftRef.current !== null) {
        // 임시 저장 글을 발행한다: 연재도 서버가 함께 넣는다. 저장 중이던 임시 저장이 끝난 뒤에 보낸다
        await draftChain.current
        const res = await publishDraft(draftRef.current, { ...input, seriesId })
        setSavedPostId(res.postId)
        return
      }
      const res = editingId === null ? await createPost(input, requestKey) : await updatePost(editingId, input)
      await saveSeries(res.postId)
      setSavedPostId(res.postId)
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

  /** 연재가 바뀌었으면 넣거나 뺀다. 글은 이미 저장됐으니 실패해도 글 화면으로 간다(연재 관리에서 다시 넣을 수 있다) */
  async function saveSeries(postId: number) {
    if (seriesId === initialSeriesId) return
    try {
      if (seriesId !== null) await addPostToSeries(seriesId, postId)
      else if (initialSeriesId !== null) await removePostFromSeries(initialSeriesId, postId)
    } catch {
      // 위 주석대로 넘어간다
    }
  }

  /** 서버의 칸 오류를 칸마다 보여 준다. 칸 오류가 없으면 false */
  function showFieldErrors(err: unknown): boolean {
    if (!(err instanceof ApiError) || err.fieldErrors.length === 0) return false
    setErrors({
      title: err.messageFor('title'),
      content: err.messageFor('content'),
      categoryId: err.messageFor('categoryId'),
      topicId: err.messageFor('topicId'),
      visibility: err.messageFor('visibility'),
      tags: err.messageFor('tags'),
      tagItems: tagItemErrors(err),
    })
    return true
  }

  async function saveDraftNow() {
    if (sending.current) return
    sending.current = true
    setBusy(true)
    setDraftMessage(null)
    try {
      await saveDraft()
    } catch (err) {
      if (!showFieldErrors(err)) {
        setDraftMessage({ text: err instanceof ApiError ? err.message : '※ 임시 저장하지 못했습니다', error: true })
      }
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  function openSchedule() {
    setScheduleProblem(null)
    setScheduleValue(publishAt ? isoToLocalInput(publishAt) : '')
    setScheduling(true)
  }

  /** 예약: 보통 발행과 같은 검사 + 시각 검사. 아직 임시 저장 글이 없으면 먼저 만든다 */
  async function confirmSchedule() {
    if (sending.current) return
    const found = check()
    setErrors(found)
    if (Object.keys(found).length > 0) {
      setScheduling(false)
      return
    }
    const iso = localInputToIso(scheduleValue)
    const problem = scheduleError(iso, new Date(), writing.scheduleMaxDays)
    if (problem || !iso) {
      setScheduleProblem(problem)
      return
    }
    sending.current = true
    setBusy(true)
    try {
      const saved = await saveDraft()
      const res = await scheduleDraft(saved.draftId, { ...input, seriesId }, iso)
      setPublishAt(res.publishAt)
      setSavedAt(res.updatedAt)
      setScheduling(false)
      setDraftMessage({ text: '예약했습니다. 그 시각이 되면 글이 올라갑니다', error: false })
    } catch (err) {
      if (err instanceof ApiError && err.messageFor('publishAt')) {
        setScheduleProblem(err.messageFor('publishAt') ?? null)
      } else {
        setScheduling(false)
        if (!showFieldErrors(err)) setFormError(err instanceof ApiError ? err.message : postMessages.failed)
      }
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  async function cancelSchedule() {
    const id = draftRef.current
    if (id === null || sending.current) return
    sending.current = true
    setBusy(true)
    try {
      const res = await unscheduleDraft(id)
      setPublishAt(res.publishAt)
      setDraftMessage({ text: '예약을 취소했습니다. 임시 저장 글로 남습니다', error: false })
    } catch (err) {
      setDraftMessage({ text: err instanceof ApiError ? err.message : postMessages.failed, error: true })
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  function togglePreview() {
    const next = !previewOpen
    setPreviewOpen(next)
    try {
      localStorage.setItem(PREVIEW_KEY, next ? 'on' : 'off')
    } catch {
      // 기억하지 못해도 지금 화면은 바꾼다
    }
  }

  /** 본문을 내리면 미리보기도 같은 비율만큼 내린다 */
  function syncScroll(area: HTMLTextAreaElement) {
    const preview = previewRef.current
    if (!preview) return
    const ratio = area.scrollTop / Math.max(1, area.scrollHeight - area.clientHeight)
    preview.scrollTop = ratio * (preview.scrollHeight - preview.clientHeight)
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
    <form
      className={previewOpen ? 'editor has-preview' : 'editor'}
      onSubmit={submit}
      noValidate
      aria-labelledby="editor-title"
    >
      <h1 id="editor-title" className="visually-hidden">
        {editingId === null ? '새 글 쓰기' : '글 고치기'}
      </h1>

      {/* 맨 위 한 줄: 분류·주제·공개 여부, 그리고 미리보기 접기(넓은 화면) / 쓰기·미리보기 탭(좁은 화면) */}
      <div className="editor-top">
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

          {form.series.length > 0 && (
            <div className="field">
              <label htmlFor="post-series">연재</label>
              <select
                id="post-series"
                value={seriesId ?? ''}
                disabled={busy}
                onChange={(e) => setSeriesId(e.target.value === '' ? null : Number(e.target.value))}
              >
                <option value="">연재 없음</option>
                {form.series.map((series) => (
                  <option key={series.seriesId} value={series.seriesId}>
                    {series.name}
                  </option>
                ))}
              </select>
            </div>
          )}

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
        <div className="editor-view-switch">
          <button type="button" className="editor-preview-toggle" aria-pressed={previewOpen} onClick={togglePreview}>
            {previewOpen ? '미리보기 접기' : '미리보기 펼치기'}
          </button>
          <div className="editor-mode-tabs" role="group" aria-label="보기">
            <button
              type="button"
              className="editor-mode-tab"
              aria-pressed={mode === 'write'}
              onClick={() => setMode('write')}
            >
              쓰기
            </button>
            <button
              type="button"
              className="editor-mode-tab"
              aria-pressed={mode === 'preview'}
              onClick={() => setMode('preview')}
            >
              미리보기
            </button>
          </div>
        </div>
      </div>

      {/* 두 칸: 왼쪽 쓰기 종이와 오른쪽 미리보기 종이의 위·아래를 맞춘다 */}
      <div className="editor-split" data-mode={mode}>
        <section className="editor-write" aria-label="쓰기">
          <header className="editor-pane-head">
            <label htmlFor="post-content" className="editor-pane-label">
              쓰기 <span className="hint">마크다운</span>
            </label>
            <span
              id="post-content-count"
              className={`editor-count${contentCount > contentMaxLength ? ' is-over' : ''}`}
            >
              {contentCount.toLocaleString('ko-KR')} / {contentMaxLength.toLocaleString('ko-KR')}자
            </span>
          </header>
          <div className="editor-pane-body">
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

            <MarkdownToolbar
              target={contentRef}
              disabled={busy}
              onFallback={(value) => update('content', value)}
              imageButton={<ImageUploadButton postId={editingId} disabled={busy} onUploaded={insertImage} />}
            />
            <textarea
              ref={contentRef}
              id="post-content"
              className="editor-content-input"
              value={input.content}
              readOnly={busy}
              rows={18}
              placeholder="오늘의 이야기를 적어 보세요"
              aria-invalid={errors.content ? true : undefined}
              aria-describedby="post-content-count post-content-error post-content-hint"
              onChange={(e) => update('content', e.target.value)}
              onScroll={(e) => syncScroll(e.currentTarget)}
            />
            {errors.content ? (
              <p id="post-content-error" className="msg msg-error">
                {errors.content}
              </p>
            ) : (
              <p id="post-content-hint" className="hint editor-content-hint">
                마크다운 기호와 이미지 주소도 글자 수에 들어갑니다
              </p>
            )}
            <CoverPicker content={input.content} disabled={busy} onChange={(value) => update('content', value)} />
          </div>
        </section>

        {previewOpen && (
          <EditorPreview
            ref={previewRef}
            title={input.title}
            content={input.content}
            tags={input.tags}
            topicName={form.topics.find((t) => t.topicId === input.topicId)?.name}
            categoryName={form.categories.find((c) => c.categoryId === input.categoryId)?.name}
          />
        )}
      </div>

      {/* 맨 아래 막대: 오류·임시 저장 상태와 저장 */}
      <div className="editor-bar">
        {formError ? (
          <p className="msg msg-error form-error" role="alert">
            {formError}
          </p>
        ) : (
          <p className={`editor-draft-status ${draftMessage?.error ? 'msg msg-error' : 'hint'}`} role="status" aria-live="polite">
            {draftMessage?.text ?? (savedAt ? savedAtLabel(savedAt) : '')}
            {publishAt && <span className="editor-draft-scheduled">{publishAtLabel(publishAt)}</span>}
          </p>
        )}
        <div className="editor-bar-actions">
          {editingId === null && (
            <>
              <button type="button" className="btn btn-quiet" disabled={busy} onClick={() => void saveDraftNow()}>
                임시 저장
              </button>
              {publishAt && (
                <button type="button" className="btn btn-quiet" disabled={busy} onClick={() => void cancelSchedule()}>
                  예약 취소
                </button>
              )}
              <button type="button" className="btn btn-outline" disabled={busy} onClick={openSchedule}>
                {publishAt ? '예약 바꾸기' : '예약'}
              </button>
            </>
          )}
          <button type="submit" className="btn btn-primary" disabled={busy || (editingId !== null && !dirty)}>
            {busy ? '저장하는 중' : editingId === null ? '발행' : '저장'}
          </button>
        </div>
      </div>

      <ConfirmDialog
        open={scheduling}
        title="예약 발행"
        confirmLabel="예약"
        onCancel={() => setScheduling(false)}
        onConfirm={() => void confirmSchedule()}
      >
        <div className="field editor-schedule">
          <label htmlFor="post-publish-at">올릴 시각 (한국 시간)</label>
          <input
            id="post-publish-at"
            type="datetime-local"
            value={scheduleValue}
            aria-invalid={scheduleProblem ? true : undefined}
            aria-describedby="post-publish-at-help"
            onChange={(e) => {
              setScheduleValue(e.target.value)
              setScheduleProblem(null)
            }}
          />
          <p id="post-publish-at-help" className={scheduleProblem ? 'msg msg-error' : 'hint'}>
            {scheduleProblem ?? `지금부터 1분 뒤 ~ ${writing.scheduleMaxDays}일 안. 그 시각이 될 때까지는 나만 봅니다`}
          </p>
        </div>
      </ConfirmDialog>

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
