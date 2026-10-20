// 화면 주소: / 첫 화면, /login 로그인, /signup 회원 가입, /signup/social 소셜 가입(specs/010), /mypage 마이페이지, /mypage/password 비밀번호 변경, /mypage/withdrawal 회원 탈퇴(로그인 필요)
// /blog/:blogId 블로그(누구나), /posts/:postId 글(누구나), /me/blog 내 블로그로 가기·/write 글쓰기·/write/:postId 글 고치기(로그인 필요) — specs/003
// /manage 블로그 관리(로그인 필요, specs/006): 대시보드, /manage/posts 글, /manage/categories 분류(003 화면), /manage/comments 댓글, /manage/stats 통계, /manage/blog 설정(003 화면)
// 004: /blog/:blogId의 ?category= 분류·?page= 페이지, /search?q= 글 검색 결과(누구나)
// 005: /tags/:tagName 같은 태그의 공개 글(누구나)
// 012: /notices 공지사항(누구나, ?kind= 탭), /notices/:noticeId 읽기, /notices/new·/notices/:noticeId/edit 쓰기·고치기(관리자)
// createBrowserRouter를 쓴다: "저장하지 않은 내용" 확인(useBlocker, specs/002 FR-011)이 이 방식에서만 동작한다
import { lazy, Suspense, type ReactNode } from 'react'
import { createBrowserRouter, Outlet, RouterProvider, useNavigate, useParams } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { LoginModalProvider } from './auth/LoginModal'
import PasswordChangePage from './account/PasswordChangePage'
import WithdrawalPage from './account/WithdrawalPage'
import RequireLogin from './auth/RequireLogin'
import BlogSettingsPage from './blog/BlogSettingsPage'
import CategoryManagePage from './blog/CategoryManagePage'
import MyBlogRedirect from './blog/MyBlogRedirect'
import ManageCommentsPage from './manage/ManageCommentsPage'
import ManageLayout from './manage/ManageLayout'
import ManagePostsPage from './manage/ManagePostsPage'
import NewCommentCountProvider from './manage/NewCommentCountProvider'
import BoardEditorPage from './board/BoardEditorPage'
import BoardListPage from './board/BoardListPage'
import BoardPostPage from './board/BoardPostPage'
import GuestbookPage from './guestbook/GuestbookPage'
import ConnectAuthorizePage from './connect/ConnectAuthorizePage'
import EditorialPage from './editorial/EditorialPage'
import NoticeAlertProvider from './notice/NoticeAlertProvider'
import NoticeDetailPage from './notice/NoticeDetailPage'
import NoticeEditorPage from './notice/NoticeEditorPage'
import NoticeListPage from './notice/NoticeListPage'
import SiteFooter from './components/SiteFooter'
import InkTouch from './components/InkTouch'
import SiteHeader from './components/SiteHeader'
import BlogHomePage from './pages/BlogHomePage'
import LibraryPage from './community/clip/LibraryPage'
import LetterStopPage from './subscription/LetterStopPage'
import SubscribedPage from './subscription/SubscribedPage'
import HomePage from './pages/HomePage'
import PostDetailPage from './post/PostDetailPage'
import PostEditorPage from './post/PostEditorPage'
import LoginPage from './pages/LoginPage'
import MyPage from './pages/MyPage'
import NotFoundPage from './pages/NotFoundPage'
import SearchPage from './pages/SearchPage'
import SignupPage from './pages/SignupPage'
import SocialSignupPage from './pages/SocialSignupPage'
import TagPostsPage from './pages/TagPostsPage'
import SeriesManagePage from './series/SeriesManagePage'
import SeriesPage from './series/SeriesPage'

// 그래프 도구(uPlot)를 쓰는 화면은 따로 불러온다: 첫 화면이 가벼워지고, 블로그 관리를 열 때만 받는다 (specs/006 T058)
const DashboardPage = lazy(() => import('./manage/DashboardPage'))
const StatsPage = lazy(() => import('./manage/StatsPage'))

function Loading({ children }: { children: ReactNode }) {
  return <Suspense fallback={<p className="hint">불러오는 중</p>}>{children}</Suspense>
}

/** 글 번호마다 화면을 새로 만든다: 다른 글로 옮겨 가면 이전 글의 입력·확인 창 상태가 남지 않는다 */
function PostDetailRoute() {
  const { postId } = useParams()
  return <PostDetailPage key={postId} />
}

