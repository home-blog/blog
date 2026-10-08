// 이미지 올리기 버튼 (specs/005 US4, T059): 파일을 고르면 한 장씩 올리고, 받은 주소를 본문에 ![](주소)로 넣는다 (D-4)
// - 크기는 화면에서도 먼저 본다(보조). 실패 문구는 서버 것 그대로 보여 준다
// - 수정 중이면 글 번호를 같이 보낸다 (그 글의 10장 세기)
import { useId, useRef, useState } from 'react'
import { ApiError } from '../api/client'
import { uploadImage } from './imageApi'
import { IMAGE_ACCEPT, IMAGE_MAX_BYTES, imageMessages } from './rules'

interface Props {
  postId: number | null
  disabled?: boolean
  /** 올린 이미지의 주소를 받는다. 본문에 넣는 것은 부르는 쪽이 한다 */
  onUploaded: (url: string) => void
}

export default function ImageUploadButton({ postId, disabled, onUploaded }: Props) {
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function upload(files: FileList | null) {
    if (!files || files.length === 0) return
    setBusy(true)
    setError(null)
    try {
      // 여러 장을 골라도 한 장씩 차례로 보낸다. 실패하면 거기서 멈추고 이유를 보여 준다
      for (const file of Array.from(files)) {
        if (file.size > IMAGE_MAX_BYTES) {
          setError(imageMessages.invalid)
          return
        }
        const uploaded = await uploadImage(file, postId)
        onUploaded(uploaded.url)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : imageMessages.failed)
    } finally {
      setBusy(false)
      if (input.current) input.current.value = ''
    }
  }

  return (
    <span className="image-upload">
      <input
        ref={input}
        id={id}
        type="file"
        accept={IMAGE_ACCEPT}
        multiple
        className="visually-hidden"
        disabled={disabled || busy}
        onChange={(e) => void upload(e.target.files)}
      />
      <label htmlFor={id} className={`btn btn-outline btn-small${disabled || busy ? ' is-disabled' : ''}`}>
        {busy ? '올리는 중' : '이미지 올리기'}
      </label>
      {error && (
        <span className="msg msg-error" role="alert">
          {error}
        </span>
      )}
    </span>
  )
}
