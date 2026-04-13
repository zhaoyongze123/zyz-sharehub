import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from '@unocss/vite'
import { fileURLToPath, URL } from 'node:url'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:18080'

export default defineConfig({
  plugins: [vue(), UnoCSS()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true
      },
      '/oauth2': {
        target: apiProxyTarget,
        changeOrigin: true
      },
      '/login/oauth2': {
        target: apiProxyTarget,
        changeOrigin: true
      },
      '/actuator': {
        target: apiProxyTarget,
        changeOrigin: true
      }
    }
  }
})
