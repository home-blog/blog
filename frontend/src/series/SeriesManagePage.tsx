// 연재 관리 /manage/series (specs/013 US2): 만들기, 이름·소개 고치기, 지우기, 글 순서 바꾸기·빼기
// 글을 연재에 넣는 것은 글쓰기·글 수정 화면에서 한다. 거절 문구는 서버 것 그대로 보여 준다
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import { getMyBlog, type MyBlog } from '../blog/blogApi'
import ConfirmDialog from '../components/ConfirmDialog'
import '../blog/manage.css'
import { moveItem, SERIES_DESCRIPTION_MAX, SERIES_NAME_MAX, seriesInputError } from './rules'
import {
  createSeries,
  deleteSeries,
  getBlogSeries,
  getSeries,
  removePostFromSeries,
  reorderSeries,
  updateSeries,
  type SeriesDetail,
  type SeriesSummary,
} from './seriesApi'
import './series.css'

function messageOf(err: unknown): string {
  if (err instanceof ApiError) return err.fieldErrors[0]?.message ?? err.message
  return '※ 잠시 뒤 다시 시도해 주세요'
}

export default function SeriesManagePage() {
  const [blog, setBlog] = useState<MyBlog | null>(null)
  const [list, setList] = useState<SeriesSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)
  const [openId, setOpenId] = useState<number | null>(null)
  const [detail, setDetail] = useState<SeriesDetail | null>(null)
  const [editing, setEditing] = useState<{ seriesId: number; name: string; description: string } | null>(null)
  const [deleting, setDeleting] = useState<SeriesSummary | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getMyBlog(controller.signal)
      .then(async (mine) => {
        const items = await getBlogSeries(mine.blogId, controller.signal)
        setBlog(mine)
        setList(items)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  async function run(action: () => Promise<void>, done?: string) {
    if (busy) return
    setBusy(true)
    setMessage(null)
    try {
      await action()
      if (done) setMessage({ text: done, error: false })
    } catch (err) {
      setMessage({ text: messageOf(err), error: true })
    } finally {
      setBusy(false)
    }
  }

  function summaryOf(next: SeriesDetail): SeriesSummary {
    return { seriesId: next.seriesId, name: next.name, description: next.description, postCount: next.posts.length }
  }

  function replace(next: SeriesDetail) {
    setList((items) => items?.map((item) => (item.seriesId === next.seriesId ? summaryOf(next) : item)) ?? null)
    if (openId === next.seriesId) setDetail(next)
  }

  function add(e: FormEvent) {
    e.preventDefault()
    const problem = seriesInputError(name, description)
    if (problem) return setMessage({ text: problem, error: true })
    void run(async () => {
      const created = await createSeries(name, description)
      setList((items) => [...(items ?? []), summaryOf(created)])
      setName('')
      setDescription('')
    }, '연재를 만들었습니다')
  }

  function save(e: FormEvent) {
    e.preventDefault()
    if (!editing) return
    const problem = seriesInputError(editing.name, editing.description)
    if (problem) return setMessage({ text: problem, error: true })
    void run(async () => {
      replace(await updateSeries(editing.seriesId, editing.name, editing.description))
      setEditing(null)
    }, '고쳤습니다')
  }

  function toggle(seriesId: number) {
    if (openId === seriesId) {
      setOpenId(null)
      setDetail(null)
      return
    }
    setOpenId(seriesId)
    setDetail(null)
    void run(async () => setDetail(await getSeries(seriesId)))
  }

  function move(index: number, delta: number) {
    if (!detail) return
    const order = moveItem(
      detail.posts.map((post) => post.postId),
      index,
      delta,
    )
    void run(async () => replace(await reorderSeries(detail.seriesId, order)))
  }

  function removePost(postId: number) {
    if (!detail) return
    void run(async () => {
      await removePostFromSeries(detail.seriesId, postId)
      replace(await getSeries(detail.seriesId))
    }, '연재에서 뺐습니다. 글은 그대로 있습니다')
  }

  function remove(series: SeriesSummary) {
    setDeleting(null)
    void run(async () => {
      await deleteSeries(series.seriesId)
      setList((items) => items?.filter((item) => item.seriesId !== series.seriesId) ?? null)
      if (openId === series.seriesId) setOpenId(null)
    }, '연재를 지웠습니다. 글은 그대로 있습니다')
  }

  if (error) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {error}
      </p>
    )
  }
  if (!blog || !list) return <p className="blog-status hint">불러오는 중</p>

  return (
    <div className="manage">
      <header className="manage-head">
        <h1 className="manage-title">연재 관리</h1>
        <p className="hint">
          여러 글을 1화, 2화…로 묶습니다. 글은 글쓰기·글 수정 화면에서 연재를 골라 넣습니다.{' '}
          <Link to={`/blog/${blog.blogId}`}>내 블로그로</Link>
        </p>
      </header>

      {list.length === 0 ? (
        <p className="hint">아직 연재가 없습니다.</p>
      ) : (
        <ul className="series-manage-list">
          {list.map((series) => (
            <li key={series.seriesId} className="series-manage-item">
              {editing?.seriesId === series.seriesId ? (
                <form className="series-form" onSubmit={save} noValidate>
                  <label htmlFor={`series-name-${series.seriesId}`}>이름</label>
                  <input id={`series-name-${series.seriesId}`} value={editing.name} maxLength={SERIES_NAME_MAX * 2}
                    onChange={(e) => setEditing({ ...editing, name: e.target.value })} />
                  <label htmlFor={`series-description-${series.seriesId}`}>소개</label>
                  <textarea id={`series-description-${series.seriesId}`} value={editing.description} rows={2}
                    maxLength={SERIES_DESCRIPTION_MAX * 2} onChange={(e) => setEditing({ ...editing, description: e.target.value })} />
                  <div className="series-form-actions">
                    <button type="submit" className="btn btn-primary btn-small" disabled={busy}>저장</button>
                    <button type="button" className="btn btn-quiet btn-small" onClick={() => setEditing(null)}>취소</button>
                  </div>
                </form>
              ) : (
                <div className="series-manage-row">
                  <div className="series-manage-text">
                    <Link to={`/series/${series.seriesId}`} className="series-manage-name">{series.name}</Link>
                    <span className="hint"> 글 {series.postCount}개</span>
                    {series.description && <p className="series-manage-description hint">{series.description}</p>}
                  </div>
                  <div className="category-actions">
                    <button type="button" className="btn btn-quiet btn-small" aria-expanded={openId === series.seriesId}
                      onClick={() => toggle(series.seriesId)}>
                      글 순서
                    </button>
                    <button type="button" className="btn btn-quiet btn-small" disabled={busy}
                      onClick={() => setEditing({ seriesId: series.seriesId, name: series.name, description: series.description })}>
                      고치기
                    </button>
                    <button type="button" className="btn btn-quiet btn-small" disabled={busy} onClick={() => setDeleting(series)}>
                      삭제
                    </button>
                  </div>
                </div>
              )}

              {openId === series.seriesId && detail?.seriesId === series.seriesId && (
                detail.posts.length === 0 ? (
                  <p className="hint series-manage-empty">아직 이 연재에 넣은 글이 없습니다.</p>
                ) : (
                  <ol className="series-manage-posts">
                    {detail.posts.map((post, index) => (
                      <li key={post.postId} className="category-row">
                        <div className="category-order">
                          <button type="button" className="btn btn-quiet btn-icon" disabled={busy || index === 0}
                            aria-label={`${post.title} 위로`} onClick={() => move(index, -1)}>
                            ↑
                          </button>
                          <button type="button" className="btn btn-quiet btn-icon" disabled={busy || index === detail.posts.length - 1}
                            aria-label={`${post.title} 아래로`} onClick={() => move(index, 1)}>
                            ↓
                          </button>
                        </div>
                        <span className="category-name">
                          <span className="series-episode-number">{post.episode}화</span>
                          <Link to={`/posts/${post.postId}`}>{post.title}</Link>
                          {post.visibility === 'private' && <span className="blog-badge">비공개</span>}
                        </span>
                        <div className="category-actions">
                          <button type="button" className="btn btn-quiet btn-small" disabled={busy} onClick={() => removePost(post.postId)}>
                            연재에서 빼기
                          </button>
                        </div>
                      </li>
                    ))}
                  </ol>
                )
              )}
            </li>
          ))}
        </ul>
      )}

      <form className="series-form series-add" onSubmit={add} noValidate>
        <h2 className="series-add-title">새 연재</h2>
        <label htmlFor="new-series-name">이름</label>
        <input id="new-series-name" value={name} placeholder="예: 제주 한 달 살기" maxLength={SERIES_NAME_MAX * 2}
          onChange={(e) => setName(e.target.value)} />
        <label htmlFor="new-series-description">소개 (선택)</label>
        <textarea id="new-series-description" value={description} rows={2} maxLength={SERIES_DESCRIPTION_MAX * 2}
          onChange={(e) => setDescription(e.target.value)} />
        <div className="series-form-actions">
          <button type="submit" className="btn btn-primary btn-small" disabled={busy}>만들기</button>
        </div>
      </form>

      <p className={`msg ${message?.error ? 'msg-error' : 'msg-ok'} category-message`} role="status" aria-live="polite">
        {message?.text}
      </p>

      <ConfirmDialog
        open={deleting !== null}
        title="연재 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && remove(deleting)}
      >
        <p>{deleting?.name} 연재를 삭제할까요? 연재에 넣은 글은 지워지지 않습니다.</p>
      </ConfirmDialog>
    </div>
  )
}
