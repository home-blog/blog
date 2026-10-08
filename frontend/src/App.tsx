// 화면 전환: 지금은 가입 화면과 가입 완료만 있다. 로그인 화면은 001 US2에서 더한다.
import { useState } from 'react'
import ManuscriptSheet from './pages/ManuscriptSheet'
import SignupPage from './pages/SignupPage'

type View = 'signup' | 'signed-up'

export default function App() {
  const [view, setView] = useState<View>('signup')

  if (view === 'signed-up') {
    return (
      <main className="done">
        <ManuscriptSheet lines={['가입 완료']} rows={1} />
        <h1>가입이 완료되었습니다</h1>
        <p>로그인해 주세요. 로그인 화면은 다음 단계에서 열립니다.</p>
      </main>
    )
  }

  return (
    <main>
      <SignupPage onSignedUp={() => setView('signed-up')} />
    </main>
  )
}
