<template>
  <div class="learning-page">
    <Header />

    <div class="main-content">
      <AiLoading v-if="loading" text="AI 正在生成你的专属学习方案..." />

      <!-- AI个性化专项训练 -->
      <section class="section">
        <h3 class="section-title">📝 AI个性化专项训练</h3>
        <div class="training-grid">
          <el-card v-for="subject in subjects" :key="subject.name" shadow="hover" class="subject-card" @click="startTraining(subject.name)">
            <div class="subject-icon">{{ subject.icon }}</div>
            <div class="subject-name">{{ subject.name }}</div>
            <div class="subject-progress">
              <el-progress :percentage="subject.mastery" :color="subject.mastery >= 70 ? '#67c23a' : subject.mastery >= 40 ? '#e6a23c' : '#f56c6c'" :stroke-width="6" />
            </div>
            <div class="subject-mastery">
              <span class="mastery-label">掌握度</span>
              <span class="mastery-value">{{ subject.mastery }}%</span>
            </div>
            <el-button type="primary" size="small" round class="start-btn">开始训练</el-button>
          </el-card>
        </div>
      </section>

      <!-- AI错题智能复盘 -->
      <section class="section">
        <h3 class="section-title">📖 AI错题智能复盘</h3>
        <el-card shadow="never" class="error-card">
          <div v-if="errors.length > 0" class="error-list">
            <div v-for="err in errors" :key="err.id" class="error-item">
              <div class="error-header">
                <span class="error-subject-tag" :class="err.subject">{{ err.subject }}</span>
                <span class="error-knowledge">{{ err.knowledgePoint }}</span>
                <el-tag v-if="err.reinforcementFlag === 1" type="success" size="small">已补强</el-tag>
                <el-tag v-else type="warning" size="small">待补强</el-tag>
              </div>
              <div class="error-question">{{ err.questionContent }}</div>
              <div class="error-analysis" v-if="err.aiAnalysis">
                <span class="analysis-label">AI分析：</span>
                {{ err.aiAnalysis }}
              </div>
              <div class="error-actions" v-if="err.reinforcementFlag === 0">
                <el-button size="small" type="primary" link @click="doReinforcement(err)">同类补强</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无错题记录，继续保持！" />
        </el-card>
      </section>

      <!-- 知识点掌握图谱 -->
      <section class="section">
        <h3 class="section-title">📊 知识点掌握图谱</h3>
        <el-card shadow="never" class="knowledge-card">
          <div v-if="knowledgeData.length > 0" class="knowledge-grid">
            <div v-for="subject in knowledgeData" :key="subject.subject" class="knowledge-subject">
              <h4 class="ks-title">{{ subject.subject }}</h4>
              <div class="ks-points">
                <div v-for="point in subject.knowledgePoints" :key="point.name" class="ks-point">
                  <div class="ks-point-name">{{ point.name }}</div>
                  <el-progress
                    :percentage="point.masteryLevel"
                    :color="point.masteryLevel >= 70 ? '#67c23a' : point.masteryLevel >= 40 ? '#e6a23c' : '#f56c6c'"
                    :stroke-width="8"
                    :text-inside="true"
                  />
                </div>
              </div>
            </div>
          </div>
          <AiLoading v-else text="正在分析知识点掌握情况..." />
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import AiLoading from '@/components/AiLoading.vue'
import { getErrorQuestions, getKnowledgeStatus } from '@/api/learning'
import { getStudentId } from '@/utils/auth'

const loading = ref(true)

interface SubjectMastery { name: string; icon: string; mastery: number }
interface ErrorItem { id: number; subject: string; knowledgePoint: string; questionContent: string; wrongAnswer: string; aiAnalysis: string; reinforcementFlag: number; createdAt: string }
interface KnowledgeSubject { subject: string; knowledgePoints: { name: string; masteryLevel: number; status: string }[] }

const subjects = ref<SubjectMastery[]>([])
const errors = ref<ErrorItem[]>([])
const knowledgeData = ref<KnowledgeSubject[]>([])

