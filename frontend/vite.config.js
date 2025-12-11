import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://192.168.184.192:8081',
        changeOrigin: true,
      }
    }
  },
  preview: {
    host: true,
    port: 4173
  }
})
