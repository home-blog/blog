// 서버와 주고받는 요청 도구 (specs/001 T015, contracts/auth-api.md `공통 약속`, 9)
// - 변경 요청(POST, PUT, PATCH, DELETE)에는 CSRF 토큰을 헤더에 싣는다. 토큰은 처음 한 번 GET /api/auth/csrf로 받는다.
// - 401 UNAUTHENTICATED를 받으면 "로그인 필요" 신호를 낸다. (로그인 창 띄우기는 US4에서 이 신호를 받아 처리)
// - 오류는 서버의 공통 오류 모양(code, message, fieldErrors)을 그대로 ApiError로 넘긴다.

export interface FieldErrorItem {
  field: string
  code: string
  message: string
}

export interface ErrorBody {
  code: string
  message: string
  fieldErrors?: FieldErrorItem[]
  /** "몇 초 뒤에 다시"가 있을 때만 (예: 로그인 잠금) */
  retryAfterSeconds?: number
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldErrorItem[]
  readonly retryAfterSeconds?: number

  constructor(status: number, body: ErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.fieldErrors = body.fieldErrors ?? []
    this.retryAfterSeconds = body.retryAfterSeconds
  }

  /** 칸 이름으로 그 칸의 오류 문구를 찾는다 (FR-009: 칸 아래에 이유를 보여 줌). */
  messageFor(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message
  }
}

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])
const FALLBACK_ERROR: ErrorBody = { code: 'INTERNAL_ERROR', message: '※ 잠시 뒤 다시 시도해 주세요' }

interface CsrfToken {
  headerName: string
  token: string
}

let csrfToken: Promise<CsrfToken> | null = null
const unauthenticatedListeners = new Set<() => void>()

/** 401을 받았을 때 부를 함수를 등록한다. 돌려준 함수를 부르면 등록을 푼다. */
export function onUnauthenticated(listener: () => void): () => void {
  unauthenticatedListeners.add(listener)
  return () => unauthenticatedListeners.delete(listener)
}

/** 로그인·로그아웃 뒤처럼 토큰이 바뀌었을 때 부른다. 다음 변경 요청 때 새로 받는다. */
export function resetCsrfToken(): void {
  csrfToken = null
}

async function getCsrfToken(): Promise<CsrfToken> {
  if (!csrfToken) {
    csrfToken = fetch('/api/auth/csrf', { credentials: 'same-origin' })
      .then(async (res) => {
        if (!res.ok) throw new ApiError(res.status, await readError(res))
        return (await res.json()) as CsrfToken
      })
      .catch((err: unknown) => {
        csrfToken = null
        throw err
      })
  }
  return csrfToken
}

async function readError(res: Response): Promise<ErrorBody> {
  try {
    const body = (await res.json()) as Partial<ErrorBody>
    if (typeof body.code === 'string' && typeof body.message === 'string') {
      return { code: body.code, message: body.message, fieldErrors: body.fieldErrors, retryAfterSeconds: body.retryAfterSeconds }
    }
  } catch {
    // 본문이 JSON이 아니면 기본 문구를 쓴다
  }
  return FALLBACK_ERROR
}

export interface RequestOptions {
  method?: string
  body?: unknown
  signal?: AbortSignal
  /** false면 401이어도 "로그인 필요" 신호를 내지 않는다 (예: 로그인했는지 확인만 하는 GET /api/auth/me) */
  notifyUnauthenticated?: boolean
}

/**
 * 서버에 요청을 보내고 JSON 결과를 돌려준다. 본문이 없는 응답(204)은 undefined.
 * 실패하면 ApiError를 던진다.
 */
export async function api<T = unknown>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (UNSAFE_METHODS.has(method)) {
    const { headerName, token } = await getCsrfToken()
    headers[headerName] = token
  }

  const res = await fetch(path, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: 'same-origin',
    signal: options.signal,
  })

  if (!res.ok) {
    const error = new ApiError(res.status, await readError(res))
    if (res.status === 401 && error.code === 'UNAUTHENTICATED' && options.notifyUnauthenticated !== false) {
      unauthenticatedListeners.forEach((listener) => listener())
    }
    if (res.status === 403 && error.code === 'CSRF_TOKEN_INVALID') {
      resetCsrfToken()
    }
    throw error
  }

  if (res.status === 204 || res.headers.get('Content-Length') === '0') return undefined as T
  return (await res.json()) as T
}
