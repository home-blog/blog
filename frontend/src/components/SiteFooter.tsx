// 모든 화면 아래의 바닥글: 지금 릴리스 번호와 API 주소 버전, 바뀐 것 기록(CHANGELOG) 링크
// (2026-10-12 사용자: 릴리스는 항상 문서로 남기고 API에도 반영한다)
import { Link } from 'react-router'
import { API_VERSION } from '../api/client'
import BrandMark from './BrandMark'
import './site-footer.css'

const CHANGELOG_URL = 'https://github.com/home-blog/blog/blob/main/CHANGELOG.md'

export default function SiteFooter() {
  return (
    <footer className="site-footer">
      <span className="site-footer-brand">
        <BrandMark size={18} />
        글결
      </span>
      <span className="site-footer-version">
        릴리스 v{__APP_VERSION__} · API {API_VERSION}
      </span>
      <Link to="/notices">공지사항</Link>
      <a href={CHANGELOG_URL} target="_blank" rel="noopener noreferrer">
        바뀐 것
      </a>
    </footer>
  )
}
