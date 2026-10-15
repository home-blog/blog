// 글쓰기 잔디 (specs/013 US1): 서버가 준 날짜별 글 수를 주 단위 칸으로 놓고, 진하기와 칸 설명을 정한다

export interface ActivityDay {
  date: string // YYYY-MM-DD (한국 날짜)
  postCount: number
  sheets: number
}

/** 진하기 5단계 (D-14): 0 / 1 / 2 / 3~4 / 5 이상 */
export function activityLevel(postCount: number): 0 | 1 | 2 | 3 | 4 {
  if (!Number.isFinite(postCount) || postCount <= 0) return 0
  if (postCount === 1) return 1
  if (postCount === 2) return 2
  if (postCount <= 4) return 3
  return 4
}

/** "10월 12일 · 글 2개 · 원고지 18매" */
export function dayLabel(day: ActivityDay): string {
  const [, month, date] = day.date.split('-').map(Number)
  const head = `${month}월 ${date}일`
  if (day.postCount <= 0) return `${head} · 글 없음`
  return `${head} · 글 ${day.postCount}개 · 원고지 ${day.sheets}매`
}

// 날짜는 시간대와 상관없이 달력 날짜로만 다룬다 (세계 시간 0시로 계산)
function toUtc(date: string): number {
  const [y, m, d] = date.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

function fromUtc(time: number): string {
  return new Date(time).toISOString().slice(0, 10)
}

const DAY = 86_400_000

/**
 * from~to의 모든 날을 일요일 시작 주(7칸)로 나눈다. 범위 밖 칸은 null.
 * 서버는 글이 있는 날만 주므로 없는 날은 0으로 채운다.
 */
export function activityWeeks(from: string, to: string, days: ActivityDay[]): (ActivityDay | null)[][] {
  const byDate = new Map(days.map((day) => [day.date, day]))
  const start = toUtc(from)
  const end = toUtc(to)
  const weeks: (ActivityDay | null)[][] = []
  let week: (ActivityDay | null)[] = Array.from({ length: new Date(start).getUTCDay() }, () => null)
  for (let time = start; time <= end; time += DAY) {
    const date = fromUtc(time)
    week.push(byDate.get(date) ?? { date, postCount: 0, sheets: 0 })
    if (week.length === 7) {
      weeks.push(week)
      week = []
    }
  }
  if (week.length > 0) {
    while (week.length < 7) week.push(null)
    weeks.push(week)
  }
  return weeks
}
