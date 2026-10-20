// 위임장 (specs/023 US1, T004): Claude 앱 같은 AI 앱이 글결 MCP에 붙으려고 회원의 브라우저를 여기로 보낸다.
// 원고지 칸 표에 누가·무엇을·언제까지가 적혀 있고, 허락하면 붉은 인장이 쾅 찍힌 뒤 앱으로 돌아간다.
// 로그인하지 않았으면 로그인 창을 먼저 띄우고, 로그인하면 이 화면(같은 요청 번호)으로 돌아온다.
// 다른 사이트 안(틀)에서는 열지 않고, 위임장 전에 난 오류는 회원이 눌러야 앱으로 돌아간다 (보안 검토)
import { useEffect, useId, useState, type CSSProperties, type ReactNode } from 'react'
import { useLocation, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import {
  approveConnect,
  denyConnect,
  getConnectRequest,
  getOAuthError,
  type ConnectRequest,
  type OAuthErrorView,
  type OAuthScope,
} from './oauthApi'
import './connect.css'

/** 인장이 찍히는 동안 기다린다 (움직임 줄이기면 바로 간다) */
const STAMP_MS = 900

function reducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

/** 다른 사이트가 이 화면을 틀 안에 띄웠나 (눌러 속이기 막기). 위를 볼 수 없으면 띄워진 것으로 본다 */
function isFramed(): boolean {
  try {
    return window.top !== window.self
  } catch {
    return true
  }
}

/** 공식 Claude 앱이 돌아가는 주소. 앱 이름은 앱이 스스로 적으니 이름이 아니라 주소로 가린다 */
const OFFICIAL_HOSTS = new Set(['claude.ai', 'claude.com'])

/** 위임장 전에 난 오류마다 회원에게 보일 말 */
const ERROR_TEXT: Record<string, string> = {
  invalid_request: '앱이 보낸 연결 요청이 규칙에 맞지 않아요.',
  unsupported_response_type: '앱이 보낸 연결 요청이 규칙에 맞지 않아요.',
  invalid_scope: '앱이 글결에 없는 권한을 요청했어요.',
  invalid_target: '앱이 글결 MCP가 아닌 곳의 허락을 요청했어요.',
  temporarily_unavailable: '글결이 잠시 연결 요청을 받을 수 없어요. 잠시 뒤 다시 시도해 주세요.',
}

type Loaded = { id: string; request: ConnectRequest } | { id: string; error: string; gone: boolean }

interface Props {
  /** 앱으로 옮겨 간다. 테스트에서 바꿔 끼운다 */
  go?: (url: string) => void
  /** 다른 사이트의 틀 안에 띄워졌나. 테스트에서 바꿔 끼운다 */
  framed?: boolean
}

export default function ConnectAuthorizePage({
  go = (url) => window.location.assign(url),
  framed = isFramed(),
}: Props) {
  const [params] = useSearchParams()
  const location = useLocation()
  const requestId = params.get('request')
  const urlError = params.get('error')
  const backId = params.get('back')
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const titleId = useId()
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [scope, setScope] = useState<OAuthScope | null>(null)
  const [busy, setBusy] = useState(false)
  // sealed: 떠나는 주소(넘어가지 않으면 누를 링크), stamped: 허락 인장을 찍었나 (거절에는 찍지 않는다)
  const [sealed, setSealed] = useState<string | null>(null)
  const [stamped, setStamped] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    if (member === null && requestId && !urlError && !framed) openLogin()
  }, [member, requestId, urlError, framed, openLogin])

  useEffect(() => {
    if (!member || !requestId || urlError || framed) return
    const controller = new AbortController()
    getConnectRequest(requestId, controller.signal)
      .then((request) => setLoaded({ id: requestId, request }))
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setLoaded({
          id: requestId,
          error: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요',
          gone: err instanceof ApiError && err.status === 404,
        })
      })
    return () => controller.abort()
  }, [member, requestId, urlError, framed])

  if (framed) {
    return (
      <Shell titleId={titleId}>
        <div className="warrant-problem" role="alert">
          <p className="warrant-problem-title">다른 사이트 안에서는 위임장을 열 수 없어요</p>
          <p>허락 단추를 몰래 누르게 하는 속임수를 막으려고, 위임장은 글결 창에서만 열려요.</p>
        </div>
        <a className="btn" href={location.pathname + location.search} target="_blank" rel="noopener noreferrer">
          새 창에서 열기
        </a>
      </Shell>
    )
  }
  if (urlError || !requestId) {
    return (
      <Shell titleId={titleId}>
        <ErrorNotice code={urlError} backId={urlError ? backId : null} />
      </Shell>
    )
  }
  if (member === undefined) return null
  if (member === null) {
    return (
      <Shell titleId={titleId}>
        <p className="warrant-login">위임장을 보려면 먼저 로그인해 주세요.</p>
        <button type="button" className="btn btn-primary" onClick={() => openLogin()}>
          로그인
        </button>
      </Shell>
    )
  }
  const current = loaded?.id === requestId ? loaded : null
  if (!current) {
    return (
      <Shell titleId={titleId}>
        <p className="hint" role="status">
          위임장을 펴는 중…
        </p>
      </Shell>
    )
  }
  if ('error' in current) {
    return (
      <Shell titleId={titleId}>
        <div className="warrant-problem" role="alert">
          <p className="warrant-problem-title">
            {current.gone ? '시간이 지난 위임장이에요' : '위임장을 열지 못했어요'}
          </p>
          <p>{current.gone ? '연결 요청은 10분 동안만 기다려요. 앱에서 다시 연결해 주세요.' : current.error}</p>
        </div>
      </Shell>
    )
  }

  const request = current.request
  const readOnly = request.scope === 'read'
  // 앱이 초안까지 달라고 해도 읽기만이 먼저 골라져 있다: 더 넓은 권한은 회원이 직접 고른다
  const chosen: OAuthScope = scope ?? 'read'
  const official = OFFICIAL_HOSTS.has(request.redirectHost.toLowerCase())

  async function answer(approve: boolean) {
    if (!requestId) return
    setBusy(true)
    setActionError(null)
    try {
      const { redirectTo } = approve ? await approveConnect(requestId, chosen) : await denyConnect(requestId)
      setSealed(redirectTo)
      setStamped(approve)
      if (approve && !reducedMotion()) {
        window.setTimeout(() => go(redirectTo), STAMP_MS)
      } else {
        go(redirectTo)
      }
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      setBusy(false)
    }
  }

  return (
    <Shell titleId={titleId}>
      <form
        className={stamped ? 'warrant is-sealed' : 'warrant'}
        aria-labelledby={titleId}
        onSubmit={(event) => {
          event.preventDefault()
          void answer(true)
        }}
      >
        <dl className="warrant-table">
          <div className="warrant-row">
            <dt>누가</dt>
            <dd>
              <strong className="warrant-client">{request.clientName}</strong>
              <span className="warrant-host-line">
                <span className="warrant-host">{request.redirectHost}</span>
                {official ? (
                  <span className="warrant-badge is-official">Claude 공식</span>
                ) : (
                  <span className="warrant-badge is-unverified">확인되지 않은 앱</span>
                )}
              </span>
              <span className="hint">
                {official
                  ? '허락하면 이 주소로 돌아가요'
                  : '허락하면 이 주소로 돌아가요. 앱 이름은 앱이 스스로 적은 것이니 주소를 꼭 확인해 주세요'}
              </span>
            </dd>
          </div>
          <div className="warrant-row">
            <dt>누구의</dt>
            <dd>
              <strong>{member.nickname}</strong> 님의 글결
            </dd>
          </div>
          <div className="warrant-row">
            <dt>무엇을</dt>
            <dd>
              <fieldset className="warrant-scope" disabled={busy || Boolean(sealed)}>
                <legend className="visually-hidden">할 수 있는 일</legend>
                <label className={chosen === 'read' ? 'warrant-option is-on' : 'warrant-option'}>
                  <input type="radio" name="scope" checked={chosen === 'read'} onChange={() => setScope('read')} />
                  <span>
                    <b>읽기만</b>
                    <small>글 찾기·단락 찾기·글 읽기, 내 글 목록, 내 통계</small>
                  </span>
                </label>
                <label className={chosen === 'draft' ? 'warrant-option is-on' : 'warrant-option'}>
                  <input
                    type="radio"
                    name="scope"
                    checked={chosen === 'draft'}
                    disabled={readOnly}
                    onChange={() => setScope('draft')}
                  />
                  <span>
                    <b>읽기 + 초안 남기기</b>
                    <small>
                      {readOnly ? '이 앱은 읽기만 요청했어요' : '임시 저장 글까지. 발행은 언제나 내가 해요'}
                    </small>
                  </span>
                </label>
              </fieldset>
            </dd>
          </div>
          <div className="warrant-row">
            <dt>언제까지</dt>
            <dd>
              끊기 전까지. 30일 동안 쓰지 않으면 저절로 끊기고, 마이페이지 <b>AI 비서 연결</b>에서 언제든 끊어요.
            </dd>
          </div>
        </dl>

        <div className="warrant-actions">
          <button type="submit" className="btn btn-primary warrant-approve" disabled={busy || Boolean(sealed)}>
            허락하고 인장 찍기
          </button>
          <button
            type="button"
            className="btn btn-quiet warrant-deny"
            disabled={busy || Boolean(sealed)}
            onClick={() => void answer(false)}
          >
            거절
          </button>
        </div>
        {actionError && (
          <p className="msg msg-error" role="alert">
            {actionError}
          </p>
        )}
        <span className="warrant-seal" aria-hidden="true">
          <span>허</span>
          <span>락</span>
        </span>
        {sealed && (
          <p className="warrant-leaving" role="status">
            {request.clientName}(으)로 돌아가는 중…{' '}
            <a href={sealed} className="warrant-leaving-link">
              넘어가지 않으면 여기
            </a>
          </p>
        )}
      </form>
    </Shell>
  )
}

