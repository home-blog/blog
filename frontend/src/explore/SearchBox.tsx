// 머리글 검색창 (specs/004 US5, US6, T031, T034)
// 요청 전에 서버와 같은 검사(앞뒤 공백을 지우고 2자 이상)를 해서 짧으면 이동하지 않고 안내한다. 50자를 넘는 입력은 받지 않는다 (D-1: A)
// 검색어는 입력한 그대로 주소(/search?q=)에 담는다. 결과 화면에서는 주소의 q를 그대로 다시 채운다 (FR-017)
import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'
import { checkKeyword, KEYWORD_MAX, limitKeyword } from './rules'

export default function SearchBox() {
  const { pathname } = useLocation()
  const [searchParams] = useSearchParams()
  const urlKeyword = pathname === '/search' ? (searchParams.get('q') ?? '') : ''
  // 주소의 검색어가 바뀌면(뒤로 가기, 새 검색) 검색창도 그 값으로 다시 시작한다
  return <SearchForm key={urlKeyword} initial={urlKeyword} />
}

function SearchForm({ initial }: { initial: string }) {
  const navigate = useNavigate()
  const [value, setValue] = useState(initial)
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const problem = checkKeyword(value)
    setError(problem)
    if (problem) return
    navigate(`/search?${new URLSearchParams({ q: value }).toString()}`)
  }

  return (
    <form className="search-box" role="search" onSubmit={handleSubmit} noValidate>
      <label htmlFor="site-search" className="visually-hidden">
        글 검색
      </label>
      <input
        id="site-search"
        type="search"
        className="search-input"
        placeholder="글 검색"
        value={value}
        onChange={(e) => {
          setValue(limitKeyword(e.target.value))
          setError(null)
        }}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? 'site-search-error' : 'site-search-hint'}
      />
      <button type="submit" className="btn btn-outline btn-small">
        검색
      </button>
      <span id="site-search-hint" className="visually-hidden">
        2자 이상 {KEYWORD_MAX}자 이하
      </span>
      {error && (
        <span id="site-search-error" className="search-error msg msg-error" role="alert">
          {error}
        </span>
      )}
    </form>
  )
}
