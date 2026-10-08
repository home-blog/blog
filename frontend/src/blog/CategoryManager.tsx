// 분류 관리 (specs/003 US6, T046): 추가, 이름 바꾸기, 공개/비공개, 순서(위·아래), 삭제
// - 분류마다 색 점 (specs/006 T024, 색 번호는 서버가 정함)
// - 006의 블로그 관리 화면(BM-04)에 그대로 넣을 수 있게 화면 조각(컴포넌트)으로 둔다
// - 거절 문구는 서버 것 그대로 보여 준다 (글이 있는 분류, 미분류, 이름 중복 등)
// - 미분류에는 삭제 버튼이 없다 (FR-042). 개수는 주인 기준(비공개 글 포함)
import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import ConfirmDialog from '../components/ConfirmDialog'
import { categoryColor } from '../manage/categoryColors'
import {
  createCategory,
  deleteCategory,
  reorderCategories,
  updateCategory,
  type CategorySummary,
  type Visibility,
} from './blogApi'
import { CATEGORY_NAME_MAX, categoryNameLength } from './rules'

interface Props {
  categories: CategorySummary[]
  onChange: (categories: CategorySummary[]) => void
}

function messageOf(err: unknown): string {
  if (err instanceof ApiError) return err.fieldErrors[0]?.message ?? err.message
  return '※ 잠시 뒤 다시 시도해 주세요'
}

export default function CategoryManager({ categories, onChange }: Props) {
  const [newName, setNewName] = useState('')
  const [newVisibility, setNewVisibility] = useState<Visibility>('public')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null)
  const [deleting, setDeleting] = useState<CategorySummary | null>(null)

  async function run(action: () => Promise<void>, done?: string) {
    if (busy) return
    setBusy(true)
    setMessage(null)
    try {
      await action()
      if (done) setMessage({ text: done, error: false })
    } catch (err) {
      setMessage({ text: messageOf(err), error: true })
    } finally {
      setBusy(false)
    }
  }

  function add(e: FormEvent) {
    e.preventDefault()
    const length = categoryNameLength(newName)
    if (length === 0) return setMessage({ text: '※ 분류 이름을 입력해 주세요', error: true })
    if (length > CATEGORY_NAME_MAX) return setMessage({ text: `※ 분류 이름은 ${CATEGORY_NAME_MAX}자 이하로 입력해 주세요`, error: true })
    void run(async () => {
      const created = await createCategory(newName.trim(), newVisibility)
      onChange([...categories, created])
      setNewName('')
      setNewVisibility('public')
    }, '분류를 추가했습니다')
  }

  function rename(category: CategorySummary) {
    void run(async () => {
      const updated = await updateCategory(category.categoryId, { name: editName.trim() })
      onChange(categories.map((c) => (c.categoryId === updated.categoryId ? updated : c)))
      setEditingId(null)
    }, '이름을 바꿨습니다')
  }

  function toggleVisibility(category: CategorySummary) {
    const next: Visibility = category.visibility === 'public' ? 'private' : 'public'
    void run(async () => {
      const updated = await updateCategory(category.categoryId, { visibility: next })
      onChange(categories.map((c) => (c.categoryId === updated.categoryId ? updated : c)))
    }, next === 'private' ? '비공개로 바꿨습니다. 이 분류의 글은 나만 봅니다' : '공개로 바꿨습니다')
  }

  function move(index: number, delta: number) {
    const order = categories.map((c) => c.categoryId)
    const target = index + delta
    if (target < 0 || target >= order.length) return
    ;[order[index], order[target]] = [order[target], order[index]]
    void run(async () => onChange(await reorderCategories(order)))
  }

  function remove(category: CategorySummary) {
    setDeleting(null)
    void run(async () => {
      await deleteCategory(category.categoryId)
      onChange(categories.filter((c) => c.categoryId !== category.categoryId))
    }, '분류를 지웠습니다')
  }

  return (
    <div className="category-manager">
      <ol className="category-list">
        {categories.map((category, index) => (
          <li key={category.categoryId} className="category-row">
            <div className="category-order">
              <button type="button" className="btn btn-quiet btn-icon" disabled={busy || index === 0} aria-label={`${category.name} 위로`}
                onClick={() => move(index, -1)}>
                ↑
              </button>
              <button type="button" className="btn btn-quiet btn-icon" disabled={busy || index === categories.length - 1}
                aria-label={`${category.name} 아래로`} onClick={() => move(index, 1)}>
                ↓
              </button>
            </div>

            {editingId === category.categoryId ? (
              <form className="category-rename" onSubmit={(e) => { e.preventDefault(); rename(category) }}>
                <label className="visually-hidden" htmlFor={`rename-${category.categoryId}`}>새 이름</label>
                <input id={`rename-${category.categoryId}`} value={editName} maxLength={CATEGORY_NAME_MAX * 2} autoFocus
                  onChange={(e) => setEditName(e.target.value)} />
                <button type="submit" className="btn btn-primary btn-small" disabled={busy}>저장</button>
                <button type="button" className="btn btn-quiet btn-small" onClick={() => setEditingId(null)}>취소</button>
              </form>
            ) : (
              <span className="category-name">
                <span className="category-dot" style={{ background: categoryColor(category.colorIndex) }} aria-hidden="true" />
                {category.name}
                {category.isDefault && <span className="hint"> (기본)</span>}
                {category.visibility === 'private' && <span className="blog-badge">비공개</span>}
              </span>
            )}

            <span className="category-count">글 {category.postCount}개</span>

            {editingId !== category.categoryId && (
              <div className="category-actions">
                <button type="button" className="btn btn-quiet btn-small" disabled={busy}
                  onClick={() => { setEditingId(category.categoryId); setEditName(category.name) }}>
                  이름 바꾸기
                </button>
                <button type="button" className="btn btn-quiet btn-small" disabled={busy} onClick={() => toggleVisibility(category)}>
                  {category.visibility === 'public' ? '비공개로' : '공개로'}
                </button>
                {!category.isDefault && (
                  <button type="button" className="btn btn-quiet btn-small" disabled={busy} onClick={() => setDeleting(category)}>
                    삭제
                  </button>
                )}
              </div>
            )}
          </li>
        ))}
      </ol>

      <form className="category-add" onSubmit={add} noValidate>
        <label htmlFor="new-category">새 분류</label>
        <input id="new-category" value={newName} placeholder="분류 이름" onChange={(e) => setNewName(e.target.value)} />
        <select aria-label="새 분류의 공개 여부" value={newVisibility} onChange={(e) => setNewVisibility(e.target.value as Visibility)}>
          <option value="public">공개</option>
          <option value="private">비공개</option>
        </select>
        <button type="submit" className="btn btn-primary btn-small" disabled={busy}>추가</button>
      </form>

      <p className={`msg ${message?.error ? 'msg-error' : 'msg-ok'} category-message`} role="status" aria-live="polite">
        {message?.text}
      </p>

      <ConfirmDialog
        open={deleting !== null}
        title="분류 삭제"
        confirmLabel="삭제"
        danger
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && remove(deleting)}
      >
        <p>{deleting?.name} 분류를 삭제할까요? 글이 있는 분류는 지울 수 없습니다.</p>
      </ConfirmDialog>
    </div>
  )
}
