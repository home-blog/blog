import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 중에는 /api 요청을 Spring Boot 서버(8080)로 넘긴다.
// 화면과 서버를 같은 주소처럼 써서 세션 쿠키와 CORS 문제가 생기지 않게 한다. (specs/001 plan, 가안)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
})
