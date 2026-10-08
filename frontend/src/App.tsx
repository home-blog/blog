// 화면 주소: / 첫 화면, /login 로그인, /signup 회원 가입, /mypage 마이페이지, /mypage/password 비밀번호 변경, /mypage/withdrawal 회원 탈퇴(로그인 필요)
// /blog/:blogId 블로그(누구나), /posts/:postId 글(누구나), /me/blog 내 블로그로 가기·/write 글쓰기·/write/:postId 글 고치기·/manage/categories 분류 관리·/manage/blog 블로그 설정(로그인 필요) — specs/003
// createBrowserRouter를 쓴다: "저장하지 않은 내용" 확인(useBlocker, specs/002 FR-011)이 이 방식에서만 동작한다
import { createBrowserRouter, Outlet, RouterProvider, useNavigate, useParams } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { LoginModalProvider } from './auth/LoginModal'
import PasswordChangePage from './account/PasswordChangePage'
import WithdrawalPage from './account/WithdrawalPage'
import RequireLogin from './auth/RequireLogin'
import BlogSettingsPage from './blog/BlogSettingsPage'
import CategoryManagePage from './blog/CategoryManagePage'
import MyBlogRedirect from './blog/MyBlogRedirect'
import SiteHeader from './components/SiteHeader'
import BlogHomePage from './pages/BlogHomePage'
import HomePage from './pages/HomePage'
import PostDetailPage from './post/PostDetailPage'
import PostEditorPage from './post/PostEditorPage'
import LoginPage from './pages/LoginPage'
import MyPage from './pages/MyPage'
import SignupPage from './pages/SignupPage'

/** 글 번호마다 화면을 새로 만든다: 다른 글로 옮겨 가면 이전 글의 입력·확인 창 상태가 남지 않는다 */
function PostDetailRoute() {
  const { postId } = useParams()
  return <PostDetailPage key={postId} />
}

function PostEditRoute() {
  const { postId } = useParams()
  return <PostEditorPage key={postId} />
}

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
      {
        path: '/mypage/password',
        element: (
          <RequireLogin>
            <PasswordChangePage />
          </RequireLogin>
        ),
      },
      {
        path: '/mypage/withdrawal',
        element: (
          <RequireLogin>
            <WithdrawalPage />
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
      { path: '/posts/:postId', element: <PostDetailRoute /> },
      {
        path: '/write',
        element: (
          <RequireLogin>
            <PostEditorPage />
          </RequireLogin>
        ),
      },
      {
        path: '/write/:postId',
        element: (
          <RequireLogin>
            <PostEditRoute />
          </RequireLogin>
        ),
      },
      {
        path: '/manage/categories',
        element: (
          <RequireLogin>
            <CategoryManagePage />
          </RequireLogin>
        ),
      },
      {
        path: '/manage/blog',
        element: (
          <RequireLogin>
            <BlogSettingsPage />
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
