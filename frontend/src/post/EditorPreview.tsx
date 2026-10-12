// 글쓰기 미리보기 (specs/007 US7): 쓰는 동안 오른쪽 원고지 종이에 글 상세와 같은 모양으로 바로 보여 준다
// - 원고지 매수·읽는 시간이 실시간으로 바뀐다
// - 본문을 고칠 때마다 다시 그리되, 입력이 밀리지 않게 한 박자 늦게 그린다(useDeferredValue)
import { forwardRef, lazy, Suspense, useDeferredValue } from 'react'
import { measureManuscript } from './manuscript'

const MarkdownView = lazy(() => import('./MarkdownView'))

interface Props {
  title: string
  content: string
  tags: string[]
  topicName?: string
  categoryName?: string
}

const EditorPreview = forwardRef<HTMLElement, Props>(function EditorPreview(
  { title, content, tags, topicName, categoryName },
  ref,
) {
  const deferred = useDeferredValue(content)
  const { sheets, readMinutes, chars } = measureManuscript(deferred)
  return (
    <section ref={ref} className="editor-preview" aria-label="미리보기">
      <header className="editor-preview-head">
        <span className="editor-preview-label">미리보기</span>
        <span className="manuscript-count" aria-live="polite">
          원고지 {sheets}매 · {readMinutes}분 <span className="hint">({chars.toLocaleString('ko-KR')}자)</span>
        </span>
      </header>
      <article className="editor-preview-paper">
        {(topicName || categoryName) && (
          <p className="post-meta-top">
            {categoryName && <span className="post-category">{categoryName}</span>}
            {topicName && <span className="post-topic">{topicName}</span>}
          </p>
        )}
        <h1 className={title.trim() ? 'post-title' : 'post-title is-placeholder'}>{title.trim() || '제목'}</h1>
        {deferred.trim() ? (
          <Suspense fallback={<div className="post-content-raw">{deferred}</div>}>
            <MarkdownView source={deferred} />
          </Suspense>
        ) : (
          <p className="editor-preview-empty">왼쪽에 쓰면 여기에 원고지처럼 보입니다.</p>
        )}
        {tags.length > 0 && (
          <ul className="post-tags" aria-label="태그">
            {tags.map((tag) => (
              <li key={tag}>
                <span className="post-tag">#{tag}</span>
              </li>
            ))}
          </ul>
        )}
      </article>
    </section>
  )
})

export default EditorPreview
