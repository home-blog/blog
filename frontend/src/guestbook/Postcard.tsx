// 엽서 한 장 (specs/021 US1·US2): 앞면은 우표·소인·내용·쓴 사람, 뒷면은 블로그 주인의 답장.
// 남의 비밀 엽서는 밀랍으로 봉한 봉투로 보인다. 엽서마다 조금씩 기울어 꽂혀 있고, 새로 꽂힌 엽서는 떨어지며 소인이 찍힌다
import { useState, type CSSProperties, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { formatDate, lengthOf } from '../explore/rules'
import { replyEntry, deleteReply, type GuestbookEntry } from './guestbookApi'
import Stamp from './Stamp'

/** 소인 날짜: 10.19 */
function postmarkDate(iso: string) {
  const date = new Date(iso)
  return `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

/** 엽서마다 늘 같은 기울기 (-1.8° ~ 1.8°) */
function tiltOf(entryId: number) {
  return `${(((entryId * 37) % 7) - 3) * 0.6}deg`
}

interface Props {
  entry: GuestbookEntry
  index: number
  isNew: boolean
  maxLength: number
  onChange: (entry: GuestbookEntry) => void
  onAskDelete: (entry: GuestbookEntry) => void
}

export default function Postcard({ entry, index, isNew, maxLength, onChange, onAskDelete }: Props) {
  const [flipped, setFlipped] = useState(false)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const hasBack = !entry.sealed && (entry.reply !== null || entry.canReply)
  const className = ['postcard', `postcard-${entry.stamp}`, isNew && 'is-new', entry.sealed && 'is-sealed']
    .filter(Boolean)
    .join(' ')
  const style = { '--tilt': tiltOf(entry.entryId), '--i': index } as CSSProperties

  function turn(toBack: boolean) {
    setFlipped(toBack)
    setError(null)
    if (toBack && entry.reply === null && entry.canReply) {
      setDraft('')
      setEditing(true)
    } else {
      setEditing(false)
    }
  }

  async function sendReply(event: FormEvent) {
    event.preventDefault()
    if (!draft.trim()) {
      setError('※ 답장에 몇 자 적어 주세요')
      return
    }
    setBusy(true)
    setError(null)
    try {
      onChange(await replyEntry(entry.entryId, draft))
      setEditing(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    } finally {
      setBusy(false)
    }
  }

  async function removeReply() {
    setBusy(true)
    setError(null)
    try {
      await deleteReply(entry.entryId)
      onChange({ ...entry, reply: null, repliedAt: null })
      setDraft('')
      setEditing(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    } finally {
      setBusy(false)
    }
  }

  const author = entry.author.withdrawn ? '탈퇴한 회원' : (entry.author.nickname ?? '')

  return (
    <li className={className} style={style}>
      <div key={flipped ? 'back' : 'front'} className={flipped ? 'postcard-face postcard-back' : 'postcard-face'}>
        <div className="postcard-corner">
          <Stamp name={entry.stamp} />
          <span className="postcard-postmark" aria-hidden="true">
            <span>{postmarkDate(entry.createdAt)}</span>
            <small>글결</small>
          </span>
        </div>

        {!flipped ? (
          entry.sealed ? (
            <div className="postcard-sealed">
              <span className="wax-seal" aria-hidden="true">
                봉
              </span>
              <p className="postcard-sealed-title">봉한 엽서</p>
              <p className="hint">쓴 사람과 블로그 주인만 읽을 수 있어요</p>
            </div>
          ) : (
            <>
              {entry.secret && (
                <span className="postcard-secret" title="쓴 사람과 블로그 주인만 읽어요">
                  비밀
                </span>
              )}
              <p className="postcard-body">{entry.body}</p>
              <p className="postcard-from">
                <span aria-hidden="true">— </span>
                {author}
              </p>
            </>
          )
        ) : (
          <div className="postcard-reply">
            <p className="postcard-reply-label">주인의 답장</p>
            {editing ? (
              <form className="postcard-reply-form" onSubmit={sendReply}>
                <textarea
                  aria-label="답장"
                  value={draft}
                  maxLength={maxLength}
                  rows={4}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="찾아와 줘서 고마워요…"
                />
                <span className="postcard-count hint">
                  {lengthOf(draft)} / {maxLength}
                </span>
                <div className="postcard-actions">
                  <button type="submit" className="btn btn-primary btn-small" disabled={busy}>
                    답장 보내기
                  </button>
                  {entry.reply !== null && (
                    <button type="button" className="btn btn-quiet btn-small" onClick={() => setEditing(false)}>
                      그만두기
                    </button>
                  )}
                </div>
              </form>
            ) : (
              <>
                <p className="postcard-body postcard-reply-body">{entry.reply}</p>
                {entry.canReply && (
                  <div className="postcard-actions">
                    <button
                      type="button"
                      className="btn btn-quiet btn-small"
                      onClick={() => {
                        setDraft(entry.reply ?? '')
                        setEditing(true)
                      }}
                    >
                      답장 고치기
                    </button>
                    <button type="button" className="btn btn-quiet btn-small" onClick={removeReply} disabled={busy}>
                      답장 지우기
                    </button>
                  </div>
                )}
              </>
            )}
            {error && (
              <p className="msg msg-error" role="alert">
                {error}
              </p>
            )}
          </div>
        )}

        <div className="postcard-foot">
          <time className="hint" dateTime={entry.createdAt}>
            {formatDate(entry.createdAt)}
          </time>
          <span className="postcard-tools">
            {hasBack &&
              (flipped ? (
                <button type="button" className="postcard-flip" aria-expanded="true" onClick={() => turn(false)}>
                  엽서 앞면
                </button>
              ) : (
                <button type="button" className="postcard-flip" aria-expanded="false" onClick={() => turn(true)}>
                  {entry.reply !== null ? '답장 보기' : '답장 쓰기'}
                  {entry.reply !== null && <span className="postcard-replied" aria-hidden="true" />}
                </button>
              ))}
            {entry.canDelete && (
              <button type="button" className="postcard-delete" onClick={() => onAskDelete(entry)}>
                엽서 지우기
              </button>
            )}
          </span>
        </div>
      </div>
    </li>
  )
}
