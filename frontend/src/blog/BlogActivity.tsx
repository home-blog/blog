// 글쓰기 잔디 (specs/013 US1): 지난 1년 동안 공개 글을 쓴 날을 원고지 칸에 채운다. 많이 쓴 날일수록 진하다
// 칸 하나하나는 탭으로 오가지 않는다(365칸). 전체 요약은 aria-label로, 칸 설명은 마우스를 올리거나 누르면 아래에 보인다
import { useEffect, useState } from 'react'
import { activityLevel, activityWeeks, dayLabel, type ActivityDay } from './activity'
import { getBlogActivity, type BlogActivity as Activity } from './blogApi'

interface Props {
  blogId: number
}

export default function BlogActivity({ blogId }: Props) {
  const [activity, setActivity] = useState<Activity | null>(null)
  const [picked, setPicked] = useState<ActivityDay | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getBlogActivity(blogId, controller.signal)
      .then(setActivity)
      // 잔디는 꾸밈이라 못 불러오면 보여 주지 않는다
      .catch(() => undefined)
    return () => controller.abort()
  }, [blogId])

  if (!activity || activity.blogId !== blogId) return null
  const weeks = activityWeeks(activity.from, activity.to, activity.days)
  const summary = `지난 1년 글 ${activity.totalPosts.toLocaleString('ko-KR')}개 · 원고지 ${activity.totalSheets.toLocaleString('ko-KR')}매`

  return (
    <section className="blog-activity" aria-labelledby="blog-activity-title">
      <h2 id="blog-activity-title" className="blog-activity-title">
        글쓰기 잔디
      </h2>
      <div className="blog-activity-scroll">
        <div className="blog-activity-grid" role="img" aria-label={summary}>
          {weeks.map((week) => (
            <div className="blog-activity-week" key={week.find((day) => day)?.date}>
              {week.map((day, index) =>
                day ? (
                  <span
                    key={day.date}
                    className="blog-activity-cell"
                    data-level={activityLevel(day.postCount)}
                    title={dayLabel(day)}
                    onMouseEnter={() => setPicked(day)}
                    onClick={() => setPicked(day)}
                  />
                ) : (
                  <span key={`empty-${index}`} className="blog-activity-cell is-empty" />
                ),
              )}
            </div>
          ))}
        </div>
      </div>
      <p className="blog-activity-note hint" aria-live="polite">
        {picked ? dayLabel(picked) : summary}
      </p>
    </section>
  )
}
