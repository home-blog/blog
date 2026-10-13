// 원고지 칸 버튼 (v0.5.1 원고지 손길): 글자를 칸마다 한 자씩. 띄어쓰기는 빈 칸 없이 붙인다
// 화면 읽기 도구에는 칸 글자 대신 label 전체를 읽힌다
import { Link } from 'react-router'
import './cell-link.css'

interface Props {
  to: string
  label: string
  primary?: boolean
}

export default function CellLink({ to, label, primary }: Props) {
  const chars = [...label].filter((char) => char.trim() !== '')
  return (
    <Link to={to} className={primary ? 'cell-link cell-link-primary' : 'cell-link'} aria-label={label}>
      {chars.map((char, i) => (
        <span key={i} aria-hidden="true">
          {char}
        </span>
      ))}
    </Link>
  )
}
