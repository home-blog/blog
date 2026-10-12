// 소셜 로그인 버튼 (specs/010 US1, D-9): 서버에 열쇠가 들어 있는 회사만 보인다. 하나도 없으면 아무것도 그리지 않는다
// 버튼은 주소 이동이다: 회사 화면을 다녀오면 서버가 첫 화면(로그인됨)이나 소셜 가입 화면으로 보낸다
import { useEffect, useState } from 'react'
import { authorizationUrl, getProviders, PROVIDER_LABEL, type SocialProvider } from './socialApi'
import './social.css'

interface Props {
  /** 버튼 위 구분선 문구 */
  divider?: string
}

export default function SocialButtons({ divider = '또는' }: Props) {
  const [providers, setProviders] = useState<SocialProvider[]>([])

  useEffect(() => {
    const controller = new AbortController()
    getProviders(controller.signal)
      .then(setProviders)
      .catch(() => {
        // 버튼 목록을 못 받으면 이메일 로그인만 보인다
      })
    return () => controller.abort()
  }, [])

  if (providers.length === 0) return null
  return (
    <div className="social-login">
      <p className="social-divider">
        <span>{divider}</span>
      </p>
      <ul className="social-buttons">
        {providers.map((provider) => (
          <li key={provider}>
            <a className={`social-btn social-${provider}`} href={authorizationUrl(provider)}>
              <span className="social-mark" aria-hidden="true">
                {MARK[provider]}
              </span>
              {PROVIDER_LABEL[provider]}로 계속하기
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** 회사 표시: 글자 하나 (로고 그림 대신) */
const MARK: Record<SocialProvider, string> = {
  github: 'G',
  google: 'G',
  kakao: 'K',
  naver: 'N',
}
