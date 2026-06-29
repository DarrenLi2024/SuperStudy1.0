import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

// 生产环境使用环境变量 VITE_API_BASE_URL，开发环境使用 vite proxy
const API_BASE_URL = import.meta.env.PROD
  ? (import.meta.env.VITE_API_BASE_URL || 'https://backend-production-a907.up.railway.app')
  : ''

const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // API 返回业务错误（如 401），仅 401 需要跳转
      if (res.code === 401) {
        clearAuth()
        router.push('/login')
      }
      // 统一错误提示（组件内 catch 若已处理需设置 silent 避免重复提示）
      if (!response.config._silent) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.warn('[API Error]', error.message)
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        clearAuth()
        router.push('/login')
      }
    }
    if (!error.config?._silent) {
      ElMessage.error(error.response?.data?.message || error.message || '网络请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
