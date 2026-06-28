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
import { getExamRecords } from '@/api/exam'
import type { ExamRecordItem } from '@/types/growth'
import { getStudentId } from '@/utils/auth'

const examRecords = ref<ExamRecordItem[]>([])
const showDialog = ref(false)
const dialogTitle = ref('')
const currentQuestions = ref<any[]>([])
const selectedAnswers = ref<Record<number, string>>({})
const showAnswers = ref(false)

onMounted(async () => {
  try {
    const res = await getExamRecords(getStudentId() || 1)
    examRecords.value = res.data || []
  } catch {
    examRecords.value = getMockRecords()
  }
})

const startExam = (type: string) => {
  dialogTitle.value = type === 'weekly' ? '本周AI全科周测' : '月度真题模考'
  showAnswers.value = false
  selectedAnswers.value = {}
  currentQuestions.value = generateQuestions(type)
  showDialog.value = true
}

const submitExam = () => {
  showAnswers.value = true
  const total = currentQuestions.value.length
  const answered = Object.keys(selectedAnswers.value).length
  ElMessage.success(`提交成功！共${total}题，已作答${answered}题`)
}

const viewDetail = (record: ExamRecordItem) => {
  ElMessage.info(`查看${formatDate(record.examDate)}考试详情`)
}

const formatDate = (date: string) => {
  if (!date) return ''
  return date.substring(0, 10)
}

function generateQuestions(type: string) {
  // 按高考真题难度排列：基础→中档→较难
  const templates = [
    { subject: '语文', difficulty: 'basic', content: '下列词语中加点字的读音全部正确的一项是：', options: ['A. 慰藉(jí) 炽热(zhì)', 'B. 解剖(pōu) 挫折(cuò)', 'C. 酗酒(xiōng) 联袂(mèi)', 'D. 桎梏(kù) 针砭(biān)'], answer: 'B. 解剖(pōu) 挫折(cuò)' },
    { subject: '数学', difficulty: 'basic', content: '已知集合 A={x|x²-3x+2=0}，B={x|0<x<5,x∈N}，则 A∩B = ?', options: ['A. {1}', 'B. {2}', 'C. {1,2}', 'D. ∅'], answer: 'C. {1,2}' },
    { subject: '数学', difficulty: 'medium', content: '已知函数 f(x)=ax³+bx²+cx 在 x=1 处有极大值 4，在 x=2 处有极小值 0，求 a+b+c 的值。', options: ['A. 2', 'B. 4', 'C. 6', 'D. 8'], answer: 'C. 6' },
    { subject: '数学', difficulty: 'hard', content: '椭圆 C: x²/9 + y²/4 = 1 的左、右焦点分别为 F₁, F₂，点 P 为椭圆 C 上一点，且 ∠F₁PF₂=60°，则 △F₁PF₂ 的面积为：', options: ['A. 2√3', 'B. 4√3/3', 'C. 4√3', 'D. 8√3/3'], answer: 'B. 4√3/3' },
    { subject: '英语', difficulty: 'basic', content: 'The experiment, ___ will be conducted next month, aims to test the new drug\'s effectiveness.', options: ['A. that', 'B. which', 'C. what', 'D. whose'], answer: 'B. which' },
    { subject: '英语', difficulty: 'medium', content: 'Not until she arrived at the airport ___ that she had left her passport at home.', options: ['A. she realized', 'B. did she realize', 'C. she did realize', 'D. had she realized'], answer: 'B. did she realize' },
    { subject: '历史', difficulty: 'basic', content: '明清时期，我国统一多民族国家进一步巩固。下列属于清政府巩固统一措施的是：', options: ['A. 设立西域都护', 'B. 设立宣政院', 'C. 设立驻藏大臣', 'D. 设立安西都护府'], answer: 'C. 设立驻藏大臣' },
    { subject: '历史', difficulty: 'medium', content: '1915年，陈独秀在上海创办《青年杂志》，新文化运动由此开始。新文化运动提倡的"德先生"和"赛先生"分别指：', options: ['A. 民主与科学', 'B. 自由与平等', 'C. 法治与科学', 'D. 民主与自由'], answer: 'A. 民主与科学' },
    { subject: '地理', difficulty: 'basic', content: '我国地势西高东低，呈阶梯状分布。其中第一级阶梯平均海拔在：', options: ['A. 1000米以上', 'B. 2000米以上', 'C. 4000米以上', 'D. 500米以上'], answer: 'C. 4000米以上' },
    { subject: '地理', difficulty: 'medium', content: '下图为某地地质剖面示意图，图中岩层由老到新的顺序是：', options: ['A. ①②③④', 'B. ②①④③', 'C. ③④①②', 'D. ④③②①'], answer: 'A. ①②③④' },
    { subject: '政治', difficulty: 'basic', content: '我国的根本政治制度是：', options: ['A. 社会主义制度', 'B. 人民代表大会制度', 'C. 中国共产党领导的多党合作和政治协商制度', 'D. 民族区域自治制度'], answer: 'B. 人民代表大会制度' },
    { subject: '政治', difficulty: 'medium', content: '2025年中央经济工作会议强调，要实施更加积极的财政政策和适度宽松的货币政策。下列属于积极财政政策措施的是：', options: ['A. 提高存款准备金率', 'B. 增加地方政府专项债券发行', 'C. 提高贷款利率', 'D. 减少财政支出'], answer: 'B. 增加地方政府专项债券发行' },
  ]
  const count = type === 'weekly' ? 6 : 10
  // 打乱后取 count 道题，保证难度均衡
  const shuffled = [...templates].sort(() => Math.random() - 0.5)
  const basics = shuffled.filter(t => t.difficulty === 'basic')
  const mediums = shuffled.filter(t => t.difficulty === 'medium')
  const hards = shuffled.filter(t => t.difficulty === 'hard')
  const selected = [
    ...basics.slice(0, Math.ceil(count * 0.5)),
    ...mediums.slice(0, Math.ceil(count * 0.35)),
    ...hards.slice(0, Math.ceil(count * 0.15))
  ].slice(0, count)
  return selected.map((t, i) => ({ ...t, id: i + 1 }))
}

