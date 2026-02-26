import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [vue()],
  server: {
    host: true,
    port: 5179,
    proxy: {
      '/api': {
        target: 'http://localhost:8002',
        changeOrigin: true,
        secure: false
      },
      '/chat': {
        target: 'http://localhost:8002/api',
        changeOrigin: true,
        secure: false,
        ws: true
      }
    }
  },
  preview: {
    host: true,
    port: 4173
  }
})