/** 위임장을 펴지 못한 까닭. 돌아갈 주소는 서버가 만들어 둔 것만 쓰고, 회원이 눌러야 간다 */
function ErrorNotice({ code, backId }: { code: string | null; backId: string | null }) {
  const [view, setView] = useState<{ id: string; value: OAuthErrorView | null } | null>(null)

  useEffect(() => {
    if (!backId) return
    const controller = new AbortController()
    getOAuthError(backId, controller.signal)
      .then((value) => setView({ id: backId, value }))
      .catch(() => {
        if (!controller.signal.aborted) setView({ id: backId, value: null })
      })
    return () => controller.abort()
  }, [backId])

  const loaded = backId && view?.id === backId ? view.value : null
  const waiting = Boolean(backId) && view?.id !== backId
  const text =
    code === 'invalid_client'
      ? '알 수 없는 앱이거나, 앱이 돌아갈 주소가 등록되지 않았어요. 앱에서 커넥터를 지우고 다시 연결해 주세요.'
      : code
        ? (ERROR_TEXT[code] ?? '연결 요청에 문제가 있어요.') + ' 앱에서 다시 연결해 주세요.'
        : '연결 요청이 없어요. 앱에서 다시 연결해 주세요.'
  return (
    <>
      <div className="warrant-problem" role="alert">
        <p className="warrant-problem-title">위임장을 쓸 수 없어요</p>
        {loaded && (
          <p>
            <strong>{loaded.clientName}</strong>이(가) 보낸 연결 요청이에요.
          </p>
        )}
        <p>{text}</p>
      </div>
      {waiting && (
        <p className="hint" role="status">
          오류를 읽는 중…
        </p>
      )}
      {loaded && (
        <a className="btn warrant-back" href={loaded.redirectTo} rel="noreferrer">
          앱으로 돌아가기 ({loaded.redirectHost})
        </a>
      )}
    </>
  )
}

function Shell({ titleId, children }: { titleId: string; children: ReactNode }) {
  return (
    <section className="warrant-page">
      <p className="warrant-kicker">글결 우체국 · AI 앱 연결</p>
      <h1 id={titleId} className="warrant-title">
        <span className="warrant-title-cells" aria-hidden="true">
          {[...'위임장'].map((ch, i) => (
            <span key={i} style={{ '--i': i } as CSSProperties}>
              {ch}
            </span>
          ))}
        </span>
        <span className="visually-hidden">위임장</span>
      </h1>
      {children}
    </section>
  )
}
