// 글쓰기 잔디 (specs/013 US1): 지난 1년 동안 공개 글을 쓴 날을 원고지 칸에 채운다. 많이 쓴 날일수록 진하다
// 칸 하나하나는 탭으로 오가지 않는다(365칸). 잔디 전체가 탭 한 번이고, 화살표로 날짜를 고르면 아래 글자가 읽어 준다
// (위·아래 하루, 왼쪽·오른쪽 한 주, Home 첫날, End 오늘). 마우스는 칸에 올리거나 눌러 고른다
import { useEffect, useLayoutEffect, useRef, useState, type KeyboardEvent } from 'react'
import { activityLevel, activityWeeks, dayLabel, type ActivityDay } from './activity'
import { getBlogActivity, type BlogActivity as Activity } from './blogApi'

interface Props {
  blogId: number
}

const MOVES: Record<string, (index: number, last: number) => number> = {
  ArrowUp: (i) => i - 1,
  ArrowDown: (i) => i + 1,
  ArrowLeft: (i) => i - 7,
  ArrowRight: (i) => i + 7,
  Home: () => 0,
  End: (_, last) => last,
}

export default function BlogActivity({ blogId }: Props) {
  const [activity, setActivity] = useState<Activity | null>(null)
  const [picked, setPicked] = useState<string | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const controller = new AbortController()
    getBlogActivity(blogId, controller.signal)
      .then(setActivity)
      // 잔디는 꾸밈이라 못 불러오면 보여 주지 않는다
      .catch(() => undefined)
    return () => controller.abort()
  }, [blogId])

  // 좁은 화면에서는 칸 줄이 옆으로 밀린다: 오늘(맨 오른쪽)이 먼저 보이게 끝으로 옮겨 둔다
  useLayoutEffect(() => {
    const box = scrollRef.current
    if (box) box.scrollLeft = box.scrollWidth
  }, [activity])

  if (!activity || activity.blogId !== blogId) return null
  const weeks = activityWeeks(activity.from, activity.to, activity.days)
  const days = weeks.flat().filter((day): day is ActivityDay => day !== null)
  const pickedDay = days.find((day) => day.date === picked) ?? null
  const summary = `지난 1년 글 ${activity.totalPosts.toLocaleString('ko-KR')}개 · 원고지 ${activity.totalSheets.toLocaleString('ko-KR')}매`

  function onKeyDown(e: KeyboardEvent<HTMLDivElement>) {
    const move = MOVES[e.key]
    if (!move) return
    e.preventDefault()
    const current = days.findIndex((day) => day.date === picked)
    const last = days.length - 1
    const next = Math.min(last, Math.max(0, move(current < 0 ? last : current, last)))
    setPicked(days[next].date)
  }

  return (
    <section className="blog-activity" aria-labelledby="blog-activity-title">
      <h2 id="blog-activity-title" className="blog-activity-title">
        글쓰기 잔디
      </h2>
      <div className="blog-activity-scroll" ref={scrollRef}>
        <div
          className="blog-activity-grid"
          role="group"
          tabIndex={0}
          aria-label={`${summary}. 화살표로 날짜를 고릅니다`}
          aria-describedby="blog-activity-note"
          onFocus={() => setPicked((value) => value ?? days[days.length - 1]?.date ?? null)}
          onKeyDown={onKeyDown}
        >
          {weeks.map((week) => (
            <div className="blog-activity-week" key={week.find((day) => day)?.date}>
              {week.map((day, index) =>
                day ? (
                  <span
                    key={day.date}
                    className={`blog-activity-cell${day.date === picked ? ' is-picked' : ''}`}
                    data-level={activityLevel(day.postCount)}
                    title={dayLabel(day)}
                    onMouseEnter={() => setPicked(day.date)}
                    onClick={() => setPicked(day.date)}
                  />
                ) : (
                  <span key={`empty-${index}`} className="blog-activity-cell is-empty" />
                ),
              )}
            </div>
          ))}
        </div>
      </div>
      <p id="blog-activity-note" className="blog-activity-note hint" role="status">
        {pickedDay ? dayLabel(pickedDay) : summary}
      </p>
    </section>
  )
}
