// 글 좋아요 버튼 (specs/005 US3, T033): 개수와 "내가 눌렀나"(aria-pressed)
// - 지금 상태(likedByMe)를 보고 누르기(PUT)·취소(DELETE)를 고른다 (research B-8). 보내는 중에는 잠근다
// - 자기 글에서는 누를 수 없다 (서버도 막는다, FR-012)
// - 로그인하지 않았으면 로그인 창을 띄우고, 로그인하면 이 글로 돌아온다 (FR-009)
import { useRef, useState } from 'react'
import { useLocation } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import { likePost, unlikePost, type LikeState } from './communityApi'
import './community.css'

interface Props {
  postId: number
  initial: LikeState
  isOwner: boolean
}

export default function LikeButton({ postId, initial, isOwner }: Props) {
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const location = useLocation()
  const [state, setState] = useState(initial)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)

  async function toggle() {
    if (busyRef.current) return
    if (!member) {
      openLogin(location.pathname)
      return
    }
    busyRef.current = true
    setBusy(true)
    setError(null)
    try {
      setState(await (state.likedByMe ? unlikePost(postId) : likePost(postId)))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  return (
    <div className="like">
      <button
        type="button"
        className={`like-button${state.likedByMe ? ' is-on' : ''}`}
        aria-pressed={state.likedByMe}
        aria-label={`좋아요 ${state.likeCount}개${isOwner ? ' (내 글에는 누를 수 없습니다)' : ''}`}
        disabled={busy || isOwner}
        onClick={toggle}
      >
        <span aria-hidden="true">{state.likedByMe ? '♥' : '♡'}</span> 좋아요{' '}
        <span className="like-count">{state.likeCount}</span>
      </button>
      {error && (
        <p className="msg msg-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
