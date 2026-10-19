// 필사 노트 (specs/015 US3): 내가 모은 문장을 원고지 칸에 한 글자씩 적은 카드로, 최근 순. 나만 본다.
// 카드 아래에 원문 글 제목·블로그 이름·모은 날짜, 원문 보기와 지우기. 원문이 비공개가 되거나 지워졌으면 "볼 수 없는 글"
// 주소: /mypage/library?page=2
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client'
import EmptySheet from '../../components/EmptySheet'
import Pagination from '../../explore/Pagination'
import { formatDate, pageFrom } from '../../explore/rules'
import { deleteClip, getClips, type Clip, type ClipPage } from './clipApi'
import './clip.css'

type Loaded = { key: number; data: ClipPage } | { key: number; error: string }

function messageOf(err: unknown): string {
  return err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요'
}

export default function LibraryPage() {
  const [searchParams] = useSearchParams()
  const page = pageFrom(searchParams.get('page'))
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [reload, setReload] = useState(0)
  const [removeError, setRemoveError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getClips(page, controller.signal)
      .then((data) => setLoaded({ key: page, data }))
      .catch((err: unknown) => {
        if (!controller.signal.aborted) setLoaded({ key: page, error: messageOf(err) })
      })
    return () => controller.abort()
  }, [page, reload])

  async function remove(clip: Clip) {
    setRemoveError(null)
    try {
      await deleteClip(clip.clipId)
    } catch (err) {
      // 다른 창에서 이미 지웠으면(404) 목록만 새로 읽는다
      if (!(err instanceof ApiError && err.status === 404)) {
        setRemoveError(messageOf(err))
        return
      }
    }
    setReload((n) => n + 1)
  }

  const shown = loaded?.key === page ? loaded : null

  return (
    <div className="library-page">
      <h1 className="library-title">
        <span className="ink-highlight">필사 노트</span>
      </h1>
      <p className="library-lead">글을 읽다 마음에 남은 문장을 옮겨 적어 두는 곳입니다. 나만 볼 수 있어요.</p>
      {removeError && (
        <p className="msg msg-error" role="alert">
          {removeError}
        </p>
      )}
      {!shown ? (
        <p className="hint">불러오는 중</p>
      ) : 'error' in shown ? (
        <p className="msg msg-error" role="alert">
          {shown.error}
        </p>
      ) : shown.data.totalCount === 0 ? (
        <EmptySheet
          lines={['아직 옮겨 적은', '문장이 없어요.']}
          message="글 본문에서 문장을 끌어 고르면 수집 단추가 나타납니다."
        />
      ) : (
        <>
          <p className="post-count">문장 {shown.data.totalCount.toLocaleString('ko-KR')}개</p>
          <ul className="clip-list rise-list" aria-label="모은 문장">
            {shown.data.items.map((clip) => (
              <ClipCard key={clip.clipId} clip={clip} onRemove={() => remove(clip)} />
            ))}
          </ul>
          <Pagination
            page={shown.data.page}
            totalPages={shown.data.totalPages}
            hrefFor={(target) => (target > 1 ? `/mypage/library?page=${target}` : '/mypage/library')}
          />
        </>
      )}
    </div>
  )
}

function ClipCard({ clip, onRemove }: { clip: Clip; onRemove: () => void }) {
  const [busy, setBusy] = useState(false)
  const chars = Array.from(clip.text)
  return (
    <li className="clip-card">
      {/* 원고지 칸에 한 글자씩. 읽는 프로그램에는 문장을 그대로 읽힌다 */}
      <blockquote className="clip-sheet" aria-label={clip.text}>
        {chars.map((char, index) => (
          <span key={index} className={char === ' ' ? 'clip-cell clip-cell-space' : 'clip-cell'} aria-hidden="true">
            {char}
          </span>
        ))}
      </blockquote>
      <div className="clip-meta">
        <span className="clip-source">
          {clip.post.available && clip.post.postId !== null ? (
            <Link to={`/posts/${clip.post.postId}`} className="clip-source-title">
              {clip.post.title}
            </Link>
          ) : (
            <span className="clip-source-title is-gone" title={clip.post.title}>
              볼 수 없는 글
            </span>
          )}
          <span className="clip-source-blog">{clip.post.blogName}</span>
          <time dateTime={clip.createdAt}>{formatDate(clip.createdAt)}</time>
        </span>
        <span className="clip-actions">
          {clip.post.available && clip.post.postId !== null && (
            <Link to={`/posts/${clip.post.postId}`} className="btn btn-outline btn-small">
              원문 보기
            </Link>
          )}
          <button
            type="button"
            className="btn btn-quiet btn-small"
            disabled={busy}
            onClick={async () => {
              setBusy(true)
              await onRemove()
              setBusy(false)
            }}
          >
            지우기
          </button>
        </span>
      </div>
    </li>
  )
}
