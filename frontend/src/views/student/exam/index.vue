<template>
  <div class="exam-page">
    <Header />

    <div class="main-content">
      <!-- 考试入口 -->
      <section class="section">
        <h3 class="section-title">🎯 AI全真模考</h3>
        <div class="exam-entry-grid">
          <el-card shadow="hover" class="exam-entry" @click="startExam('weekly')">
            <div class="entry-icon">📝</div>
            <div class="entry-title">本周AI全科周测</div>
            <div class="entry-desc">AI根据本周学习内容自动生成</div>
            <el-button type="primary" round class="entry-btn">开始测试</el-button>
          </el-card>
          <el-card shadow="hover" class="exam-entry" @click="startExam('monthly')">
            <div class="entry-icon">📋</div>
            <div class="entry-title">月度真题模考</div>
            <div class="entry-desc">AI精选真题，模拟真实考场</div>
            <el-button type="warning" round class="entry-btn">开始测试</el-button>
          </el-card>
        </div>
      </section>

      <!-- 历史考试记录 -->
      <section class="section">
        <h3 class="section-title">📊 历史考试记录</h3>
        <el-card shadow="never" class="records-card">
          <div v-if="examRecords.length > 0" class="records-list">
            <div v-for="record in examRecords" :key="record.id" class="record-item" @click="viewDetail(record)">
              <div class="record-left">
                <div class="record-date">{{ formatDate(record.examDate) }}</div>
                <div class="record-type">{{ record.examType === 'weekly' ? '周测' : '月考' }}</div>
              </div>
              <div class="record-center">
                <div class="record-scores">
                  <span class="score-item">总分 <strong>{{ record.totalScore }}</strong></span>
                  <span class="score-divider">|</span>
                  <span class="score-item">等效分 <strong>{{ record.equivalentGaokaoScore }}</strong></span>
                </div>
                <div class="record-rank">
                  等效位次 <strong>{{ record.equivalentRank || '-' }}</strong>
                </div>
              </div>
              <div class="record-right">
                <span class="batch-tag">{{ record.currentBatch }}</span>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无考试记录，开始你的第一次模考吧！">
            <el-button type="primary" @click="startExam('weekly')">开始周测</el-button>
          </el-empty>
        </el-card>
      </section>

      <!-- 考试弹窗 -->
      <el-dialog v-model="showDialog" :title="dialogTitle" width="90%" class="exam-dialog">
        <div v-if="currentQuestions.length > 0" class="question-list">
          <div v-for="(q, idx) in currentQuestions" :key="idx" class="question-item">
            <div class="q-header">
              <span class="q-num">第{{ idx + 1 }}题</span>
              <span class="q-subject">{{ q.subject }}</span>
              <el-tag :type="q.difficulty === 'basic' ? 'success' : q.difficulty === 'medium' ? 'warning' : 'danger'" size="small">
                {{ q.difficulty === 'basic' ? '基础' : q.difficulty === 'medium' ? '中档' : '较难' }}
              </el-tag>
            </div>
            <div class="q-content">{{ q.questionContent }}</div>
            <div v-if="q.options" class="q-options">
              <div v-for="(opt, oi) in q.options" :key="oi" class="q-option" :class="{ selected: selectedAnswers[q.id] === opt }" @click="selectedAnswers[q.id] = opt">
                {{ opt }}
              </div>
            </div>
            <div class="q-answer" v-if="showAnswers">
              <span class="answer-label">正确答案：</span>
              <span class="answer-value">{{ q.answer }}</span>
            </div>
          </div>
        </div>
        <div v-else class="exam-empty">
          <AiLoading text="AI 正在生成试卷..." />
        </div>
        <template #footer>
          <el-button @click="showDialog = false">关闭</el-button>
          <el-button v-if="!showAnswers" type="primary" @click="submitExam">提交答案</el-button>
          <el-button v-else type="primary" @click="showDialog = false">完成</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import Header from '@/components/Header.vue'
import AiLoading from '@/components/AiLoading.vue'
import { getExamRecords, submitExam as submitExamApi } from '@/api/exam'
import { getTrainingQuestions } from '@/api/question'
import type { ExamRecordItem } from '@/types/growth'
import { getStudentId } from '@/utils/auth'

const examRecords = ref<ExamRecordItem[]>([])
const showDialog = ref(false)
const dialogTitle = ref('')
const currentQuestions = ref<any[]>([])
const selectedAnswers = ref<Record<number, string>>({})
const showAnswers = ref(false)
const currentExamType = ref('weekly')

onMounted(async () => {
  try {
    const studentId = requireStudentId()
    if (!studentId) return
    const res = await getExamRecords(studentId)
    examRecords.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '考试记录加载失败')
  }
})

