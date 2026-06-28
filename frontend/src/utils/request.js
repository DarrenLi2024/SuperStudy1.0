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
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        clearAuth()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.error('Response error:', error)
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        clearAuth()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('权限不足')
      } else if (status === 404) {
        ElMessage.error('请求地址不存在')
      } else if (status === 500) {
        ElMessage.error('服务器错误')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
