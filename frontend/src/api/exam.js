import request from '@/utils/request'

/**
 * 提交考试成绩
 */
export function submitExam(data) {
  return request({
    url: '/api/v1/exam/submit',
    method: 'post',
    data
  })
}

/**
 * 获取考试记录列表
 */
export function getExamRecords(studentId) {
  return request({
    url: `/api/v1/exam/records/${studentId}`,
    method: 'get'
  })
}

/**
 * 获取考试详情
 */
export function getExamDetail(examId) {
  return request({
    url: `/api/v1/exam/detail/${examId}`,
    method: 'get'
  })
}
