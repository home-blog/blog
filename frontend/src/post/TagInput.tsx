// 태그 입력 (specs/005 US5, T046): Enter나 쉼표로 하나씩 더하고, 붙은 태그는 x로 뺀다
// - 더할 때 같은 규칙으로 바로 안내한다(보조). 서버가 돌려준 tags / tags[n] 오류는 그 자리에 보여 준다
// - 태그는 소문자로 저장된다 (D-10). 입력칸 옆에 안내한다
import { useId, useState, type KeyboardEvent } from 'react'
import { normalizeTag, TAG_MAX_PER_POST, tagMessages } from './tagRules'

interface Props {
  tags: string[]
  onChange: (tags: string[]) => void
  disabled?: boolean
  /** 서버가 돌려준 칸별 문구: 목록 전체(tags)와 태그마다(tags[n]) */
  serverError?: string
  serverTagErrors?: Record<number, string>
}

export default function TagInput({ tags, onChange, disabled, serverError, serverTagErrors = {} }: Props) {
  const id = useId()
  const [draft, setDraft] = useState('')
  const [error, setError] = useState<string | null>(null)

  /** 더했거나 비어 있으면 true, 규칙에 맞지 않아 안내를 띄웠으면 false (입력한 글자는 그대로 둔다) */
  function add(raw: string): boolean {
    if (raw.trim() === '') return true
    const name = normalizeTag(raw)
    const problem =
      name === null
        ? tagMessages.invalid
        : tags.includes(name)
          ? tagMessages.duplicated
          : tags.length >= TAG_MAX_PER_POST
            ? tagMessages.tooMany
            : null
    if (problem !== null || name === null) {
      setError(problem)
      return false
    }
    onChange([...tags, name])
    setError(null)
    return true
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    // 한글 조합 중의 Enter는 글자를 끝내는 것이므로 태그로 더하지 않는다
    if (e.nativeEvent.isComposing) return
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      if (add(draft)) setDraft('')
    } else if (e.key === 'Backspace' && draft === '' && tags.length > 0) {
      onChange(tags.slice(0, -1))
    }
  }

  const message = error ?? serverError
  return (
    <div className="field tag-field">
      <label htmlFor={id}>
        태그 <span className="hint">Enter나 쉼표로 더합니다 · {tagMessages.lowercase}</span>
      </label>
      <div className="tag-box">
        <ul className="tag-chips" aria-label="붙인 태그">
          {tags.map((tag, index) => (
            <li key={tag} className={serverTagErrors[index] ? 'tag-chip is-error' : 'tag-chip'}>
              #{tag}
              <button
                type="button"
                className="tag-remove"
                aria-label={`${tag} 태그 빼기`}
                disabled={disabled}
                onClick={() => onChange(tags.filter((t) => t !== tag))}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
        <input
          id={id}
          className="tag-draft"
          value={draft}
          disabled={disabled}
          placeholder={tags.length >= TAG_MAX_PER_POST ? '' : '#태그'}
          aria-invalid={message ? true : undefined}
          aria-describedby={message ? `${id}-error` : undefined}
          onChange={(e) => {
            setDraft(e.target.value)
            setError(null)
          }}
          onKeyDown={onKeyDown}
          onBlur={() => {
            if (add(draft)) setDraft('')
          }}
        />
      </div>
      <div className="editor-field-foot">
        {message ? (
          <p id={`${id}-error`} className="msg msg-error">
            {message}
          </p>
        ) : (
          <span />
        )}
        <span className="editor-count">
          {tags.length} / {TAG_MAX_PER_POST}개
        </span>
      </div>
      {Object.entries(serverTagErrors).map(([index, text]) => (
        <p key={index} className="msg msg-error">
          #{tags[Number(index)] ?? ''} {text}
        </p>
      ))}
    </div>
  )
}
