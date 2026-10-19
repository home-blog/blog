// 원고 편지 그만 받기 (specs/016 US3, S3-3): 메일 속 링크(/letters/stop?token=…)로 로그인 없이 온다.
// 메일 미리보기 로봇이 링크만 열어도 꺼지지 않게, 이 화면에서 단추를 눌러야 끈다
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { stopLetters } from './subscriptionApi'
import './subscription.css'

type State =
  | { kind: 'ready' }
  | { kind: 'busy' }
  | { kind: 'done'; blogName: string | null }
  | { kind: 'error'; message: string }

export default function LetterStopPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [state, setState] = useState<State>({ kind: 'ready' })

  async function stop() {
    setState({ kind: 'busy' })
    try {
      const { blogName } = await stopLetters(token)
      setState({ kind: 'done', blogName })
    } catch (err) {
      setState({ kind: 'error', message: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
    }
  }

  return (
    <div className="letter-stop">
      <h1 className="library-title">
        <span className="ink-highlight">원고 편지 그만 받기</span>
      </h1>
      {state.kind === 'done' ? (
        <p className="msg msg-ok" role="status">
          {state.blogName ? `"${state.blogName}"의 ` : ''}원고 편지를 그만 받습니다. 받아 보기는 그대로라 우편함에는 새
          원고 소식이 옵니다.
        </p>
      ) : !token ? (
        <p className="msg msg-error" role="alert">
          ※ 메일의 그만 받기 주소가 올바르지 않습니다
        </p>
      ) : (
        <>
          <p>이 블로그의 새 원고를 더는 메일로 받지 않습니다. 로그인하지 않아도 됩니다.</p>
          {state.kind === 'error' && (
            <p className="msg msg-error" role="alert">
              {state.message}
            </p>
          )}
          <p>
            <button type="button" className="btn" disabled={state.kind === 'busy'} onClick={stop}>
              메일 그만 받기
            </button>
          </p>
        </>
      )}
      <p>
        <Link to="/">글결 첫 화면으로</Link>
      </p>
    </div>
  )
}
