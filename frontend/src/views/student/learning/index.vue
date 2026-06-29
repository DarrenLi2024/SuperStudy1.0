<template>
  <div class="learning-page">
    <Header />

    <div class="main-content">
      <!-- AI思考状态 -->
      <AiLoading v-if="loading"
        text="AI 正在生成你的专属学习方案..."
        :streaming="aiStreaming"
        :streamText="aiStreamText"
        :streamFinished="aiStreamFinished" />

      <!-- AI个性化专项训练 -->
      <section class="section" v-if="!loading">
        <h3 class="section-title">📝 AI个性化专项训练</h3>
        <div class="training-grid">
          <el-card v-for="subject in subjects" :key="subject.name" shadow="hover"
            class="subject-card" @click="startTraining(subject.name)">
            <div class="subject-icon">{{ subject.icon }}</div>
            <div class="subject-name">{{ subject.name }}</div>
            <div class="subject-progress">
              <el-progress :percentage="subject.mastery"
                :color="subject.mastery >= 70 ? '#67c23a' : subject.mastery >= 40 ? '#e6a23c' : '#f56c6c'"
                :stroke-width="6" />
            </div>
            <div class="subject-mastery">
              <span class="mastery-label">掌握度</span>
              <span class="mastery-value">{{ subject.mastery }}%</span>
            </div>
            <el-button type="primary" size="small" round class="start-btn" :loading="trainingLoading === subject.name">
              {{ trainingLoading === subject.name ? 'AI出题中...' : '开始训练' }}
            </el-button>
          </el-card>
        </div>
      </section>

      <!-- AI错题智能复盘 -->
      <section class="section" v-if="!loading">
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
                <span class="analysis-label">🤖 AI诊断：</span>
                {{ err.aiAnalysis }}
              </div>
              <div class="error-actions" v-if="err.reinforcementFlag === 0">
                <el-button size="small" type="primary" link @click="doReinforcement(err)">
                  同类补强
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无错题记录，继续保持！">
            <template #image>
              <div style="font-size:48px">🎉</div>
            </template>
          </el-empty>
        </el-card>
      </section>

      <!-- 知识点掌握图谱 -->
      <section class="section" v-if="!loading">
        <h3 class="section-title">📊 AI知识点掌握图谱</h3>
        <el-card shadow="never" class="knowledge-card">
          <KnowledgeHeatmap v-if="knowledgeData.length > 0" :data="knowledgeData" />
          <div v-else class="empty-knowledge">
            <span class="empty-icon">🔍</span>
            <span>AI正在分析你的知识点掌握情况，请先完成一次模考</span>
          </div>
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
import KnowledgeHeatmap from '@/components/KnowledgeHeatmap.vue'
import { getErrorQuestions, getKnowledgeStatus } from '@/api/learning'
import { getTrainingQuestions, getReinforcementQuestions } from '@/api/question'
import { getStudentId } from '@/utils/auth'
import { useAI } from '@/composables/useAI'
import { useRouter } from 'vue-router'

const router = useRouter()

const {
  streaming: aiStreaming,
  streamText: aiStreamText,
  streamFinished: aiStreamFinished,
  generate,
  reset: resetAI
} = useAI()

const loading = ref(true)
const trainingLoading = ref<string | null>(null)

interface SubjectMastery { name: string; icon: string; mastery: number }
interface ErrorItem { id: number; subject: string; knowledgePoint: string; questionContent: string; wrongAnswer: string; aiAnalysis: string; reinforcementFlag: number; createdAt: string }
interface KnowledgeSubject { subject: string; knowledgePoints: { name: string; masteryLevel: number; status: string }[] }

const subjects = ref<SubjectMastery[]>([])
const errors = ref<ErrorItem[]>([])
const knowledgeData = ref<KnowledgeSubject[]>([])

