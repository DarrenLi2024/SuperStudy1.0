<template>
  <div class="growth-page">
    <Header />

    <div class="main-content">
      <AiLoading v-if="loading" text="正在加载成长数据..." />

      <!-- 总分提分趋势 -->
      <section class="section">
        <h3 class="section-title">📈 总分提分趋势</h3>
        <el-card shadow="never" class="chart-card">
          <div ref="scoreChartRef" class="chart-container"></div>
        </el-card>
      </section>

      <!-- 等效位次波动 -->
      <section class="section">
        <h3 class="section-title">📉 全省等效位次波动</h3>
        <el-card shadow="never" class="chart-card">
          <div ref="rankChartRef" class="chart-container"></div>
        </el-card>
      </section>

      <!-- 各科单科提分 -->
      <section class="section">
        <h3 class="section-title">📊 各科单科提分曲线</h3>
        <el-card shadow="never" class="chart-card">
          <div class="subject-tabs">
            <el-tag v-for="subject in subjectNames" :key="subject" :type="activeSubject === subject ? 'primary' : 'info'" style="cursor:pointer" @click="switchSubject(subject)">
              {{ subject }}
            </el-tag>
          </div>
          <div ref="subjectChartRef" class="chart-container"></div>
        </el-card>
      </section>

      <!-- 段位升级历史 -->
      <section class="section">
        <h3 class="section-title">🏆 段位升级历史</h3>
        <el-card shadow="never" class="history-card">
          <div v-if="historyRecords.length > 0" class="history-timeline">
            <div v-for="(record, idx) in historyRecords" :key="record.id" class="timeline-item" :class="{ first: idx === 0 }">
              <div class="timeline-dot">
                <span class="dot-icon">🏅</span>
              </div>
              <div class="timeline-content">
                <div class="tl-header">
                  <span class="tl-badge">{{ record.previousBatch }} → {{ record.currentBatch }}</span>
                  <span class="tl-date">{{ record.upgradeTime?.substring(0, 10) }}</span>
                </div>
                <div class="tl-score">升级时总分：{{ record.scoreAtUpgrade }}分</div>
                <div class="tl-incentive" v-if="record.aiIncentiveText">💬 {{ record.aiIncentiveText }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无段位升级记录，继续加油！" />
        </el-card>
      </section>

      <!-- AI月度报告 -->
      <section class="section">
        <h3 class="section-title">📄 AI月度成长总结</h3>
        <el-card shadow="never" class="report-card">
          <div class="report-content">{{ monthlyReport }}</div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import Header from '@/components/Header.vue'
import AiLoading from '@/components/AiLoading.vue'
import { getGrowthData, getGrowthHistory } from '@/api/growth'
import type { GrowthData, GrowthRecordItem } from '@/types/growth'
import * as echarts from 'echarts'
import { getStudentId } from '@/utils/auth'

const loading = ref(true)
const scoreChartRef = ref<HTMLElement>()
const rankChartRef = ref<HTMLElement>()
const subjectChartRef = ref<HTMLElement>()
const activeSubject = ref('数学')
const subjectNames = ref<string[]>([])
const historyRecords = ref<GrowthRecordItem[]>([])
const monthlyReport = ref('')
const growthData = ref<GrowthData>({ scoreTrend: [], rankTrend: [], subjectTrends: {}, monthlyReport: '' })

onMounted(async () => {
  try {
    const [dataRes, historyRes] = await Promise.allSettled([
      getGrowthData(getStudentId() || 1),
      getGrowthHistory(getStudentId() || 1)
    ])

    if (dataRes.status === 'fulfilled') {
      growthData.value = dataRes.value.data
    } else {
      growthData.value = getMockGrowthData()
    }

    if (historyRes.status === 'fulfilled') {
      historyRecords.value = historyRes.value.data || []
    } else {
      historyRecords.value = getMockHistory()
    }
  } catch {
    growthData.value = getMockGrowthData()
    historyRecords.value = getMockHistory()
  }

  monthlyReport.value = growthData.value.monthlyReport || 'AI月报将在后续版本中自动生成。'
  subjectNames.value = Object.keys(growthData.value.subjectTrends || {})

  await nextTick()
  renderCharts()
  loading.value = false
})

const switchSubject = (subject: string) => {
  activeSubject.value = subject
  nextTick(() => renderSubjectChart(subject))
}

function renderCharts() {
  renderScoreChart()
  renderRankChart()
  if (subjectNames.value.length > 0) {
    renderSubjectChart(subjectNames.value[0])
  }
}

function renderScoreChart() {
  if (!scoreChartRef.value) return
  const chart = echarts.init(scoreChartRef.value)
  const data = growthData.value.scoreTrend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '8%', bottom: '10%', top: '10%' },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '等效高考分' },
    series: [{
      type: 'line',
      data: data.map(d => d.score),
      smooth: true,
      lineStyle: { color: '#409eff', width: 3 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }]) },
      symbol: 'circle',
      symbolSize: 8
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function renderRankChart() {
  if (!rankChartRef.value) return
  const chart = echarts.init(rankChartRef.value)
  const data = growthData.value.rankTrend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '8%', bottom: '10%', top: '10%' },
    xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '位次', inverse: true },
    series: [{
      type: 'line',
      data: data.map(d => d.rank),
      smooth: true,
      lineStyle: { color: '#67c23a', width: 3 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(103,194,58,0.3)' }, { offset: 1, color: 'rgba(103,194,58,0.05)' }]) },
      symbol: 'circle',
      symbolSize: 8
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function renderSubjectChart(subject: string) {
  if (!subjectChartRef.value) return
  const chart = echarts.init(subjectChartRef.value)
  const data = growthData.value.subjectTrends?.[subject] || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '8%', bottom: '10%', top: '10%' },
    xAxis: { type: 'category', data: data.map((d: any) => d.date), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '分数' },
    series: [{
      type: 'line',
      data: data.map((d: any) => d.score),
      smooth: true,
      lineStyle: { color: '#e6a23c', width: 3 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(230,162,60,0.3)' }, { offset: 1, color: 'rgba(230,162,60,0.05)' }]) },
      symbol: 'circle',
      symbolSize: 8
    }]
  })
  window.addEventListener('resize', () => chart.resize())
}

function getMockGrowthData(): GrowthData {
  return {
    scoreTrend: [
      { date: '2026-04-01', score: 380 }, { date: '2026-04-15', score: 395 },
      { date: '2026-05-01', score: 410 }, { date: '2026-05-15', score: 425 },
      { date: '2026-06-01', score: 440 }, { date: '2026-06-15', score: 450 }
    ],
    rankTrend: [
      { date: '2026-04-01', rank: 120000 }, { date: '2026-04-15', rank: 115000 },
      { date: '2026-05-01', rank: 108000 }, { date: '2026-05-15', rank: 100000 },
      { date: '2026-06-01', rank: 92000 }, { date: '2026-06-15', rank: 85000 }
    ],
    subjectTrends: {
      '语文': [{ date: '2026-04-01', score: 85 }, { date: '2026-05-01', score: 88 }, { date: '2026-06-01', score: 90 }],
      '数学': [{ date: '2026-04-01', score: 55 }, { date: '2026-05-01', score: 62 }, { date: '2026-06-01', score: 70 }],
      '英语': [{ date: '2026-04-01', score: 75 }, { date: '2026-05-01', score: 78 }, { date: '2026-06-01', score: 82 }],
      '历史': [{ date: '2026-04-01', score: 60 }, { date: '2026-05-01', score: 65 }, { date: '2026-06-01', score: 68 }],
      '政治': [{ date: '2026-04-01', score: 58 }, { date: '2026-05-01', score: 62 }, { date: '2026-06-01', score: 65 }],
      '地理': [{ date: '2026-04-01', score: 62 }, { date: '2026-05-01', score: 66 }, { date: '2026-06-01', score: 70 }]
    },
    monthlyReport: ''
  }
}

function getMockHistory(): GrowthRecordItem[] {
  return [
    { id: 1, studentId: 1, previousBatch: '本科以下', currentBatch: '本科以下', scoreAtUpgrade: 380, upgradeTime: '2026-04-01', aiIncentiveText: '起点虽低，但未来可期！每一分努力都会有回报。' },
    { id: 2, studentId: 1, previousBatch: '本科以下', currentBatch: '本科以下', scoreAtUpgrade: 425, upgradeTime: '2026-05-15', aiIncentiveText: '持续进步中！距离公办二本仅差一步之遥。' },
  ]
}
</script>

<style scoped lang="scss">
.growth-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.section { margin-bottom: 24px; }
.section-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 12px; }

.chart-card {
  border-radius: 12px;
  padding: 8px;
}

.chart-container {
  width: 100%;
  height: 280px;
}

.subject-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

// 升级历史
.history-card { border-radius: 12px; }

.timeline-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  position: relative;

  &:last-child { border-bottom: none; }
}

.timeline-dot {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f9eb;
  display: flex;
  align-items: center;
  justify-content: center;
  .dot-icon { font-size: 18px; }
}

.timeline-content { flex: 1; min-width: 0; }

.tl-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.tl-badge {
  font-size: 13px;
  font-weight: 500;
  color: #67c23a;
  background: #f0f9eb;
  padding: 2px 10px;
  border-radius: 4px;
}

.tl-date { font-size: 12px; color: #c0c4cc; }
.tl-score { font-size: 13px; color: #606266; margin-bottom: 4px; }
.tl-incentive { font-size: 13px; color: #909399; font-style: italic; }

// 报告
.report-card { border-radius: 12px; }

.report-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
