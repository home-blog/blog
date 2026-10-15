// 종류 꼬리표 (specs/012 US1-2): 업데이트는 초록, 공지는 노란 형광펜. 색만으로 나누지 않도록 글자를 함께 쓴다
import { KIND_LABEL } from './rules'
import type { NoticeKind } from './noticeApi'

export default function NoticeTag({ kind }: { kind: NoticeKind }) {
  return <span className={`notice-tag notice-tag-${kind}`}>{KIND_LABEL[kind]}</span>
}
