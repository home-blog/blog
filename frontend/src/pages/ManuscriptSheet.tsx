// 원고지 한 장. 글자를 칸마다 한 자씩 앉힌다. 닉네임을 입력하면 만들어질 블로그 이름이 바로 칸에 적힌다.
import './manuscript.css'

const COLUMNS = 10

interface Props {
  /** 위에서부터 한 줄씩. 빈 문자열은 빈 줄. */
  lines: string[]
  rows: number
  /** 이 줄 번호(0부터)는 입력에 따라 바뀌는 줄이라 등장 효과를 주지 않는다 */
  liveLine?: number
}

export default function ManuscriptSheet({ lines, rows, liveLine }: Props) {
  const cells: { char: string; delay: number; live: boolean }[] = []
  let typed = 0
  for (let r = 0; r < rows; r++) {
    const chars = [...(lines[r] ?? '')].slice(0, COLUMNS)
    for (let c = 0; c < COLUMNS; c++) {
      const char = chars[c] ?? ''
      const live = r === liveLine
      cells.push({ char, delay: char && !live ? typed++ : 0, live })
    }
  }

  return (
    <div className="sheet" aria-hidden="true" style={{ ['--cols' as string]: COLUMNS }}>
      {cells.map((cell, i) => (
        <span
          key={i}
          className={cell.live ? 'cell cell-live' : 'cell'}
          style={cell.char && !cell.live ? { ['--i' as string]: cell.delay } : undefined}
        >
          {cell.char}
        </span>
      ))}
    </div>
  )
}
