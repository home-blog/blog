// 편집실 회원 탭 (specs/022 US2): 닉네임으로 찾기, 정지(기간·사유)·풀기. 정지된 회원 줄에는 붉은 "정지" 도장
// - 주소: /editorial?tab=members&q=&page=. 관리자 줄에는 정지 단추가 없다 (서버도 409로 막는다)
// - 정지 창: 기간 단추(서버가 알려 준 일 수), 사유 1~200자. 실패하면 서버 문구를 창 안에 보여 준다
import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { countChars } from '../auth/rules'
import Pagination from '../explore/Pagination'
import { formatDate, pageFrom } from '../explore/rules'
import '../components/confirm-dialog.css'
import '../components/suspend-seal.css'
import {
  getMembers,
  messageOf,
  REASON_MAX,
  suspendMember,
  unsuspendMember,
  type MemberLine,
  type MemberPage,
} from './editorialApi'

interface Props {
  /** 처리한 뒤 위에 안내를 띄우고 숫자를 다시 읽는다 */
  onDone: (message: string) => void
  /** 바뀌면 목록을 다시 읽는다 */
  reload: number
}

type Loaded = { key: string; data: MemberPage } | { key: string; error: string }

function hrefFor(q: string, page: number): string {
  const query = new URLSearchParams({ tab: 'members' })
  if (q) query.set('q', q)
  if (page > 1) query.set('page', String(page))
  return `/editorial?${query.toString()}`
}

