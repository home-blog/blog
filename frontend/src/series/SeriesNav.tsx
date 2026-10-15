// 글 읽기의 연재 표시 (specs/013 US2-4): 머리에 "연재 이름 N화 / 전체", 바닥에 이전 화·다음 화와 접힌 전체 목록
// 화 번호는 서버가 보는 사람에게 보이는 글로 센 값이다 (D-10)
import { useState } from 'react'
import { Link } from 'react-router'
import type { PostSeries } from './seriesApi'
import './series.css'

export function SeriesHead({ series }: { series: PostSeries }) {
  return (
    <p className="series-head">
      <span className="series-head-label">연재</span>
      <Link to={`/series/${series.seriesId}`} className="series-head-name">
        {series.name}
      </Link>
      <span className="series-head-episode">{series.episode}화</span>
      <span className="series-head-total hint">/ {series.total}화</span>
    </p>
  )
}

export function SeriesFooter({ series, currentPostId }: { series: PostSeries; currentPostId: number }) {
  const [open, setOpen] = useState(false)
  return (
    <nav className="series-footer" aria-label={`${series.name} 연재`}>
      {(series.previous || series.next) && (
        <div className="series-footer-links">
          {series.previous ? (
            <Link to={`/posts/${series.previous.postId}`} className="series-footer-link is-previous">
              <span className="hint">이전 화</span> {series.previous.title}
            </Link>
          ) : (
            <span />
          )}
          {series.next && (
            <Link to={`/posts/${series.next.postId}`} className="series-footer-link is-next">
              <span className="hint">다음 화</span> {series.next.title}
            </Link>
          )}
        </div>
      )}
      <button type="button" className="btn btn-quiet btn-small" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
        연재 전체 보기 ({series.total}화)
      </button>
      {open && (
        <ol className="series-footer-list">
          {series.posts.map((post) => (
            <li key={post.postId} aria-current={post.postId === currentPostId ? 'page' : undefined}>
              {post.postId === currentPostId ? (
                <strong>
                  <span className="series-episode-number">{post.episode}화</span> {post.title}
                </strong>
              ) : (
                <Link to={`/posts/${post.postId}`}>
                  <span className="series-episode-number">{post.episode}화</span> {post.title}
                </Link>
              )}
            </li>
          ))}
        </ol>
      )}
    </nav>
  )
}
