// 빨간 펜 교정지 (specs/014 US1): 원고지 미리보기 자리에 본문을 그대로 두고, 고칠 곳마다 빨간 교정부호를 긋는다
// - 띄어쓰기: ∨(띄움)·⁀(붙임) 표, 맞춤법: 줄 긋고 위에 고친 말, 다듬기: 물결 밑줄과 고친 말
// - 교정부호를 누르면 아래에 이유와 받아들이기·넘기기가 나온다. 받아들인 곳은 고친 말로, 넘긴 곳은 원문으로 보인다
import { useState } from 'react'
import Seal from '../../components/Seal'
import { KIND_LABEL, pendingCount, segmentsOf, spacingMark, type Correction, type Decision } from './redPenRules'
import './redpen.css'

interface Props {
  base: string
  corrections: Correction[]
  decisions: Decision[]
  remaining: number
  onDecide: (index: number, decision: Decision) => void
  onAcceptAll: () => void
  onClose: () => void
}

export default function RedPenSheet({ base, corrections, decisions, remaining, onDecide, onAcceptAll, onClose }: Props) {
  const [selected, setSelected] = useState<number | null>(() => (corrections.length > 0 ? 0 : null))
  const left = pendingCount(decisions)
  const current = selected === null ? null : corrections[selected]

  function decide(index: number, decision: Decision) {
    onDecide(index, decision)
    // 다음으로 아직 정하지 않은 곳을 고른다
    const next = decisions.findIndex((d, i) => i > index && d === 'pending')
    const first = decisions.findIndex((d, i) => i !== index && d === 'pending')
    setSelected(next >= 0 ? next : first >= 0 ? first : null)
  }

  return (
    <section className="redpen" aria-label="빨간 펜 교정지">
      <header className="redpen-head">
        <span className="redpen-title">
          <span className="redpen-pen" aria-hidden="true">
            ✎
          </span>
          빨간 펜
        </span>
        {corrections.length > 0 && left === 0 && <Seal text="퇴고" small stampOnView label="퇴고 완료" />}
        <span className="redpen-count" role="status" aria-live="polite">
          {corrections.length === 0 ? '고칠 곳이 없습니다' : left > 0 ? `고칠 곳 ${left}개 남음` : '모두 정했습니다'}
          <span className="hint"> · 오늘 {remaining}번 남음</span>
        </span>
        <span className="redpen-head-actions">
          {left > 0 && (
            <button type="button" className="btn btn-outline btn-small" onClick={onAcceptAll}>
              모두 받아들이기
            </button>
          )}
          <button type="button" className="btn btn-quiet btn-small" onClick={onClose}>
            닫기
          </button>
        </span>
      </header>

      <article className="redpen-paper">
        <p className="redpen-text">
          {segmentsOf(base, corrections, decisions).map((segment, i) => {
            if (segment.type === 'text') return <span key={i}>{segment.text}</span>
            const { correction: c, decision, index } = segment
            if (decision === 'accepted') {
              return (
                <span key={i} className="redpen-done" title="받아들임">
                  {c.replacement}
                </span>
              )
            }
            if (decision === 'skipped') return <span key={i}>{c.original}</span>
            return (
              <button
                key={i}
                type="button"
                className={`redpen-mark is-${c.kind}${selected === index ? ' is-selected' : ''}`}
                aria-label={`${KIND_LABEL[c.kind]} 교정: ${c.original} → ${c.replacement}`}
                aria-pressed={selected === index}
                onClick={() => setSelected(index)}
              >
                <span className="redpen-ins" aria-hidden="true">
                  {c.kind === 'spacing' && <span className="redpen-caret">{spacingMark(c)}</span>}
                  {c.replacement}
                </span>
                <span className="redpen-orig">{c.original}</span>
              </button>
            )
          })}
        </p>
      </article>

      {current && decisions[selected!] === 'pending' && (
        <div className="redpen-note" role="region" aria-label="고칠 곳">
          <p className="redpen-note-line">
            <span className={`redpen-kind is-${current.kind}`}>{KIND_LABEL[current.kind]}</span>
            <del>{current.original}</del>
            <span aria-hidden="true">→</span>
            <ins>{current.replacement}</ins>
          </p>
          {current.reason && <p className="redpen-reason">{current.reason}</p>}
          <p className="redpen-note-actions">
            <button type="button" className="btn btn-primary btn-small" onClick={() => decide(selected!, 'accepted')}>
              받아들이기
            </button>
            <button type="button" className="btn btn-quiet btn-small" onClick={() => decide(selected!, 'skipped')}>
              넘기기
            </button>
          </p>
        </div>
      )}
    </section>
  )
}
