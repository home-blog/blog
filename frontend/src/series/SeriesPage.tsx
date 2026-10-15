// 연재 한 개 /series/:seriesId (specs/013 US2-5): 그 연재의 글을 화 순서로. 화 번호는 보는 사람에게 보이는 글로 센다
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import ProofNotFound from '../components/ProofNotFound'
import { getSeries, type SeriesDetail } from './seriesApi'
import './series.css'

type Result = { key: string; series: SeriesDetail } | { key: string; error: string; notFound: boolean }

const DATE = new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', timeZone: 'Asia/Seoul' })

export default function SeriesPage() {
  const { seriesId } = useParams()
  const id = Number(seriesId)
  const validId = Number.isInteger(id) && id > 0
  const { member } = useAuth()
  const viewer = member === undefined ? null : (member?.id ?? 'guest')
  const key = `${id}:${viewer}`
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (!validId || viewer === null) return
    const controller = new AbortController()
    const requestKey = `${id}:${viewer}`
    getSeries(id, controller.signal)
      .then((series) => setResult({ key: requestKey, series }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setResult({
          key: requestKey,
          error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요',
          notFound: err instanceof ApiError && err.status === 404,
        })
      })
    return () => controller.abort()
  }, [id, validId, viewer])

  const current = result?.key === key ? result : null
  if (!validId || (current && 'error' in current && current.notFound)) {
    return <ProofNotFound struck="권" inserted="연재" particle="가" message="※ 존재하지 않는 연재입니다" />
  }
  if (current && 'error' in current) {
    return (
      <p className="blog-status msg msg-error" role="alert">
        {current.error}
      </p>
    )
  }
  if (!current) return <p className="blog-status hint">불러오는 중</p>

  const { series } = current
  return (
    <div className="series-page">
      <p className="series-head">
        <span className="series-head-label">연재</span>
        <Link to={`/blog/${series.blogId}`}>블로그로</Link>
      </p>
      <h1 className="series-page-title">{series.name}</h1>
      {series.description && <p className="hint">{series.description}</p>}
      {series.posts.length === 0 ? (
        <p className="hint">아직 볼 수 있는 글이 없습니다.</p>
      ) : (
        <ol className="series-episodes">
          {series.posts.map((post) => (
            <li key={post.postId}>
              <span className="series-episode-number">{post.episode}화</span>
              <Link to={`/posts/${post.postId}`}>{post.title}</Link>
              {post.visibility === 'private' && <span className="blog-badge">비공개</span>}
              <span className="hint">{DATE.format(new Date(post.createdAt))}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
