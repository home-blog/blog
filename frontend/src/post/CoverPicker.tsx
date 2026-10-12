// 글쓰기: 대표 이미지 고르기 (specs/008). 본문에 이미지가 있을 때만 보인다.
// 고른 이미지가 첫 화면·목록 카드의 썸네일이 된다. 고르지 않으면 첫 이미지
import { imagesIn, setCover } from './cover'

interface Props {
  content: string
  disabled?: boolean
  onChange: (content: string) => void
}

export default function CoverPicker({ content, disabled, onChange }: Props) {
  const images = imagesIn(content)
  if (images.length === 0) return null
  return (
    <div className="cover-picker" role="group" aria-labelledby="cover-picker-label">
      <span id="cover-picker-label" className="cover-picker-label">
        대표 이미지 <span className="hint">카드 썸네일</span>
      </span>
      <ul className="cover-picker-list">
        {images.map((image, i) => (
          <li key={`${i}-${image.src}`}>
            <button
              type="button"
              className="cover-choice"
              aria-pressed={image.cover}
              aria-label={`${i + 1}번째 이미지${image.alt ? ` (${image.alt})` : ''}를 대표로`}
              disabled={disabled}
              onClick={() => onChange(setCover(content, i))}
            >
              <img src={image.src} alt="" loading="lazy" referrerPolicy="no-referrer" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
