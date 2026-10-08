// 글 상세 화면 자리 (specs/003 US3에서 채운다). 지금은 저장한 글 번호만 알려 준다
import { Link, useParams } from 'react-router'

export default function PostDetailPage() {
  const { postId } = useParams()
  return (
    <p className="editor-status hint">
      {postId}번 글을 저장했습니다. 글 읽기 화면은 다음 단계에서 열립니다. <Link to="/me/blog">내 블로그</Link>
    </p>
  )
}
