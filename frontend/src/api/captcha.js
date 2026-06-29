import request from '@/utils/request'

/**
 * 获取图形验证码
 */
export function getCaptcha() {
  return request({
    url: '/api/v1/captcha/generate',
    method: 'get'
  })
}
