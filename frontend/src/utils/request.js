import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

const service = axios.create({
  baseURL: '',
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
    // 不弹错误提示 — 各页面有 Mock 降级
    return Promise.reject(error)
  }
)

export default service