function getMockRecords(): ExamRecordItem[] {
  // 模拟提分过程：分数逐步上升，位次稳步前进
  return [
    { id: 1, examType: 'monthly', subjectScores: '{}', totalScore: 385, equivalentGaokaoScore: 327, equivalentRank: 105000, currentBatch: '本科以下', examDate: '2026-03-15', aiDiagnosisReport: '', createdAt: '' },
    { id: 2, examType: 'weekly', subjectScores: '{}', totalScore: 395, equivalentGaokaoScore: 336, equivalentRank: 98500, currentBatch: '本科以下', examDate: '2026-04-12', aiDiagnosisReport: '', createdAt: '' },
    { id: 3, examType: 'monthly', subjectScores: '{}', totalScore: 410, equivalentGaokaoScore: 349, equivalentRank: 92000, currentBatch: '本科以下', examDate: '2026-04-26', aiDiagnosisReport: '', createdAt: '' },
    { id: 4, examType: 'weekly', subjectScores: '{}', totalScore: 425, equivalentGaokaoScore: 361, equivalentRank: 86000, currentBatch: '本科以下', examDate: '2026-05-10', aiDiagnosisReport: '', createdAt: '' },
    { id: 5, examType: 'weekly', subjectScores: '{}', totalScore: 440, equivalentGaokaoScore: 374, equivalentRank: 80000, currentBatch: '本科以下', examDate: '2026-05-24', aiDiagnosisReport: '', createdAt: '' },
    { id: 6, examType: 'monthly', subjectScores: '{}', totalScore: 455, equivalentGaokaoScore: 387, equivalentRank: 73000, currentBatch: '本科以下', examDate: '2026-06-15', aiDiagnosisReport: '', createdAt: '' },
  ]
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
