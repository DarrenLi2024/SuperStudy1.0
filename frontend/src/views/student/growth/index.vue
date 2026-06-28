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

    <!-- 段位升级庆祝弹窗 -->
    <el-dialog v-model="showUpgradeModal" :title="null" width="400px" class="upgrade-dialog" :close-on-click-modal="false" :show-close="false">
      <div class="upgrade-celebration">
        <div class="upgrade-animation">🎉🎊🏆</div>
        <div class="upgrade-title">恭喜升段！</div>
        <div class="upgrade-badge" v-if="latestUpgrade">
          {{ latestUpgrade.previousBatch }} → {{ latestUpgrade.currentBatch }}
        </div>
        <div class="upgrade-score" v-if="latestUpgrade">
          升级分数：{{ latestUpgrade.scoreAtUpgrade }}分
        </div>
        <div class="upgrade-incentive" v-if="latestUpgrade?.aiIncentiveText">
          💬 {{ latestUpgrade.aiIncentiveText }}
        </div>
        <div class="upgrade-actions">
          <el-button type="primary" round @click="showUpgradeModal = false">继续加油！</el-button>
        </div>
      </div>
    </el-dialog>
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
import { ElMessage } from 'element-plus'

const loading = ref(true)
const scoreChartRef = ref<HTMLElement>()
const rankChartRef = ref<HTMLElement>()
const subjectChartRef = ref<HTMLElement>()
const activeSubject = ref('数学')
const subjectNames = ref<string[]>([])
const historyRecords = ref<GrowthRecordItem[]>([])
const monthlyReport = ref('')
const growthData = ref<GrowthData>({ scoreTrend: [], rankTrend: [], subjectTrends: {}, monthlyReport: '' })
const showUpgradeModal = ref(false)
const latestUpgrade = ref<GrowthRecordItem | null>(null)

onMounted(async () => {
  try {
    const studentId = requireStudentId()
    if (!studentId) return
    const [dataRes, historyRes] = await Promise.allSettled([
      getGrowthData(studentId),
      getGrowthHistory(studentId)
    ])

    if (dataRes.status === 'fulfilled') {
      growthData.value = dataRes.value.data
    } else {
      throw dataRes.reason
    }

    if (historyRes.status === 'fulfilled') {
      historyRecords.value = historyRes.value.data || []
    } else {
      throw historyRes.reason
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '成长数据加载失败')
  }

  monthlyReport.value = growthData.value.monthlyReport || '【AI月度成长总结】本月数据已汇总，建议继续按薄弱学科优先级推进复盘。'
  subjectNames.value = Object.keys(growthData.value.subjectTrends || {})

  // 检查是否有最近新增的段位升级记录，自动弹出庆祝弹窗
  if (historyRecords.value.length > 0) {
    const first = historyRecords.value[0]
    if (first.upgradeTime) {
      const upgradeTime = new Date(first.upgradeTime).getTime()
      const now = Date.now()
      // 如果升级发生在最近24小时内，弹出庆祝弹窗
      if (now - upgradeTime < 24 * 60 * 60 * 1000) {
        latestUpgrade.value = first
        showUpgradeModal.value = true
      }
    }
  }

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

function requireStudentId() {
  const studentId = getStudentId()
  if (!studentId) {
    ElMessage.error('未获取到绑定学生信息，请重新登录')
    loading.value = false
    return null
  }
  return studentId
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

// 升级弹窗
.upgrade-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.upgrade-celebration {
  text-align: center;
  padding: 32px 24px;
}

.upgrade-animation {
  font-size: 48px;
  margin-bottom: 16px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.upgrade-title {
  font-size: 24px;
  font-weight: 700;
  color: #e6a23c;
  margin-bottom: 16px;
}

.upgrade-badge {
  display: inline-block;
  font-size: 16px;
  font-weight: 600;
  color: #67c23a;
  background: #f0f9eb;
  padding: 6px 20px;
  border-radius: 20px;
  margin-bottom: 12px;
}

.upgrade-score {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.upgrade-incentive {
  font-size: 14px;
  color: #909399;
  font-style: italic;
  margin-bottom: 24px;
  line-height: 1.6;
}

.upgrade-actions {
  margin-top: 8px;
}
</style>
