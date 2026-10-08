// 이미지 올리기 (specs/005 contracts 10). 한 장씩 multipart/form-data로 보낸다
import { api } from '../api/client'

export interface UploadedImage {
  imageId: number
  /** 본문에 넣을 우리 서버 주소 (/api/images/…) */
  url: string
}

/** postId는 수정 중인 글의 번호. 새 글이면 보내지 않는다 */
export function uploadImage(file: File, postId: number | null): Promise<UploadedImage> {
  const form = new FormData()
  form.append('file', file)
  if (postId !== null) form.append('postId', String(postId))
  return api<UploadedImage>('/api/images', { method: 'POST', body: form })
}
