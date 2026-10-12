// 원고지 매수·읽는 시간을 화면에서 바로 센다 (글쓰기 미리보기, specs/007 US3·US7)
// 서버(ManuscriptMeasure, PostPreview.plainText)와 같은 규칙: 마크다운 기호를 걷어 낸 글자 수(띄어쓰기 포함) ÷ 200매, ÷ 500분, 올림, 최소 1
// 저장한 뒤의 숫자는 서버가 다시 센다. 기호 규칙이 조금 달라도 글쓰기 중의 어림값으로 쓴다
const CHARS_PER_SHEET = 200
const CHARS_PER_MINUTE = 500

function plainText(markdown: string): string {
  return markdown
    .replace(/^\s*(```|~~~).*$/gm, '')
    .replace(/!\[([^\]]*)]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
    .replace(/^\s*([-*_]\s*){3,}$/gm, '')
    .replace(/^\s{0,3}#{1,6}\s+/gm, '')
    .replace(/^\s{0,3}(>\s?)+/gm, '')
    .replace(/^\s*([-*+]|\d+[.)])\s+(\[[ xX]]\s+)?/gm, '')
    .replace(/(\*{1,3}|~~)(?=\S)(.+?)(?<=\S)\1/g, '$2')
    .replace(/`+/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

export interface Manuscript {
  chars: number
  sheets: number
  readMinutes: number
}

export function measureManuscript(markdown: string): Manuscript {
  const chars = Array.from(plainText(markdown)).length
  return {
    chars,
    sheets: Math.max(1, Math.ceil(chars / CHARS_PER_SHEET)),
    readMinutes: Math.max(1, Math.ceil(chars / CHARS_PER_MINUTE)),
  }
}
