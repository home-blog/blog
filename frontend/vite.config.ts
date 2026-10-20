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
      // 소셜 로그인 (specs/010): 회사 화면으로 보내기와 돌아오는 주소. Host를 그대로 넘겨 서버가 콜백 주소를
      // http://localhost:5173/login/oauth2/code/{회사}로 만든다 (회사 앱에 등록한 주소). /login 자체는 화면 주소라 넘기지 않는다
      '/oauth2/': { target: 'http://localhost:8080', changeOrigin: false },
      '/login/oauth2/': { target: 'http://localhost:8080', changeOrigin: false },
      // Claude 앱 커스텀 커넥터 (specs/023): OAuth 메타데이터와 등록·인가·토큰·폐기. 위임장(/connect/authorize)은 화면 주소
      '^/\\.well-known/oauth-': { target: 'http://localhost:8080', changeOrigin: false },
      '/oauth/': { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
})
