<template>
  <div class="college-card" :class="[type, { 'dream-card': type === 'dream' }]">
    <div class="card-header">
      <span class="card-title">{{ title }}</span>
    </div>
    <div class="card-body">
      <!-- 院校列表 -->
      <div v-if="colleges && colleges.length > 0" class="college-list">
        <div v-for="college in colleges" :key="college.id" class="college-item">
          <div class="college-logo">
            <img :src="college.logo || '/logos/default.png'" :alt="college.name" @error="handleImgError" />
          </div>
          <div class="college-name">{{ college.name }}</div>
          <div class="college-batch-tag">{{ college.batch }}</div>
        </div>
      </div>
      <!-- 心仪院校（单个） -->
      <div v-else-if="dreamCollege" class="dream-college">
        <div class="dream-logo">
          <img :src="dreamCollege.logo || '/logos/dream.png'" :alt="dreamCollege.name" @error="handleImgError" />
        </div>
        <div class="dream-name">{{ dreamCollege.name }}</div>
        <div class="dream-batch">{{ dreamCollege.batch }}</div>
        <div class="score-gap" v-if="dreamCollege.scoreGap !== undefined">
          <span class="gap-label">总分还差</span>
          <span class="gap-value">{{ dreamCollege.scoreGap }}</span>
          <span class="gap-unit">分</span>
        </div>
        <div class="subject-gaps" v-if="dreamCollege.subjectGaps && dreamCollege.subjectGaps.length > 0">
          <div v-for="gap in dreamCollege.subjectGaps" :key="gap.subject" class="subject-gap">
            <span class="subj-name">{{ gap.subject }}</span>
            <span class="subj-gap">还差 {{ gap.gap }} 分</span>
          </div>
        </div>
        <div class="ai-incentive" v-if="dreamCollege.aiIncentive">
          <span class="incentive-icon">💪</span>
          {{ dreamCollege.aiIncentive }}
        </div>
      </div>
      <!-- 空状态 -->
      <div v-else class="empty-state">
        <span class="empty-icon">📚</span>
        <span class="empty-text">{{ emptyText }}</span>
      </div>
    </div>
    <div class="card-footer" v-if="summary">
      <span class="summary-text">{{ summary }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CollegeInfo, DreamCollegeInfo } from '@/types/growth'

defineProps<{
  type: 'current' | 'target' | 'dream'
  title: string
  colleges?: CollegeInfo[]
  dreamCollege?: DreamCollegeInfo
  summary?: string
  emptyText?: string
}>()

const handleImgError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.src = '/logos/default.svg'
}
</script>

<style scoped lang="scss">
.college-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s, box-shadow 0.3s;
  flex: 1;
  min-width: 0;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }

  &.dream-card {
    border: 1px solid #e6a23c;
    background: linear-gradient(135deg, #fffdf5 0%, #fff8e8 100%);
  }
}

.card-header {
  margin-bottom: 12px;

  .card-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}

.college-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.college-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  background: #f8f9fa;
}

.college-logo, .dream-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #e8eaed;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.college-name, .dream-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.college-batch-tag {
  font-size: 11px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.dream-college {
  text-align: center;

  .dream-logo {
    width: 56px;
    height: 56px;
    margin: 0 auto 8px;
  }

  .dream-name {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 4px;
  }

  .dream-batch {
    font-size: 13px;
    color: #e6a23c;
    background: #fdf6ec;
    display: inline-block;
    padding: 2px 12px;
    border-radius: 4px;
    margin-bottom: 12px;
  }
}

.score-gap {
  margin: 12px 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;

  .gap-label { font-size: 13px; color: #909399; }
  .gap-value {
    font-size: 28px;
    font-weight: 700;
    color: #e6a23c;
    line-height: 1;
  }
  .gap-unit { font-size: 13px; color: #909399; }
}

.subject-gaps {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.subject-gap {
  display: flex;
  gap: 4px;
  font-size: 12px;
  background: #fdf6ec;
  padding: 4px 10px;
  border-radius: 4px;
  .subj-name { color: #e6a23c; }
  .subj-gap { color: #909399; }
}

.ai-incentive {
  font-size: 13px;
  color: #67c23a;
  line-height: 1.5;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 8px;

  .incentive-icon { margin-right: 4px; }
}

.card-footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;

  .summary-text {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  gap: 8px;
  .empty-icon { font-size: 32px; }
  .empty-text { font-size: 13px; color: #c0c4cc; }
}
</style>
