// 글 본문을 마크다운으로 그린다 (specs/003 D-1, research D-8: react-markdown + remark-gfm, 2026-10-08 사용자 결정)
// - 마크다운 문법만 그린다. 본문의 HTML은 그리지 않고 글자 그대로 보인다 (react-markdown 기본)
// - javascript:, data: 같은 위험한 링크 주소는 비운다 (react-markdown 기본 urlTransform)
// - HTML 글자를 화면에 끼워 넣지 않는다 (dangerouslySetInnerHTML 없음). 결과는 React 조각이다
// - 이미지: 지금은 다른 사이트 주소도 그린다(주소 정보는 넘기지 않음). 우리 이미지 저장소(/api/images/…)만 그릴지는
//   사용자 결정 전이다 (docs 정할-것.md 34번). 정해지면 ONLY_OWN_IMAGES 하나만 바꾸면 된다 (다른 주소는 링크로 보인다)
// 이 파일은 글 상세 화면에서만 불러온다 (PostDetailPage의 lazy)
import Markdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'

/** true면 우리 이미지 저장소의 주소만 그림으로 그린다 (정할-것 34번, 지금은 false = 모두 그림) */
const ONLY_OWN_IMAGES = false
const OWN_IMAGE_PATH = '/api/images/'

function isOwnImage(src: string): boolean {
  return src.startsWith(OWN_IMAGE_PATH)
}

const components: Components = {
  // 본문의 링크는 새 창으로 열고, 이 사이트 정보를 넘기지 않는다. 막힌 주소(빈 값)는 링크가 아닌 글자로
  a: ({ href, children }) =>
    href ? (
      <a href={href} target="_blank" rel="noopener noreferrer nofollow">
        {children}
      </a>
    ) : (
      <span>{children}</span>
    ),
  img: ({ src, alt }) => {
    const url = typeof src === 'string' ? src : undefined
    if (ONLY_OWN_IMAGES && url && !isOwnImage(url)) {
      return (
        <a href={url} target="_blank" rel="noopener noreferrer nofollow">
          {alt || url}
        </a>
      )
    }
    return <img src={url} alt={alt ?? ''} loading="lazy" referrerPolicy="no-referrer" />
  },
}

export default function MarkdownView({ source }: { source: string }) {
  return (
    <div className="markdown">
      <Markdown remarkPlugins={[remarkGfm]} components={components}>
        {source}
      </Markdown>
    </div>
  )
}