const startExam = async (type: string) => {
  currentExamType.value = type
  dialogTitle.value = type === 'weekly' ? '本周AI全科周测' : '月度真题模考'
  showAnswers.value = false
  selectedAnswers.value = {}
  currentQuestions.value = []
  showDialog.value = true
  try {
    const studentId = requireStudentId()
    if (!studentId) {
      showDialog.value = false
      return
    }
    const res = await getTrainingQuestions(studentId, '数学', type === 'weekly' ? 6 : 10)
    currentQuestions.value = (res.data || []).map((q: any, index: number) => ({ ...q, id: q.id || index + 1 }))
  } catch (error: any) {
    showDialog.value = false
    ElMessage.error(error?.message || 'AI试卷生成失败')
  }
}

const submitExam = async () => {
  const total = currentQuestions.value.length
  const answered = Object.keys(selectedAnswers.value).length
  const subjectScores = calculateSubjectScores()
  try {
    const today = new Date().toISOString().substring(0, 10)
    await submitExamApi({
      examType: currentExamType.value,
      subjectScores,
      examDate: today
    })
    showAnswers.value = true
    ElMessage.success(`提交成功！共${total}题，已作答${answered}题`)
    const studentId = requireStudentId()
    if (!studentId) return
    const res = await getExamRecords(studentId)
    examRecords.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '考试提交失败')
  }
}

const viewDetail = (record: ExamRecordItem) => {
  ElMessage.info(`查看${formatDate(record.examDate)}考试详情`)
}

const formatDate = (date: string) => {
  if (!date) return ''
  return date.substring(0, 10)
}

function calculateSubjectScores() {
  const subjects = ['语文', '数学', '英语', '历史', '政治', '地理']
  const scores: Record<string, number> = Object.fromEntries(subjects.map(subject => [subject, 0]))
  const totals: Record<string, number> = Object.fromEntries(subjects.map(subject => [subject, 0]))
  currentQuestions.value.forEach((question) => {
    const subject = question.subject || '数学'
    const weight = question.difficulty === 'hard' ? 18 : question.difficulty === 'medium' ? 14 : 10
    totals[subject] = (totals[subject] || 0) + weight
    const selected = selectedAnswers.value[question.id]
    if (selected && selected === question.answer) {
      scores[subject] = (scores[subject] || 0) + weight
    }
  })
  subjects.forEach((subject) => {
    if (totals[subject] === 0) {
      scores[subject] = 60
    } else {
      scores[subject] = Math.round((scores[subject] / totals[subject]) * 100)
    }
  })
  return scores
}

function requireStudentId() {
  const studentId = getStudentId()
  if (!studentId) {
    ElMessage.error('未获取到绑定学生信息，请重新登录')
    return null
  }
  return studentId
}
</script>

<style scoped lang="scss">
.exam-page {
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

// 入口
.exam-entry-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  @media (max-width: 480px) { grid-template-columns: 1fr; }
}

.exam-entry {
  text-align: center;
  padding: 24px 16px;
  cursor: pointer;
  transition: transform 0.2s;
  &:hover { transform: translateY(-3px); }

  .entry-icon { font-size: 42px; margin-bottom: 12px; }
  .entry-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 6px; }
  .entry-desc { font-size: 13px; color: #909399; margin-bottom: 16px; }
  .entry-btn { width: 140px; }
}

// 记录
.records-card { border-radius: 12px; }

.record-item {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  gap: 16px;

  &:last-child { border-bottom: none; }
  &:hover { background: #f8f9fa; border-radius: 8px; padding: 14px 8px; }
}

.record-left { text-align: center; min-width: 64px; }
.record-date { font-size: 12px; color: #909399; }
.record-type { font-size: 13px; font-weight: 500; color: #409eff; background: #ecf5ff; padding: 1px 8px; border-radius: 4px; margin-top: 4px; }

.record-center { flex: 1; }
.record-scores { font-size: 13px; color: #606266; margin-bottom: 2px; .score-item { margin: 0 4px; } .score-divider { color: #dcdfe6; } }
.record-rank { font-size: 12px; color: #909399; }

.record-right {
  display: flex;
  align-items: center;
  gap: 8px;
  .batch-tag { font-size: 12px; color: #409eff; background: #ecf5ff; padding: 2px 8px; border-radius: 4px; }
}

// 试卷
.exam-dialog {
  :deep(.el-dialog__body) { max-height: 60vh; overflow-y: auto; }
}

.question-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }
}

.q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  .q-num { font-weight: 600; color: #409eff; }
  .q-subject { font-size: 13px; color: #606266; }
}

.q-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 12px;
}

.q-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.q-option {
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { border-color: #409eff; color: #409eff; }
  &.selected { border-color: #409eff; background: #ecf5ff; color: #409eff; font-weight: 500; }
}

.q-answer {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 6px;
  .answer-label { font-size: 13px; color: #67c23a; font-weight: 500; }
  .answer-value { font-size: 14px; color: #303133; }
}

.exam-empty {
  padding: 40px 0;
  text-align: center;
}
</style>
