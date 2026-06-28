<template>
  <div class="knowledge-heatmap">
    <div v-for="subject in data" :key="subject.subject" class="subject-block">
      <div class="subject-title">{{ subject.subject }}</div>
      <div class="point-grid">
        <button
          v-for="point in subject.knowledgePoints"
          :key="point.name"
          class="point-cell"
          :class="point.status"
          :title="`${point.name}：${point.masteryLevel}%`"
          type="button"
        >
          <span class="point-name">{{ point.name }}</span>
          <span class="point-value">{{ point.masteryLevel }}%</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface KnowledgePoint {
  name: string
  masteryLevel: number
  status: string
}

interface KnowledgeSubject {
  subject: string
  knowledgePoints: KnowledgePoint[]
}

defineProps<{ data: KnowledgeSubject[] }>()
</script>

<style scoped lang="scss">
.knowledge-heatmap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.subject-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.point-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 8px;
}

.point-cell {
  min-height: 64px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 8px;
  background: #f4f7fb;
  text-align: left;
  cursor: default;
}

.point-cell.strong {
  background: #e8f6ed;
  border-color: #9bd8ad;
}

.point-cell.normal {
  background: #fff6e5;
  border-color: #edc980;
}

.point-cell.weak {
  background: #fdecec;
  border-color: #eeaaa6;
}

.point-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.25;
}

.point-value {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
}
</style>
