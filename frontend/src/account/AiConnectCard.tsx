// 마이페이지 "AI 비서 연결" (specs/020 US1, T009·T010): Claude 같은 AI 비서가 MCP로 내 블로그에 붙는 개인 열쇠.
// 열쇠 원문은 만들 때 한 번만 보인다(서버는 해시만 둔다). 끊으면 그 열쇠를 쓰던 비서는 바로 막힌다.
// 모양: 열쇠마다 구멍 뚫린 꼬리표, 새 열쇠는 "한 번만" 도장이 찍힌 쪽지에 형광펜이 한 번 그어진다
import { useEffect, useId, useRef, useState, type CSSProperties, type FormEvent, type KeyboardEvent } from 'react'
import { ApiError } from '../api/client'
import {
  createAccessKey,
  getAccessKeys,
  mcpEndpoint,
  revokeAccessKey,
  setupSnippet,
  type AccessKeyList,
  type ClientKind,
  type CreatedKey,
  type KeyScope,
} from './connectApi'
import './ai-connect.css'

const NAME_MAX = 40
const SCOPE_LABEL: Record<KeyScope, string> = { read: '읽기만', draft: '읽기 + 초안' }
const CLIENTS: { kind: ClientKind; label: string; note: string }[] = [
  { kind: 'claude-code', label: 'Claude Code', note: '터미널에서 한 줄을 붙여 넣습니다.' },
  {
    kind: 'claude-desktop',
    label: 'Claude Desktop',
    note: '설정 → 개발자 → 설정 편집으로 연 claude_desktop_config.json에 넣고 앱을 다시 켭니다. Node.js가 있어야 합니다.',
  },
  { kind: 'other', label: '다른 앱', note: 'MCP를 지원하는 앱이면 이 주소와 머리글로 붙습니다.' },
]
/** AI 비서가 할 수 있는 일 (도구 이름은 contracts/mcp-api.md 5) */
const TOOLS = [
  ['글 찾기', 'search_posts'],
  ['단락 찾기 · 출처', 'search_passages'],
  ['글 읽기', 'read_post'],
  ['내 글 목록', 'list_my_posts'],
  ['내 블로그 통계', 'my_blog_stats'],
  ['초안 남기기', 'save_draft'],
] as const

