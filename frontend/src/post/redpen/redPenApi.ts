// 빨간 펜 교정 요청 (specs/014 contracts 1). 로그인한 회원만
import { api } from '../../api/client'
import type { Correction } from './redPenRules'

export interface RedPenStatus {
  enabled: boolean
  dailyLimit: number
  remaining: number
  maxLength: number
}

export interface RedPenResult {
  corrections: Correction[]
  remaining: number
  cached: boolean
}

export function getRedPenStatus(signal?: AbortSignal): Promise<RedPenStatus> {
  return api<RedPenStatus>('/api/v1/me/proofread', { signal })
}

export function proofread(content: string): Promise<RedPenResult> {
  return api<RedPenResult>('/api/v1/me/proofread', { method: 'POST', body: { content } })
}
