// 소셜 로그인이 주소로 알려 주는 오류의 문구 (specs/010, specs/022 S2-2)
import { describe, expect, it } from 'vitest'
import { socialErrorMessage } from './socialApi'

describe('socialErrorMessage', () => {
  it('정지된 회원이면 정지 안내를, 모르는 코드면 일반 실패 문구를 보인다', () => {
    expect(socialErrorMessage('ACCOUNT_SUSPENDED')).toContain('이용이 정지된 계정입니다')
    expect(socialErrorMessage('SOMETHING_ELSE')).toContain('소셜 로그인을 마치지 못했습니다')
    expect(socialErrorMessage(null)).toBeNull()
  })
})