onMounted(async () => {
  try {
    const [errRes, knowRes] = await Promise.allSettled([
      getErrorQuestions(getStudentId() || 1),
      getKnowledgeStatus(getStudentId() || 1)
    ])

    if (errRes.status === 'fulfilled') {
      errors.value = errRes.value.data || []
    }

    if (knowRes.status === 'fulfilled') {
      const raw = knowRes.value.data || {}
      knowledgeData.value = Object.entries(raw).map(([subject, points]: [string, any]) => ({
        subject,
        knowledgePoints: points.map((p: any) => ({ name: p.name, masteryLevel: p.masteryLevel || 0, status: p.status || 'normal' }))
      }))
    }
  } catch {
    // 使用Mock数据
    errors.value = getMockErrors()
    knowledgeData.value = getMockKnowledge()
  }

  // 默认掌握数据
  const mockSubjects: SubjectMastery[] = [
    { name: '语文', icon: '📖', mastery: 65 },
    { name: '数学', icon: '📐', mastery: 38 },
    { name: '英语', icon: '🌍', mastery: 55 },
    { name: '历史', icon: '🏛️', mastery: 72 },
    { name: '政治', icon: '📜', mastery: 68 },
    { name: '地理', icon: '🌏', mastery: 60 }
  ]

  if (knowledgeData.value.length > 0) {
    mockSubjects.forEach(s => {
      const found = knowledgeData.value.find(k => k.subject === s.name)
      if (found && found.knowledgePoints.length > 0) {
        const avg = Math.round(found.knowledgePoints.reduce((sum, p) => sum + p.masteryLevel, 0) / found.knowledgePoints.length)
        s.mastery = avg
      }
    })
  }
  subjects.value = mockSubjects

  loading.value = false
})

const startTraining = (subject: string) => {
  ElMessage.info(`正在为你生${subject}专项训练题...`)
}

const doReinforcement = (err: ErrorItem) => {
  ElMessage.success(`已推送${err.subject}同类补强题`)
}

function getMockErrors(): ErrorItem[] {
  return [
    { id: 1, subject: '数学', knowledgePoint: '函数与导数', questionContent: '已知函数 f(x)=x²+2x-3，求 f(-1) 的值', wrongAnswer: '-4', aiAnalysis: '代入公式错误，忘记将x=-1代入后计算平方项，建议加强基础运算练习。', reinforcementFlag: 0, createdAt: '2026-06-25' },
    { id: 2, subject: '英语', knowledgePoint: '阅读理解', questionContent: 'What is the main idea of paragraph 2?', wrongAnswer: 'A', aiAnalysis: '未能准确理解段落主旨，建议进行精读训练，关注段落首尾句。', reinforcementFlag: 0, createdAt: '2026-06-24' }
  ]
}

function getMockKnowledge(): KnowledgeSubject[] {
  const templates: Record<string, string[]> = {
    '语文': ['文言文阅读', '现代文阅读', '诗歌鉴赏', '作文'],
    '数学': ['函数与导数', '三角函数', '数列', '概率统计', '解析几何'],
    '英语': ['阅读理解', '完形填空', '语法', '写作', '词汇'],
    '历史': ['中国古代史', '中国近现代史', '世界史'],
    '政治': ['经济生活', '政治生活', '文化生活', '哲学'],
    '地理': ['自然地理', '人文地理', '区域地理']
  }
  return Object.entries(templates).map(([subject, points]) => ({
    subject,
    knowledgePoints: points.map(name => {
      const level = 30 + Math.round(Math.random() * 60)
      return { name, masteryLevel: level, status: level >= 70 ? 'strong' : level >= 40 ? 'normal' : 'weak' }
    })
  }))
}
</script>

<style scoped lang="scss">
.learning-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

// 专项训练
.training-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.subject-card {
  text-align: center;
  padding: 8px;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-3px);
  }
}

.subject-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.subject-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.subject-progress {
  margin-bottom: 4px;
}

.subject-mastery {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-bottom: 10px;

  .mastery-label { font-size: 12px; color: #909399; }
  .mastery-value { font-size: 14px; font-weight: 600; color: #303133; }
}

.start-btn {
  width: 100%;
}

// 错题
.error-card { border-radius: 12px; }
.error-list { display: flex; flex-direction: column; gap: 0; }

.error-item {
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}

.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.error-subject-tag {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 4px;
  color: #fff;
  background: #909399;

  &.数学 { background: #f56c6c; }
  &.英语 { background: #409eff; }
  &.语文 { background: #67c23a; }
  &.历史 { background: #e6a23c; }
  &.政治 { background: #9c27b0; }
  &.地理 { background: #00bcd4; }
}

.error-knowledge { font-size: 13px; color: #606266; }
.error-question { font-size: 14px; color: #303133; margin-bottom: 6px; line-height: 1.5; }

.error-analysis {
  font-size: 13px;
  color: #909399;
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  line-height: 1.5;

  .analysis-label { color: #409eff; font-weight: 500; }
}

.error-actions {
  display: flex;
  gap: 8px;
}

// 知识点图谱
.knowledge-card { border-radius: 12px; }

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;

  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

.knowledge-subject {
  .ks-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 12px;
    padding-bottom: 6px;
    border-bottom: 2px solid #409eff;
  }
}

.ks-points {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ks-point-name {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
</style>
