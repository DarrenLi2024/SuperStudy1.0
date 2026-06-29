import request from '@/utils/request'

/**
 * AI 服务状态检查
 * 返回当前 LLM 是否可用、使用什么 provider/model
 */
export function getAiStatus() {
  return request({
    url: '/api/v1/ai/status',
    method: 'get'
  })
}

/**
 * AI 同步生成（通用端点）
 * @param {object} params - { taskType, systemPrompt, userPrompt, temperature, maxTokens, variables, responseSchema }
 */
export function aiGenerate(params) {
  return request({
    url: '/api/v1/ai/generate',
    method: 'post',
    data: params
  })
}

/**
 * AI 流式生成（SSE）
 * 注意：此函数返回 fetch Response 对象，由 useAI().streamGenerate() 处理
 */
export function aiStreamUrl() {
  return '/api/v1/ai/stream'
}
