<template>
  <div class="page-header">
    <div class="header-left">
      <span class="logo-text">🎓 AI升学陪伴</span>
      <span v-if="roleBadge" class="role-badge">{{ roleBadge }}</span>
    </div>
    <div class="header-center">
      <el-menu mode="horizontal" :ellipsis="false" :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </div>
    <div class="header-right">
      <span class="user-name">{{ nickname || defaultNickname }}</span>
      <el-button type="primary" link @click="handleLogout">退出</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserInfo, clearAuth, getRole } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const userInfo = computed(() => getUserInfo())
const nickname = computed(() => userInfo.value?.nickname || '')
const role = computed(() => getRole())

const defaultNickname = computed(() => {
  switch (role.value) {
    case 'admin': return '管理员'
    case 'parent': return '家长'
    default: return '学生用户'
  }
})

const roleBadge = computed(() => {
  switch (role.value) {
    case 'parent': return '家长端'
    case 'admin': return '管理端'
    default: return ''
  }
})

const menuItems = computed(() => {
  switch (role.value) {
    case 'admin':
      return [{ path: '/admin', label: '用户管理' }]
    case 'parent':
      return [{ path: '/parent', label: '首页' }]
    default:
      return [
        { path: '/student', label: '首页' },
        { path: '/student/learning', label: '学习中心' },
        { path: '/student/exam', label: '模考中心' },
        { path: '/student/growth', label: '成长数据' }
      ]
  }
})

const activeMenu = computed(() => route.path)

const handleMenuSelect = (path: string) => {
  router.push(path)
}

const handleLogout = () => {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
  white-space: nowrap;
}

.role-badge {
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  padding: 2px 8px;
  border-radius: 4px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;

  :deep(.el-menu--horizontal) {
    border-bottom: none;

    .el-menu-item {
      height: 60px;
      line-height: 60px;
      font-size: 14px;
      font-weight: 500;
      padding: 0 20px;

      &.is-active {
        color: #409eff;
        border-bottom: 2px solid #409eff;
      }
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  color: #606266;
}
</style>
