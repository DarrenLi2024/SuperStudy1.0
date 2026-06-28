<template>
  <div class="admin-page">
    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <span class="logo">🎓 管理后台</span>
          <span class="role-badge">管理员</span>
        </div>
        <div class="header-right">
          <span class="user-name">{{ userInfo?.nickname || '管理员' }}</span>
          <el-button type="primary" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-container class="admin-body">
        <el-aside width="200px" class="admin-sidebar">
          <el-menu :default-active="activeMenu" @select="handleMenuSelect" router>
            <el-menu-item index="user-manage">
              <el-icon><User /></el-icon>
              <span>账号管理</span>
            </el-menu-item>
            <el-menu-item index="system-monitor">
              <el-icon><Monitor /></el-icon>
              <span>系统监控</span>
            </el-menu-item>
            <el-menu-item index="ai-config">
              <el-icon><Setting /></el-icon>
              <span>AI参数配置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="admin-main">
          <!-- 账号管理 -->
          <div v-if="activeMenu === 'user-manage'" class="admin-section">
            <div class="section-header">
              <h3>用户账号管理</h3>
              <el-button type="primary" @click="showCreateDialog = true">+ 新增用户</el-button>
            </div>

            <el-table :data="userList" stripe style="width: 100%">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="username" label="账号" min-width="120" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'parent' ? 'warning' : 'primary'" size="small">
                    {{ row.role === 'admin' ? '管理员' : row.role === 'parent' ? '家长' : '学生' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="studentId" label="绑定学生ID" width="110" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="160">
                <template #default="{ row }">
                  {{ row.createdAt?.substring(0, 16) || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'"
                    @click="toggleStatus(row)">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                  <el-button size="small" type="primary" link @click="resetPwd(row)">重置密码</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="prev, pager, next" @current-change="loadUsers" />
            </div>
          </div>

          <!-- 系统监控 -->
          <div v-if="activeMenu === 'system-monitor'" class="admin-section">
            <h3>系统监控</h3>
            <div class="monitor-grid">
              <el-card class="monitor-card">
                <div class="monitor-label">CPU使用率</div>
                <el-progress :percentage="monitor.serverInfo?.cpuUsage || 0" :stroke-width="12" />
              </el-card>
              <el-card class="monitor-card">
                <div class="monitor-label">内存使用率</div>
                <el-progress :percentage="monitor.serverInfo?.memoryUsage || 0" :stroke-width="12" :status="(monitor.serverInfo?.memoryUsage || 0) > 80 ? 'exception' : 'success'" />
              </el-card>
              <el-card class="monitor-card">
                <div class="monitor-label">磁盘使用率</div>
                <el-progress :percentage="monitor.serverInfo?.diskUsage || 0" :stroke-width="12" />
              </el-card>
            </div>

            <div class="monitor-status">
              <el-descriptions :column="3" border>
                <el-descriptions-item label="数据库状态">
                  <el-tag :type="monitor.databaseStatus === '正常' ? 'success' : 'danger'" size="small">{{ monitor.databaseStatus }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Redis状态">
                  <el-tag :type="monitor.redisStatus === '正常' ? 'success' : 'danger'" size="small">{{ monitor.redisStatus }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="AI模型">
                  {{ monitor.aiStatus?.modelName || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="上次备份">
                  {{ monitor.backupStatus?.lastBackupTime || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="AI接口状态">
                  {{ monitor.aiStatus?.apiStatus || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="备份状态">
                  <el-tag :type="monitor.backupStatus?.status === '正常' ? 'success' : 'danger'" size="small">{{ monitor.backupStatus?.status }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <!-- AI参数配置 -->
          <div v-if="activeMenu === 'ai-config'" class="admin-section">
            <h3>AI参数配置</h3>
            <el-form label-width="160px" class="config-form">
              <el-divider>分值赛道阈值</el-divider>
              <el-form-item label="低分段上限">
                <el-input-number v-model="aiConfig.scoreTrackThresholds.low" :min="300" :max="700" />
                <span class="form-hint">低于此分数为低分段（默认500）</span>
              </el-form-item>
              <el-form-item label="中分段上限">
                <el-input-number v-model="aiConfig.scoreTrackThresholds.medium" :min="400" :max="750" />
                <span class="form-hint">高于低分且低于此分数为中分段（默认580）</span>
              </el-form-item>
              <el-divider>院校匹配参数</el-divider>
              <el-form-item label="稳妥院校位次差值">
                <el-input-number v-model="aiConfig.safeCollegeRankDiff" :min="1000" :max="20000" :step="1000" />
                <span class="form-hint">等效位次±此范围内为稳妥院校（默认5000）</span>
              </el-form-item>
              <el-divider>学情分析参数</el-divider>
              <el-form-item label="薄弱学科阈值(%)">
                <el-input-number v-model="aiConfig.weakSubjectThreshold" :min="20" :max="80" />
                <span class="form-hint">得分率低于此值为极弱学科（默认40%）</span>
              </el-form-item>
              <el-divider>文案风格</el-divider>
              <el-form-item label="激励文案风格">
                <el-select v-model="aiConfig.incentiveStyle">
                  <el-option label="正向激励" value="正向激励" />
                  <el-option label="温暖鼓励" value="温暖鼓励" />
                  <el-option label="务实理性" value="务实理性" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveAiConfig">保存配置</el-button>
                <el-button @click="resetAiConfig">恢复默认</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-main>
      </el-container>
    </el-container>

    <!-- 创建用户弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新增用户" width="420px">
      <el-form :model="createForm" ref="createFormRef" label-width="80px">
        <el-form-item label="账号" prop="username" :rules="[{ required: true, message: '请输入账号' }]">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password" :rules="[{ required: true, message: '请输入密码' }]">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role" :rules="[{ required: true, message: '请选择角色' }]">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="学生" value="student" />
            <el-option label="家长" value="parent" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定学生" v-if="createForm.role === 'parent'">
          <el-input-number v-model="createForm.studentId" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="doCreateUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { User, Monitor, Setting } from '@element-plus/icons-vue'
import { getUserInfo, clearAuth } from '@/utils/auth'
import { getUserList, createUser, updateUserStatus, resetPassword } from '@/api/admin'
import { getMonitor, getAiConfig, updateAiConfig } from '@/api/admin'

const router = useRouter()
const userInfo = computed(() => getUserInfo())
const activeMenu = ref('user-manage')

// 用户管理
const userList = ref<any[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ username: '', password: '', role: 'student', studentId: 0 })

// 监控
const monitor = ref<any>({
  serverInfo: { cpuUsage: 0, memoryUsage: 0, diskUsage: 0 },
  databaseStatus: '-', redisStatus: '-', backupStatus: {}, aiStatus: {}, apiStats: []
})

// AI配置
const aiConfig = reactive({
  scoreTrackThresholds: { low: 500, medium: 580 },
  safeCollegeRankDiff: 5000,
  weakSubjectThreshold: 40,
  incentiveStyle: '正向激励'
})

onMounted(async () => {
  loadUsers()
  loadMonitor()
  loadAiConfig()
})

const handleMenuSelect = (index: string) => {
  activeMenu.value = index
  if (index === 'user-manage') loadUsers()
  if (index === 'system-monitor') loadMonitor()
  if (index === 'ai-config') loadAiConfig()
}

// 用户管理
async function loadUsers() {
  try {
    const res = await getUserList({ page: page.value, size: size.value })
    userList.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (error: any) {
    userList.value = []
    total.value = 0
    ElMessage.error(error?.message || '用户列表加载失败')
  }
}

async function doCreateUser() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await createUser(createForm)
    ElMessage.success('用户创建成功')
    showCreateDialog.value = false
    createForm.username = ''; createForm.password = ''; createForm.role = 'student'; createForm.studentId = 0
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error?.message || '用户创建失败')
  }
}

async function toggleStatus(row: any) {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await updateUserStatus(row.id, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadUsers()
  } catch (error: any) {
    ElMessage.error(error?.message || '状态更新失败')
  }
}

async function resetPwd(row: any) {
  try {
    const res = await resetPassword(row.id)
    ElMessage.success(`密码已重置为：${res.data.newPassword}`)
  } catch (error: any) {
    ElMessage.error(error?.message || '密码重置失败')
  }
}

// 监控
async function loadMonitor() {
  try {
    const res = await getMonitor()
    monitor.value = res.data
  } catch (error: any) {
    ElMessage.error(error?.message || '监控数据加载失败')
  }
}

// AI配置
async function loadAiConfig() {
  try {
    const res = await getAiConfig()
    const data = res.data
    Object.assign(aiConfig, data)
  } catch {
    // 保持默认
  }
}

async function saveAiConfig() {
  try {
    await updateAiConfig({ ...aiConfig })
    ElMessage.success('配置保存成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '配置保存失败')
  }
}

function resetAiConfig() {
  aiConfig.scoreTrackThresholds = { low: 500, medium: 580 }
  aiConfig.safeCollegeRankDiff = 5000
  aiConfig.weakSubjectThreshold = 40
  aiConfig.incentiveStyle = '正向激励'
  ElMessage.success('已恢复默认配置')
}

const handleLogout = () => {
  clearAuth()
  router.push('/login')
}

</script>

<style scoped lang="scss">
.admin-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .header-left { display: flex; align-items: center; gap: 8px; }
  .logo { font-size: 18px; font-weight: 700; color: #409eff; }
  .role-badge { font-size: 12px; color: #f56c6c; background: #fef0f0; padding: 2px 8px; border-radius: 4px; }
  .header-right { display: flex; align-items: center; gap: 12px; }
  .user-name { font-size: 14px; color: #606266; }
}

.admin-body {
  height: calc(100vh - 60px);
}

.admin-sidebar {
  background: #fff;
  border-right: 1px solid #e8e8e8;
}

.admin-main {
  padding: 20px;
  overflow-y: auto;
}

.admin-section {
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

// 监控
.monitor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.monitor-card {
  .monitor-label {
    font-size: 14px;
    color: #606266;
    margin-bottom: 12px;
  }
}

.monitor-status {
  margin-top: 16px;
}

// AI配置
.config-form {
  max-width: 640px;
  .form-hint {
    font-size: 12px;
    color: #909399;
    margin-left: 12px;
  }
}
</style>
