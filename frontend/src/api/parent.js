import request from '@/utils/request'

/**
 * 获取孩子学习概况
 */
export function getChildOverview(studentId) {
  return request({
    url: `/api/v1/parent/overview/${studentId}`,
    method: 'get'
  })
}
