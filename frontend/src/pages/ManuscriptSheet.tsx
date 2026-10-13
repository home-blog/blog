// 원고지 한 장. 글자를 칸마다 한 자씩 앉힌다. 닉네임을 입력하면 만들어질 블로그 이름이 바로 칸에 적힌다.
// 빈 목록·블로그 이름에도 쓴다 (v0.5.1 원고지 손길): caretLine 줄의 첫 빈 칸에서 커서가 깜박인다
import './manuscript.css'

const COLUMNS = 10

interface Props {
  /** 위에서부터 한 줄씩. 빈 문자열은 빈 줄. */
  lines: string[]
  rows: number
  /** 한 줄의 칸 수 (기본 10) */
  columns?: number
  /** 이 줄 번호(0부터)는 입력에 따라 바뀌는 줄이라 등장 효과를 주지 않는다 */
  liveLine?: number
  /** 이 줄 번호(0부터)의 첫 빈 칸에 깜박이는 커서를 둔다 (글자가 다 적힌 뒤 깜박이기 시작한다) */
  caretLine?: number
  /** 작은 원고지 (칸이 작다) */
  small?: boolean
}

export default function ManuscriptSheet({ lines, rows, columns = COLUMNS, liveLine, caretLine, small }: Props) {
  const cells: { char: string; delay: number; live: boolean; caret: boolean; start: boolean }[] = []
  let typed = 0
  let caretPlaced = false
  for (let r = 0; r < rows; r++) {
    const chars = [...(lines[r] ?? '')].slice(0, columns)
    for (let c = 0; c < columns; c++) {
      const char = chars[c] ?? ''
      const live = r === liveLine
      const caret = !char && !caretPlaced && r === caretLine
      if (caret) caretPlaced = true
      cells.push({ char, delay: char && !live ? typed++ : 0, live, caret, start: c === 0 })
    }
  }

  return (
    <div
      className={small ? 'sheet sheet-small' : 'sheet'}
      aria-hidden="true"
      style={{ ['--cols' as string]: columns, ['--typed' as string]: typed }}
    >
      {cells.map((cell, i) => (
        <span
          key={i}
          className={['cell', cell.start && 'cell-start', cell.live && 'cell-live', cell.caret && 'cell-caret']
            .filter(Boolean)
            .join(' ')}
          style={cell.char && !cell.live ? { ['--i' as string]: cell.delay } : undefined}
        >
          {cell.char}
        </span>
      ))}
    </div>
  )
}
