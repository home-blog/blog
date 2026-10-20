// 방명록 = 엽서함 (specs/021): 블로그에 들른 회원이 우표 붙인 엽서를 남기고, 주인은 엽서를 뒤집어 답장한다.
// 누구나 읽는다. 쓰기 칸은 로그인한 회원(주인 말고)에게만 보인다 (실제 권한은 서버가 다시 본다)
import { useEffect, useState } from 'react'
import { Link, useLocation, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { useLoginPrompt } from '../auth/loginPrompt'
import { getBlog, type Blog } from '../blog/blogApi'
import BlogTabs from '../blog/BlogTabs'
import ConfirmDialog from '../components/ConfirmDialog'
import ProofNotFound from '../components/ProofNotFound'
import Pagination from '../explore/Pagination'
import { pageFrom } from '../explore/rules'
import { deleteEntry, getGuestbook, type GuestbookEntry, type GuestbookPage as Page } from './guestbookApi'
import Postcard from './Postcard'
import PostcardForm from './PostcardForm'
import '../pages/auth-layout.css'
import '../blog/blog.css'
import './guestbook.css'

type Result = { key: string; blog: Blog; data: Page } | { key: string; error: string; notFound: boolean }

export default function GuestbookPage() {
  const { blogId } = useParams()
  const [searchParams] = useSearchParams()
  const location = useLocation()
  const id = Number(blogId)
  const validId = Number.isInteger(id) && id > 0
  const page = pageFrom(searchParams.get('page'))
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  // 로그인 상태를 확인하는 중(undefined)에는 부르지 않는다
  const viewer = member === undefined ? null : (member?.id ?? 'guest')
  const key = `${id}:${viewer}:${page}`
  const [result, setResult] = useState<Result | null>(null)
  const [fresh, setFresh] = useState<ReadonlySet<number>>(new Set())
  const [notice, setNotice] = useState('')
  const [deleting, setDeleting] = useState<GuestbookEntry | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    if (!validId || viewer === null) return
    const controller = new AbortController()
    const requestKey = `${id}:${viewer}:${page}`
    Promise.all([getBlog(id, controller.signal), getGuestbook(id, page, controller.signal)])
      .then(([blog, data]) => setResult({ key: requestKey, blog, data }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({
          key: requestKey,
          error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요',
          notFound: err instanceof ApiError && err.status === 404,
        })
      })
    return () => controller.abort()
  }, [id, validId, viewer, page])

  const current = result?.key === key ? result : null
  if (!validId || (current && 'error' in current && current.notFound)) {
    return (
      <ProofNotFound
        struck="글"
        inserted="블로그"
        particle="가"
        message={current && 'error' in current ? current.error : '※ 존재하지 않는 블로그입니다'}
      />
    )
  }
  if (current && 'error' in current) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {current.error}
      </p>
    )
  }
  if (!current) {
    return <p className="blog-status hint">엽서함을 여는 중</p>
  }

  const { blog, data } = current

  function update(change: (data: Page) => Page) {
    setResult((prev) => (prev && 'data' in prev ? { ...prev, data: change(prev.data) } : prev))
  }

  function written(entry: GuestbookEntry) {
    setFresh((set) => new Set(set).add(entry.entryId))
    update((d) => ({ ...d, totalCount: d.totalCount + 1, items: [entry, ...d.items] }))
    setNotice('엽서를 꽂았어요')
  }

  async function confirmDelete() {
    if (!deleting) return
    const target = deleting
    setDeleting(null)
    setDeleteError(null)
    try {
      await deleteEntry(target.entryId)
      update((d) => ({
        ...d,
        totalCount: Math.max(0, d.totalCount - 1),
        items: d.items.filter((e) => e.entryId !== target.entryId),
      }))
      setNotice('엽서를 지웠어요')
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    }
  }

  return (
    <div className="blog-home guestbook">
      <header className="blog-head guestbook-head">
        <p className="guestbook-kicker" aria-hidden="true">
          글결 우체국 · 엽서함
        </p>
        <h1 className="guestbook-title">
          <Link to={`/blog/${id}`}>{blog.name}</Link>
          <span className="guestbook-title-tail">의 방명록</span>
        </h1>
        <BlogTabs blogId={id} current="guestbook" />
        <p className="guestbook-total">
          꽂힌 엽서 <strong>{data.totalCount.toLocaleString('ko-KR')}</strong>장
          <span className="visually-hidden"> (엽서 {data.totalCount}장)</span>
        </p>
      </header>

      <section className="guestbook-write" aria-label="엽서 남기기">
        {member === null ? (
          <p className="guestbook-login hint">
            로그인하면 엽서를 남길 수 있어요.{' '}
            <button type="button" className="btn btn-outline btn-small" onClick={() => openLogin(location.pathname)}>
              로그인
            </button>
          </p>
        ) : blog.isOwner ? (
          <p className="guestbook-owner hint">내 방명록이에요. 엽서를 뒤집어 답장을 남길 수 있어요.</p>
        ) : data.canWrite ? (
          <PostcardForm blogId={id} stamps={data.stamps} maxLength={data.bodyMaxLength} onWritten={written} />
        ) : null}
        <p className="guestbook-notice" role="status">
          {notice}
        </p>
      </section>

      {deleteError && (
        <p className="msg msg-error" role="alert">
          {deleteError}
        </p>
      )}

      {data.items.length === 0 ? (
        <div className="guestbook-empty">
          <span className="guestbook-empty-rack" aria-hidden="true" />
          <p className="hint">아직 꽂힌 엽서가 없어요. 첫 엽서를 남겨 보세요.</p>
        </div>
      ) : (
        <ul className="postcard-grid" aria-label="엽서">
          {data.items.map((entry, index) => (
            <Postcard
              key={entry.entryId}
              entry={entry}
              index={index}
              isNew={fresh.has(entry.entryId)}
              maxLength={data.bodyMaxLength}
              onChange={(changed) =>
                update((d) => ({ ...d, items: d.items.map((e) => (e.entryId === changed.entryId ? changed : e)) }))
              }
              onAskDelete={setDeleting}
            />
          ))}
        </ul>
      )}

      {data.totalPages > 1 && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          hrefFor={(p) => `/blog/${id}/guestbook${p > 1 ? `?page=${p}` : ''}`}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="이 엽서를 지울까요?"
        confirmLabel="지우기"
        danger
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      >
        <p className="hint">답장도 함께 지워지고 되돌릴 수 없어요.</p>
      </ConfirmDialog>
    </div>
  )
}
