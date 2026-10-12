// 소셜 로그인 (specs/010 contracts). 회사 화면 오가기는 주소 이동(/oauth2/authorization/{회사})이라 여기 없다
import { api } from '../api/client'
import type { Member } from './useAuth'

export type SocialProvider = 'github' | 'google' | 'kakao' | 'naver'

/** 버튼과 마이페이지에 보이는 회사 이름 */
export const PROVIDER_LABEL: Record<SocialProvider, string> = {
  github: 'GitHub',
  google: 'Google',
  kakao: '카카오',
  naver: '네이버',
}

/** 회사 화면으로 가는 주소 (서버의 Spring Security가 받는다. 개발 중에는 Vite가 서버로 넘긴다) */
export function authorizationUrl(provider: SocialProvider): string {
  return `/oauth2/authorization/${provider}`
}

/** 서버가 화면 주소(?socialError=, ?linkError=)로 알려 주는 오류의 문구. 서버 ErrorCode 문구와 같다 */
export const SOCIAL_ERROR_MESSAGE: Record<string, string> = {
  SOCIAL_LOGIN_FAILED: '※ 소셜 로그인을 마치지 못했습니다. 다시 시도해 주세요',
  SOCIAL_ACCOUNT_IN_USE: '※ 이미 다른 회원과 연결된 계정입니다',
  SOCIAL_ALREADY_LINKED: '※ 이미 이 회사의 다른 계정과 연결되어 있습니다. 연결을 해제한 뒤 다시 시도해 주세요',
}

export function socialErrorMessage(code: string | null): string | null {
  if (!code) return null
  return SOCIAL_ERROR_MESSAGE[code] ?? SOCIAL_ERROR_MESSAGE.SOCIAL_LOGIN_FAILED
}

/** 열쇠가 들어 있는 회사만 (D-9) */
export async function getProviders(signal?: AbortSignal): Promise<SocialProvider[]> {
  const res = await api<{ providers: SocialProvider[] }>('/api/v1/auth/social/providers', { signal })
  return res.providers
}

export interface SocialPending {
  provider: SocialProvider
  email: string | null
  /** 참이면 이메일 인증 없이 이 이메일로 가입한다 */
  emailVerified: boolean
  /** 회사가 알려 준 이름 중 쓸 수 있는 닉네임 제안. 없으면 null */
  nickname: string | null
  expiresInSeconds: number
}

export function getPending(signal?: AbortSignal): Promise<SocialPending> {
  return api<SocialPending>('/api/v1/auth/social/pending', { signal, notifyUnauthenticated: false })
}

export function cancelPending(): Promise<void> {
  return api('/api/v1/auth/social/pending', { method: 'DELETE' })
}

/** 소셜 가입. email·verificationToken은 회사가 확인한 이메일이 없을 때만 쓴다 */
export async function socialSignup(nickname: string, email?: string, verificationToken?: string): Promise<Member> {
  const res = await api<{ member: Member }>('/api/v1/auth/social/signup', {
    method: 'POST',
    body: { nickname, email, verificationToken },
  })
  return res.member
}
