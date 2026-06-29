import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 5173,
      host: '0.0.0.0',
      open: false,
      proxy: {
        '/api': {
          // 本地开发时通过环境变量 VITE_API_URL 指定后端地址
          target: env.VITE_API_URL || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router'],
            'vendor-ui': ['element-plus'],
            'vendor-echarts': ['echarts']
          }
        }
      },
      chunkSizeWarningLimit: 500
    }
  }
})
