<template>
  <div class="ai-loading" :class="{ 'streaming': streaming }">
    <div class="ai-dots" v-if="!streaming">
      <span class="dot" v-for="i in 3" :key="i"></span>
    </div>
    <div class="ai-status" v-if="!streaming">
      <span class="ai-loading-text">{{ text }}</span>
    </div>
    <div class="ai-stream-container" v-if="streaming && streamText">
      <div class="ai-stream-header">
        <span class="ai-stream-badge">AI 实时生成</span>
        <span class="ai-stream-timer">{{ elapsed }}s</span>
      </div>
      <div class="ai-stream-content" ref="streamContent">
        <span class="ai-stream-cursor" v-if="!streamFinished">▌</span>
        {{ streamText }}
      </div>
      <div class="ai-stream-footer" v-if="streamFinished">
        <span class="ai-stream-done">生成完成</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  text?: string
  streaming?: boolean
  streamText?: string
  streamFinished?: boolean
}>(), {
  text: 'AI 正在思考中...',
  streaming: false,
  streamText: '',
  streamFinished: false
})

const emit = defineEmits<{
  (e: 'cancel'): void
}>()

const streamContent = ref<HTMLElement | null>(null)
const elapsed = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

watch(() => props.streaming, (val) => {
  if (val) {
    elapsed.value = 0
    timer = setInterval(() => { elapsed.value++ }, 1000)
  } else {
    if (timer) { clearInterval(timer); timer = null }
  }
})

watch(() => props.streamFinished, (val) => {
  if (val && timer) {
    clearInterval(timer)
    timer = null
  }
})

onUnmounted(() => {
  if (timer) { clearInterval(timer) }
})
</script>

<style scoped lang="scss">
.ai-loading {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f5f7ff 0%, #f0f2ff 100%);
  border-radius: 12px;
  border: 1px solid #e8ecff;
  transition: all 0.3s ease;

  &.streaming {
    background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
    border-color: #c4ccf0;
    min-height: 80px;
  }
}

.ai-dots {
  display: flex;
  gap: 6px;
  align-items: center;
  padding-top: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  animation: dot-pulse 1.4s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes dot-pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.ai-status {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ai-loading-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

/* 流式输出 */
.ai-stream-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-stream-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-stream-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-weight: 600;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.ai-stream-timer {
  font-size: 11px;
  color: #909399;
}

.ai-stream-content {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-stream-cursor {
  display: inline;
  color: #667eea;
  animation: blink 1s step-end infinite;
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.ai-stream-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-stream-done {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
</style>
