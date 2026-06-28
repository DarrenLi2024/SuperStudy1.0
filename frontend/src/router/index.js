import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken, getRole } from '@/utils/auth'

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
    redirect: '/login'
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

  if (to.meta.requiresAuth === false) {
    if (token && role) {
      if (role === 'student') {
        next('/student')
      } else if (role === 'parent') {
        next('/parent')
      } else if (role === 'admin') {
        next('/admin')
      } else {
        next()
      }
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else {
      if (to.meta.roles && to.meta.roles.length > 0) {
        if (to.meta.roles.includes(role)) {
          next()
        } else {
          if (role === 'student') {
            next('/student')
          } else if (role === 'parent') {
            next('/parent')
          } else if (role === 'admin') {
            next('/admin')
          } else {
            next('/login')
          }
        }
      } else {
        next()
      }
    }
  }
})

export default router
