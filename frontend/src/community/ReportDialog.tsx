// 글 신고 (specs/005 US6, T039): 신고 버튼 → 창에서 사유 넷 중 하나, 기타일 때만 설명(0~200자)
// - 접수되면 "신고가 접수되었습니다", 이미 신고했으면 서버 문구("이미 신고한 글입니다")를 보여 준다
// - 자기 글에서는 버튼을 숨긴다 (서버도 막는다, FR-017). 로그인하지 않았으면 로그인 창
// - 취소하거나 창을 닫으면 아무것도 보내지 않는다. 보내는 중에는 닫을 수 없다
import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { countChars } from '../auth/rules'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import '../comment/comment.css'
import '../components/confirm-dialog.css'
import { reportPost, type ReportReason } from './communityApi'
import './community.css'

/** 서버 설정 community.report.detail-max-length와 같다 */
const DETAIL_MAX_LENGTH = 200

const REASONS: { value: ReportReason; label: string }[] = [
  { value: 'SPAM', label: '스팸' },
  { value: 'ABUSE', label: '욕설·혐오' },
  { value: 'ADULT', label: '음란물' },
  { value: 'OTHER', label: '기타' },
]

const messages = {
  reasonRequired: '※ 신고 사유를 골라 주세요',
  detailTooLong: `※ 신고 내용은 ${DETAIL_MAX_LENGTH}자 이하로 입력해 주세요`,
  failed: '※ 잠시 뒤 다시 시도해 주세요',
} as const

export default function ReportDialog({ postId }: { postId: number }) {
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const location = useLocation()
  const ref = useRef<HTMLDialogElement>(null)
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState<ReportReason | null>(null)
  const [detail, setDetail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState<string | null>(null)
  const id = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  const detailLength = countChars(detail.trim())

  function start() {
    if (!member) {
      openLogin(location.pathname)
      return
    }
    setReason(null)
    setDetail('')
    setError(null)
    setOpen(true)
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    if (!reason) return setError(messages.reasonRequired)
    if (reason === 'OTHER' && detailLength > DETAIL_MAX_LENGTH) return setError(messages.detailTooLong)
    setBusy(true)
    setError(null)
    try {
      const res = await reportPost(postId, reason, detail)
      setOpen(false)
      setDone(res.message)
    } catch (err) {
      if (err instanceof ApiError && err.code === 'ALREADY_REPORTED') {
        setOpen(false)
        setDone(err.message)
      } else {
        setError(err instanceof ApiError ? (err.fieldErrors[0]?.message ?? err.message) : messages.failed)
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="report">
      <button type="button" className="btn btn-quiet btn-small" onClick={start}>
        신고
      </button>
      {done && (
        <p className="msg msg-ok" role="status">
          {done}
        </p>
      )}
      <dialog
        ref={ref}
        className="confirm-dialog"
        aria-labelledby={`${id}-title`}
        onCancel={(event) => {
          // 보내는 중에는 Esc로 닫지 않는다 (실패 문구가 창 안에 나오므로)
          if (busy) event.preventDefault()
        }}
        onClose={() => setOpen(false)}
      >
        <form onSubmit={submit} noValidate>
          <h2 id={`${id}-title`} className="confirm-title">
            글 신고
          </h2>
          <fieldset className="report-reasons" disabled={busy}>
            <legend className="hint">신고 사유를 골라 주세요</legend>
            {REASONS.map((item) => (
              <label key={item.value} className="report-reason">
                <input
                  type="radio"
                  name={`${id}-reason`}
                  value={item.value}
                  checked={reason === item.value}
                  onChange={() => {
                    setReason(item.value)
                    setError(null)
                  }}
                />
                {item.label}
              </label>
            ))}
          </fieldset>
          {reason === 'OTHER' && (
            <div className="report-detail">
              <label htmlFor={`${id}-detail`} className="hint">
                신고 내용 (선택)
              </label>
              <textarea
                id={`${id}-detail`}
                className="comment-input"
                rows={3}
                value={detail}
                disabled={busy}
                onChange={(e) => {
                  setDetail(e.target.value)
                  setError(null)
                }}
              />
              <span className={`comment-length${detailLength > DETAIL_MAX_LENGTH ? ' is-over' : ''}`}>
                {detailLength} / {DETAIL_MAX_LENGTH}
              </span>
            </div>
          )}
          {error && (
            <p className="msg msg-error" role="alert">
              {error}
            </p>
          )}
          <div className="confirm-actions">
            <button type="button" className="btn btn-quiet" disabled={busy} onClick={() => setOpen(false)}>
              취소
            </button>
            <button type="submit" className="btn btn-danger" disabled={busy}>
              {busy ? '보내는 중' : '신고'}
            </button>
          </div>
        </form>
      </dialog>
    </div>
  )
}
