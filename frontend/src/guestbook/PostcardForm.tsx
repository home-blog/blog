// 엽서 쓰기 (specs/021 US1-2): 원고지 줄 엽서에 적고 우표를 골라 붙인다. 비밀 엽서는 쓴 사람과 주인만 읽는다
import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { lengthOf } from '../explore/rules'
import { writeEntry, type GuestbookEntry, type StampName } from './guestbookApi'
import Stamp from './Stamp'
import { STAMP_LABEL } from './stampLabels'

interface Props {
  blogId: number
  stamps: StampName[]
  maxLength: number
  onWritten: (entry: GuestbookEntry) => void
}

export default function PostcardForm({ blogId, stamps, maxLength, onWritten }: Props) {
  const [body, setBody] = useState('')
  const [stamp, setStamp] = useState<StampName>(stamps[0] ?? 'pine')
  const [secret, setSecret] = useState(false)
  const [empty, setEmpty] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const count = lengthOf(body)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!body.trim()) {
      setEmpty(true)
      return
    }
    setEmpty(false)
    setError(null)
    setBusy(true)
    try {
      const saved = await writeEntry(blogId, body, stamp, secret)
      setBody('')
      setSecret(false)
      onWritten(saved)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="postcard-form" onSubmit={submit} noValidate>
      <div className="postcard-form-sheet">
        <div className="postcard-form-corner" aria-hidden="true">
          <Stamp key={stamp} name={stamp} labelled={false} className="is-stuck" />
        </div>
        <label className="visually-hidden" htmlFor="guestbook-body">
          엽서 내용
        </label>
        <textarea
          id="guestbook-body"
          className="postcard-form-body"
          value={body}
          maxLength={maxLength}
          rows={4}
          placeholder="들른 김에 몇 줄 남겨 주세요"
          aria-invalid={empty || undefined}
          aria-describedby={empty ? 'guestbook-body-error' : undefined}
          onChange={(event) => {
            setBody(event.target.value)
            setEmpty(false)
          }}
        />
        <span className={count > maxLength ? 'postcard-count msg-error' : 'postcard-count hint'}>
          {count} / {maxLength}
        </span>
      </div>
      {empty && (
        <p id="guestbook-body-error" className="msg msg-error">
          ※ 엽서에 몇 자 적어 주세요
        </p>
      )}
      <fieldset className="stamp-picker">
        <legend>우표 고르기</legend>
        {stamps.map((name) => (
          <label key={name} className={name === stamp ? 'stamp-choice is-picked' : 'stamp-choice'}>
            <input
              type="radio"
              name="guestbook-stamp"
              value={name}
              checked={name === stamp}
              onChange={() => setStamp(name)}
            />
            <Stamp name={name} labelled={false} />
            <span className="stamp-choice-name">{STAMP_LABEL[name]}</span>
          </label>
        ))}
      </fieldset>
      <div className="postcard-form-foot">
        <label className="postcard-secret-choice">
          <input type="checkbox" checked={secret} onChange={(event) => setSecret(event.target.checked)} />
          비밀 엽서 <span className="hint">(쓴 사람과 주인만 읽어요)</span>
        </label>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          엽서 보내기
        </button>
      </div>
      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}
