// 없는 주소 (v0.5.1 원고지 손길). 예전에는 첫 화면을 그대로 보여 줘서 주소가 틀린 줄 몰랐다
import ProofNotFound from '../components/ProofNotFound'

export default function NotFoundPage() {
  return <ProofNotFound struck="글" inserted="쪽" message="주소가 바뀌었거나 없는 쪽입니다." />
}
