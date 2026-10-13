// 글을 올리면 "탈고", 고쳐 쓰면 "퇴고" 도장을 찍은 알림 (v0.5.1 원고지 손길)
// 글쓰기 화면이 저장한 뒤 이 글로 옮기며 넘긴 표시(location.state)로 한 번만 보여 준다
import Seal from '../components/Seal'

interface Props {
  kind: 'created' | 'edited'
  sheets: number
  onClose: () => void
}

export default function SavedToast({ kind, sheets, onClose }: Props) {
  return (
    <div className="saved-toast" role="status">
      <Seal text={kind === 'created' ? '탈고' : '퇴고'} small stampOnView />
      <span>
        {kind === 'created' ? '글을 올렸습니다' : '고친 글을 올렸습니다'}. 원고지 {sheets}매.
      </span>
      <button type="button" className="btn btn-quiet btn-small" onClick={onClose} aria-label="알림 닫기">
        닫기
      </button>
    </div>
  )
}
