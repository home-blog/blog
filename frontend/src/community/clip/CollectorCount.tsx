// 글 주인에게만: "N명이 이 글에서 문장을 수집했어요" (specs/015 US3). 누가 무엇을 모았는지는 보여 주지 않는다.
// 아직 아무도 모으지 않았거나 불러오지 못하면 그리지 않는다
import { useEffect, useState } from 'react'
import { getCollectors } from './clipApi'
import './clip.css'

export default function CollectorCount({ postId }: { postId: number }) {
  const [count, setCount] = useState<number | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getCollectors(postId, controller.signal)
      .then((data) => setCount(data.collectors))
      .catch(() => {
        if (!controller.signal.aborted) setCount(null)
      })
    return () => controller.abort()
  }, [postId])

  if (!count) return null
  return (
    <p className="clip-collectors">
      <span aria-hidden="true">✎</span> {count.toLocaleString('ko-KR')}명이 이 글에서 문장을 수집했어요
    </p>
  )
}
