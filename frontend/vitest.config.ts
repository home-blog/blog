// 화면 테스트 설정 (docs/3-설계/테스트-전략.md). 실행: npm test, 커버리지: npm run test:coverage
// vite.config.ts의 설정(React 플러그인, __APP_VERSION__)을 그대로 이어받는다
import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config.ts'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      include: ['src/**/*.test.{ts,tsx}'],
      coverage: {
        provider: 'v8',
        // lcov는 SonarQube가 읽는다 (sonar.javascript.lcov.reportPaths)
        reporter: ['text-summary', 'lcov'],
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/main.tsx', 'src/**/*.d.ts'],
      },
    },
  }),
)
