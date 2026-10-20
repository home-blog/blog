// 연결된 앱 (specs/023 US2, T006): Claude 앱 커스텀 커넥터처럼 위임장으로 허락한 앱. 끊으면 그 앱은 바로 막힌다.
// 모양은 열쇠 꼬리표와 같고, 이름 옆에 붉은 "위임" 도장이 찍혀 있다
import { useEffect, useState, type CSSProperties } from 'react'
import { ApiError } from '../api/client'
import { disconnectApp, getConnectedApps, type ConnectedApps, type OAuthScope } from '../connect/oauthApi'

const SCOPE_LABEL: Record<OAuthScope, string> = { read: '읽기만', draft: '읽기 + 초안' }

function day(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

export default function ConnectedAppList() {
  const [apps, setApps] = useState<ConnectedApps | null>(null)
  const [failed, setFailed] = useState(false)
  const [confirming, setConfirming] = useState<number | null>(null)
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getConnectedApps(controller.signal)
      .then(setApps)
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true)
      })
    return () => controller.abort()
  }, [reload])

  async function disconnect(grantId: number, name: string) {
    try {
      await disconnectApp(grantId)
      setApps((current) => current && { ...current, items: current.items.filter((app) => app.grantId !== grantId) })
      setMessage({ ok: true, text: `${name} 연결을 끊었습니다` })
    } catch (err) {
      setMessage({ ok: false, text: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
    } finally {
      setConfirming(null)
    }
  }

  return (
    <div className="ai-apps">
      <h3 className="ai-setup-title">연결된 앱</h3>
      {failed && (
        <>
          <p className="msg msg-error" role="alert">
            ※ 연결된 앱을 불러오지 못했습니다
          </p>
          <button
            type="button"
            className="btn btn-outline btn-small"
            onClick={() => {
              setFailed(false)
              setReload((n) => n + 1)
            }}
          >
            다시 시도
          </button>
        </>
      )}
      {apps && apps.items.length === 0 && (
        <p className="hint">아직 연결된 앱이 없어요. 위 “Claude 앱(커넥터)” 탭의 주소로 붙여 보세요.</p>
      )}
      {apps && apps.items.length > 0 && (
        <ul className="ai-keys" aria-label="연결된 앱">
          {apps.items.map((app, index) => (
            <li key={app.grantId} className="ai-key ai-app" style={{ '--i': index } as CSSProperties}>
              <div className="ai-key-main">
                <span className="ai-key-name">{app.clientName}</span>
                <span className="ai-app-seal" aria-hidden="true">
                  위임
                </span>
                <span className={`ai-key-scope scope-${app.scope}`}>{SCOPE_LABEL[app.scope]}</span>
              </div>
              <span className="ai-key-meta">
                {day(app.createdAt)} 허락 · {app.lastUsedAt ? `${day(app.lastUsedAt)} 마지막 사용` : '아직 안 씀'}
              </span>
              {confirming === app.grantId ? (
                <div className="ai-key-confirm" role="group" aria-label={`${app.clientName} 연결을 끊을까요?`}>
                  <span>이 앱은 바로 막혀요. 다시 쓰려면 앱에서 다시 연결해요.</span>
                  <button
                    type="button"
                    className="btn btn-danger btn-small"
                    onClick={() => disconnect(app.grantId, app.clientName)}
                  >
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
                  aria-label={`${app.clientName} 연결 끊기`}
                  onClick={() => setConfirming(app.grantId)}
                >
                  끊기
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      {apps && apps.items.length >= apps.max && (
        <p className="hint">앱은 {apps.max}개까지 이어 둘 수 있어요. 더 이으면 가장 오래 쓰지 않은 앱이 끊겨요.</p>
      )}
      {message && (
        <p className={message.ok ? 'msg msg-ok' : 'msg msg-error'} role={message.ok ? 'status' : 'alert'}>
          {message.text}
        </p>
      )}
    </div>
  )
}
