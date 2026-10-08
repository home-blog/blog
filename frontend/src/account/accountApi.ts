// 계정 관리 요청 (specs/002 contracts/account-api.md). 회원 번호는 보내지 않는다: 서버가 세션으로 정한다
import { api } from '../api/client'

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
  return api<Account>('/api/account', { signal })
}

export function updateProfile(nickname: string, intro: string): Promise<ProfileUpdated> {
  return api<ProfileUpdated>('/api/account/profile', { method: 'PATCH', body: { nickname, intro } })
}

export function changePassword(currentPassword: string, newPassword: string, newPasswordConfirm: string): Promise<MessageResult> {
  return api<MessageResult>('/api/account/password', {
    method: 'POST',
    body: { currentPassword, newPassword, newPasswordConfirm },
  })
}

export function withdraw(password: string, agreed: boolean): Promise<MessageResult> {
  return api<MessageResult>('/api/account/withdrawal', { method: 'POST', body: { password, agreed } })
}
