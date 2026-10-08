// 화면 주소: / 첫 화면, /login 로그인, /signup 회원 가입
import { BrowserRouter, Route, Routes, useNavigate } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { LoginModalProvider } from './auth/LoginModal'
import SiteHeader from './components/SiteHeader'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'

function SignupRoute() {
  const navigate = useNavigate()
  // 가입 뒤 자동 로그인은 하지 않고 로그인 화면으로 보낸다 (CF-01-9)
  return <SignupPage onSignedUp={() => navigate('/login', { replace: true, state: { signedUp: true } })} />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <LoginModalProvider>
          <SiteHeader />
          <main>
            {/* 회원 전용 화면은 <RequireLogin>으로 감싼다 (예: 글쓰기, 블로그 관리 — 003, 006) */}
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupRoute />} />
              <Route path="*" element={<HomePage />} />
            </Routes>
          </main>
        </LoginModalProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
