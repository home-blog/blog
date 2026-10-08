// 분류 색 목록 (specs/006 T025, D-9). 값은 index.css의 --category-1 ~ 6 토큰이고, 개수는 서버 category.color-count(6)와 같다
// 서버가 준 색 번호(colorIndex)로 고른다. 목록 밖의 번호(설정을 줄인 경우)는 처음부터 다시 돈다
const CATEGORY_COLORS = [
  'var(--category-1)',
  'var(--category-2)',
  'var(--category-3)',
  'var(--category-4)',
  'var(--category-5)',
  'var(--category-6)',
]

export function categoryColor(colorIndex: number): string {
  const n = CATEGORY_COLORS.length
  return CATEGORY_COLORS[((colorIndex % n) + n) % n]
}
