// 머리글의 밝게/어둡게 버튼 (specs/007 US1). 아이콘은 글꼴마다 위치가 달라지지 않게 그림(SVG)으로
import { setTheme, useTheme } from './theme'

export default function ThemeToggle() {
  const theme = useTheme()
  const next = theme === 'dark' ? 'light' : 'dark'
  const label = next === 'dark' ? '어두운 화면으로' : '밝은 화면으로'
  return (
    <button type="button" className="theme-toggle" onClick={() => setTheme(next)} aria-label={label} title={label}>
      {theme === 'dark' ? (
        <svg viewBox="0 0 20 20" width="16" height="16" aria-hidden="true" focusable="false">
          <circle cx="10" cy="10" r="3.6" fill="currentColor" />
          <path
            d="M10 1.8v2.2M10 16v2.2M1.8 10H4M16 10h2.2M4.2 4.2l1.6 1.6M14.2 14.2l1.6 1.6M4.2 15.8l1.6-1.6M14.2 5.8l1.6-1.6"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
          />
        </svg>
      ) : (
        <svg viewBox="0 0 20 20" width="16" height="16" aria-hidden="true" focusable="false">
          <path d="M16.5 12.6A7 7 0 0 1 7.4 3.5a7 7 0 1 0 9.1 9.1Z" fill="currentColor" />
        </svg>
      )}
    </button>
  )
}
