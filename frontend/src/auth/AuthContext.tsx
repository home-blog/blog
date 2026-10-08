// 로그인 상태 (specs/001 T030): 처음 열 때 GET /api/auth/me로 확인하고, 로그인·로그아웃을 화면 전체에 알린다.
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, ApiError, resetCsrfToken } from '../api/client'
import { AuthContext, type Member } from './useAuth'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [member, setMember] = useState<Member | null | undefined>(undefined)

  useEffect(() => {
    let cancelled = false
    api<{ member: Member }>('/api/auth/me', { notifyUnauthenticated: false })
      .then((res) => !cancelled && setMember(res.member))
      .catch((err: unknown) => {
        if (!cancelled) setMember(null)
        if (!(err instanceof ApiError)) console.error(err)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api<{ member: Member }>('/api/auth/login', { method: 'POST', body: { email, password } })
    resetCsrfToken() // 로그인으로 상태가 바뀌었으니 다음 변경 요청 때 토큰을 새로 받는다
    setMember(res.member)
    return res.member
  }, [])

  /** 서버가 로그아웃을 마쳤을 때만 화면도 로그아웃으로 바꾼다. 실패하면 오류를 그대로 던진다 (공용 기기에서 안전하게) */
  const logout = useCallback(async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } finally {
      resetCsrfToken()
    }
    setMember(null)
  }, [])

  const value = useMemo(() => ({ member, login, logout }), [member, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
