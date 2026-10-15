// 글 관리의 "임시 저장" 칸 (specs/013 US3-3, US4-2): 이어 쓰기, 지우기, 예약 표시·취소
// 임시 저장 글이 없으면 아무것도 보이지 않는다
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import ConfirmDialog from '../components/ConfirmDialog'
import { deleteDraft, getDrafts, unscheduleDraft, type DraftItem, type DraftList } from '../post/draftApi'
import { publishAtLabel, savedAtLabel } from '../post/draftRules'

export default function DraftsPanel() {
  const [list, setList] = useState<DraftList | null>(null)
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)
  const [deleting, setDeleting] = useState<DraftItem | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    getDrafts(controller.signal)
      .then(setList)
      .catch(() => undefined)
    return () => controller.abort()
  }, [])

  async function run(action: () => Promise<void>, done: string) {
    if (busy) return
    setBusy(true)
    setMessage(null)
    try {
      await action()
      setList(await getDrafts())
      setMessage({ text: done, error: false })
    } catch (err) {
      setMessage({ text: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요', error: true })
    } finally {
      setBusy(false)
    }
  }

  if (!list || list.items.length === 0) return null
  return (
    <section className="manage-drafts" aria-labelledby="manage-drafts-title">
      <h2 id="manage-drafts-title" className="manage-drafts-title">
        임시 저장 <span className="hint">{list.count} / {list.maxCount}</span>
      </h2>
      <ul className="manage-list">
        {list.items.map((draft) => (
          <li key={draft.draftId} className="manage-row">
            <div className="manage-row-main">
              <Link to={`/write?draft=${draft.draftId}`} className="manage-row-title">
                {draft.title.trim() === '' ? <span className="hint">제목 없음</span> : draft.title}
              </Link>
            </div>
            <p className="manage-row-meta">
              {draft.publishAt ? (
                <span className="manage-draft-scheduled">{publishAtLabel(draft.publishAt)}</span>
              ) : (
                <span>임시 저장</span>
              )}
              <time dateTime={draft.updatedAt}>{savedAtLabel(draft.updatedAt).replace('임시 저장됨', '마지막 저장')}</time>
            </p>
            <div className="manage-row-actions">
              <Link to={`/write?draft=${draft.draftId}`} className="btn btn-quiet btn-small">
                이어 쓰기
              </Link>
              {draft.publishAt && (
                <button type="button" className="btn btn-quiet btn-small" disabled={busy}
                  onClick={() => void run(() => unscheduleDraft(draft.draftId).then(() => undefined), '예약을 취소했습니다. 임시 저장 글로 남습니다')}>
                  예약 취소
                </button>
              )}
              <button type="button" className="btn btn-quiet btn-small" disabled={busy} onClick={() => setDeleting(draft)}>
                삭제
              </button>
            </div>
          </li>
        ))}
      </ul>
      <p className={`msg ${message?.error ? 'msg-error' : 'msg-ok'} manage-message`} role="status" aria-live="polite">
        {message?.text}
      </p>
      <ConfirmDialog
        open={deleting !== null}
        title="임시 저장 글 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setDeleting(null)}
        onConfirm={() => {
          const target = deleting
          setDeleting(null)
          if (target) void run(() => deleteDraft(target.draftId), '임시 저장 글을 지웠습니다')
        }}
      >
        <p>지우면 되돌릴 수 없습니다. 지울까요?</p>
      </ConfirmDialog>
    </section>
  )
}
