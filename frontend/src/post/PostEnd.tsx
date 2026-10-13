// 글 끝 (v0.5.1 원고지 손길): 끝까지 읽으면 "글결" 도장이 찍히고 원고지 몇 매였는지 다시 보여 준다
import Seal from '../components/Seal'

interface Props {
  sheets: number
  readMinutes: number
}

export default function PostEnd({ sheets, readMinutes }: Props) {
  return (
    <div className="post-end">
      <p className="post-end-meta hint">
        원고지 {sheets}매 · 읽는 데 {readMinutes}분
      </p>
      <Seal text="글결원고" stampOnView />
    </div>
  )
}
