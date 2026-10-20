// 엽서 우표 다섯 가지 (specs/021 research D-5): 톱니 테두리 안에 그린 작은 그림. 색은 분류 색 토큰을 써서 어두운 화면에서도 맞는다
import type { StampName } from './guestbookApi'
import { STAMP_LABEL } from './stampLabels'

/** 우표 가운데 그림 (viewBox 40×48 안, 4~36 × 4~44 칸) */
function Art({ name }: { name: StampName }) {
  switch (name) {
    case 'pine':
      return (
        <g className="stamp-art">
          <path className="art-fill" d="M20 9 29 21h-5l7 9h-6l7 9H8l7-9H9l7-9h-5z" />
          <rect className="art-line" x="18.5" y="38" width="3" height="5" />
        </g>
      )
    case 'wave':
      return (
        <g className="stamp-art">
          <path
            className="art-stroke"
            d="M6 22c4-5 8-5 11 0 2.5 4 7 4 9-1 2-4 6-4 8 0M6 30c4-5 8-5 11 0 2.5 4 7 4 9-1 2-4 6-4 8 0M6 38c4-5 8-5 11 0 2.5 4 7 4 9-1 2-4 6-4 8 0"
          />
          <circle className="art-glow" cx="28" cy="12" r="4" />
        </g>
      )
    case 'moon':
      return (
        <g className="stamp-art">
          <path className="art-glow" d="M25 10a12 12 0 1 0 6 21 10 10 0 1 1-6-21z" />
          <circle className="art-dot" cx="10" cy="12" r="1" />
          <circle className="art-dot" cx="31" cy="38" r="1" />
          <circle className="art-dot" cx="9" cy="36" r="0.8" />
        </g>
      )
    case 'plum':
      return (
        <g className="stamp-art">
          <path className="art-line-stroke" d="M8 42c6-8 10-14 22-26M18 31c3 0 6 1 8 4" />
          {[
            [26, 16],
            [14, 26],
            [27, 33],
          ].map(([cx, cy]) => (
            <g key={`${cx}-${cy}`} className="art-flower">
              {[0, 72, 144, 216, 288].map((deg) => (
                <circle
                  key={deg}
                  cx={cx + 3.2 * Math.cos((deg * Math.PI) / 180)}
                  cy={cy + 3.2 * Math.sin((deg * Math.PI) / 180)}
                  r="2.6"
                />
              ))}
              <circle className="art-glow" cx={cx} cy={cy} r="1.4" />
            </g>
          ))}
        </g>
      )
    case 'crane':
      return (
        <g className="stamp-art">
          <path
            className="art-fill"
            d="M7 25c7-6 13-8 18-6l9-7-4 9c2 4 1 9-3 12l-9 3 6 6h-3l-6-5-3 1 2-5c-4-1-6-4-7-8z"
          />
          <circle className="art-crown" cx="31" cy="13" r="1.8" />
        </g>
      )
  }
}

interface Props {
  name: StampName
  /** 우표 이름을 화면 읽기 도구에 알리나 (고르기 칸 안에서는 글자가 따로 있어 숨긴다) */
  labelled?: boolean
  className?: string
}

export default function Stamp({ name, labelled = true, className }: Props) {
  return (
    <svg
      className={`gb-stamp gb-stamp-${name}${className ? ` ${className}` : ''}`}
      viewBox="0 0 40 48"
      {...(labelled
        ? { role: 'img', 'aria-label': `${STAMP_LABEL[name]} 우표` }
        : { 'aria-hidden': true, focusable: false })}
    >
      {/* 톱니 테두리: 종이 바탕에 점선 구멍 */}
      <rect className="stamp-paper" x="0.5" y="0.5" width="39" height="47" rx="1" />
      <rect className="stamp-field" x="4" y="4" width="32" height="40" />
      <Art name={name} />
      <text className="stamp-mark" x="33" y="42" textAnchor="end">
        글결
      </text>
    </svg>
  )
}