export default function MembersPanel({ onDone, reload }: Props) {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const page = pageFrom(searchParams.get('page'))
  const key = `${q}\n${page}\n${reload}`
  const [draft, setDraft] = useState(q)
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [target, setTarget] = useState<MemberLine | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getMembers(q, page, controller.signal)
      .then((data) => setLoaded({ key, data }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setLoaded({ key, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [q, page, key])

  function search(event: FormEvent) {
    event.preventDefault()
    const next = new URLSearchParams({ tab: 'members' })
    if (draft.trim()) next.set('q', draft.trim())
    setSearchParams(next)
  }

  async function unsuspend(member: MemberLine) {
    setError(null)
    try {
      const { unsuspended } = await unsuspendMember(member.memberId)
      onDone(unsuspended ? `${member.nickname} 회원의 정지를 풀었습니다` : '이미 정지가 풀린 회원입니다')
    } catch (err) {
      setError(messageOf(err))
    }
  }

  // 다시 읽는 동안에도 앞서 읽은 목록을 그대로 둔다 (처리한 줄이 깜빡이지 않게)
  const current = loaded

  return (
    <div className="members-panel">
      <form role="search" className="members-search" onSubmit={search}>
        <input
          type="search"
          aria-label="닉네임"
          placeholder="닉네임 일부"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
        />
        <button type="submit" className="btn btn-outline btn-small">
          찾기
        </button>
      </form>
      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
      {!current ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in current ? (
        <p className="msg msg-error" role="alert">
          {current.error}
        </p>
      ) : current.data.items.length === 0 ? (
        <p className="editorial-empty">{q ? `"${q}"이 들어간 닉네임이 없습니다.` : '회원이 없습니다.'}</p>
      ) : (
        <>
          <ol className="editorial-list members-list" aria-label="회원">
            {current.data.items.map((member) => (
              <li key={member.memberId} className={member.suspended ? 'editorial-row is-suspended' : 'editorial-row'}>
                {member.suspended ? (
                  <span className="suspend-seal">정지</span>
                ) : (
                  <span className="member-cell" aria-hidden="true">
                    {member.nickname.slice(0, 1)}
                  </span>
                )}
                <span className="editorial-row-main">
                  <strong>
                    {member.nickname}
                    {member.role === 'admin' && <span className="member-role">관리자</span>}
                  </strong>
                  <span className="hint">
                    {member.blogName ?? '블로그 없음'} · 가입 {formatDate(member.joinedAt)}
                    {member.suspended &&
                      member.suspendedUntil &&
                      ` · ${formatDate(member.suspendedUntil)}까지 정지 · 사유: ${member.suspendReason ?? ''}`}
                  </span>
                </span>
                {member.role !== 'admin' &&
                  (member.suspended ? (
                    <button type="button" className="btn btn-quiet btn-small" onClick={() => unsuspend(member)}>
                      정지 풀기
                    </button>
                  ) : (
                    <button type="button" className="stamp-button is-red" onClick={() => setTarget(member)}>
                      정지
                    </button>
                  ))}
              </li>
            ))}
          </ol>
          {current.data.totalPages > 1 && (
            <Pagination page={current.data.page} totalPages={current.data.totalPages} hrefFor={(p) => hrefFor(q, p)} />
          )}
          <SuspendDialog
            member={target}
            days={current.data.suspendDays}
            onClose={() => setTarget(null)}
            onDone={(message) => {
              setTarget(null)
              onDone(message)
            }}
          />
        </>
      )}
    </div>
  )
}

interface DialogProps {
  member: MemberLine | null
  days: number[]
  onClose: () => void
  onDone: (message: string) => void
}

/** 정지 창: 기간과 사유. 정지하면 그 회원의 세션이 모두 끝나고 AI 비서 열쇠가 끊긴다 */
function SuspendDialog({ member, days, onClose, onDone }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const id = useId()
  const [choice, setChoice] = useState<number>(days.includes(7) ? 7 : (days[0] ?? 1))
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (member && !dialog.open) {
      setReason('')
      setError(null)
      dialog.showModal()
    }
    if (!member && dialog.open) dialog.close()
  }, [member])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!member || busy) return
    const text = reason.trim()
    if (countChars(text) === 0 || countChars(text) > REASON_MAX) {
      setError(`※ 사유를 1~${REASON_MAX}자로 적어 주세요`)
      return
    }
    setBusy(true)
    setError(null)
    try {
      await suspendMember(member.memberId, choice, text)
      onDone(`${member.nickname} 회원을 정지했습니다 (${choice}일)`)
    } catch (err) {
      setError(messageOf(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <dialog
      ref={ref}
      className="confirm-dialog suspend-dialog"
      aria-labelledby={`${id}-title`}
      onCancel={(event) => {
        if (busy) event.preventDefault()
      }}
      onClose={onClose}
    >
      <form onSubmit={submit} noValidate>
        <h2 id={`${id}-title`} className="confirm-title">
          {member ? `${member.nickname} 회원 정지` : '회원 정지'}
        </h2>
        <p className="hint">정지하면 그 회원의 로그인이 모두 끝나고, 기간이 지날 때까지 로그인할 수 없습니다.</p>
        <fieldset className="suspend-days" disabled={busy}>
          <legend className="hint">기간</legend>
          {days.map((d) => (
            <label key={d} className={choice === d ? 'suspend-day is-chosen' : 'suspend-day'}>
              <input type="radio" name={`${id}-days`} checked={choice === d} onChange={() => setChoice(d)} />
              {d}일
            </label>
          ))}
        </fieldset>
        <label htmlFor={`${id}-reason`} className="review-label">
          정지 사유 (로그인 창에 보입니다)
        </label>
        <textarea
          id={`${id}-reason`}
          rows={2}
          value={reason}
          disabled={busy}
          onChange={(e) => {
            setReason(e.target.value)
            setError(null)
          }}
          placeholder="예: 광고 댓글을 되풀이해 남겼습니다"
        />
        {error && (
          <p className="msg msg-error" role="alert">
            {error}
          </p>
        )}
        <div className="confirm-actions">
          <button type="button" className="btn btn-quiet" disabled={busy} onClick={onClose}>
            취소
          </button>
          <button type="submit" className="btn btn-danger" disabled={busy}>
            {busy ? '정지하는 중' : '정지하기'}
          </button>
        </div>
      </form>
    </dialog>
  )
}
