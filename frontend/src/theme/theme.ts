// 밝게/어둡게 (specs/007 US1). 고른 값은 브라우저에 기억하고, 고르지 않았으면 기기 설정을 따른다.
// 처음 그리기 전의 값은 index.html 머리의 짧은 스크립트가 붙인다 (밝은 화면이 번쩍이지 않게). 같은 저장 이름을 쓴다
import { useSyncExternalStore } from 'react'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'geulgyeol-theme'
const CHANGE_EVENT = 'geulgyeol-themechange'
const darkQuery = () => window.matchMedia('(prefers-color-scheme: dark)')

function saved(): Theme | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    return value === 'dark' || value === 'light' ? value : null
  } catch {
    return null // 저장소를 쓸 수 없는 브라우저 설정
  }
}

function apply(theme: Theme) {
  document.documentElement.dataset.theme = theme
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

export function currentTheme(): Theme {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

/** 직접 고른 값으로 바꾸고 기억한다. */
export function setTheme(theme: Theme) {
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // 기억하지 못해도 지금 화면은 바꾼다
  }
  apply(theme)
}

function subscribe(onChange: () => void) {
  // 직접 고르지 않았으면 기기 설정이 바뀔 때 따라간다
  const query = darkQuery()
  const followSystem = () => {
    if (saved() === null) apply(query.matches ? 'dark' : 'light')
  }
  query.addEventListener('change', followSystem)
  window.addEventListener(CHANGE_EVENT, onChange)
  return () => {
    query.removeEventListener('change', followSystem)
    window.removeEventListener(CHANGE_EVENT, onChange)
  }
}

/** 지금 테마. 바뀌면 다시 그린다 (그래프처럼 색을 직접 읽는 곳에서 쓴다). */
export function useTheme(): Theme {
  return useSyncExternalStore(subscribe, currentTheme, () => 'light')
}
