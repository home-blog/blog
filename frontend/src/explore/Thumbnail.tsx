// 글 카드 썸네일 (specs/008, 2026-10-12 사용자: 티스토리처럼 이미지가 크게, 원고지 콘셉트는 그대로)
// - 사진이 있으면(서버가 준 thumbnail: 대표 이미지, 없으면 본문 첫 이미지) 원고지 위에 테이프로 붙인 사진처럼
// - 사진이 없거나 못 불러오면 글 첫 문장을 원고지 칸에 한 자씩 (첫 칸은 들여쓰기). 칸 선 색은 주제마다 다르다
// 썸네일은 꾸밈이라 화면 읽기 프로그램에는 숨긴다 (제목이 바로 옆에 있다)
import { useState } from 'react'
import './thumbnail.css'

type Variant = 'card' | 'feature' | 'row'

/** 원고지 칸 수 (가로 × 세로) */
const GRID: Record<Variant, { cols: number; rows: number }> = {
  card: { cols: 12, rows: 5 },
  feature: { cols: 14, rows: 6 },
  row: { cols: 6, rows: 3 },
}

interface Props {
  src?: string | null
  /** 사진이 없을 때 원고지에 적을 글 (글 앞부분) */
  text: string
  /** 주제 이름: 칸 선 색 (없으면 원고지 초록) */
  topic?: string
  variant?: Variant
}

export default function Thumbnail({ src, text, topic, variant = 'card' }: Props) {
  // 사진 주소가 바뀌면 다시 시도한다 (못 불러온 주소만 기억)
  const [failedSrc, setFailedSrc] = useState<string | null>(null)
  if (src && src !== failedSrc) {
    return (
      <span className={`thumb thumb-photo thumb-${variant}`} aria-hidden="true">
        <span className="thumb-frame">
          <img
            src={src}
            alt=""
            loading="lazy"
            decoding="async"
            referrerPolicy="no-referrer"
            onError={() => setFailedSrc(src)}
          />
        </span>
      </span>
    )
  }
  const { cols, rows } = GRID[variant]
  const chars = ['', ...firstSentence(text)].slice(0, cols * rows)
  return (
    <span
      className={`thumb thumb-sheet thumb-${variant}`}
      data-topic={topic}
      aria-hidden="true"
      style={{ ['--cols' as string]: cols }}
    >
      <span className="thumb-grid">
        {Array.from({ length: cols * rows }, (_, i) => (
          <span key={i} className="thumb-cell">
            {chars[i] ?? ''}
          </span>
        ))}
      </span>
    </span>
  )
}

/** 첫 문장 (마침표·물음표·느낌표까지, 공백은 한 칸). 칸에 다 못 들어가면 잘린다 */
function firstSentence(text: string): string[] {
  const plain = text.replace(/\s+/g, ' ').trim()
  const end = plain.search(/[.?!。]\s/)
  return [...(end >= 0 ? plain.slice(0, end + 1) : plain)]
}
