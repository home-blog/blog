// 첫 화면 머리 "원고지 표지" (v0.5.1 원고지 손길, 2026-10-13 사용자 "추천대로"):
// 원고지 첫 장의 제목·쓴 사람·분량 칸 양식. 분량은 서버가 센 실제 숫자이고 세어 올린다
// 손님은 "원고지 받기"(가입)와 로그인, 회원은 "이어 쓰기"(글쓰기)와 내 블로그
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import CellLink from '../components/CellLink'
import { useCountUp } from '../components/useCountUp'
import { getSiteSummary, type SiteSummary } from '../explore/exploreApi'

interface Props {
  /** undefined: 로그인 확인 중, null: 손님 */
  member: { nickname: string } | null | undefined
}

function format(n: number | null): string {
  return n === null ? '' : n.toLocaleString('ko-KR')
}

export default function HomeCover({ member }: Props) {
  const [summary, setSummary] = useState<SiteSummary | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    getSiteSummary(controller.signal)
      .then(setSummary)
      // 분량은 꾸밈이라 못 불러오면 그 칸만 숨긴다
      .catch(() => undefined)
    return () => controller.abort()
  }, [])

  const sheets = useCountUp(summary ? summary.sheets : null)

  return (
    <div className="home-cover">
      <dl className="cover-form">
        <div className="cover-row">
          <dt>제목</dt>
          <dd>
            <h1 id="home-hero-title" className="cover-title">
              여럿이 나눠 쓰는 <span className="ink-highlight">원고지 한 묶음</span>
            </h1>
          </dd>
        </div>
        {summary && (
          <>
            <div className="cover-row">
              <dt>쓴 사람</dt>
              <dd className="cover-stat">
                <b>{format(summary.bloggerCount)}</b>명의 블로거
              </dd>
            </div>
            <div className="cover-row">
              <dt>분량</dt>
              <dd className="cover-stat">
                글 <b>{format(summary.postCount)}</b>편 · 원고지 <b>{format(sheets)}</b>매
              </dd>
            </div>
          </>
        )}
      </dl>
      {member ? (
        <p className="cover-actions">
          <CellLink to="/write" label="이어 쓰기" primary />
          <Link to="/me/blog" className="pen-link">
            <b>내 원고지</b>로 가기
          </Link>
        </p>
      ) : member === null ? (
        <p className="cover-actions">
          <CellLink to="/signup" label="원고지 받기" primary />
          <Link to="/login" className="pen-link">
            이미 원고지가 있어요 · <b>로그인</b>
          </Link>
        </p>
      ) : null}
    </div>
  )
}
