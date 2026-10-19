// 원고 받아 보기 (specs/016 US1, US3): 블로그 머리의 받아 보는 사람 수와 단추. 받아 보는 중이면 "새 원고를 메일로도 받기"를 고른다.
// 내 블로그에서는 수만 보인다. 로그인하지 않았으면 로그인 창을 띄우고, 로그인하면 이 블로그로 돌아온다
import { useRef, useState } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import { subscribe, unsubscribe, type SubscriptionState } from './subscriptionApi'
import './subscription.css'

interface Props {
  blogId: number
  isOwner: boolean
  initial: { subscriberCount: number; subscribedByMe: boolean; emailEnabled?: boolean }
}

export default function SubscribeButton({ blogId, isOwner, initial }: Props) {
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const location = useLocation()
  const [state, setState] = useState<SubscriptionState>({
    subscribed: initial.subscribedByMe,
    emailEnabled: initial.emailEnabled ?? false,
    subscriberCount: initial.subscriberCount,
  })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)

  async function run(action: () => Promise<SubscriptionState>) {
    if (busyRef.current) return
    if (!member) {
      openLogin(location.pathname)
      return
    }
    busyRef.current = true
    setBusy(true)
    setError(null)
    try {
      setState(await action())
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  const count = (
    <span className="subscribe-count">
      받아 보는 사람 <strong>{state.subscriberCount.toLocaleString('ko-KR')}</strong>명
    </span>
  )
  if (isOwner) return <p className="subscribe">{count}</p>

  return (
    <div className="subscribe">
      <button
        type="button"
        className={
          state.subscribed ? 'btn btn-outline btn-small subscribe-button is-on' : 'btn btn-small subscribe-button'
        }
        aria-pressed={state.subscribed}
        disabled={busy}
        onClick={() => run(() => (state.subscribed ? unsubscribe(blogId) : subscribe(blogId, false)))}
      >
        {state.subscribed ? '✓ 받아 보는 중' : '✉ 원고 받아 보기'}
      </button>
      {count}
      {state.subscribed && (
        <label className="subscribe-email">
          <input
            type="checkbox"
            checked={state.emailEnabled}
            disabled={busy}
            onChange={(event) => run(() => subscribe(blogId, event.target.checked))}
          />
          새 원고를 메일로도 받기
        </label>
      )}
      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
