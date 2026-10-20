// 위임장 (specs/023 US1, T004): Claude 앱 같은 AI 앱이 글결 MCP에 붙으려고 회원의 브라우저를 여기로 보낸다.
// 원고지 칸 표에 누가·무엇을·언제까지가 적혀 있고, 허락하면 붉은 인장이 쾅 찍힌 뒤 앱으로 돌아간다.
// 로그인하지 않았으면 로그인 창을 먼저 띄우고, 로그인하면 이 화면(같은 요청 번호)으로 돌아온다
import { useEffect, useId, useState, type CSSProperties, type ReactNode } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useLoginPrompt } from '../auth/loginPrompt'
import { useAuth } from '../auth/useAuth'
import { approveConnect, denyConnect, getConnectRequest, type ConnectRequest, type OAuthScope } from './oauthApi'
import './connect.css'

/** 인장이 찍히는 동안 기다린다 (움직임 줄이기면 바로 간다) */
const STAMP_MS = 900

function reducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

type Loaded = { id: string; request: ConnectRequest } | { id: string; error: string; gone: boolean }

interface Props {
  /** 앱으로 옮겨 간다. 테스트에서 바꿔 끼운다 */
  go?: (url: string) => void
}

export default function ConnectAuthorizePage({ go = (url) => window.location.assign(url) }: Props) {
  const [params] = useSearchParams()
  const requestId = params.get('request')
  const urlError = params.get('error')
  const { member } = useAuth()
  const { open: openLogin } = useLoginPrompt()
  const titleId = useId()
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [scope, setScope] = useState<OAuthScope | null>(null)
  const [busy, setBusy] = useState(false)
  const [sealed, setSealed] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    if (member === null && requestId && !urlError) openLogin()
  }, [member, requestId, urlError, openLogin])

  useEffect(() => {
    if (!member || !requestId || urlError) return
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
  }, [member, requestId, urlError])

  if (urlError || !requestId) {
    return (
      <Shell titleId={titleId}>
        <div className="warrant-problem" role="alert">
          <p className="warrant-problem-title">위임장을 쓸 수 없어요</p>
          <p>
            {urlError === 'invalid_client'
              ? '알 수 없는 앱이거나, 앱이 돌아갈 주소가 등록되지 않았어요. 앱에서 커넥터를 지우고 다시 연결해 주세요.'
              : '연결 요청이 없어요. 앱에서 다시 연결해 주세요.'}
          </p>
        </div>
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
  const chosen: OAuthScope = scope ?? request.scope ?? 'read'

  async function answer(approve: boolean) {
    if (!requestId) return
    setBusy(true)
    setActionError(null)
    try {
      const { redirectTo } = approve ? await approveConnect(requestId, chosen) : await denyConnect(requestId)
      if (approve && !reducedMotion()) {
        setSealed(redirectTo)
        window.setTimeout(() => go(redirectTo), STAMP_MS)
      } else {
        setSealed(redirectTo)
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
        className={sealed ? 'warrant is-sealed' : 'warrant'}
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
              <span className="hint">허락하면 {request.redirectHost}(으)로 돌아가요</span>
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