function day(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

type CopyTarget = 'token' | 'snippet'

export default function AiConnectCard() {
  const titleId = useId()
  const [list, setList] = useState<AccessKeyList | null>(null)
  // 기한 지남을 가르는 기준 시각. 불러올 때 정한다 (그리는 동안 시계를 읽지 않게)
  const [loadedAt, setLoadedAt] = useState(0)
  const [loadFailed, setLoadFailed] = useState(false)
  const [reload, setReload] = useState(0)

  const [name, setName] = useState('')
  const [scope, setScope] = useState<KeyScope>('read')
  const [days, setDays] = useState<number | null>(null)
  const [nameError, setNameError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const [issued, setIssued] = useState<CreatedKey | null>(null)
  const [confirming, setConfirming] = useState<number | null>(null)
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)
  const [tab, setTab] = useState<ClientKind>('claude-code')
  const [copied, setCopied] = useState<{ target: CopyTarget; text: string } | null>(null)
  const tokenRef = useRef<HTMLElement>(null)
  const snippetRef = useRef<HTMLElement>(null)
  const tabRefs = useRef<Partial<Record<ClientKind, HTMLButtonElement | null>>>({})

  useEffect(() => {
    const controller = new AbortController()
    getAccessKeys(controller.signal)
      .then((res) => {
        setList(res)
        setLoadedAt(Date.now())
        setLoadFailed(false)
      })
      .catch(() => {
        if (!controller.signal.aborted) setLoadFailed(true)
      })
    return () => controller.abort()
  }, [reload])

  if (!list) {
    return (
      <section className="mypage-card ai-connect" aria-labelledby={titleId}>
        <Heading id={titleId} />
        {loadFailed ? (
          <>
            <p className="msg msg-error" role="alert">
              ※ 열쇠를 불러오지 못했습니다
            </p>
            <button type="button" className="btn btn-outline btn-small" onClick={() => setReload((n) => n + 1)}>
              다시 시도
            </button>
          </>
        ) : (
          <p className="hint">불러오는 중</p>
        )}
      </section>
    )
  }

  const full = list.items.length >= list.maxCount
  const chosenDays = days ?? (list.allowedDays.includes(90) ? 90 : list.allowedDays[0])
  const endpoint = mcpEndpoint()
  const client = CLIENTS.find((c) => c.kind === tab) ?? CLIENTS[0]
  const snippet = setupSnippet(tab, endpoint, issued?.token)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    setMessage(null)
    const trimmed = name.trim()
    if (!trimmed) {
      setNameError('※ 열쇠 이름을 적어 주세요')
      return
    }
    setBusy(true)
    try {
      const created = await createAccessKey(trimmed, scope, chosenDays)
      setIssued(created)
      setCopied(null)
      setList((prev) => prev && { ...prev, items: [{ ...created, lastUsedAt: null }, ...prev.items] })
      setName('')
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
    } finally {
      setBusy(false)
    }
  }

  async function revoke(keyId: number, keyName: string) {
    setMessage(null)
    try {
      await revokeAccessKey(keyId)
      setList((prev) => prev && { ...prev, items: prev.items.filter((k) => k.keyId !== keyId) })
      if (issued?.keyId === keyId) setIssued(null)
      setMessage({ ok: true, text: `${keyName} 열쇠를 끊었습니다` })
    } catch (err) {
      setMessage({ ok: false, text: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
    } finally {
      setConfirming(null)
    }
  }

  async function copy(target: CopyTarget, text: string) {
    try {
      await navigator.clipboard.writeText(text)
      setCopied({ target, text: '복사했어요' })
    } catch {
      // 클립보드를 못 쓰면 글자를 골라 둔다: 사용자가 직접 복사한다
      const element = target === 'token' ? tokenRef.current : snippetRef.current
      if (element) window.getSelection()?.selectAllChildren(element)
      setCopied({ target, text: '골라 두었어요. 직접 복사해 주세요' })
    }
  }

  function moveTab(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    const step = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0
    if (step === 0) return
    event.preventDefault()
    const next = CLIENTS[(index + step + CLIENTS.length) % CLIENTS.length].kind
    setTab(next)
    tabRefs.current[next]?.focus()
  }

  return (
    <section className="mypage-card ai-connect" aria-labelledby={titleId}>
      <Heading id={titleId} />
      <p className="ai-connect-lead">
        Claude 같은 AI 비서가 내 블로그의 글을 찾아 읽고, 맞는 단락을 <strong>출처와 함께</strong> 가져와 답하게 합니다.
        초안 열쇠면 임시 저장 글도 남깁니다. <em>글을 올리는 일은 언제나 내가 합니다.</em>
      </p>
      <ul className="ai-tools" aria-label="AI 비서가 할 수 있는 일">
        {TOOLS.map(([label, tool]) => (
          <li key={tool} title={tool}>
            {label}
          </li>
        ))}
      </ul>
      <p className="hint ai-example">
        이렇게 물어보세요: “내 블로그에서 제주 여행 글을 찾아 맛집 이야기만 출처와 함께 정리해 줘”
      </p>

      {issued && (
        <section className="ai-key-issued" aria-label="새 열쇠">
          <span className="ai-once-seal" aria-hidden="true">
            한 번만
          </span>
          <p className="ai-issued-title">
            <span className="ink-highlight">{issued.name}</span> 열쇠가 생겼어요
          </p>
          <code ref={tokenRef} className="ai-token">
            {issued.token}
          </code>
          <div className="ai-issued-tools">
            <button
              type="button"
              className="btn btn-primary btn-small"
              aria-label="열쇠 복사"
              onClick={() => copy('token', issued.token)}
            >
              {copied?.target === 'token' ? copied.text : '복사'}
            </button>
            <button type="button" className="btn btn-quiet btn-small" onClick={() => setIssued(null)}>
              다 적어 두었어요
            </button>
          </div>
          <p className="hint">
            열쇠는 지금 한 번만 보입니다. 글결에도 남지 않으니 잃어버리면 끊고 새로 만드세요. 비밀번호를 바꾸면 열쇠는
            모두 끊깁니다. 아래 붙이는 법에 이미 채워 두었어요.
          </p>
        </section>
      )}

      <form className="ai-key-form" onSubmit={submit} noValidate aria-label="새 열쇠 만들기">
        <div className="field">
          <label htmlFor={`${titleId}-name`}>열쇠 이름</label>
          <input
            id={`${titleId}-name`}
            value={name}
            maxLength={NAME_MAX}
            placeholder="예: 노트북 Claude Code"
            disabled={busy || full}
            onChange={(e) => {
              setName(e.target.value)
              setNameError(null)
            }}
            aria-invalid={Boolean(nameError)}
            aria-describedby={nameError ? `${titleId}-name-error` : undefined}
          />
          {nameError && (
            <p id={`${titleId}-name-error`} className="msg msg-error" role="alert">
              {nameError}
            </p>
          )}
        </div>
        <fieldset className="ai-scope" disabled={busy || full}>
          <legend>할 수 있는 일</legend>
          <label className={scope === 'read' ? 'ai-scope-option is-on' : 'ai-scope-option'}>
            <input type="radio" name="scope" checked={scope === 'read'} onChange={() => setScope('read')} />
            <span>
              <b>읽기만</b>
              <small>글 찾기·읽기, 내 글 목록, 통계</small>
            </span>
          </label>
          <label className={scope === 'draft' ? 'ai-scope-option is-on' : 'ai-scope-option'}>
            <input type="radio" name="scope" checked={scope === 'draft'} onChange={() => setScope('draft')} />
            <span>
              <b>읽기 + 초안 남기기</b>
              <small>임시 저장 글까지. 발행은 하지 않아요</small>
            </span>
          </label>
        </fieldset>
        <div className="field ai-days">
          <label htmlFor={`${titleId}-days`}>기한</label>
          <select
            id={`${titleId}-days`}
            value={chosenDays}
            disabled={busy || full}
            onChange={(e) => setDays(Number(e.target.value))}
          >
            {list.allowedDays.map((d) => (
              <option key={d} value={d}>
                {d}일
              </option>
            ))}
          </select>
        </div>
        <button type="submit" className="btn btn-primary" disabled={busy || full}>
          {busy ? '만드는 중' : '열쇠 만들기'}
        </button>
        {full && (
          <p className="hint ai-full">
            열쇠는 {list.maxCount}개까지 만들 수 있어요. 쓰지 않는 열쇠를 끊으면 새로 만들 수 있습니다.
          </p>
        )}
        {formError && (
          <p className="msg msg-error ai-full" role="alert">
            {formError}
          </p>
        )}
      </form>

      {list.items.length > 0 && (
        <ul className="ai-keys" aria-label="내 열쇠">
          {list.items.map((k, index) => {
            const expired = new Date(k.expiresAt).getTime() <= loadedAt
            return (
              <li
                key={k.keyId}
                className={expired ? 'ai-key is-expired' : 'ai-key'}
                style={{ '--i': index } as CSSProperties}
              >
                <div className="ai-key-main">
                  <span className="ai-key-name">{k.name}</span>
                  <span className={`ai-key-scope scope-${k.scope}`}>{SCOPE_LABEL[k.scope]}</span>
                  {expired && <span className="ai-key-expired">기한 지남</span>}
                </div>
                <code className="ai-key-hint">{k.hint}…</code>
                <span className="ai-key-meta">
                  {day(k.createdAt)} 만듦 · {day(k.expiresAt)}까지 ·{' '}
                  {k.lastUsedAt ? `${day(k.lastUsedAt)} 마지막 사용` : '아직 안 씀'}
                </span>
                {confirming === k.keyId ? (
                  <div className="ai-key-confirm" role="group" aria-label={`${k.name} 열쇠를 끊을까요?`}>
                    <span>이 열쇠를 쓰는 비서는 바로 막혀요.</span>
                    <button type="button" className="btn btn-danger btn-small" onClick={() => revoke(k.keyId, k.name)}>
                      끊기
                    </button>
                    <button type="button" className="btn btn-quiet btn-small" onClick={() => setConfirming(null)}>
                      그만두기
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    className="btn btn-quiet btn-small ai-key-revoke"
                    aria-label={`${k.name} 끊기`}
                    onClick={() => setConfirming(k.keyId)}
                  >
                    끊기
                  </button>
                )}
              </li>
            )
          })}
        </ul>
      )}
      {message && (
        <p className={message.ok ? 'msg msg-ok' : 'msg msg-error'} role={message.ok ? 'status' : 'alert'}>
          {message.text}
        </p>
      )}

      {/* 복사 결과를 화면 읽기 도구에 알린다 (단추 이름은 "열쇠 복사"·"설정 복사"로 그대로) */}
      <p className="visually-hidden" role="status" aria-live="polite">
        {copied ? `${copied.target === 'token' ? '열쇠' : '설정'}: ${copied.text}` : ''}
      </p>

      <div className="ai-setup">
        <h3 className="ai-setup-title">붙이는 법</h3>
        <div className="ai-tabs" role="tablist" aria-label="AI 앱 고르기">
          {CLIENTS.map((c, index) => (
            <button
              key={c.kind}
              ref={(el) => {
                tabRefs.current[c.kind] = el
              }}
              type="button"
              role="tab"
              id={`${titleId}-tab-${c.kind}`}
              aria-selected={tab === c.kind}
              aria-controls={`${titleId}-panel`}
              tabIndex={tab === c.kind ? 0 : -1}
              onClick={() => setTab(c.kind)}
              onKeyDown={(e) => moveTab(e, index)}
            >
              {c.label}
            </button>
          ))}
        </div>
        <div className="ai-panel" role="tabpanel" id={`${titleId}-panel`} aria-labelledby={`${titleId}-tab-${tab}`}>
          <p className="hint">{client.note}</p>
          <pre className="ai-snippet">
            <code ref={snippetRef}>{snippet}</code>
          </pre>
          <button
            type="button"
            className="btn btn-outline btn-small"
            aria-label="설정 복사"
            onClick={() => copy('snippet', snippet)}
          >
            {copied?.target === 'snippet' ? copied.text : '복사'}
          </button>
          {!issued && <p className="hint">&lt;열쇠&gt; 자리에 만든 열쇠를 넣으세요.</p>}
        </div>
      </div>
    </section>
  )
}

function Heading({ id }: { id: string }) {
  return (
    <h2 id={id} className="mypage-card-title ai-connect-title">
      AI 비서 연결 <span className="ai-mcp-tag">MCP</span>
    </h2>
  )
}
