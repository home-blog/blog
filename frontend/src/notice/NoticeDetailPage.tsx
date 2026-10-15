// 공지사항 읽기 (specs/012 US2): /notices/:noticeId. 누구나 본다
// 업데이트 글에는 그 번호의 GitHub 릴리스 링크. 관리자에게는 수정·삭제
import { lazy, Suspense, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import ConfirmDialog from '../components/ConfirmDialog'
import ProofNotFound from '../components/ProofNotFound'
import { formatDate } from '../explore/rules'
import NoticeTag from './NoticeTag'
import { deleteNotice, getNotice, type NoticeDetail } from './noticeApi'
import '../post/post-detail.css'
import './notice.css'

const MarkdownView = lazy(() => import('../post/MarkdownView'))

type Result = { key: string; notice: NoticeDetail } | { key: string; error: string; notFound: boolean }

export default function NoticeDetailPage() {
  const { noticeId = '' } = useParams()
  const id = Number(noticeId)
  const validId = /^\d+$/.test(noticeId) && Number.isSafeInteger(id) && id > 0
  const { member } = useAuth()
  const navigate = useNavigate()
  const [result, setResult] = useState<Result | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    if (!validId) return
    const controller = new AbortController()
    getNotice(id, controller.signal)
      .then((notice) => setResult({ key: noticeId, notice }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        const notFound = err instanceof ApiError && err.status === 404
        setResult({
          key: noticeId,
          notFound,
          error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요',
        })
      })
    return () => controller.abort()
  }, [id, noticeId, validId])

  const current = result?.key === noticeId ? result : null
  if (!validId || (current && 'error' in current && current.notFound)) {
    return (
      <ProofNotFound
        struck="쪽"
        inserted="공지"
        particle="가"
        message={current && 'error' in current ? current.error : '※ 존재하지 않는 공지사항입니다'}
      />
    )
  }
  if (current && 'error' in current) {
    return (
      <p className="post-status msg msg-error" role="alert">
        {current.error}
      </p>
    )
  }
  if (!current) return <p className="post-status hint">불러오는 중</p>

  const { notice } = current

  async function handleDelete() {
    setConfirmDelete(false)
    setDeleteError(null)
    try {
      await deleteNotice(notice.noticeId)
      navigate('/notices', { replace: true })
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    }
  }

  return (
    <article className="post notice-detail">
      <p className="notice-back">
        <Link to="/notices">← 공지사항</Link>
      </p>
      <header className="post-head">
        <p className="notice-row-meta">
          {notice.pinned && <span className="notice-pin">고정</span>}
          <NoticeTag kind={notice.kind} />
          {notice.releaseVersion && <span className="notice-version">v{notice.releaseVersion}</span>}
          <time dateTime={notice.createdAt}>{formatDate(notice.createdAt)}</time>
          {notice.updatedAt && <span className="hint">(수정됨)</span>}
        </p>
        <h1 className="post-title">{notice.title}</h1>
      </header>
      <Suspense fallback={<p className="hint">불러오는 중</p>}>
        <MarkdownView source={notice.content} />
      </Suspense>
      {notice.releaseUrl && (
        <p className="notice-release">
          <a href={notice.releaseUrl} target="_blank" rel="noopener noreferrer">
            자세한 변경 기록 (v{notice.releaseVersion}) ↗
          </a>
        </p>
      )}
      {member?.admin && (
        <p className="notice-actions">
          <Link to={`/notices/${notice.noticeId}/edit`} className="btn btn-outline btn-small">
            수정
          </Link>
          <button type="button" className="btn btn-danger btn-small" onClick={() => setConfirmDelete(true)}>
            삭제
          </button>
        </p>
      )}
      {deleteError && (
        <p className="msg msg-error" role="alert">
          {deleteError}
        </p>
      )}
      <ConfirmDialog
        open={confirmDelete}
        title="공지사항 삭제"
        confirmLabel="삭제"
        danger
        onConfirm={handleDelete}
        onCancel={() => setConfirmDelete(false)}
      >
        <p>지운 공지사항은 다시 볼 수 없습니다. 삭제할까요?</p>
      </ConfirmDialog>
    </article>
  )
}
