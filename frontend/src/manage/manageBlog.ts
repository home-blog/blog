// 관리 화면 머리 정보를 아래 화면(설정 등)과 나눈다 (specs/006 T015). 값은 ManageLayout이 채운다
import { createContext, useContext } from 'react'
import type { ManageHeader } from './manageApi'

export interface ManageBlogState {
  header: ManageHeader
  /** 서버에서 다시 읽는다 (블로그 이름을 바꾼 뒤 위쪽 이름을 바로 바꿀 때, FR-040) */
  refresh: () => Promise<void>
}

export const ManageBlogContext = createContext<ManageBlogState | null>(null)

export function useManageBlog(): ManageBlogState {
  const ctx = useContext(ManageBlogContext)
  if (!ctx) throw new Error('ManageLayout 안에서만 쓸 수 있습니다')
  return ctx
}
