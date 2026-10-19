// 글마당 쓰기·고치기 (specs/017 US2): /board/write, /board/:boardPostId/edit. 로그인한 회원만 (고치기는 쓴 사람만, 서버가 다시 확인한다)
// 주제 목록은 서버의 주제 표를 그대로 쓴다(블로그 글과 같은 다섯 주제)
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { countChars } from '../auth/rules'
import ProofNotFound from '../components/ProofNotFound'
import {
  createBoardPost,
  getBoardPost,
  getBoardPosts,
  KIND_LABEL,
  messageOf,
  updateBoardPost,
  type BoardInput,
  type BoardKind,
} from './boardApi'
import { BOARD_CONTENT_MAX, BOARD_FIELDS, BOARD_TITLE_MAX, checkBoard, type BoardErrors } from './rules'
import '../pages/auth-layout.css'
import './board.css'

export default function BoardEditorPage() {
  const { boardPostId } = useParams()
  const editing = boardPostId !== undefined
  const id = Number(boardPostId)
  const validId = !editing || (/^\d+$/.test(boardPostId) && Number.isSafeInteger(id) && id > 0)
  const navigate = useNavigate()
  const [topics, setTopics] = useState<string[]>([])
  const [input, setInput] = useState<BoardInput>({ topic: '', kind: 'talk', title: '', content: '' })
  const [loaded, setLoaded] = useState(!editing)
  const [errors, setErrors] = useState<BoardErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getBoardPosts({}, controller.signal)
      .then((page) => setTopics(page.topics))
      .catch(() => {
        // 주제를 못 읽으면 고르기가 비고, 보내면 서버가 알린다
      })
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!editing || !validId) return
    const controller = new AbortController()
    getBoardPost(id, controller.signal)
      .then((post) => {
        if (!post.mine) {
          setFormError('※ 내가 쓴 글만 고칠 수 있습니다')
          return
        }
        setInput({ topic: post.topic, kind: post.kind, title: post.title, content: post.content })
        setLoaded(true)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setFormError(messageOf(err))
      })
    return () => controller.abort()
  }, [editing, id, validId])

  if (!validId) return <ProofNotFound struck="쪽" inserted="글" particle="이" message="※ 존재하지 않는 글입니다" />

  function set<K extends keyof BoardInput>(key: K, value: BoardInput[K]) {
    setInput((prev) => ({ ...prev, [key]: value }))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (busy) return
    setFormError(null)
    const found = checkBoard(input)
    setErrors(found)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    const value = { ...input, title: input.title.trim() }
    try {
      const saved = editing ? await updateBoardPost(id, value) : await createBoardPost(value)
      navigate(`/board/${saved.boardPostId}`, { replace: editing })
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        const next: BoardErrors = {}
        for (const field of BOARD_FIELDS) {
          const message = err.messageFor(field)
          if (message) next[field] = message
        }
        setErrors(next)
      } else {
        setFormError(messageOf(err))
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="board-editor">
      <h1 className="board-heading">{editing ? '글마당 글 고치기' : '글마당 글쓰기'}</h1>
      {formError && (
        <p className="msg msg-error" role="alert">
          {formError}
        </p>
      )}
      {!loaded ? (
        !formError && <p className="hint">불러오는 중</p>
      ) : (
        <form className="auth-form" onSubmit={submit} noValidate>
          <div className="field">
            <label htmlFor="board-topic">주제</label>
            <select
              id="board-topic"
              value={input.topic}
              onChange={(e) => set('topic', e.target.value)}
              aria-invalid={Boolean(errors.topic)}
              aria-describedby={errors.topic ? 'board-topic-error' : undefined}
            >
              <option value="">주제 고르기</option>
              {topics.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
            {errors.topic && (
              <p id="board-topic-error" className="msg msg-error">
                {errors.topic}
              </p>
            )}
          </div>

          <fieldset className="field board-kind-field" disabled={busy}>
            <legend>종류</legend>
            {(['talk', 'question'] as BoardKind[]).map((kind) => (
              <label key={kind}>
                <input
                  type="radio"
                  name="board-kind"
                  value={kind}
                  checked={input.kind === kind}
                  onChange={() => set('kind', kind)}
                />
                {KIND_LABEL[kind]}
              </label>
            ))}
            <p className="hint">물음을 올리면 가장 도움이 된 댓글에 채택 도장을 찍을 수 있어요.</p>
            {errors.kind && <p className="msg msg-error">{errors.kind}</p>}
          </fieldset>

          <div className="field">
            <label htmlFor="board-title">제목</label>
            <input
              id="board-title"
              value={input.title}
              onChange={(e) => set('title', e.target.value)}
              aria-invalid={Boolean(errors.title)}
              aria-describedby={errors.title ? 'board-title-error' : undefined}
            />
            <span className="hint board-count">
              {countChars(input.title.trim())} / {BOARD_TITLE_MAX}
            </span>
            {errors.title && (
              <p id="board-title-error" className="msg msg-error">
                {errors.title}
              </p>
            )}
          </div>

          <div className="field">
            <label htmlFor="board-content">본문 (마크다운)</label>
            <textarea
              id="board-content"
              rows={12}
              value={input.content}
              onChange={(e) => set('content', e.target.value)}
              aria-invalid={Boolean(errors.content)}
              aria-describedby={errors.content ? 'board-content-error' : undefined}
            />
            <span className="hint board-count">
              {countChars(input.content).toLocaleString('ko-KR')} / {BOARD_CONTENT_MAX.toLocaleString('ko-KR')}
            </span>
            {errors.content && (
              <p id="board-content-error" className="msg msg-error">
                {errors.content}
              </p>
            )}
          </div>

          <p className="board-actions">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? '올리는 중' : editing ? '고치기' : '올리기'}
            </button>
            <Link to={editing ? `/board/${boardPostId}` : '/board'} className="btn btn-outline">
              취소
            </Link>
          </p>
        </form>
      )}
    </div>
  )
}
