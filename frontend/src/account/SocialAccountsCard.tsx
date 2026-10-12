// 마이페이지 "연결된 계정" (specs/010 US4): 회사마다 연결됨/안 됨, 연결하기·연결 해제
// 연결하기는 서버 세션에 "이 회원에 연결"을 적은 뒤 회사 화면으로 이동한다. 다녀오면 서버가 /mypage?linked= 또는 ?linkError=로 보낸다
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { authorizationUrl, PROVIDER_LABEL, socialErrorMessage, type SocialProvider } from '../auth/socialApi'
import { getSocialAccounts, startLink, unlink, type SocialAccounts } from './accountApi'

function linkedDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

export default function SocialAccountsCard() {
  const [params, setParams] = useSearchParams()
  const [accounts, setAccounts] = useState<SocialAccounts | null>(null)
  const [busy, setBusy] = useState<SocialProvider | null>(null)
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(() => {
    const linked = params.get('linked') as SocialProvider | null
    if (linked && linked in PROVIDER_LABEL) return { ok: true, text: `${PROVIDER_LABEL[linked]} 계정을 연결했습니다` }
    const error = socialErrorMessage(params.get('linkError'))
    return error ? { ok: false, text: error } : null
  })

  useEffect(() => {
    // 결과를 한 번 보여 줬으면 주소에서 지운다 (새로 고침해도 다시 뜨지 않게)
    if (params.has('linked') || params.has('linkError')) setParams({}, { replace: true })
  }, [params, setParams])

  useEffect(() => {
    const controller = new AbortController()
    getSocialAccounts(controller.signal)
      .then(setAccounts)
      .catch(() => undefined)
    return () => controller.abort()
  }, [])

  if (!accounts || accounts.providers.length + accounts.linked.length === 0) return null

  // 연결할 수 있는 회사 + 열쇠가 빠졌어도 이미 연결된 회사 (해제는 할 수 있게)
  const shown = [
    ...accounts.providers,
    ...accounts.linked.map((l) => l.provider).filter((p) => !accounts.providers.includes(p)),
  ]

  async function connect(provider: SocialProvider) {
    setBusy(provider)
    setMessage(null)
    try {
      await startLink(provider)
      window.location.assign(authorizationUrl(provider))
    } catch (err) {
      setMessage({ ok: false, text: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
      setBusy(null)
    }
  }

  async function disconnect(provider: SocialProvider) {
    setBusy(provider)
    setMessage(null)
    try {
      await unlink(provider)
      setAccounts((prev) => prev && { ...prev, linked: prev.linked.filter((l) => l.provider !== provider) })
      setMessage({ ok: true, text: `${PROVIDER_LABEL[provider]} 연결을 해제했습니다` })
    } catch (err) {
      setMessage({ ok: false, text: err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요' })
    } finally {
      setBusy(null)
    }
  }

  return (
    <section className="mypage-card" aria-labelledby="social-title">
      <h2 id="social-title" className="mypage-card-title">
        연결된 계정
      </h2>
      <p className="hint mypage-note">연결하면 그 계정으로도 로그인할 수 있습니다.</p>
      <ul className="social-accounts">
        {shown.map((provider) => {
          const linked = accounts.linked.find((l) => l.provider === provider)
          return (
            <li key={provider} className="social-account">
              <span className={`social-account-name social-dot-${provider}`}>{PROVIDER_LABEL[provider]}</span>
              <span className="social-account-state">
                {linked ? `${linkedDate(linked.linkedAt)} 연결됨` : '연결 안 됨'}
              </span>
              {linked ? (
                <button
                  type="button"
                  className="btn btn-quiet btn-small"
                  disabled={busy !== null}
                  onClick={() => disconnect(provider)}
                  aria-label={`${PROVIDER_LABEL[provider]} 연결 해제`}
                >
                  {busy === provider ? '해제하는 중' : '연결 해제'}
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-outline btn-small"
                  disabled={busy !== null || !accounts.providers.includes(provider)}
                  onClick={() => connect(provider)}
                  aria-label={`${PROVIDER_LABEL[provider]} 연결하기`}
                >
                  {busy === provider ? '이동하는 중' : '연결하기'}
                </button>
              )}
            </li>
          )
        })}
      </ul>
      {message && (
        <p className={message.ok ? 'msg msg-ok' : 'msg msg-error'} role={message.ok ? 'status' : 'alert'}>
          {message.text}
        </p>
      )}
      {!accounts.hasPassword && (
        <p className="hint">
          비밀번호가 없어서 마지막 남은 연결은 해제할 수 없습니다. <Link to="/mypage/password">비밀번호 만들기</Link>
        </p>
      )}
    </section>
  )
}
