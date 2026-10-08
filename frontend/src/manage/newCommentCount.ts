// 새 댓글 수를 머리글(사용자 메뉴)과 관리 화면 메뉴가 함께 쓴다 (specs/006 T036, FR-028). 값은 NewCommentCountProvider가 채운다
import { createContext, useContext } from 'react'

export interface NewCommentCountState {
  /** 아직 모르면 null (로그인하지 않았거나 묻는 중) */
  count: number | null
  /** 서버가 준 같은 계산의 숫자로 맞춘다 (관리 화면 머리 정보, 읽음 처리 뒤 0) */
  setCount: (count: number) => void
}

export const NewCommentCountContext = createContext<NewCommentCountState | null>(null)

export function useNewCommentCount(): NewCommentCountState {
  const ctx = useContext(NewCommentCountContext)
  if (!ctx) throw new Error('NewCommentCountProvider 안에서만 쓸 수 있습니다')
  return ctx
}
