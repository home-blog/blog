// 답글 묶기와 화면 안의 목록 고치기 (specs/019). 서버 규칙과 같게: 답글은 한 단계, 답글이 남은 원댓글을 지우면 지운 칸,
// 마지막 답글이 지워지면 지운 칸도 사라진다. 실제 판단은 서버가 하고, 화면은 다시 읽지 않고 바로 보여 주려고 따라 한다
import type { CommentItem } from './commentApi'

export interface Thread {
  root: CommentItem
  replies: CommentItem[]
}

/** 원댓글마다 그 답글을 묶는다. 원댓글이 목록에 없는 답글은 버린다 (그런 일은 없어야 한다) */
export function threads(comments: CommentItem[]): Thread[] {
  const byRoot = new Map<number, Thread>()
  const result: Thread[] = []
  for (const comment of comments) {
    if (comment.parentId === null) {
      const thread = { root: comment, replies: [] }
      byRoot.set(comment.id, thread)
      result.push(thread)
    } else {
      byRoot.get(comment.parentId)?.replies.push(comment)
    }
  }
  return result
}

/** 지운 칸을 뺀 수 (서버의 count와 같다) */
export function liveCount(comments: CommentItem[]): number {
  return comments.filter((c) => !c.deleted).length
}

/** 새 답글을 그 원댓글의 마지막 답글 뒤에 넣는다. 원댓글이 없으면 맨 뒤 */
export function insertReply(comments: CommentItem[], reply: CommentItem): CommentItem[] {
  let at = -1
  comments.forEach((c, i) => {
    if (c.id === reply.parentId || c.parentId === reply.parentId) at = i
  })
  if (at < 0) return [...comments, reply]
  return [...comments.slice(0, at + 1), reply, ...comments.slice(at + 1)]
}

/** 댓글 하나를 지운 뒤의 목록 */
export function afterDelete(comments: CommentItem[], id: number): CommentItem[] {
  const target = comments.find((c) => c.id === id)
  if (!target) return comments
  if (target.parentId === null) {
    const hasReplies = comments.some((c) => c.parentId === id)
    if (!hasReplies) return comments.filter((c) => c.id !== id)
    // 지운 칸에는 답글을 달 수 없다: 그 아래 답글의 답글 단추도 함께 거둔다
    return comments.map((c) =>
      c.id === id
        ? {
            ...c,
            deleted: true,
            body: '',
            author: { id: null, nickname: null, withdrawn: false },
            canDelete: false,
            canReply: false,
          }
        : c.parentId === id
          ? { ...c, canReply: false }
          : c,
    )
  }
  const rest = comments.filter((c) => c.id !== id)
  const root = rest.find((c) => c.id === target.parentId)
  if (root?.deleted && !rest.some((c) => c.parentId === root.id)) {
    return rest.filter((c) => c.id !== root.id)
  }
  return rest
}
