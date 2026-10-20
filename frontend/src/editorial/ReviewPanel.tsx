// 신고 자세히와 처리 (specs/018 US1-3~5, US2-2): 신고마다 사유·설명·시각(신고한 사람은 없음), 글 앞부분,
// 사유를 적고 붉은 "내림"(글마당은 "지우기", 댓글은 "가리기", specs/022) 도장 또는 먹색 "반려" 도장을 찍는다
import { useEffect, useId, useState } from 'react'
import { ApiError } from '../api/client'
import { formatDate } from '../explore/rules'
import { countChars } from '../auth/rules'
import {
  dismissBoardPost,
  dismissComment,
  dismissPost,
  getBoardReview,
  getCommentReview,
  getPostReview,
  hideComment,
  messageOf,
  REASON_LABEL,
  REASON_MAX,
  removeBoardPost,
  takeDown,
  type ReportView,
} from './editorialApi'

export type ReviewKind = 'post' | 'board' | 'comment'

const LABELS: Record<ReviewKind, { stamp: string; reason: string; done: string }> = {
  post: { stamp: '내림', reason: '내림 사유', done: '글을 내렸습니다' },
  board: { stamp: '지우기', reason: '지우기 사유', done: '글을 지웠습니다' },
  comment: { stamp: '가리기', reason: '가리기 사유', done: '댓글을 가렸습니다' },
}

interface Props {
  kind: ReviewKind
  id: number
  /** 처리한 뒤 목록을 다시 읽는다 */
  onDone: (message: string) => void
}

interface Review {
  title: string
  where: string
  excerpt: string
  reports: ReportView[]
  /** 저절로 가린 댓글: 반려하면 다시 보인다 */
  autoHidden?: boolean
}

export default function ReviewPanel({ kind, id, onDone }: Props) {
  const reasonId = useId()
  const [review, setReview] = useState<Review | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    const load =
      kind === 'post'
        ? getPostReview(id, controller.signal).then((r) => ({
            title: r.title,
            where: `${r.blogName}${r.visibility === 'private' ? ' · 비공개' : ''}`,
            excerpt: r.excerpt,
            reports: r.reports,
          }))
        : kind === 'board'
          ? getBoardReview(id, controller.signal).then((r) => ({
              title: r.title,
              where: `글마당 · ${r.topic}`,
              excerpt: r.excerpt,
              reports: r.reports,
            }))
          : getCommentReview(id, controller.signal).then((r) => ({
              title: r.postTitle ?? '',
              where: `댓글 · ${r.writerNickname ?? '탈퇴한 사용자'}${r.hidden ? (r.hiddenBy === 'auto' ? ' · 저절로 가림' : ' · 가림') : ''}`,
              excerpt: r.body,
              reports: r.reports,
              autoHidden: r.hidden && r.hiddenBy === 'auto',
            }))
    load.then(setReview).catch((err: unknown) => {
      if (!controller.signal.aborted) setError(messageOf(err))
    })
    return () => controller.abort()
  }, [kind, id])

  async function act(action: 'remove' | 'dismiss') {
    setError(null)
    const text = reason.trim()
    if (action === 'remove' && (countChars(text) === 0 || countChars(text) > REASON_MAX)) {
      setError(`※ 사유를 1~${REASON_MAX}자로 적어 주세요`)
      return
    }
    setBusy(true)
    try {
      if (kind === 'post') {
        await (action === 'remove' ? takeDown(id, text) : dismissPost(id))
      } else if (kind === 'board') {
        await (action === 'remove' ? removeBoardPost(id, text) : dismissBoardPost(id))
      } else {
        await (action === 'remove' ? hideComment(id, text) : dismissComment(id))
      }
      onDone(
        action === 'remove'
          ? LABELS[kind].done
          : review?.autoHidden
            ? '반려했습니다. 댓글이 다시 보입니다'
            : '반려했습니다',
      )
    } catch (err) {
      // 다른 관리자가 먼저 처리했으면 목록을 다시 읽어 그 줄을 치운다
      if (err instanceof ApiError && err.status === 409) {
        onDone(err.message)
        return
      }
      setError(messageOf(err))
      setBusy(false)
    }
  }

  if (!review) {
    return error ? (
      <p className="msg msg-error" role="alert">
        {error}
      </p>
    ) : (
      <p className="hint">불러오는 중</p>
    )
  }

  return (
    <div className="review-panel">
      <p className="review-where">{review.where}</p>
      <blockquote className="review-excerpt">{review.excerpt || '(본문 없음)'}</blockquote>
      <ol className="review-reports">
        {review.reports.map((report, i) => (
          <li key={i}>
            <span className="review-reason">{REASON_LABEL[report.reason]}</span>
            {report.detail && <span className="review-detail">{report.detail}</span>}
            <time dateTime={report.createdAt}>{formatDate(report.createdAt)}</time>
          </li>
        ))}
      </ol>
      <label htmlFor={reasonId} className="review-label">
        {LABELS[kind].reason} (쓴 사람에게 보입니다)
      </label>
      <textarea
        id={reasonId}
        rows={2}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        placeholder="예: 광고 링크만 있는 글입니다"
      />
      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
      <p className="review-actions">
        <button type="button" className="stamp-button is-red" disabled={busy} onClick={() => act('remove')}>
          {LABELS[kind].stamp}
        </button>
        <button type="button" className="stamp-button" disabled={busy} onClick={() => act('dismiss')}>
          반려
        </button>
      </p>
    </div>
  )
}
