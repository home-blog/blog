// 블로그 관리 화면 틀 /manage (specs/006 T012, T015, FR-001, FR-002, FR-005)
// - 왼쪽 메뉴: 대시보드, 글 관리, 분류 관리, 댓글 관리, 통계, 설정. 휴대폰 화면에서는 메뉴가 위로 (NF-03)
// - 메뉴 위에 블로그 이름, "내 블로그 보기", "글쓰기". 블로그는 서버가 세션으로 정한다 (주소에 블로그 번호가 없다)
// - 로그인하지 않았으면 RequireLogin이 로그인 창을 띄운다 (App.tsx)
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router'
import { ApiError } from '../api/client'
import '../pages/auth-layout.css'
import { getManageHeader, type ManageHeader } from './manageApi'
import { ManageBlogContext } from './manageBlog'
import './manage-layout.css'

const MENU = [
  { to: '/manage', label: '대시보드', end: true },
  { to: '/manage/posts', label: '글 관리' },
  { to: '/manage/categories', label: '분류 관리' },
  { to: '/manage/comments', label: '댓글 관리' },
  { to: '/manage/stats', label: '통계' },
  { to: '/manage/blog', label: '설정' },
]

export default function ManageLayout() {
  const [header, setHeader] = useState<ManageHeader | null>(null)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setHeader(await getManageHeader())
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    getManageHeader(controller.signal)
      .then(setHeader)
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof ApiError ? err.message : '※ 잠시 뒤 다시 시도해 주세요')
      })
    return () => controller.abort()
  }, [])

  const value = useMemo(() => (header ? { header, refresh } : null), [header, refresh])

  if (error) {
    return (
      <p className="manage-status msg msg-error" role="alert">
        {error}
      </p>
    )
  }
  if (!value) return <p className="manage-status hint">불러오는 중</p>

  return (
    <ManageBlogContext.Provider value={value}>
      <div className="manage-shell">
        <aside className="manage-side">
          <p className="manage-blog-name">{value.header.name}</p>
          <div className="manage-side-actions">
            <Link to={value.header.blogPath} className="btn btn-outline btn-small">
              내 블로그 보기
            </Link>
            <Link to="/write" className="btn btn-primary btn-small">
              글쓰기
            </Link>
          </div>
          <nav aria-label="블로그 관리">
            <ul className="manage-menu">
              {MENU.map((item) => (
                <li key={item.to}>
                  <NavLink to={item.to} end={item.end} className="manage-menu-link">
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </aside>
        <section className="manage-main">
          <Outlet />
        </section>
      </div>
    </ManageBlogContext.Provider>
  )
}
