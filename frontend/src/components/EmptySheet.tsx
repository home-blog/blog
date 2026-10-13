// 빈 목록을 원고지로 (v0.5.1 원고지 손길): 빈 칸에 한 줄이 적히고 다음 칸에서 커서가 깜박인다
// 원고지는 꾸밈이라 화면 읽기 도구에는 아래 문구(message)만 읽힌다
import type { ReactNode } from 'react'
import ManuscriptSheet from '../pages/ManuscriptSheet'

interface Props {
  /** 원고지에 적을 두 줄 (한 줄에 10자까지) */
  lines: [string, string]
  /** 화면에 함께 보이는 문구 */
  message: string
  children?: ReactNode
}

export default function EmptySheet({ lines, message, children }: Props) {
  return (
    <div className="post-empty empty-sheet">
      <ManuscriptSheet lines={lines} rows={2} caretLine={1} small />
      <p>{message}</p>
      {children}
    </div>
  )
}
