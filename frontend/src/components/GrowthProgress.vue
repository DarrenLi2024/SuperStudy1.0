<template>
  <div class="growth-progress">
    <div class="progress-header">
      <span class="progress-title">段位成长进度</span>
      <span class="progress-total">{{ progressData.totalProgress?.toFixed(1) || 0 }}%</span>
    </div>

    <div class="progress-phases">
      <div
        v-for="(phase, index) in progressData.phases"
        :key="phase.name"
        class="phase-item"
        :class="{ completed: phase.completed }"
      >
        <div class="phase-indicator">
          <div class="phase-dot" :class="{ active: phase.currentScore >= phase.startScore }">
            <span class="dot-icon" v-if="phase.completed">✓</span>
          </div>
          <div class="phase-connector" v-if="index < progressData.phases.length - 1"
               :class="{ filled: phase.completed }"></div>
        </div>
        <div class="phase-content">
          <div class="phase-name">{{ phase.name }}</div>
          <div class="phase-scores">{{ phase.startScore }} - {{ phase.endScore }}分</div>
          <div class="phase-bar">
            <div class="bar-bg">
              <div class="bar-fill" :style="{ width: Math.min(phase.progress || 0, 100) + '%' }"></div>
            </div>
            <span class="bar-label">{{ phase.progress?.toFixed(1) || 0 }}%</span>
          </div>
        </div>
      </div>
    </div>

    <div class="rank-summary" v-if="progressData.targetScore">
      <span class="rank-label">目标分数：</span>
      <span class="rank-value">{{ progressData.targetScore }}分</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { GrowthProgressData } from '@/types/growth'

defineProps<{
  progressData: GrowthProgressData
}>()
</script>

<style scoped lang="scss">
.growth-progress {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.progress-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.progress-total {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
}

.progress-phases {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.phase-item {
  display: flex;
  gap: 12px;
  position: relative;
  padding-bottom: 20px;

  &.completed .phase-dot {
    background: linear-gradient(135deg, #67c23a, #409eff);
  }
}

.phase-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24px;
  flex-shrink: 0;
}

.phase-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e8eaed;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  transition: all 0.3s;

  &.active {
    background: linear-gradient(135deg, #667eea, #764ba2);
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  }

  .dot-icon {
    color: #fff;
    font-size: 11px;
    font-weight: bold;
  }
}

.phase-connector {
  width: 2px;
  flex: 1;
  background: #e8eaed;
  margin: 2px 0;

  &.filled {
    background: linear-gradient(180deg, #667eea, #764ba2);
  }
}

.phase-content {
  flex: 1;
  min-width: 0;
}

.phase-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.phase-scores {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.phase-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-bg {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: #f0f0f0;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.8s ease;
}

.bar-label {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  width: 42px;
  text-align: right;
}

.rank-summary {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
  .rank-label { font-size: 13px; color: #909399; }
  .rank-value { font-size: 16px; font-weight: 600; color: #409eff; }
}
</style>
