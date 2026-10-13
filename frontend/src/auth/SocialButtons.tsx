// 소셜 로그인 버튼 (specs/010 US1, D-9): 서버에 열쇠가 들어 있는 회사만 보인다. 하나도 없으면 아무것도 그리지 않는다
// 버튼은 주소 이동이다: 회사 화면을 다녀오면 서버가 첫 화면(로그인됨)이나 소셜 가입 화면으로 보낸다
// 모양은 회사 로고만 있는 동그란 버튼 한 줄 (2026-10-13 사용자: 큰 버튼 셋이 원고지 화면에서 로그인 버튼보다 눈에 띈다)
import { useEffect, useState, type ReactNode } from 'react'
import { authorizationUrl, getProviders, PROVIDER_LABEL, type SocialProvider } from './socialApi'
import './social.css'

interface Props {
  /** 버튼 위 구분선 문구 */
  divider?: string
}

export default function SocialButtons({ divider = '다른 계정으로' }: Props) {
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
        {providers.map((provider) => {
          const label = `${PROVIDER_LABEL[provider]}로 계속하기`
          return (
            <li key={provider}>
              <a
                className={`social-btn social-${provider}`}
                href={authorizationUrl(provider)}
                aria-label={label}
                title={label}
              >
                {LOGO[provider]}
              </a>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

/** 회사 로고 (각 회사 안내의 기호 모양을 그대로 그린 것) */
const LOGO: Record<SocialProvider, ReactNode> = {
  github: (
    <svg viewBox="0 0 16 16" aria-hidden="true" focusable="false">
      <path
        fill="currentColor"
        d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"
      />
    </svg>
  ),
  google: (
    <svg viewBox="0 0 48 48" aria-hidden="true" focusable="false">
      <path
        fill="#EA4335"
        d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
      />
      <path
        fill="#4285F4"
        d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
      />
      <path
        fill="#FBBC05"
        d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
      />
      <path
        fill="#34A853"
        d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
      />
    </svg>
  ),
  kakao: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="currentColor"
        d="M12 3C6.48 3 2 6.58 2 11c0 2.83 1.84 5.31 4.6 6.72l-.94 3.44c-.08.3.26.54.52.37l4.1-2.72c.56.08 1.13.12 1.72.12 5.52 0 10-3.58 10-8S17.52 3 12 3z"
      />
    </svg>
  ),
  naver: (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path fill="currentColor" d="M15.6 12.8 8.2 2H2v20h6.4V11.2L15.8 22H22V2h-6.4z" />
    </svg>
  ),
}
