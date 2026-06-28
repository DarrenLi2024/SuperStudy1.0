import request from '@/utils/request'

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/api/v1/user/login',
    method: 'post',
    data
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/api/v1/user/info',
    method: 'get'
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/api/v1/user/logout',
    method: 'post'
  })
}
