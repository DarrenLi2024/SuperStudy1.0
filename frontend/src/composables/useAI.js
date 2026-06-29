import { ref, readonly } from 'vue'
import { aiStreamRequest, simulateStream } from '@/utils/ai-stream'
import request from '@/utils/request'

/**
 * AI 功能组合式函数
 * 提供统一的AI请求管理、流式输出、状态追踪
 *
 * @example
 * const { generate, streamGenerate, loading, streaming, streamText } = useAI()
 * // 同步生成
 * const result = await generate('/api/v1/ai/generate', { prompt: '...' })
 * // 流式生成
 * streamGenerate('/api/v1/ai/stream', { prompt: '...' })
 */
export function useAI() {
  const loading = ref(false)
  const streaming = ref(false)
  const streamText = ref('')
  const streamFinished = ref(false)
  const error = ref(null)
  const elapsedMs = ref(0)

  let abortController = null
  let streamTimer = null

  /**
   * 同步AI生成请求
   * @param {string} url - API端点
   * @param {object} params - 请求参数
   * @returns {Promise<any>} 响应数据
   */
  async function generate(url, params = {}) {
    loading.value = true
    error.value = null
    const startTime = Date.now()

    try {
      const res = await request({
        url,
        method: 'post',
        data: params
      })
      elapsedMs.value = Date.now() - startTime
      return res.data
    } catch (err) {
      error.value = err.message || 'AI请求失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 流式AI生成请求
   * @param {string} url - SSE端点
   * @param {object} params - 请求参数
   */
  function streamGenerate(url, params = {}) {
    // 重置状态
    streaming.value = true
    streamText.value = ''
    streamFinished.value = false
    error.value = null
    const startTime = Date.now()

    if (streamTimer) clearInterval(streamTimer)
    streamTimer = setInterval(() => {
      elapsedMs.value = Date.now() - startTime
    }, 100)

    // 尝试真实SSE流
    abortController = aiStreamRequest(url, params, {
      onToken(token) {
        streamText.value += token
      },
      onDone() {
        streaming.value = false
        streamFinished.value = true
        elapsedMs.value = Date.now() - startTime
        cleanup()
      },
      onError(err) {
        // SSE失败时降级：使用同步请求 + 前端模拟流式
        fallbackStream(url, params, startTime)
      }
    })
  }

  /**
   * 降级流式：同步获取完整内容，前端模拟逐字输出
   */
  async function fallbackStream(url, params, startTime) {
    try {
      const res = await request({
        url: url.replace('/stream', '').replace('/ai/v1/', '/api/v1/'),
        method: 'post',
        data: params
      })

      const content = res.data?.content || res.data?.data?.content || JSON.stringify(res.data)
      simulateStream(content, {
        onToken(token) {
          streamText.value += token
        },
        onDone() {
          streaming.value = false
          streamFinished.value = true
          elapsedMs.value = Date.now() - startTime
          cleanup()
        },
        speed: 25
      })
    } catch (err) {
      streaming.value = false
      error.value = err.message || 'AI请求失败'
      cleanup()
    }
  }

  /**
   * 取消流式请求
   */
  function cancelStream() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    streaming.value = false
    streamFinished.value = true
    cleanup()
  }

  /**
   * 重置所有状态
   */
  function reset() {
    cancelStream()
    loading.value = false
    streaming.value = false
    streamText.value = ''
    streamFinished.value = false
    error.value = null
    elapsedMs.value = 0
  }

  function cleanup() {
    if (streamTimer) {
      clearInterval(streamTimer)
      streamTimer = null
    }
    abortController = null
  }

  return {
    // 方法
    generate,
    streamGenerate,
    cancelStream,
    reset,
    // 状态（只读）
    loading: readonly(loading),
    streaming: readonly(streaming),
    streamText: readonly(streamText),
    streamFinished: readonly(streamFinished),
    error: readonly(error),
    elapsedMs: readonly(elapsedMs)
  }
}
