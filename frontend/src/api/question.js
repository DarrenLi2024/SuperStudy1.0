import request from '@/utils/request'

/**
 * 获取专项训练题目
 */
export function getTrainingQuestions(studentId, subject = '数学', count = 5) {
  return request({
    url: `/api/v1/question/training/${studentId}`,
    method: 'get',
    params: { subject, count }
  })
}

/**
 * 获取补强训练题目
 */
export function getReinforcementQuestions(studentId, knowledgePoint = '函数与导数', count = 5) {
  return request({
    url: `/api/v1/question/reinforcement/${studentId}`,
    method: 'get',
    params: { knowledgePoint, count }
  })
}
