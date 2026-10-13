// 없는 쪽·없는 글을 교정부호로 (v0.5.1 원고지 손길): 빨간 펜이 틀린 말을 긋고 위에 고쳐 쓴다
// 예: "찾으시는 [쪽→글]이 이 원고에 없습니다." 고쳐 쓴 말이 실제로 없던 것이다
import { Link } from 'react-router'
import './proof.css'

interface Props {
  /** 지우는 말 */
  struck: string
  /** 고쳐 쓰는 말 (실제로 찾던 것) */
  inserted: string
  /** 고쳐 쓴 말 뒤의 조사 (받침이 없으면 "가") */
  particle?: '이' | '가'
  /** 화면 읽기 도구와 화면에 함께 보이는 한 줄 (예: "존재하지 않는 글입니다") */
  message: string
}

export default function ProofNotFound({ struck, inserted, particle = '이', message }: Props) {
  return (
    <section className="proof" role="alert">
      <p className="proof-line" aria-hidden="true">
        찾으시는 <span className="proof-del">{struck}</span>
        <span className="proof-ins">
          <span className="proof-caret">∨</span>
          {inserted}
        </span>
        {particle} 이 원고에 없습니다.
      </p>
      <p className="proof-message hint">{message}</p>
      <p className="proof-actions">
        <Link to="/" className="btn btn-primary btn-small">
          첫 화면으로
        </Link>
        <Link to="/search" className="btn btn-outline btn-small">
          검색하기
        </Link>
      </p>
    </section>
  )
}
