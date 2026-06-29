/**
 * 学科常量配置
 * 统一管理所有学科相关配置，避免硬编码散落各处
 */

// 全科学科列表（高考6科标准配置）
export const ALL_SUBJECTS = ['语文', '数学', '英语', '历史', '政治', '地理'] as const

// 理科扩展（物理方向）
export const SCIENCE_SUBJECTS = ['语文', '数学', '英语', '物理', '化学', '生物'] as const

// 学科图标映射
export const SUBJECT_ICONS: Record<string, string> = {
  '语文': '📖',
  '数学': '📐',
  '英语': '🌍',
  '历史': '🏛️',
  '政治': '📜',
  '地理': '🌏',
  '物理': '⚡',
  '化学': '🧪',
  '生物': '🧬'
}

// 学科颜色映射（用于图表和标签）
export const SUBJECT_COLORS: Record<string, string> = {
  '语文': '#67c23a',
  '数学': '#f56c6c',
  '英语': '#409eff',
  '历史': '#e6a23c',
  '政治': '#9c27b0',
  '地理': '#00bcd4',
  '物理': '#ff7043',
  '化学': '#26a69a',
  '生物': '#66bb6a'
}

// 学科默认知识点
export const DEFAULT_KNOWLEDGE_POINTS: Record<string, string[]> = {
  '语文': ['文言文阅读', '现代文阅读', '诗歌鉴赏', '作文'],
  '数学': ['函数与导数', '三角函数', '数列', '概率统计', '解析几何'],
  '英语': ['阅读理解', '完形填空', '语法', '写作', '词汇'],
  '历史': ['历史时间轴', '历史事件分析'],
  '政治': ['基本经济制度', '政治生活'],
  '地理': ['自然地理', '人文地理'],
  '物理': ['力学', '电磁学'],
  '化学': ['有机化学', '无机化学'],
  '生物': ['遗传与进化', '细胞生物学']
}

// 学科满分映射
export const SUBJECT_MAX_SCORES: Record<string, number> = {
  '语文': 150,
  '数学': 150,
  '英语': 150,
  '历史': 100,
  '政治': 100,
  '地理': 100,
  '物理': 100,
  '化学': 100,
  '生物': 100
}

/**
 * 获取学科图标
 */
export function getSubjectIcon(subject: string): string {
  return SUBJECT_ICONS[subject] || '📌'
}

/**
 * 获取学科颜色
 */
export function getSubjectColor(subject: string): string {
  return SUBJECT_COLORS[subject] || '#909399'
}

/**
 * 获取学科默认知识点
 */
export function getDefaultKnowledgePoints(subject: string): string[] {
  return DEFAULT_KNOWLEDGE_POINTS[subject] || [`${subject}基础`, `${subject}进阶`]
}

/**
 * 获取学科满分
 */
export function getSubjectMaxScore(subject: string): number {
  return SUBJECT_MAX_SCORES[subject] || 100
}
