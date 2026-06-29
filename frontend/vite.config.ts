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
          // 本地开发时默认指向 Railway 生产后端
          // 可通过 VITE_API_URL 环境变量覆盖
          target: env.VITE_API_URL || 'https://backend-production-a907.up.railway.app',
          changeOrigin: true
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-ui': ['element-plus', 'vant'],
            'vendor-echarts': ['echarts', 'vue-echarts']
          }
        }
      },
      chunkSizeWarningLimit: 500
    }
  }
})
