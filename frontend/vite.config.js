import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development, /api is proxied to Spring Boot so the browser sees ONE origin:
// the HttpOnly session cookie then works without any cross-site cookie settings.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: process.env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080', changeOrigin: false } },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.js',
    css: false,
  },
})
