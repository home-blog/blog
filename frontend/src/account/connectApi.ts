// AI 비서 연결 (specs/020 contracts/mcp-api.md 1~3): 개인 열쇠 만들기·목록·끊기. 원문은 만들 때 한 번만 온다
import { api } from '../api/client'

export type KeyScope = 'read' | 'draft'

export interface AccessKey {
  keyId: number
  name: string
  /** 원문 앞 8자 (gk_로 시작). 어느 열쇠인지 알아보는 데만 쓴다 */
  hint: string
  scope: KeyScope
  createdAt: string
  expiresAt: string
  /** 아직 안 썼으면 null */
  lastUsedAt: string | null
}

export interface AccessKeyList {
  items: AccessKey[]
  maxCount: number
  allowedDays: number[]
  /** 열쇠 이름 글자 수 상한 (서버 설정 access-key.name-max-length) */
  nameMaxLength: number
}

export interface CreatedKey extends Omit<AccessKey, 'lastUsedAt'> {
  /** 열쇠 원문. 서버에 남지 않는다 */
  token: string
}

export function getAccessKeys(signal?: AbortSignal): Promise<AccessKeyList> {
  return api<AccessKeyList>('/api/v1/me/access-keys', { signal })
}

export function createAccessKey(name: string, scope: KeyScope, days: number): Promise<CreatedKey> {
  return api<CreatedKey>('/api/v1/me/access-keys', { method: 'POST', body: { name, scope, days } })
}

export function revokeAccessKey(keyId: number): Promise<void> {
  return api(`/api/v1/me/access-keys/${keyId}`, { method: 'DELETE' })
}

/** AI 비서가 붙을 주소. 화면과 같은 곳(배포는 Nginx, 개발은 Vite 프록시)으로 간다 */
export function mcpEndpoint(origin = window.location.origin): string {
  return `${origin}/api/v1/mcp`
}

export type ClientKind = 'claude-code' | 'claude-desktop' | 'other'

/** 앱마다 붙이는 법. 열쇠를 모르면 <열쇠> 자리로 둔다 */
export function setupSnippet(kind: ClientKind, endpoint: string, token?: string): string {
  const key = token ?? '<열쇠>'
  switch (kind) {
    case 'claude-code':
      return `claude mcp add --transport http geulgyeol ${endpoint} \\\n  --header "Authorization: Bearer ${key}"`
    case 'claude-desktop':
      // Claude Desktop 설정 파일은 머리글을 바로 못 넣어서 mcp-remote로 잇는다 (Windows 띄어쓰기 문제로 값은 env에)
      return JSON.stringify(
        {
          mcpServers: {
            geulgyeol: {
              command: 'npx',
              args: ['-y', 'mcp-remote', endpoint, '--header', 'Authorization:${GEULGYEOL_AUTH}'],
              env: { GEULGYEOL_AUTH: `Bearer ${key}` },
            },
          },
        },
        null,
        2,
      )
    case 'other':
      return `주소: ${endpoint}\n방식: Streamable HTTP (POST, JSON)\n머리글: Authorization: Bearer ${key}`
  }
}
