// 이 글과 비슷한 글 (specs/014 US2): 글 아래에 카드 4개. 하나도 없으면 칸을 그리지 않는다
// 뜻으로 고른 글이면 "뜻이 비슷한 글", 태그·주제로 대신 고른 글이면 "태그가 겹치는 글"이라고 적는다
import { useEffect, useState } from 'react'
import FeedItem from '../../explore/FeedItem'
import '../../explore/explore.css'
import '../../pages/home.css'
import { getRelatedPosts, type RelatedPosts as Related } from './relatedApi'
import './related.css'

interface Props {
  postId: number
}

export default function RelatedPosts({ postId }: Props) {
  const [related, setRelated] = useState<{ postId: number; data: Related } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getRelatedPosts(postId, controller.signal).then(
      (data) => setRelated({ postId, data }),
      () => undefined, // 비슷한 글은 덤이다. 실패하면 칸을 그리지 않는다
    )
    return () => controller.abort()
  }, [postId])

  // 글이 바뀌면 앞 글의 결과는 보이지 않는다
  const data = related?.postId === postId ? related.data : null
  if (!data || data.posts.length === 0) return null

  return (
    <section className="related" aria-labelledby="related-title">
      <h2 id="related-title" className="related-title">
        이 글과 비슷한 글
        <span className="related-basis hint">{data.basis === 'meaning' ? '뜻이 비슷한 글' : '태그가 겹치는 글'}</span>
      </h2>
      <ul className="post-list feed-list related-list">
        {data.posts.map((post) => (
          <FeedItem key={post.postId} post={post} />
        ))}
      </ul>
    </section>
  )
}
