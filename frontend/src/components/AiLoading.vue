<template>
  <div class="ai-loading">
    <div class="ai-dots">
      <span class="dot" v-for="i in 3" :key="i"></span>
    </div>
    <span class="ai-loading-text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  text?: string
}>(), {
  text: 'AI 正在思考中...'
})
</script>

<style scoped lang="scss">
.ai-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
}

.ai-dots {
  display: flex;
  gap: 6px;
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

.ai-loading-text {
  font-size: 13px;
  color: #909399;
}
</style>
