import request from '@/utils/request'

/**
 * 获取三段式院校卡片数据
 */
export function getCollegeCards(studentId) {
  return request({
    url: `/api/v1/growth/cards/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取段位进度条数据
 */
export function getGrowthProgress(studentId) {
  return request({
    url: `/api/v1/growth/progress/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取段位升级历史记录
 */
export function getGrowthHistory(studentId) {
  return request({
    url: `/api/v1/growth/history/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取成长数据
 */
export function getGrowthData(studentId) {
  return request({
    url: `/api/v1/growth/data/${studentId}`,
    method: 'get'
  })
}
