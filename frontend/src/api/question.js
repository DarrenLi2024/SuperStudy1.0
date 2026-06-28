import request from '@/utils/request'

/**
 * 获取专项训练题目
 */
export function getTrainingQuestions(studentId) {
  return request({
    url: `/api/v1/question/training/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取补强训练题目
 */
export function getReinforcementQuestions(studentId) {
  return request({
    url: `/api/v1/question/reinforcement/${studentId}`,
    method: 'get'
  })
}
