// 이미지 규칙과 문구 (specs/005 contracts 10, FR-022 ~ FR-025). 숫자는 서버 설정(community.image.*)과 같다.
// 화면 검사는 보조다. 형식은 서버가 파일 앞부분으로 다시 본다

export const IMAGE_MAX_BYTES = 5 * 1024 * 1024
export const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp'

export const imageMessages = {
  invalid: '이미지는 5MB 이하의 jpg, png, gif, webp만 올릴 수 있습니다',
  failed: '※ 잠시 뒤 다시 시도해 주세요',
} as const
