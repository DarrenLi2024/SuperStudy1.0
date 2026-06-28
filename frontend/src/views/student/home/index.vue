<template>
  <div class="student-home">
    <Header />

    <div class="main-content">
      <!-- 顶部状态栏 -->
      <div class="status-bar">
        <div class="status-item">
          <div class="status-label">高考倒计时</div>
          <div class="status-value countdown">{{ remainingDays }}<span class="unit">天</span></div>
        </div>
        <div class="status-item">
          <div class="status-label">当前段位</div>
          <div class="status-value batch-tag">{{ collegeCardData.currentBatch || '暂未评估' }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">今日完成率</div>
          <div class="status-value">{{ taskData.completionRate || 0 }}%</div>
        </div>
      </div>

      <!-- AI加载中 -->
      <AiLoading v-if="loading" text="AI 正在分析你的学习数据..." />

      <!-- 三段式院校对标卡片 -->
      <section class="section">
        <h3 class="section-title">🎯 三段式院校对标</h3>
        <div class="college-cards">
          <CollegeCard
            type="current"
            title="当前稳妥段位"
            :colleges="collegeCardData.currentBatchCards"
            :summary="'当前总分数 ' + collegeCardData.currentScore + '分'"
            empty-text="暂无院校数据"
          />
          <CollegeCard
            type="target"
            title="目标稳妥段位"
            :colleges="collegeCardData.targetBatchCards"
            :summary="'目标院校批次展示'"
            empty-text="请先设定目标分数"
          />
          <CollegeCard
            type="dream"
            title="心仪院校差距"
            :dreamCollege="collegeCardData.dreamCollege"
            empty-text="请先设定心仪院校"
          />
        </div>
      </section>

      <!-- 段位成长进度条 -->
      <section class="section">
        <GrowthProgress :progressData="progressData" />
      </section>

      <!-- AI今日学习任务 -->
      <section class="section">
        <div class="task-section-header">
          <h3 class="section-title">📋 AI今日自适应学习任务</h3>
          <span class="task-progress">完成率 {{ taskData.completionRate || 0 }}%</span>
        </div>
        <el-card v-if="taskData.tasks && taskData.tasks.length > 0" shadow="never" class="task-card">
          <div v-for="task in taskData.tasks" :key="task.id" class="task-item">
            <div class="task-check">
              <el-checkbox v-model="task.status" :true-label="'completed'" :false-label="'pending'"
                @change="() => handleTaskComplete(task)" />
            </div>
            <div class="task-body">
              <div class="task-header">
                <span class="task-type-tag" :class="task.type">
                  {{ task.type === '专项刷题' ? '📝' : task.type === '错题复盘' ? '📖' : '📌' }} {{ task.type }}
                </span>
                <span class="task-subject">{{ task.subject }}</span>
              </div>
              <div class="task-content">{{ task.content }}</div>
              <div class="task-hint" v-if="task.aiHint">💡 {{ task.aiHint }}</div>
            </div>
          </div>
        </el-card>
        <el-card v-else shadow="never" class="task-card">
          <el-empty description="暂无今日任务，AI正在生成中..." />
        </el-card>
      </section>

      <!-- AI今日成长点评 -->
      <section class="section" v-if="taskData.aiComment">
        <h3 class="section-title">🌟 AI今日成长点评</h3>
        <el-card shadow="never" class="comment-card">
          <div class="ai-avatar">🤖</div>
          <div class="comment-text">{{ taskData.aiComment }}</div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import Header from '@/components/Header.vue'
import CollegeCard from '@/components/CollegeCard.vue'
import GrowthProgress from '@/components/GrowthProgress.vue'
import AiLoading from '@/components/AiLoading.vue'
import { getCollegeCards, getGrowthProgress } from '@/api/growth'
import { getTodayTasks } from '@/api/learning'
import { getMyProfile } from '@/api/student'
import type { CollegeCardData, GrowthProgressData, TodayTaskData, TaskItem } from '@/types/growth'
import { ElMessage } from 'element-plus'
import { getStudentId } from '@/utils/auth'

const loading = ref(true)
const remainingDays = ref(0)
const collegeCardData = ref<CollegeCardData>({
  currentBatchCards: [],
  targetBatchCards: [],
  dreamCollege: { name: '', logo: '', batch: '', scoreGap: 0, subjectGaps: [], aiIncentive: '' },
  currentBatch: '',
  currentScore: 0
})
const progressData = ref<GrowthProgressData>({
  phases: [],
  totalProgress: 0,
  targetScore: 0
})
const taskData = ref<TodayTaskData>({
  tasks: [],
  aiComment: '',
  completionRate: 0
})

let studentId: number | null = null

const fetchData = async () => {
  loading.value = true
  try {
    // 从登录信息获取 studentId
    studentId = getStudentId()
    if (!studentId) {
      // 先获取学生档案
      try {
        const profileRes = await getMyProfile()
        studentId = profileRes.data.id
        remainingDays.value = profileRes.data.remainingDays || 365
      } catch {
        studentId = 1
        remainingDays.value = 345
      }
    }

    if (!studentId) return

    // 并行获取卡片、进度条、今日任务数据
    const [cardsRes, progressRes, tasksRes] = await Promise.allSettled([
      getCollegeCards(studentId),
      getGrowthProgress(studentId),
      getTodayTasks(studentId)
    ])

    if (cardsRes.status === 'fulfilled') {
      collegeCardData.value = cardsRes.value.data
    } else {
      console.warn('获取院校卡片数据失败，使用Mock数据:', cardsRes.reason)
      collegeCardData.value = getMockCardData()
    }

    if (progressRes.status === 'fulfilled') {
      progressData.value = progressRes.value.data
    } else {
      progressData.value = getMockProgressData()
    }

    if (tasksRes.status === 'fulfilled') {
      taskData.value = tasksRes.value.data
    } else {
      taskData.value = getMockTaskData()
    }
  } catch (err) {
    console.error('数据加载失败，使用Mock数据:', err)
    collegeCardData.value = getMockCardData()
    progressData.value = getMockProgressData()
    taskData.value = getMockTaskData()
    remainingDays.value = 345
  } finally {
    loading.value = false
  }
}

const handleTaskComplete = async (task: TaskItem) => {
  try {
    ElMessage.success(`已完成：${task.content}`)
  } catch {
    // ignore
  }
}

onMounted(fetchData)

// Mock数据（后端不可用时的降级方案）
function getMockCardData(): CollegeCardData {
  return {
    currentBatchCards: [
      { id: 1, name: '河南大学', logo: '/logos/default.svg', batch: '公办二本' },
      { id: 2, name: '河北师范大学', logo: '/logos/default.svg', batch: '公办二本' },
      { id: 3, name: '山西大学', logo: '/logos/default.svg', batch: '公办二本' }
    ],
    targetBatchCards: [
      { id: 4, name: '郑州大学', logo: '/logos/default.svg', batch: '211' },
      { id: 5, name: '陕西师范大学', logo: '/logos/default.svg', batch: '211' },
      { id: 6, name: '西南大学', logo: '/logos/default.svg', batch: '211' }
    ],
    dreamCollege: {
      name: '北京大学',
      logo: '/logos/dream.svg',
      batch: '双一流',
      scoreGap: 150,
      subjectGaps: [{ subject: '数学', gap: 60 }, { subject: '英语', gap: 30 }],
      aiIncentive: '你的数学进步空间很大，加油！'
    },
    currentBatch: '公办二本',
    currentScore: 450
  }
}

function getMockProgressData(): GrowthProgressData {
  return {
    phases: [
      { name: '二本阶段', startScore: 400, endScore: 480, currentScore: 450, progress: 62.5, completed: false },
      { name: '一本阶段', startScore: 480, endScore: 550, currentScore: 450, progress: 0, completed: false },
      { name: '211/双一流阶段', startScore: 550, endScore: 650, currentScore: 450, progress: 0, completed: false }
    ],
    totalProgress: 20,
    targetScore: 600
  }
}

function getMockTaskData(): TodayTaskData {
  return {
    tasks: [
      { id: 1, type: '专项刷题', content: '完成10道函数综合练习题', subject: '数学', knowledgePoint: '函数与导数', aiHint: '本次任务为补齐函数与导数知识点短板', completionRate: 0, status: 'pending' },
      { id: 2, type: '专项刷题', content: '精读2篇阅读理解并整理生词', subject: '英语', knowledgePoint: '阅读理解', aiHint: '本次任务为提升阅读理解能力', completionRate: 0, status: 'pending' },
      { id: 3, type: '知识点巩固', content: '复习大气环流章节', subject: '地理', knowledgePoint: '自然地理', aiHint: '本次任务为巩固自然地理基础知识', completionRate: 0, status: 'pending' }
    ],
    aiComment: '今日学习任务已为你量身定制，从薄弱环节开始突破，点滴积累终见成效！',
    completionRate: 0
  }
}
</script>

<style scoped lang="scss">
.student-home {
  min-height: 100vh;
  background: #f5f7fa;
}

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

// 顶部状态栏
.status-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  .status-item {
    flex: 1;
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  .status-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 6px;
  }

  .status-value {
    font-size: 20px;
    font-weight: 700;
    color: #303133;

    &.countdown {
      color: #e6a23c;
    }

    &.batch-tag {
      font-size: 14px;
      color: #409eff;
      background: #ecf5ff;
      display: inline-block;
      padding: 2px 12px;
      border-radius: 4px;
    }

    .unit {
      font-size: 13px;
      font-weight: 400;
      color: #909399;
    }
  }
}

// 分段标题
.section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

// 三段式院校卡片
.college-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

// 任务卡片
.task-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .task-progress {
    font-size: 13px;
    color: #409eff;
    font-weight: 500;
  }
}

.task-card {
  border-radius: 12px;
}

.task-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.task-check {
  padding-top: 2px;
}

.task-body {
  flex: 1;
  min-width: 0;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.task-type-tag {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 1px 8px;
  border-radius: 4px;
}

.task-subject {
  font-size: 12px;
  color: #909399;
}

.task-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 4px;
}

.task-hint {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

// AI点评
.comment-card {
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
}

.ai-avatar {
  font-size: 28px;
  flex-shrink: 0;
}

.comment-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
</style>
