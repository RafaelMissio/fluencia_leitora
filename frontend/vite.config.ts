/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Encaminha /api para o backend Spring Boot em dev, evitando CORS
      // sem configurar CorsConfigurationSource (context.md, "CORS / integração dev").
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    exclude: ['node_modules/**', 'e2e/**'],
    // T1 scaffold has no tests yet; later tasks add them. Without this,
    // `npm run test` exits non-zero on an empty suite (see tasks.md T1 Done when).
    passWithNoTests: true,
  },
})
