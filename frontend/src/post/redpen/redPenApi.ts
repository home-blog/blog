// 빨간 펜 교정 요청 (specs/014 contracts 1). 로그인한 회원만
import { api } from '../../api/client'
import type { Correction } from './redPenRules'

export interface RedPenStatus {
  enabled: boolean
  /** ai: 밖의 AI(보내기 동의, 하루 횟수) · rules: 서버 안의 맞춤법 규칙(무료, 횟수 없음) · off */
  mode: 'ai' | 'rules' | 'off'
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

/** aiConsent: "본문이 AI로 보내진다"에 동의했는지. 서버의 교정기가 바깥 AI인데 거짓이면 409 PROOFREAD_CONSENT_REQUIRED */
export function proofread(content: string, aiConsent: boolean): Promise<RedPenResult> {
  return api<RedPenResult>('/api/v1/me/proofread', { method: 'POST', body: { content, aiConsent } })
}
