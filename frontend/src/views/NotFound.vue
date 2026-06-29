<template>
  <div class="not-found-page">
    <div class="nf-content">
      <div class="nf-icon">🔍</div>
      <h1>404</h1>
      <p>页面不存在或已被移除</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { getRole } from '@/utils/auth'

const router = useRouter()

const ROLE_REDIRECT: Record<string, string> = {
  student: '/student',
  parent: '/parent',
  admin: '/admin'
}

function goHome() {
  const role = getRole()
  router.push(ROLE_REDIRECT[role] || '/login')
}
</script>

<style scoped>
.not-found-page {
  width: 100vw; height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: #f5f7fa;
}
.nf-content { text-align: center; }
.nf-icon { font-size: 64px; margin-bottom: 16px; }
h1 { font-size: 72px; color: #dcdfe6; margin: 0 0 8px; }
p { font-size: 16px; color: #909399; margin-bottom: 24px; }
</style>
