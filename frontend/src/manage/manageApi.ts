// 블로그 관리 요청 (specs/006 contracts/manage-api.md). 내 블로그는 번호를 보내지 않는다: 서버가 세션으로 정한다
// 분류 관리·블로그 설정은 003의 주소(blog/blogApi.ts)를 그대로 쓴다
import { api } from '../api/client'

/** 관리 화면 머리 정보 (contracts 1) */
export interface ManageHeader {
  blogId: number
  name: string
  /** 비어 있으면 '' */
  intro: string
  /** "내 블로그 보기"가 갈 화면 주소 */
  blogPath: string
}

export function getManageHeader(signal?: AbortSignal): Promise<ManageHeader> {
  return api<ManageHeader>('/api/manage/blog', { signal })
}
