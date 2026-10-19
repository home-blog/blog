// 문장 수집 (specs/015 US3): 글 본문에서 문장을 끌어 고르면 그 위에 작은 "수집" 단추가 뜬다.
// 누르면 필사 노트에 옮겨 적고 "필사 노트에 옮겨 적었어요"를 보여 준다. 로그인하지 않았으면 로그인 창을 띄운다.
// 본문 밖을 고르거나 너무 길게 고르면 단추를 띄우지 않는다 (서버가 본문에 있는 문장인지 다시 확인한다)
import { useEffect, useState, type RefObject } from 'react'
import { Link, useLocation } from 'react-router'
import { ApiError } from '../../api/client'
import { useLoginPrompt } from '../../auth/loginPrompt'
import { useAuth } from '../../auth/useAuth'
import { CLIP_MAX_LENGTH, collectSentence, tidySelection } from './clipApi'
import './clip.css'

interface Props {
  postId: number
  /** 글 본문을 감싼 요소. 단추는 이 요소를 기준으로 놓인다 */
  container: RefObject<HTMLElement | null>
}

interface Picked {
  text: string
  top: number
  left: number
}

type Notice = { kind: 'ok'; text: string } | { kind: 'error'; text: string }

function pickedIn(container: HTMLElement): Picked | null {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed || selection.rangeCount === 0) return null
  const range = selection.getRangeAt(0)
  if (!container.contains(range.commonAncestorContainer)) return null
  const text = tidySelection(selection.toString())
  if (!text || Array.from(text).length > CLIP_MAX_LENGTH) return null
  const base = container.getBoundingClientRect()
  // 고른 곳의 위치 (아주 오래된 브라우저처럼 범위의 위치를 모르면 본문 맨 위에)
  const box = typeof range.getBoundingClientRect === 'function' ? range.getBoundingClientRect() : base
  return { text, top: box.top - base.top, left: box.left - base.left + box.width / 2 }
}

export default function SentenceCollector({ postId, container }: Props) {
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const location = useLocation()
  const [picked, setPicked] = useState<Picked | null>(null)
  const [notice, setNotice] = useState<Notice | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const element = container.current
    if (!element) return
    const update = () => setPicked(pickedIn(element))
    // 고르기를 마친 때(마우스·손가락·키보드)에 단추를 놓고, 고른 것이 풀리거나 본문 밖(댓글 등)으로 옮겨 가면 바로 거둔다
    const onSelectionChange = () => {
      const selection = window.getSelection()
      if (
        !selection ||
        selection.isCollapsed ||
        selection.rangeCount === 0 ||
        !element.contains(selection.getRangeAt(0).commonAncestorContainer)
      ) {
        setPicked(null)
      }
    }
    element.addEventListener('mouseup', update)
    element.addEventListener('touchend', update)
    element.addEventListener('keyup', update)
    document.addEventListener('selectionchange', onSelectionChange)
    return () => {
      element.removeEventListener('mouseup', update)
      element.removeEventListener('touchend', update)
      element.removeEventListener('keyup', update)
      document.removeEventListener('selectionchange', onSelectionChange)
    }
  }, [container])

  async function collect() {
    if (!picked || busy) return
    if (!member) {
      openLogin(location.pathname)
      return
    }
    setBusy(true)
    try {
      await collectSentence(postId, picked.text)
      setNotice({ kind: 'ok', text: '필사 노트에 옮겨 적었어요' })
      window.getSelection()?.removeAllRanges()
      setPicked(null)
    } catch (err) {
      setNotice({ kind: 'error', text: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      {picked && (
        <button
          type="button"
          className="clip-button"
          style={{ top: picked.top, left: picked.left }}
          // 누르는 순간 고른 글자가 풀리지 않게
          onMouseDown={(event) => event.preventDefault()}
          onClick={collect}
          disabled={busy}
        >
          <span aria-hidden="true">✎</span> 수집
        </button>
      )}
      {notice && (
        <p className={notice.kind === 'ok' ? 'clip-notice msg msg-ok' : 'clip-notice msg msg-error'} role="status">
          {notice.text}
          {notice.kind === 'ok' && <Link to="/mypage/library">필사 노트 보기</Link>}
          <button
            type="button"
            className="btn btn-quiet btn-small"
            onClick={() => setNotice(null)}
            aria-label="알림 닫기"
          >
            닫기
          </button>
        </p>
      )}
    </>
  )
}
