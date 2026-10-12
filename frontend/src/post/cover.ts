// 대표 이미지 (specs/008): 본문의 마크다운 이미지 가운데 제목이 "대표"인 것, 없으면 첫 이미지가 카드 썸네일이 된다.
// 서버 PostThumbnail과 같은 규칙이다. 표 칸 없이 본문만 고친다: ![설명](주소 "대표")
// 코드 블록(```…```, ~~~…~~~)과 인라인 코드(`…`) 안의 이미지 문법은 그림이 아니라서 후보로 보지도, 고치지도 않는다

export const COVER_TITLE = '대표'

const IMAGE = /!\[([^\]\n]*)\]\(\s*<?([^)\s>]+)>?(?:\s+"([^"\n]*)")?\s*\)/g
/** 코드 블록(닫히지 않았으면 글 끝까지)과 인라인 코드 (서버 PostThumbnail.CODE와 같다) */
const CODE = /^ {0,3}(`{3,}|~{3,})[\s\S]*?(?:^ {0,3}\1[^\n]*$|(?![\s\S]))|`[^`\n]+`/gm

/** 코드 안의 글자 범위들 [시작, 끝) */
function codeRanges(content: string): [number, number][] {
  return [...content.matchAll(CODE)].map((m) => [m.index, m.index + m[0].length])
}

function inCode(ranges: [number, number][], at: number): boolean {
  return ranges.some(([start, end]) => at >= start && at < end)
}

export interface BodyImage {
  src: string
  alt: string
  /** 지금 카드 썸네일로 쓰이는 이미지인가 */
  cover: boolean
}

/** 이 사이트의 경로와 http(s) 주소만 (서버와 같다) */
function usable(src: string): boolean {
  return src.startsWith('https://') || src.startsWith('http://') || (src.startsWith('/') && !src.startsWith('//'))
}

/** 본문 속 쓸 수 있는 이미지들 (나온 순서). 썸네일로 쓰이는 하나에 cover가 붙는다 */
export function imagesIn(content: string): BodyImage[] {
  const found: BodyImage[] = []
  const code = codeRanges(content)
  for (const match of content.matchAll(IMAGE)) {
    const [, alt, src, title] = match
    if (usable(src) && !inCode(code, match.index)) found.push({ src, alt, cover: title === COVER_TITLE })
  }
  const chosen = found.findIndex((image) => image.cover)
  return found.map((image, i) => ({ ...image, cover: i === (chosen >= 0 ? chosen : 0) }))
}

/**
 * pick번째(쓸 수 있는 이미지 가운데 0부터) 이미지를 대표로 한다. 다른 이미지의 "대표"는 지운다.
 * 첫 이미지를 고르면 표시 없이 둔다 (표시가 없으면 첫 이미지가 대표라서 본문이 깔끔하다).
 */
export function setCover(content: string, pick: number): string {
  let index = -1
  const code = codeRanges(content)
  return content.replace(IMAGE, (whole, alt: string, src: string, title: string | undefined, at: number) => {
    if (!usable(src) || inCode(code, at)) return whole
    index += 1
    const nextTitle = index === pick && pick > 0 ? COVER_TITLE : title === COVER_TITLE ? undefined : title
    if (nextTitle === title) return whole
    return `![${alt}](${src}${nextTitle ? ` "${nextTitle}"` : ''})`
  })
}
