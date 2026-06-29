import { getToken } from './auth'

/**
 * AI 流式请求（SSE）
 * 通过 EventSource 或 fetch 流式读取后端 SSE 响应
 *
 * @param {string} url - 请求URL
 * @param {object} body - 请求体
 * @param {function} onToken - 每收到一个token的回调
 * @param {function} onDone - 完成的回调
 * @param {function} onError - 错误的回调
 * @returns {AbortController} 可用于取消请求
 */
export function aiStreamRequest(url, body, { onToken, onDone, onError } = {}) {
  const controller = new AbortController()
  const token = getToken()

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify(body),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const data = line.substring(6).trim()
          if (data === '[DONE]') {
            if (onDone) onDone()
            return
          }
          try {
            const parsed = JSON.parse(data)
            const content = parsed?.choices?.[0]?.delta?.content
            if (content && onToken) {
              onToken(content)
            }
          } catch (e) {
            // 跳过非JSON行
          }
        }
      }
    }
    if (onDone) onDone()
  }).catch(err => {
    if (err.name === 'AbortError') return
    if (onError) onError(err)
    console.error('[AI Stream Error]', err)
  })

  return controller
}

/**
 * AI 模拟流式输出（用于本地降级模式的前端模拟）
 * 将完整文本逐字输出，模拟流式效果
 *
 * @param {string} fullText - 完整文本
 * @param {function} onToken - 每字回调
 * @param {function} onDone - 完成回调
 * @param {number} speed - 每字间隔（ms）
 */
export function simulateStream(fullText, { onToken, onDone, speed = 30 } = {}) {
  if (!fullText) {
    if (onDone) onDone()
    return
  }

  let index = 0
  const chars = [...fullText] // 正确处理emoji等宽字符

  const interval = setInterval(() => {
    if (index < chars.length) {
      if (onToken) onToken(chars[index])
      index++
    } else {
      clearInterval(interval)
      if (onDone) onDone()
    }
  }, speed)
}
