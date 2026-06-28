<template>
  <div class="parent-page">
    <div class="parent-header">
      <div class="header-left">
        <span class="logo">🎓 AI升学陪伴</span>
        <span class="role-badge">家长端</span>
      </div>
      <div class="header-right">
        <span class="user-name">{{ userInfo?.nickname || '家长' }}</span>
        <el-button type="primary" link @click="handleLogout">退出</el-button>
      </div>
    </div>

    <div class="main-content">
      <AiLoading v-if="loading" text="正在加载孩子学习数据..." />

      <!-- 当前段位和分差 -->
      <div class="overview-cards">
        <div class="overview-card batch-card">
          <div class="ov-label">当前段位</div>
          <div class="ov-value">{{ overview.currentBatch || '待评估' }}</div>
        </div>
        <div class="overview-card gap-card">
          <div class="ov-label">离心仪院校</div>
          <div class="ov-value gap">{{ overview.scoreGap || '-' }}<span class="ov-unit">分</span></div>
        </div>
        <div class="overview-card rate-card">
          <div class="ov-label">本周完成率</div>
          <div class="ov-value">{{ overview.weeklyCompletionRate || 0 }}%</div>
        </div>
      </div>

      <!-- 本周AI核心点评 -->
      <section class="section">
        <h3 class="section-title">💡 本周AI核心点评</h3>
        <el-card shadow="never" class="comment-card">
          <div class="ai-avatar">🤖</div>
          <div class="comment-text">{{ overview.weeklyAiComment || '暂无点评数据' }}</div>
        </el-card>
      </section>

      <!-- 近期成绩趋势 -->
      <section class="section">
        <h3 class="section-title">📈 近期模考分数趋势</h3>
        <el-card shadow="never" class="chart-card">
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </section>

      <!-- 学习概况 -->
      <section class="section">
        <h3 class="section-title">📋 本周学习完成率</h3>
        <el-card shadow="never" class="progress-card">
          <div class="progress-circle">
            <el-progress type="dashboard" :percentage="overview.weeklyCompletionRate || 0" :stroke-width="10" width="160" color="#67c23a" />
          </div>
          <div class="progress-desc">
            <p>本周学习任务完成率</p>
            <p class="sub-text">AI根据任务打卡情况自动统计</p>
          </div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, clearAuth, getStudentId } from '@/utils/auth'
import { getChildOverview } from '@/api/parent'
import AiLoading from '@/components/AiLoading.vue'
import * as echarts from 'echarts'

const router = useRouter()
const loading = ref(true)
const trendChartRef = ref<HTMLElement>()
const userInfo = computed(() => getUserInfo())

const overview = ref({
  currentBatch: '',
  dreamCollege: '',
  scoreGap: 0,
  weeklyAiComment: '',
  weeklyCompletionRate: 0,
  recentExamTrend: [] as { date: string; score: number; rank: number }[]
})

onMounted(async () => {
  try {
    const res = await getChildOverview(getStudentId() || 1)
    overview.value = res.data
  } catch {
    overview.value = getMockOverview()
  }

  await nextTick()
  renderChart()
  loading.value = false
})

function renderChart() {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  const data = overview.value.recentExamTrend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['等效高考分', '等效位次'], bottom: 0 },
    grid: { left: '8%', right: '8%', bottom: '20%', top: '8%' },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: [
      { type: 'value', name: '分数', position: 'left' },
      { type: 'value', name: '位次', position: 'right', inverse: true }
    ],
    series: [
      { name: '等效高考分', type: 'line', data: data.map(d => d.score), smooth: true, lineStyle: { color: '#409eff', width: 3 }, symbol: 'circle' },
      { name: '等效位次', type: 'line', yAxisIndex: 1, data: data.map(d => d.rank), smooth: true, lineStyle: { color: '#67c23a', width: 3 }, symbol: 'diamond' }
    ]
  })
  window.addEventListener('resize', () => chart.resize())
}

const handleLogout = () => {
  clearAuth()
  router.push('/login')
}

function getMockOverview() {
  return {
    currentBatch: '公办二本',
    dreamCollege: '北京大学',
    scoreGap: 150,
    weeklyAiComment: '本周孩子整体学习状态良好，数学和英语有显著进步，但历史科目仍需加强。建议重点关注函数与导数模块的专项训练。',
    weeklyCompletionRate: 78,
    recentExamTrend: [
      { date: '06-01', score: 420, rank: 85000 },
      { date: '06-08', score: 435, rank: 82000 },
      { date: '06-15', score: 450, rank: 78000 },
      { date: '06-22', score: 445, rank: 79500 }
    ]
  }
}
</script>

<style scoped lang="scss">
.parent-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.parent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .logo { font-size: 18px; font-weight: 700; color: #409eff; }
  .header-left { display: flex; align-items: center; gap: 8px; }
  .role-badge { font-size: 12px; color: #e6a23c; background: #fdf6ec; padding: 2px 8px; border-radius: 4px; }
  .header-right { display: flex; align-items: center; gap: 12px; }
  .user-name { font-size: 14px; color: #606266; }
}

.main-content {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

// 概览卡片
.overview-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.overview-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .ov-label { font-size: 12px; color: #909399; margin-bottom: 8px; }
  .ov-value { font-size: 20px; font-weight: 700; color: #303133; &.gap { color: #e6a23c; } }
  .ov-unit { font-size: 13px; font-weight: 400; color: #909399; }
}

.section { margin-bottom: 24px; }
.section-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 12px; }

.comment-card {
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;

  .ai-avatar { font-size: 28px; flex-shrink: 0; }
  .comment-text { font-size: 14px; color: #606266; line-height: 1.6; }
}

.chart-card { border-radius: 12px; padding: 8px; }
.chart-container { width: 100%; height: 260px; }

.progress-card {
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px;
}

.progress-circle { flex-shrink: 0; }

.progress-desc {
  p { font-size: 15px; color: #303133; font-weight: 500; margin-bottom: 4px; }
  .sub-text { font-size: 13px; color: #909399; font-weight: 400; }
}
</style>
