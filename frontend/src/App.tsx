// 화면 주소: / 첫 화면, /login 로그인, /signup 회원 가입, /mypage 마이페이지(로그인 필요)
// /blog/:blogId 블로그(누구나), /me/blog 내 블로그로 가기(로그인 필요) — specs/003
// createBrowserRouter를 쓴다: "저장하지 않은 내용" 확인(useBlocker, specs/002 FR-011)이 이 방식에서만 동작한다
import { createBrowserRouter, Outlet, RouterProvider, useNavigate } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { LoginModalProvider } from './auth/LoginModal'
import RequireLogin from './auth/RequireLogin'
import MyBlogRedirect from './blog/MyBlogRedirect'
import SiteHeader from './components/SiteHeader'
import BlogHomePage from './pages/BlogHomePage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import MyPage from './pages/MyPage'
import SignupPage from './pages/SignupPage'

function SignupRoute() {
  const navigate = useNavigate()
  // 가입 뒤 자동 로그인은 하지 않고 로그인 화면으로 보낸다 (CF-01-9)
  return <SignupPage onSignedUp={() => navigate('/login', { replace: true, state: { signedUp: true } })} />
}

/** 모든 화면의 바깥: 로그인 상태, 로그인 창, 머리글 */
function Layout() {
  return (
    <AuthProvider>
      <LoginModalProvider>
        <SiteHeader />
        <main>
          <Outlet />
        </main>
      </LoginModalProvider>
    </AuthProvider>
  )
}

// 회원 전용 화면은 <RequireLogin>으로 감싼다 (예: 마이페이지, 글쓰기, 블로그 관리 — 002, 003, 006)
const router = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/signup', element: <SignupRoute /> },
      {
        path: '/mypage',
        element: (
          <RequireLogin>
            <MyPage />
          </RequireLogin>
        ),
      },
      { path: '/blog/:blogId', element: <BlogHomePage /> },
      {
        path: '/me/blog',
        element: (
          <RequireLogin>
            <MyBlogRedirect />
          </RequireLogin>
        ),
      },
      { path: '*', element: <HomePage /> },
    ],
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
