// 머리글의 내 메뉴 (2026-10-12 사용자: 내 블로그·블로그 관리·마이페이지를 한 번에 묶고, 글로만 나열하지 말고 예쁜 버튼으로)
// - 이름 첫 글자 동그라미 + 닉네임 버튼을 누르면 카드 메뉴가 펼쳐진다 (새 댓글이 있으면 버튼에 점, 메뉴에 숫자)
// - 바깥을 누르거나 Esc를 누르면 닫히고, 메뉴에서 하나를 고르면 닫힌다. 화살표 키로 항목을 옮겨 다닌다
import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { Link } from 'react-router'
import NewBadge from '../manage/NewBadge'

interface Props {
  nickname: string
  newCommentCount: number | null
  onLogout: () => void
}

export default function UserMenu({ nickname, newCommentCount, onLogout }: Props) {
  const [open, setOpen] = useState(false)
  const box = useRef<HTMLDivElement>(null)
  const button = useRef<HTMLButtonElement>(null)
  const menuId = useId()

  useEffect(() => {
    if (!open) return
    const onDown = (e: PointerEvent) => {
      if (!box.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onDown)
    // 펼치면 첫 항목으로 간다
    box.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus()
    return () => document.removeEventListener('pointerdown', onDown)
  }, [open])

  function close(focusButton = true) {
    setOpen(false)
    if (focusButton) button.current?.focus()
  }

  function onMenuKey(e: KeyboardEvent<HTMLDivElement>) {
    const items = Array.from(box.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])
    const index = items.indexOf(document.activeElement as HTMLElement)
    if (e.key === 'Escape') {
      e.preventDefault()
      close()
    } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      const next = (index + (e.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length
      items[next]?.focus()
    } else if (e.key === 'Tab') {
      close(false)
    }
  }

  const initial = Array.from(nickname)[0] ?? '?'
  const hasNew = (newCommentCount ?? 0) > 0

  return (
    <div className="user-menu" ref={box} onKeyDown={open ? onMenuKey : undefined}>
      <button
        ref={button}
        type="button"
        className="user-menu-button"
        aria-label={`${nickname}님의 메뉴`}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="user-avatar" aria-hidden="true">
          {initial}
          {hasNew && <span className="user-avatar-dot" />}
        </span>
        <span className="user-menu-name">{nickname}</span>
        <svg
          className="user-menu-caret"
          viewBox="0 0 12 12"
          width="12"
          height="12"
          aria-hidden="true"
          focusable="false"
        >
          <path
            d="M2.5 4.5 6 8l3.5-3.5"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>
      {open && (
        <div id={menuId} className="user-menu-panel" role="menu" aria-label="내 메뉴">
          <p className="user-menu-hello">
            <strong>{nickname}</strong>님의 원고지
          </p>
          <Link to="/me/blog" role="menuitem" className="user-menu-item" onClick={() => close(false)}>
            <span className="user-menu-icon" aria-hidden="true">
              ▤
            </span>
            내 블로그
          </Link>
          <Link to="/manage" role="menuitem" className="user-menu-item" onClick={() => close(false)}>
            <span className="user-menu-icon" aria-hidden="true">
              ⚙
            </span>
            블로그 관리
            <NewBadge count={newCommentCount} />
          </Link>
          <Link to="/mypage" role="menuitem" className="user-menu-item" onClick={() => close(false)}>
            <span className="user-menu-icon" aria-hidden="true">
              ☺
            </span>
            마이페이지
          </Link>
          <Link to="/mypage/library" role="menuitem" className="user-menu-item" onClick={() => close(false)}>
            <span className="user-menu-icon" aria-hidden="true">
              ✎
            </span>
            필사 노트
          </Link>
          <span className="user-menu-sep" role="separator" />
          <button
            type="button"
            role="menuitem"
            className="user-menu-item is-quiet"
            onClick={() => {
              // 로그아웃이 실패하면 다시 누를 수 있게 메뉴 버튼으로 돌아간다
              close()
              onLogout()
            }}
          >
            <span className="user-menu-icon" aria-hidden="true">
              ⎋
            </span>
            로그아웃
          </button>
        </div>
      )}
    </div>
  )
}
