// 확인 창 (브라우저 기본 <dialog>). 취소하면 아무 요청도 보내지 않는다 (specs/003 contracts `화면이 보여 주는 확인 문구`)
// Esc나 바깥 닫기도 취소로 본다
import { useEffect, useRef, type ReactNode } from 'react'
import './confirm-dialog.css'

interface Props {
  open: boolean
  title: string
  children?: ReactNode
  confirmLabel: string
  /** 되돌릴 수 없는 일(삭제 등)이면 빨간 버튼 */
  danger?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export default function ConfirmDialog({ open, title, children, confirmLabel, danger, onConfirm, onCancel }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog ref={ref} className="confirm-dialog" aria-labelledby="confirm-dialog-title" onClose={() => open && onCancel()}>
      <h2 id="confirm-dialog-title" className="confirm-title">
        {title}
      </h2>
      {children}
      <div className="confirm-actions">
        <button type="button" className="btn btn-quiet" onClick={onCancel}>
          취소
        </button>
        <button type="button" className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm}>
          {confirmLabel}
        </button>
      </div>
    </dialog>
  )
}
