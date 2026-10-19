// 받아 보는 글 (specs/016 US1): 내가 받아 보는 블로그의 새 공개 글을 첫 화면과 같은 카드로, 옆에는 받아 보는 블로그 목록
// (메일로도 받기를 켜고 끄고, 그만 받아 보기). 주소: /subscriptions?page=2
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import EmptySheet from '../components/EmptySheet'
import FeedItem from '../explore/FeedItem'
import Pagination from '../explore/Pagination'
import { pageFrom } from '../explore/rules'
import {
  getMySubscriptions,
  getSubscribedFeed,
  subscribe,
  unsubscribe,
  type MySubscription,
  type SubscribedFeed,
} from './subscriptionApi'
import '../explore/explore.css'
import '../pages/home.css'
import './subscription.css'

type Loaded<T> = { key: number; data: T } | { key: number; error: string }

function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}

export default function SubscribedPage() {
  const [searchParams] = useSearchParams()
  const page = pageFrom(searchParams.get('page'))
  const [feed, setFeed] = useState<Loaded<SubscribedFeed> | null>(null)
  const [blogs, setBlogs] = useState<MySubscription[] | null>(null)
  const [reload, setReload] = useState(0)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getSubscribedFeed(page, controller.signal)
      .then((data) => setFeed({ key: page, data }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setFeed({ key: page, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [page, reload])

  useEffect(() => {
    const controller = new AbortController()
    getMySubscriptions(controller.signal)
      .then((data) => setBlogs(data.items))
      .catch(() => {
        if (!controller.signal.aborted) setBlogs([])
      })
    return () => controller.abort()
  }, [reload])

  async function change(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      setReload((n) => n + 1)
    } catch (err) {
      setError(messageOf(err))
    }
  }

  const shown = feed?.key === page ? feed : null

  return (
    <div className="subscribed-page">
      <h1 className="library-title">
        <span className="ink-highlight">받아 보는 글</span>
      </h1>
      <div className="home-columns">
        <section aria-label="받아 보는 블로그의 새 글">
          {!shown ? (
            <p className="hint">불러오는 중</p>
          ) : 'error' in shown ? (
            <p className="msg msg-error" role="alert">
              {shown.error}
            </p>
          ) : shown.data.totalCount === 0 ? (
            <EmptySheet
              lines={['아직 받아 보는', '원고가 없어요.']}
              message="마음에 드는 블로그 머리에서 '원고 받아 보기'를 누르면 새 글이 여기에 모입니다."
            >
              <Link to="/">둘러보러 가기</Link>
            </EmptySheet>
          ) : (
            <>
              <p className="post-count">글 {shown.data.totalCount.toLocaleString('ko-KR')}개</p>
              <ul className="post-list feed-list rise-list">
                {shown.data.posts.map((post) => (
                  <FeedItem key={post.postId} post={post} />
                ))}
              </ul>
              <Pagination
                page={shown.data.page}
                totalPages={shown.data.totalPages}
                hrefFor={(target) => (target > 1 ? `/subscriptions?page=${target}` : '/subscriptions')}
              />
            </>
          )}
        </section>

        <aside className="subscribed-blogs" aria-labelledby="subscribed-blogs-title">
          <h2 id="subscribed-blogs-title" className="home-section-title">
            <span className="ink-highlight">받아 보는 블로그</span>
          </h2>
          {error && (
            <p className="msg msg-error" role="alert">
              {error}
            </p>
          )}
          {blogs === null ? (
            <p className="hint">불러오는 중</p>
          ) : blogs.length === 0 ? (
            <p className="hint">아직 받아 보는 블로그가 없습니다.</p>
          ) : (
            <ul className="subscribed-list">
              {blogs.map((blog) => (
                <li key={blog.blogId} className="subscribed-item">
                  <Link to={`/blog/${blog.blogId}`} className="subscribed-name">
                    {blog.blogName}
                  </Link>
                  <span className="subscribed-by">{blog.nickname}</span>
                  <label className="subscribe-email">
                    <input
                      type="checkbox"
                      checked={blog.emailEnabled}
                      onChange={(event) => change(() => subscribe(blog.blogId, event.target.checked))}
                    />
                    메일로도
                  </label>
                  <button
                    type="button"
                    className="btn btn-quiet btn-small"
                    onClick={() => change(() => unsubscribe(blog.blogId))}
                  >
                    그만 받기
                  </button>
                </li>
              ))}
            </ul>
          )}
        </aside>
      </div>
    </div>
  )
}