function PostEditRoute() {
  const { postId } = useParams()
  return <PostEditorPage key={postId} />
}

function BoardPostRoute() {
  const { boardPostId } = useParams()
  return <BoardPostPage key={boardPostId} />
}

function BoardEditRoute() {
  const { boardPostId } = useParams()
  return <BoardEditorPage key={boardPostId} />
}

function NoticeDetailRoute() {
  const { noticeId } = useParams()
  return <NoticeDetailPage key={noticeId} />
}

function NoticeEditRoute() {
  const { noticeId } = useParams()
  return <NoticeEditorPage key={noticeId} />
}

function SignupRoute() {
  const navigate = useNavigate()
  // 가입 뒤 자동 로그인은 하지 않고 로그인 화면으로 보낸다 (CF-01-9)
  return <SignupPage onSignedUp={() => navigate('/login', { replace: true, state: { signedUp: true } })} />
}

/** 모든 화면의 바깥: 로그인 상태, 로그인 창, 새 댓글 수(006), 새 공지 점(012), 머리글, 바닥글(릴리스 번호) */
function Layout() {
  return (
    <AuthProvider>
      <LoginModalProvider>
        <NewCommentCountProvider>
          <NoticeAlertProvider>
            <InkTouch />
            <SiteHeader />
            <main>
              <Outlet />
            </main>
            <SiteFooter />
          </NoticeAlertProvider>
        </NewCommentCountProvider>
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
      { path: '/signup/social', element: <SocialSignupPage /> },
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
        path: '/mypage/library',
        element: (
          <RequireLogin>
            <LibraryPage />
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
      {
        path: '/subscriptions',
        element: (
          <RequireLogin>
            <SubscribedPage />
          </RequireLogin>
        ),
      },
      // 원고 편지의 그만 받기 링크 (specs/016 US3). 로그인 없이
      { path: '/letters/stop', element: <LetterStopPage /> },
      { path: '/blog/:blogId', element: <BlogHomePage /> },
      { path: '/blog/:blogId/guestbook', element: <GuestbookPage /> },
      { path: '/search', element: <SearchPage /> },
      { path: '/tags/:tagName', element: <TagPostsPage /> },
      { path: '/series/:seriesId', element: <SeriesPage /> },
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
        path: '/manage',
        element: (
          <RequireLogin>
            <ManageLayout />
          </RequireLogin>
        ),
        children: [
          {
            index: true,
            element: (
              <Loading>
                <DashboardPage />
              </Loading>
            ),
          },
          { path: 'posts', element: <ManagePostsPage /> },
          { path: 'categories', element: <CategoryManagePage /> },
          { path: 'series', element: <SeriesManagePage /> },
          { path: 'comments', element: <ManageCommentsPage /> },
          {
            path: 'stats',
            element: (
              <Loading>
                <StatsPage />
              </Loading>
            ),
          },
          { path: 'blog', element: <BlogSettingsPage /> },
        ],
      },
      // 글마당 (specs/017): 읽기는 누구나, 쓰기는 로그인
      { path: '/board', element: <BoardListPage /> },
      {
        path: '/board/write',
        element: (
          <RequireLogin>
            <BoardEditorPage />
          </RequireLogin>
        ),
      },
      { path: '/board/:boardPostId', element: <BoardPostRoute /> },
      {
        path: '/board/:boardPostId/edit',
        element: (
          <RequireLogin>
            <BoardEditRoute />
          </RequireLogin>
        ),
      },
      // 편집실 (specs/018): 관리자만 (화면과 서버가 함께 확인)
      {
        path: '/editorial',
        element: (
          <RequireLogin>
            <EditorialPage />
          </RequireLogin>
        ),
      },
      // Claude 앱 커스텀 커넥터의 위임장 (specs/023): 로그인은 화면 안에서 묻는다(앱이 보낸 오류는 로그인 없이 보인다)
      { path: '/connect/authorize', element: <ConnectAuthorizePage /> },
      { path: '/notices', element: <NoticeListPage /> },
      { path: '/notices/:noticeId', element: <NoticeDetailRoute /> },
      {
        path: '/notices/new',
        element: (
          <RequireLogin>
            <NoticeEditorPage />
          </RequireLogin>
        ),
      },
      {
        path: '/notices/:noticeId/edit',
        element: (
          <RequireLogin>
            <NoticeEditRoute />
          </RequireLogin>
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
