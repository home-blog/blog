// 임시 저장·예약 표시와 검사 (specs/013 US3, US4). 시각은 한국 시간으로 보여 주고, 최종 판단은 서버가 한다
// 숫자는 서버 application.yml의 writing.schedule.* 기본값과 같다 (min-lead 1분)

const KST_OFFSET_MS = 9 * 60 * 60 * 1000
const MINUTE_MS = 60_000
const DAY_MS = 86_400_000

const TIME = new Intl.DateTimeFormat('ko-KR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone: 'Asia/Seoul' })
const DAY_TIME = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: 'Asia/Seoul',
})

function hhmm(date: Date): string {
  const parts = TIME.formatToParts(date)
  const hour = parts.find((p) => p.type === 'hour')?.value ?? '00'
  const minute = parts.find((p) => p.type === 'minute')?.value ?? '00'
  return `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`
}

/** "임시 저장됨 14:32" */
export function savedAtLabel(iso: string): string {
  return `임시 저장됨 ${hhmm(new Date(iso))}`
}

/** "예약 10월 20일 09:00" */
export function publishAtLabel(iso: string): string {
  const date = new Date(iso)
  const parts = DAY_TIME.formatToParts(date)
  const month = parts.find((p) => p.type === 'month')?.value ?? ''
  const day = parts.find((p) => p.type === 'day')?.value ?? ''
  return `예약 ${month} ${day}일 ${hhmm(date)}`
}

/** datetime-local 칸의 값(한국 시간 "2026-10-20T09:00")을 세계 시간 ISO로. 형식이 틀리면 null */
export function localInputToIso(value: string): string | null {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) return null
  const time = Date.parse(`${value}:00+09:00`)
  return Number.isNaN(time) ? null : new Date(time).toISOString()
}

/** 세계 시간 ISO를 datetime-local 칸의 값(한국 시간)으로 */
export function isoToLocalInput(iso: string): string {
  return new Date(Date.parse(iso) + KST_OFFSET_MS).toISOString().slice(0, 16)
}

/** 예약 시각 검사 (S4-1): 초 아래를 버린 뒤 지금부터 1분 뒤 ~ maxDays일 안. 문제가 없으면 null */
export function scheduleError(iso: string | null, now: Date, maxDays: number): string | null {
  if (!iso) return '※ 예약 시각을 골라 주세요'
  const time = Math.floor(Date.parse(iso) / MINUTE_MS) * MINUTE_MS
  if (Number.isNaN(time)) return '※ 예약 시각을 골라 주세요'
  if (time < now.getTime() + MINUTE_MS) return '※ 예약 시각은 지금부터 1분 뒤부터 고를 수 있습니다'
  if (time > now.getTime() + maxDays * DAY_MS) return `※ 예약 시각은 지금부터 ${maxDays}일 안으로 골라 주세요`
  return null
}
