import { readFileSync } from 'node:fs'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 화면 아래쪽에 보이는 릴리스 번호 (package.json의 version, 서버 pom.xml과 같은 번호)
const { version } = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8')) as { version: string }

// 개발 중에는 /api 요청을 Spring Boot 서버(8080)로 넘긴다.
// 화면과 서버를 같은 주소처럼 써서 세션 쿠키와 CORS 문제가 생기지 않게 한다. (specs/001 plan, 가안)
export default defineConfig({
  plugins: [react()],
  define: { __APP_VERSION__: JSON.stringify(version) },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: false },
      // 로봇이 읽는 주소 (specs/007): RSS, 사이트 지도, 검색엔진 안내, 링크 미리보기 카드(/og/…, 브라우저로 열어 확인)
      '^/(rss\\.xml|sitemap\\.xml|robots\\.txt|blog/\\d+/rss\\.xml)$': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
      '^/og(/|$)': { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
})
