// 계정 관리 요청 (specs/002 contracts/account-api.md). 회원 번호는 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'
import type { SocialProvider } from '../auth/socialApi'

export interface Account {
  email: string
  nickname: string
  /** 비어 있으면 '' */
  intro: string
  /** 가입 시각 (ISO 8601) */
  joinedAt: string
  /** 내 블로그 바로가기용. 없으면 null */
  blog: { id: number } | null
}

export interface ProfileUpdated {
  message: string
  nickname: string
  intro: string
}

export interface MessageResult {
  message: string
}

export function getAccount(signal?: AbortSignal): Promise<Account> {
  return api<Account>('/api/v1/account', { signal })
}

export function updateProfile(nickname: string, intro: string): Promise<ProfileUpdated> {
  return api<ProfileUpdated>('/api/v1/account/profile', { method: 'PATCH', body: { nickname, intro } })
}

/** 비밀번호가 없는 회원(소셜 가입)은 currentPassword를 비워 보낸다: 비밀번호 만들기 (specs/010 D-4) */
export function changePassword(
  currentPassword: string,
  newPassword: string,
  newPasswordConfirm: string,
): Promise<MessageResult> {
  return api<MessageResult>('/api/v1/account/password', {
    method: 'POST',
    body: { currentPassword, newPassword, newPasswordConfirm },
  })
}

/** 비밀번호가 없는 회원(소셜 가입)은 password를 비워 보낸다: 동의만 (specs/010 D-4) */
export function withdraw(password: string, agreed: boolean): Promise<MessageResult> {
  return api<MessageResult>('/api/v1/account/withdrawal', { method: 'POST', body: { password, agreed } })
}

// 소셜 계정 연결 (specs/010 contracts 6 ~ 8)

export interface SocialAccounts {
  /** 소셜로만 가입한 회원은 false: 비밀번호 변경 대신 "비밀번호 만들기", 탈퇴는 동의만 */
  hasPassword: boolean
  /** 연결할 수 있는 회사 (열쇠가 들어 있는 회사) */
  providers: SocialProvider[]
  linked: { provider: SocialProvider; linkedAt: string }[]
}

export function getSocialAccounts(signal?: AbortSignal): Promise<SocialAccounts> {
  return api<SocialAccounts>('/api/v1/account/social', { signal })
}

/** 연결하기 시작: 서버 세션에 "이 회원에 연결"을 적는다. 이어서 회사 화면으로 이동한다 */
export function startLink(provider: SocialProvider): Promise<void> {
  return api(`/api/v1/account/social/${provider}/link`, { method: 'POST' })
}

export function unlink(provider: SocialProvider): Promise<void> {
  return api(`/api/v1/account/social/${provider}`, { method: 'DELETE' })
}
