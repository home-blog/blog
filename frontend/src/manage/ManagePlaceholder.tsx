// 아직 만들지 않은 관리 화면 자리 (specs/006 T012). 그 이야기를 만들 때 바꾼다
interface Props {
  title: string
}

export default function ManagePlaceholder({ title }: Props) {
  return (
    <div className="manage">
      <header className="manage-head">
        <h1 className="manage-title">{title}</h1>
        <p className="hint">준비 중인 화면입니다.</p>
      </header>
    </div>
  )
}
