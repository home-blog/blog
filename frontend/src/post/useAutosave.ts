// 자동 저장 (specs/013 US3-1, D-5): 간격마다 마지막으로 저장한 뒤 바뀐 것이 있으면 저장한다. 저장 중이면 겹쳐 보내지 않는다
import { useEffect, useRef } from 'react'

interface Options {
  /** 지금 입력을 한 글자로 나타낸 것 (JSON 등) */
  snapshot: string
  /** 마지막으로 저장한 입력 */
  savedSnapshot: string
  /** 발행 중·불러오는 중이면 끈다 */
  enabled: boolean
  intervalSeconds: number
  save: () => Promise<void>
}

export function useAutosave({ snapshot, savedSnapshot, enabled, intervalSeconds, save }: Options): void {
  const latest = useRef({ snapshot, savedSnapshot, enabled, save })
  const inFlight = useRef(false)

  useEffect(() => {
    latest.current = { snapshot, savedSnapshot, enabled, save }
  })

  useEffect(() => {
    const timer = window.setInterval(() => {
      const now = latest.current
      if (!now.enabled || inFlight.current || now.snapshot === now.savedSnapshot) return
      inFlight.current = true
      now
        .save()
        .catch(() => undefined)
        .finally(() => {
          inFlight.current = false
        })
    }, intervalSeconds * 1000)
    return () => window.clearInterval(timer)
  }, [intervalSeconds])
}
