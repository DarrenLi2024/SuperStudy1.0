import request from '@/utils/request'

/**
 * 获取学生档案
 */
export function getStudentProfile(studentId) {
  return request({
    url: `/api/v1/student/profile/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取当前登录学生的档案
 */
export function getMyProfile() {
  return request({
    url: '/api/v1/student/profile/me',
    method: 'get'
  })
}

/**
 * 创建学生档案
 */
export function createStudentProfile(data) {
  return request({
    url: '/api/v1/student/profile',
    method: 'post',
    data
  })
}

/**
 * 更新学生档案
 */
export function updateStudentProfile(studentId, data) {
  return request({
    url: `/api/v1/student/profile/${studentId}`,
    method: 'put',
    data
  })
}
