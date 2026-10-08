// 메뉴 옆 새 댓글 수 (specs/006 FR-028). 0이거나 모르면 보이지 않는다
interface Props {
  count: number | null
}

export default function NewBadge({ count }: Props) {
  if (!count) return null
  return (
    <span className="new-badge" aria-label={`새 댓글 ${count}개`}>
      {count > 99 ? '99+' : count}
    </span>
  )
}
