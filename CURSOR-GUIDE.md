# Cursor 专属开发指南

> 前端开发师角色 - 负责前端页面开发和用户交互实现

---

## 一、角色定位

**角色**：前端开发师

**擅长领域**：前端开发、UI组件、交互实现

**负责模块**：前端页面开发、组件封装

---

## 二、负责任务

| 任务ID | 任务名称 | 优先级 | 依赖任务 |
|--------|---------|-------|---------|
| TASK-FE01 | 登录统一页面 | high | TASK-INIT, TASK-BE01 |
| TASK-FE02 | 学生首页（核心页面） | high | TASK-FE01, TASK-BE02, TASK-BE03, TASK-BE05, TASK-AI02, TASK-AI05 |
| TASK-FE03 | AI智能学习中心 | high | TASK-FE01, TASK-BE04, TASK-AI03 |
| TASK-FE04 | 模考中心 | high | TASK-FE01, TASK-BE03 |
| TASK-FE05 | 成长数据页 | high | TASK-FE01, TASK-BE05 |
| TASK-FE06 | 家长端只读监督页 | medium | TASK-FE02 |
| TASK-FE07 | 管理后台 | medium | TASK-BE01, TASK-BE02 |

---

## 三、技术栈规范

### 3.1 前端技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| Vue | 3.4.x | 主框架 |
| Vite | 4.5.x | 构建工具 |
| Element Plus | 2.x | PC端UI组件 |
| Vant | 4.x | 移动端UI组件 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理 |
| Axios | 1.x | HTTP请求 |
| ECharts | 5.x | 数据可视化 |

### 3.2 前端目录结构

```
frontend/src/
├── main.ts                      # 入口文件
├── App.vue                      # 根组件
├── router/
│   └── index.js                 # 路由配置
├── api/
│   ├── user.js                  # 用户接口
│   ├── student.js               # 学生接口
│   ├── exam.js                  # 模考接口
│   ├── learning.js              # 学习接口
│   └── growth.js                # 成长接口
├── utils/
│   ├── request.js               # Axios封装
│   └── auth.js                  # Token管理
├── stores/
│   └── user.js                  # 用户状态
├── views/
│   ├── login/index.vue          # 登录页
│   ├── student/
│   │   ├── home/index.vue       # 学生首页
│   │   ├── learning/index.vue   # 学习中心
│   │   ├── exam/index.vue       # 模考中心
│   │   └── growth/index.vue     # 成长数据
│   ├── parent/
│   │   └── home/index.vue       # 家长端
│   └── admin/
│       └── index.vue            # 管理后台
├── components/
│   ├── CollegeCard.vue          # 院校卡片
│   ├── GrowthProgress.vue       # 段位进度条
│   ├── QuestionCard.vue         # 题目卡片
│   └── KnowledgeHeatmap.vue     # 知识点热力图
└── styles/
    └── index.scss               # 全局样式
```

---

## 四、开发规范

### 4.1 命名规范

| 类型 | 规范 | 示例 |
|-----|------|-----|
| 组件名 | PascalCase | `CollegeCard.vue` |
| 方法名 | camelCase | `handleLogin` |
| 变量名 | camelCase | `userInfo` |
| 常量名 | UPPER_SNAKE_CASE | `BASE_URL` |
| 文件目录 | lowercase | `views/student/home/` |

### 4.2 组件模板

#### 页面组件模板

```vue
<template>
  <div class="student-home">
    <el-header>学生首页</el-header>
    <el-main>
      <CollegeCard />
      <GrowthProgress />
    </el-main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import CollegeCard from '@/components/CollegeCard.vue'
import GrowthProgress from '@/components/GrowthProgress.vue'
import { getGrowthData } from '@/api/growth'

const growthData = ref(null)

onMounted(async () => {
  const res = await getGrowthData()
  growthData.value = res.data
})
</script>

<style scoped>
.student-home {
  min-height: 100vh;
}
</style>
```

#### 公共组件模板

```vue
<template>
  <div class="college-card">
    <div class="college-logo">🏛️</div>
    <div class="college-info">
      <h3>{{ college.name }}</h3>
      <p>{{ college.batch }}</p>
    </div>
    <div class="college-score">
      <span>目标分：{{ college.targetScore }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  college: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.college-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
</style>
```

### 4.3 API接口封装

```javascript
import request from '@/utils/request'

export const getUserInfo = () => {
  return request({
    url: '/api/v1/sys/info',
    method: 'get'
  })
}

export const login = (data) => {
  return request({
    url: '/api/v1/sys/login',
    method: 'post',
    data
  })
}
```

### 4.4 路由配置规范

```javascript
import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/student',
    name: 'Student',
    component: () => import('@/views/student/home/index.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/parent',
    name: 'Parent',
    component: () => import('@/views/parent/home/index.vue'),
    meta: { requiresAuth: true, roles: ['PARENT'] }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/index.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
```

---

## 五、最佳实践

### 5.1 性能规范

- ✅ 使用按需加载（懒加载）
- ✅ 减少不必要的渲染
- ✅ 合理使用computed和watch
- ✅ 图片懒加载

### 5.2 用户体验

- ✅ 加载状态展示
- ✅ 错误提示友好
- ✅ 响应式布局
- ✅ 动画流畅

### 5.3 安全规范

- ✅ Token存储安全
- ✅ 接口请求携带Token
- ✅ 路由权限控制
- ✅ XSS防护

---

## 六、验收标准

### 6.1 功能验收

- [ ] 页面功能完整
- [ ] 接口调用正确
- [ ] 数据展示准确
- [ ] 交互流畅

### 6.2 技术验收

- [ ] 代码结构规范
- [ ] 组件封装合理
- [ ] 路由配置正确
- [ ] 样式规范统一

---

## 七、参考文档

| 文档 | 用途 |
|-----|------|
| [AI快捷执行手册.md](AI快捷执行手册.md) | 任务卡片和执行指南 |
| [项目目录结构规范.md](项目目录结构规范.md) | 目录结构和命名规范 |
| [API接口契约规范.md](API接口契约规范.md) | 接口定义和Mock数据策略 |