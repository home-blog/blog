// 글 읽기에서 그 글의 연재 정보를 따로 불러온다 (specs/013 contracts 2-3). 연재는 덤이라 못 불러오면 보여 주지 않는다
import { useEffect, useState } from 'react'
import { getPostSeries, type PostSeries } from './seriesApi'

export function usePostSeries(postId: number, viewer: string | number | null): PostSeries | null {
  const key = `${postId}:${viewer}`
  const [state, setState] = useState<{ key: string; series: PostSeries | null } | null>(null)

  useEffect(() => {
    if (viewer === null || !Number.isInteger(postId) || postId <= 0) return
    const controller = new AbortController()
    const requestKey = `${postId}:${viewer}`
    getPostSeries(postId, controller.signal)
      .then((series) => setState({ key: requestKey, series }))
      .catch(() => undefined)
    return () => controller.abort()
  }, [postId, viewer])

  return state?.key === key ? state.series : null
}
