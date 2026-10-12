// 첫 화면: 여러 블로그의 공개 글을 한곳에서 (티스토리 첫 화면처럼).
// 주제 탭(전체·여행·음식·취미·운동·개발), 맨 위 큰 카드(specs/008), 이번 주 인기 글, 최신 글 목록과 페이지 번호. 로그인 없이 본다.
// 주소: /?topic=여행&page=2. 주제는 서버가 정한 다섯 개 이름 그대로 쓴다
import { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import FeaturedPost from '../explore/FeaturedPost'
import FeedItem from '../explore/FeedItem'
import Pagination from '../explore/Pagination'
import { getFeed, getPopular, type FeedPage, type PopularPosts } from '../explore/exploreApi'
import { formatDate, pageFrom } from '../explore/rules'
import ManuscriptSheet from './ManuscriptSheet'
import '../explore/explore.css'
import './auth-layout.css'
import './home.css'

/** 서버 응답 전에도 탭이 흔들리지 않게 보여 줄 기본 주제 (서버 topic 표와 같은 순서) */
const DEFAULT_TOPICS = ['여행', '음식', '취미', '운동', '개발']

type Loaded<T> = { key: string; data: T } | { key: string; error: string }

function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}

export default function HomePage() {
  const { member } = useAuth()
  // 탈퇴한 직후에는 서버가 준 "탈퇴가 완료되었습니다"를 보여 준다 (specs/002 FR-027)
  const withdrawn = (useLocation().state as { withdrawn?: string } | null)?.withdrawn
  const [searchParams] = useSearchParams()
  const topic = searchParams.get('topic') || null
  const page = pageFrom(searchParams.get('page'))
  const feedKey = `${topic ?? ''}\n${page}`
  const popularKey = topic ?? ''

  const [feed, setFeed] = useState<Loaded<FeedPage> | null>(null)
  const [popular, setPopular] = useState<Loaded<PopularPosts> | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const key = `${topic ?? ''}\n${page}`
    getFeed(topic, page, controller.signal)
      .then((data) => setFeed({ key, data }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setFeed({ key, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [topic, page])

  useEffect(() => {
    const controller = new AbortController()
    const key = topic ?? ''
    getPopular(topic, controller.signal)
      .then((data) => setPopular({ key, data }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setPopular({ key, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [topic])

  const shownFeed = feed?.key === feedKey ? feed : null
  const shownPopular = popular?.key === popularKey ? popular : null
  const topics = shownFeed && 'data' in shownFeed ? shownFeed.data.topics : DEFAULT_TOPICS

  function hrefFor(target: number): string {
    const next = new URLSearchParams()
    if (topic) next.set('topic', topic)
    if (target > 1) next.set('page', String(target))
    const query = next.toString()
    return query ? `/?${query}` : '/'
  }

  return (
    <div className="home">
      {withdrawn && (
        <p className="home-notice msg msg-ok" role="status">
          {withdrawn}
        </p>
      )}

      <section className="home-hero" aria-labelledby="home-hero-title">
        <div className="home-hero-text">
          <h1 id="home-hero-title" className="home-hero-title">
            오늘은 누구의 이야기를 읽을까요
          </h1>
          <p className="home-lead">
            여행, 음식, 취미, 운동, 개발. 여러 사람이 각자의 블로그에 꾹꾹 눌러 쓴 글을 한곳에서 둘러보세요.
          </p>
          {member ? (
            <p className="home-actions">
              <Link to="/write" className="btn btn-primary">
                글쓰기
              </Link>
              <Link to="/me/blog" className="home-link">
                내 블로그로 가기
              </Link>
            </p>
          ) : member === null ? (
            <p className="home-actions">
              <Link to="/signup" className="btn btn-primary">
                내 블로그 만들기
              </Link>
              <Link to="/login" className="home-link">
                로그인
              </Link>
            </p>
          ) : null}
        </div>
        <ManuscriptSheet lines={['', member ? `${member.nickname}님,` : '오늘은', '무엇을 쓸까요.']} rows={3} />
      </section>

      <nav className="topic-tabs" aria-label="주제">
        <Link to="/" className="topic-tab" aria-current={topic === null ? 'page' : undefined}>
          전체
        </Link>
        {topics.map((name) => (
          <Link
            key={name}
            to={`/?topic=${encodeURIComponent(name)}`}
            className="topic-tab"
            aria-current={topic === name ? 'page' : undefined}
          >
            {name}
          </Link>
        ))}
      </nav>

      {/* 첫 페이지에만: 이번 주 인기 글 가운데 사진이 있는 글을 크게 */}
      {page === 1 && shownPopular && 'data' in shownPopular && <FeaturedPost posts={shownPopular.data.posts} />}

      <div className="home-columns">
        <section className="home-feed" aria-labelledby="home-feed-title">
          <h2 id="home-feed-title" className="home-section-title">
            {topic ? `${topic} 새 글` : '새로 올라온 글'}
          </h2>
          {!shownFeed ? (
            <p className="hint">글을 불러오는 중</p>
          ) : 'error' in shownFeed ? (
            <div className="post-empty" role="alert">
              <p className="msg msg-error">{shownFeed.error}</p>
              <Link to="/">전체 글 보기</Link>
            </div>
          ) : shownFeed.data.totalCount === 0 ? (
            <div className="post-empty">
              <p>아직 {topic ? `${topic} 주제의 ` : ''}글이 없습니다.</p>
              {member && <Link to="/write">첫 글 쓰기</Link>}
            </div>
          ) : (
            <>
              <p className="post-count">글 {shownFeed.data.totalCount.toLocaleString('ko-KR')}개</p>
              <ul className="post-list feed-list rise-list">
                {shownFeed.data.posts.map((post) => (
                  <FeedItem key={post.postId} post={post} />
                ))}
              </ul>
              <Pagination page={shownFeed.data.page} totalPages={shownFeed.data.totalPages} hrefFor={hrefFor} />
            </>
          )}
        </section>

        <aside className="home-popular" aria-labelledby="home-popular-title">
          <h2 id="home-popular-title" className="home-section-title">
            이번 주 인기 글
          </h2>
          {!shownPopular ? (
            <p className="hint">불러오는 중</p>
          ) : 'error' in shownPopular ? (
            <p className="hint">인기 글을 불러오지 못했습니다.</p>
          ) : shownPopular.data.posts.length === 0 ? (
            <p className="hint">아직 인기 글이 없습니다.</p>
          ) : (
            <ol className="popular-list rise-list">
              {shownPopular.data.posts.map((post, index) => (
                <li key={post.postId} className={index === 0 ? 'popular-item popular-first' : 'popular-item'}>
                  <span className="popular-rank" aria-hidden="true">
                    {index + 1}
                  </span>
                  <span className="popular-body">
                    <Link to={`/posts/${post.postId}`} className="popular-title">
                      {post.title}
                    </Link>
                    <span className="popular-meta">
                      <Link to={`/blog/${post.blogId}`}>{post.blogName}</Link>
                      <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
                    </span>
                  </span>
                </li>
              ))}
            </ol>
          )}
        </aside>
      </div>
    </div>
  )
}
