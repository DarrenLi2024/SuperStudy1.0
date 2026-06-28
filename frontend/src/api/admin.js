import request from '@/utils/request'

/**
 * 获取用户列表
 */
export function getUserList(params) {
  return request({
    url: '/api/v1/admin/users',
    method: 'get',
    params
  })
}

/**
 * 创建用户
 */
export function createUser(data) {
  return request({
    url: '/api/v1/admin/users',
    method: 'post',
    data
  })
}

/**
 * 更新用户状态
 */
export function updateUserStatus(userId, data) {
  return request({
    url: `/api/v1/admin/users/${userId}/status`,
    method: 'put',
    data
  })
}

/**
 * 重置用户密码
 */
export function resetPassword(userId) {
  return request({
    url: `/api/v1/admin/users/${userId}/reset-password`,
    method: 'post'
  })
}

/**
 * 获取系统监控信息
 */
export function getMonitor() {
  return request({
    url: '/api/v1/admin/monitor',
    method: 'get'
  })
}

/**
 * 获取AI参数配置
 */
export function getAiConfig() {
  return request({
    url: '/api/v1/admin/ai/config',
    method: 'get'
  })
}

/**
 * 更新AI参数配置
 */
export function updateAiConfig(data) {
  return request({
    url: '/api/v1/admin/ai/config',
    method: 'put',
    data
  })
}
