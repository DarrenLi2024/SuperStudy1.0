<template>
  <div class="page-header">
    <div class="header-left">
      <span class="logo-text">🎓 AI升学陪伴</span>
    </div>
    <div class="header-center">
      <el-menu mode="horizontal" :ellipsis="false" :default-active="activeMenu" @select="handleMenuSelect">
        <el-menu-item index="/student">首页</el-menu-item>
        <el-menu-item index="/student/learning">学习中心</el-menu-item>
        <el-menu-item index="/student/exam">模考中心</el-menu-item>
        <el-menu-item index="/student/growth">成长数据</el-menu-item>
      </el-menu>
    </div>
    <div class="header-right">
      <span class="user-name">{{ nickname || '学生用户' }}</span>
      <el-button type="primary" link @click="handleLogout">退出</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserInfo, clearAuth } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const emit = defineEmits<{ (e: 'logout'): void }>()

const userInfo = computed(() => getUserInfo())
const nickname = computed(() => userInfo.value?.nickname || '')
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

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
  white-space: nowrap;
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