onMounted(async () => {
  try {
    const studentId = requireStudentId()
    if (!studentId) return

    const [errRes, knowRes] = await Promise.allSettled([
      getErrorQuestions(studentId),
      getKnowledgeStatus(studentId)
    ])

    if (errRes.status === 'fulfilled') {
      errors.value = (errRes.value as any).data || []
    }
    if (knowRes.status === 'fulfilled') {
      const raw = (knowRes.value as any).data || {}
      knowledgeData.value = Object.entries(raw).map(([subject, points]: [string, any]) => ({
        subject,
        knowledgePoints: Array.isArray(points)
          ? points.map((p: any) => ({
              name: p.name || p.knowledgePoint,
              masteryLevel: p.masteryLevel || p.mastery || 0,
              status: p.status || 'normal'
            }))
          : []
      }))
    }

    subjects.value = knowledgeData.value.map((item) => ({
      name: item.subject,
      icon: subjectIcon(item.subject),
      mastery: item.knowledgePoints.length > 0
        ? Math.round(item.knowledgePoints.reduce((sum, p) => sum + p.masteryLevel, 0) / item.knowledgePoints.length)
        : 0
    }))

  } catch (error: any) {
    ElMessage.error(error?.message || '学习数据加载失败')
  } finally {
    loading.value = false
  }
})

const startTraining = async (subject: string) => {
  trainingLoading.value = subject
  try {
    const studentId = getStudentId()
    if (!studentId) return
    const res = await getTrainingQuestions(studentId, subject, 5)
    const questions = (res as any).data || []
    ElMessage.success(`AI已为你生成${questions.length}道${subject}训练题，请前往模考中心作答`)
  } catch (e: any) {
    ElMessage.error('题目生成失败：' + (e.message || '请稍后重试'))
  } finally {
    trainingLoading.value = null
  }
}

const doReinforcement = async (err: ErrorItem) => {
  try {
    const studentId = getStudentId()
    if (!studentId) return
    const res = await getReinforcementQuestions(studentId, err.knowledgePoint, 5)
    const questions = (res as any).data || []
    if (questions.length > 0) {
      ElMessage.success(`已生成${questions.length}道${err.subject}补强题`)
      // 跳转到模考中心
      router.push('/student/exam')
    } else {
      ElMessage.warning('暂无补强题目，请稍后重试')
    }
  } catch (e: any) {
    ElMessage.error('补强题目生成失败：' + (e.message || '请稍后重试'))
  }
}

function subjectIcon(subject: string) {
  const icons: Record<string, string> = {
    '语文': '📖', '数学': '📐', '英语': '🌍',
    '历史': '🏛️', '政治': '📜', '地理': '🌏',
    '物理': '⚡', '化学': '🧪', '生物': '🧬'
  }
  return icons[subject] || '📌'
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

  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 480px) { grid-template-columns: 1fr; }
}

.subject-card {
  text-align: center;
  padding: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  &:hover { transform: translateY(-3px); }
}

.subject-icon { font-size: 36px; margin-bottom: 8px; }
.subject-name { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.subject-progress { margin-bottom: 4px; }

.subject-mastery {
  display: flex; justify-content: center; gap: 4px; margin-bottom: 10px;
  .mastery-label { font-size: 12px; color: #909399; }
  .mastery-value { font-size: 14px; font-weight: 600; color: #303133; }
}

.start-btn { width: 100%; }

// 错题
.error-card { border-radius: 12px; }
.error-list { display: flex; flex-direction: column; gap: 0; }

.error-item {
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}

.error-header {
  display: flex; align-items: center; gap: 8px; margin-bottom: 6px;
}

.error-subject-tag {
  font-size: 12px; padding: 1px 8px; border-radius: 4px; color: #fff; background: #909399;
  &.数学 { background: #f56c6c; } &.英语 { background: #409eff; }
  &.语文 { background: #67c23a; } &.历史 { background: #e6a23c; }
  &.政治 { background: #9c27b0; } &.地理 { background: #00bcd4; }
  &.物理 { background: #ff7043; } &.化学 { background: #26a69a; } &.生物 { background: #66bb6a; }
}

.error-knowledge { font-size: 13px; color: #606266; }
.error-question { font-size: 14px; color: #303133; margin-bottom: 6px; line-height: 1.5; }

.error-analysis {
  font-size: 13px; color: #606266; background: #f0f4ff;
  padding: 10px 14px; border-radius: 8px; margin-bottom: 8px; line-height: 1.6;
  border-left: 3px solid #409eff;
  .analysis-label { color: #409eff; font-weight: 600; }
}

.error-actions { display: flex; gap: 8px; }

// 知识点图谱
.knowledge-card { border-radius: 12px; }

.empty-knowledge {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 40px 20px; color: #909399; font-size: 14px;
  .empty-icon { font-size: 40px; }
}
</style>
