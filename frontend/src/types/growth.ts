export interface CollegeInfo {
  id: number
  name: string
  logo: string
  batch: string
}

export interface SubjectGap {
  subject: string
  gap: number
}

export interface DreamCollegeInfo {
  name: string
  logo: string
  batch: string
  scoreGap: number
  subjectGaps: SubjectGap[]
  aiIncentive: string
}

export interface CollegeCardData {
  currentBatchCards: CollegeInfo[]
  targetBatchCards: CollegeInfo[]
  dreamCollege: DreamCollegeInfo
  currentBatch: string
  currentScore: number
}

export interface Phase {
  name: string
  startScore: number
  endScore: number
  currentScore: number
  progress: number
  completed: boolean
}

export interface GrowthProgressData {
  phases: Phase[]
  totalProgress: number
  targetScore: number
}

export interface TaskItem {
  id: number
  type: string
  content: string
  subject: string
  knowledgePoint: string
  aiHint: string
  completionRate: number
  status: string
}

export interface TodayTaskData {
  tasks: TaskItem[]
  aiComment: string
  completionRate: number
}

export interface ExamRecordItem {
  id: number
  examType: string
  subjectScores: string
  totalScore: number
  equivalentGaokaoScore: number
  equivalentRank: number
  currentBatch: string
  examDate: string
  aiDiagnosisReport: string
  createdAt: string
}

export interface GrowthRecordItem {
  id: number
  studentId: number
  previousBatch: string
  currentBatch: string
  scoreAtUpgrade: number
  upgradeTime: string
  aiIncentiveText: string
}

export interface ScoreTrend {
  date: string
  score: number
}

export interface RankTrend {
  date: string
  rank: number
}

export interface GrowthData {
  scoreTrend: ScoreTrend[]
  rankTrend: RankTrend[]
  subjectTrends: Record<string, { date: string; score: number }[]>
  monthlyReport: string
}
