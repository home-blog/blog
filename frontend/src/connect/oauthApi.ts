// Claude 앱 커스텀 커넥터 (specs/023 contracts 3·5): 위임장(허락·거절)과 연결된 앱
import { api } from '../api/client'

export type OAuthScope = 'read' | 'draft'

export interface ConnectRequest {
  clientName: string
  /** 허락하면 돌아갈 곳의 호스트 (예: claude.ai) */
  redirectHost: string
  /** 앱이 요청한 범위. 없으면 null (회원이 고른다) */
  scope: OAuthScope | null
  expiresAt: string
}

export interface ConnectedApp {
  grantId: number
  clientName: string
  scope: OAuthScope
  createdAt: string
  lastUsedAt: string | null
}

export interface ConnectedApps {
  items: ConnectedApp[]
  max: number
}

export function getConnectRequest(requestId: string, signal?: AbortSignal): Promise<ConnectRequest> {
  return api<ConnectRequest>(`/api/v1/oauth/requests/${encodeURIComponent(requestId)}`, { signal })
}

export function approveConnect(requestId: string, scope: OAuthScope): Promise<{ redirectTo: string }> {
  return api(`/api/v1/oauth/requests/${encodeURIComponent(requestId)}/approve`, { method: 'POST', body: { scope } })
}

export function denyConnect(requestId: string): Promise<{ redirectTo: string }> {
  return api(`/api/v1/oauth/requests/${encodeURIComponent(requestId)}/deny`, { method: 'POST' })
}

export function getConnectedApps(signal?: AbortSignal): Promise<ConnectedApps> {
  return api<ConnectedApps>('/api/v1/me/connected-apps', { signal })
}

export function disconnectApp(grantId: number): Promise<void> {
  return api(`/api/v1/me/connected-apps/${grantId}`, { method: 'DELETE' })
}
