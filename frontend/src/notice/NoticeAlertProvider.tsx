// 새 공지 점의 상태를 머리글과 공지사항 목록이 함께 쓴다 (specs/012 US3)
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { getLatestNoticeAt } from './noticeApi'
import { isNewer, NoticeAlertContext, readSeen, writeSeen } from './noticeAlert'

/** 가장 새 글 시각을 본 것으로 적는다 (더 옛날 값으로 덮지 않는다) */
function remember(latestAt: string | null) {
  if (!latestAt) return
  const seen = readSeen()
  if (!seen || isNewer(latestAt, seen)) writeSeen(latestAt)
}

export default function NoticeAlertProvider({ children }: { children: ReactNode }) {
  const [hasNew, setHasNew] = useState(false)
  /** undefined: 아직 서버에 묻는 중 */
  const latest = useRef<string | null | undefined>(undefined)
  /** 서버 답이 오기 전에 목록을 이미 봤다 */
  const seenEarly = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    getLatestNoticeAt(controller.signal)
      .then(({ latestAt }) => {
        latest.current = latestAt
        const seen = readSeen()
        if (seenEarly.current) {
          remember(latestAt)
          return
        }
        if (seen === null) {
          // 처음 온 기기: 지금까지의 글은 본 것으로 두고, 앞으로 올라오는 글만 알린다 (S3-3)
          writeSeen(latestAt ?? new Date(0).toISOString())
          return
        }
        setHasNew(isNewer(latestAt, seen))
      })
      .catch(() => {
        // 점은 꾸밈이다. 못 받아도 조용히 넘어간다
      })
    return () => controller.abort()
  }, [])

  const markSeen = useCallback(() => {
    if (latest.current === undefined) seenEarly.current = true
    else remember(latest.current)
    setHasNew(false)
  }, [])

  const value = useMemo(() => ({ hasNew, markSeen }), [hasNew, markSeen])
  return <NoticeAlertContext.Provider value={value}>{children}</NoticeAlertContext.Provider>
}
