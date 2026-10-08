// 로그인 상태를 읽는 훅. 값은 AuthProvider(AuthContext.tsx)가 채운다.
import { createContext, useContext } from 'react'

export interface Member {
  id: number
  nickname: string
}

export interface AuthState {
  /** undefined: 아직 확인 중, null: 로그인 안 함 */
  member: Member | null | undefined
  login: (email: string, password: string) => Promise<Member>
  logout: () => Promise<void>
  /** 로그인 상태를 서버에서 다시 읽는다 (닉네임을 바꾼 뒤 머리글용, specs/002 T007) */
  refresh: () => Promise<void>
  /** 서버에 로그아웃을 보내지 않고 화면만 로그아웃 상태로 (탈퇴 뒤처럼 서버가 이미 끝낸 경우) */
  signedOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider 안에서만 쓸 수 있습니다')
  return ctx
}
