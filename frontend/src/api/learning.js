import request from '@/utils/request'

/**
 * 获取今日学习任务
 */
export function getTodayTasks(studentId) {
  return request({
    url: `/api/v1/learning/today/${studentId}`,
    method: 'get'
  })
}

/**
 * 完成任务
 */
export function completeTask(taskId, completionRate) {
  return request({
    url: '/api/v1/learning/complete',
    method: 'post',
    params: { taskId, completionRate }
  })
}

/**
 * 获取错题列表
 */
export function getErrorQuestions(studentId) {
  return request({
    url: `/api/v1/learning/errors/${studentId}`,
    method: 'get'
  })
}

/**
 * 记录错题
 */
export function recordError(data) {
  return request({
    url: '/api/v1/learning/errors',
    method: 'post',
    data
  })
}

/**
 * 获取知识点掌握情况
 */
export function getKnowledgeStatus(studentId) {
  return request({
    url: `/api/v1/learning/knowledge/${studentId}`,
    method: 'get'
  })
}
