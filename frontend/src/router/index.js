import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken, getRole, isTokenExpired, clearAuth } from '@/utils/auth'

const ROLE_REDIRECT_MAP = {
  student: '/student',
  parent: '/parent',
  admin: '/admin'
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/student',
    name: 'StudentHome',
    component: () => import('@/views/student/home/index.vue'),
    meta: { requiresAuth: true, roles: ['student'], title: '学生首页' }
  },
  {
    path: '/student/learning',
    name: 'StudentLearning',
    component: () => import('@/views/student/learning/index.vue'),
    meta: { requiresAuth: true, roles: ['student'], title: 'AI学习中心' }
  },
  {
    path: '/student/exam',
    name: 'StudentExam',
    component: () => import('@/views/student/exam/index.vue'),
    meta: { requiresAuth: true, roles: ['student'], title: '模考中心' }
  },
  {
    path: '/student/growth',
    name: 'StudentGrowth',
    component: () => import('@/views/student/growth/index.vue'),
    meta: { requiresAuth: true, roles: ['student'], title: '成长数据' }
  },
  {
    path: '/parent',
    name: 'ParentHome',
    component: () => import('@/views/parent/home/index.vue'),
    meta: { requiresAuth: true, roles: ['parent'], title: '家长端' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/index.vue'),
    meta: { requiresAuth: true, roles: ['admin'], title: '管理后台' }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { requiresAuth: false, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 高中全科AI升学成长陪伴系统` : '高中全科AI升学成长陪伴系统'

  const token = getToken()
  const role = getRole()

  // Token 过期检测：已登录但 token 过期则清除认证并跳转登录
  if (token && isTokenExpired()) {
    clearAuth()
    if (to.meta.requiresAuth !== false) {
      next('/login')
      return
    }
  }

  const hasValidAuth = token && !isTokenExpired() && role

  if (to.meta.requiresAuth === false) {
    if (hasValidAuth) {
      next(ROLE_REDIRECT_MAP[role] || '/login')
    } else {
      next()
    }
  } else {
    if (!hasValidAuth) {
      next('/login')
    } else {
      if (to.meta.roles && to.meta.roles.length > 0) {
        if (to.meta.roles.includes(role)) {
          next()
        } else {
          next(ROLE_REDIRECT_MAP[role] || '/login')
        }
      } else {
        next()
      }
    }
  }
})

export default router
