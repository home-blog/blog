// 화면에서 먼저 보여 주는 입력 규칙 (specs/001 FR-003, FR-005 ~ FR-008).
// 최종 판단은 서버가 한다(FR-034). 숫자는 서버 application.yml의 auth.* 기본값과 같게 둔다.

export const NICKNAME_MIN = 2
export const NICKNAME_MAX = 10
export const PASSWORD_MIN = 8
export const PASSWORD_MAX = 20
export const PASSWORD_SPECIALS = '!@#$%^&*()_+-='
/** 소개 최대 글자 수 (서버 account.intro.max-length, specs/002 FR-008) */
export const INTRO_MAX = 100

const EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const NICKNAME = new RegExp(`^[가-힣A-Za-z0-9]{${NICKNAME_MIN},${NICKNAME_MAX}}$`)

export const messages = {
  email: '이메일 형식이 올바르지 않습니다',
  nickname: `닉네임은 한글, 영문, 숫자로 ${NICKNAME_MIN}~${NICKNAME_MAX}자여야 합니다`,
  password: `비밀번호는 영문, 숫자, 특수문자를 포함해 ${PASSWORD_MIN}~${PASSWORD_MAX}자로 입력해 주세요`,
  passwordConfirm: '비밀번호가 일치하지 않습니다',
  verifyFirst: '이메일 인증을 먼저 완료해 주세요',
  verificationExpired: '인증 유효 시간이 지났습니다. 이메일 인증을 다시 해 주세요',
  codeExpired: '인증번호가 만료되었습니다. 인증번호를 다시 받아 주세요',
  // 계정 관리 (specs/002 contracts 2 ~ 4)
  intro: `※ 소개는 ${INTRO_MAX}자 이하로 입력해 주세요`,
  currentPasswordRequired: '※ 현재 비밀번호를 입력해 주세요',
  passwordRequired: '※ 비밀번호를 입력해 주세요',
  withdrawalNotAgreed: '※ 탈퇴 안내를 확인해 주세요',
  unsavedChanges: '저장하지 않은 내용이 있습니다. 나갈까요?',
} as const

/** 글자 수: 이모지 하나를 한 글자로 센다 (서버와 같은 기준, specs/002 research B-4) */
export function countChars(value: string): number {
  return [...value].length
}

export function isValidEmail(value: string): boolean {
  const v = value.trim()
  return v.length <= 255 && EMAIL.test(v)
}

export function isValidNickname(value: string): boolean {
  return NICKNAME.test(value.trim())
}

export interface PasswordCheck {
  label: string
  met: boolean
}

/** 비밀번호를 입력하는 동안 칸 아래에 보여 줄 규칙별 충족 여부 (FR-008). */
export function checkPassword(value: string): PasswordCheck[] {
  const chars = [...value]
  const allowed = (c: string) => /[A-Za-z0-9]/.test(c) || PASSWORD_SPECIALS.includes(c)
  return [
    { label: `${PASSWORD_MIN}~${PASSWORD_MAX}자`, met: chars.length >= PASSWORD_MIN && chars.length <= PASSWORD_MAX },
    { label: '영문', met: /[A-Za-z]/.test(value) },
    { label: '숫자', met: /\d/.test(value) },
    { label: `특수문자 (${PASSWORD_SPECIALS.split('').join(' ')})`, met: chars.some((c) => PASSWORD_SPECIALS.includes(c)) },
    { label: '공백이나 다른 기호 없이', met: value.length > 0 && chars.every(allowed) },
  ]
}

export function isValidPassword(value: string): boolean {
  return checkPassword(value).every((c) => c.met)
}
