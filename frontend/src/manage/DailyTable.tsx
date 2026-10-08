// 날짜별 숫자 표 (specs/006 T043, T048). 그래프(T058) 전에는 이것으로 보여 준다. 최근 날이 위
import type { DailyCount } from './manageApi'

interface Column<T> {
  label: string
  value: (row: T) => number
}

interface Props<T extends Pick<DailyCount, 'date'>> {
  caption: string
  rows: T[]
  columns: Column<T>[]
}

/** 2026-10-08 → 10. 8. */
function shortDate(date: string): string {
  const [, month, day] = date.split('-')
  return `${Number(month)}. ${Number(day)}.`
}

export default function DailyTable<T extends Pick<DailyCount, 'date'>>({ caption, rows, columns }: Props<T>) {
  return (
    <div className="daily-table-wrap">
      <table className="daily-table">
        <caption className="visually-hidden">{caption}</caption>
        <thead>
          <tr>
            <th scope="col">날짜</th>
            {columns.map((c) => (
              <th key={c.label} scope="col">
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.toReversed().map((row) => (
            <tr key={row.date}>
              <th scope="row">
                <time dateTime={row.date}>{shortDate(row.date)}</time>
              </th>
              {columns.map((c) => (
                <td key={c.label}>{c.value(row).toLocaleString('ko-KR')}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
