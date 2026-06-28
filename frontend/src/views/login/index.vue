<template>
  <div class="login-page">
    <div class="login-container">
      <h1 class="login-title">高中全科AI升学成长陪伴系统</h1>
      <p class="login-subtitle">AI原生全自动学情陪伴 + 智能段位成长系统</p>
      <el-form :model="form" :rules="rules" ref="loginForm" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>全链路AI自治 · 零人工运维 · 精准段位激励</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { setToken, setUserInfo } from '@/utils/auth'
import { login } from '@/api/user'

const router = useRouter()
const loginForm = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginForm.value) return
  await loginForm.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 先尝试调用后端 API（后端运行时会走这里）
        const res = await login({
          username: form.username,
          password: form.password
        })
        const { token, user } = res.data
        doLogin(token, user)
      } catch (error: any) {
        ElMessage.error(error?.message || '账号或密码错误')
      } finally {
        loading.value = false
      }
    }
  })
}

function doLogin(token: string, user: any) {
  setToken(token)
  setUserInfo({
    id: user.id,
    username: user.username,
    role: user.role,
    studentId: user.studentId || null,
    nickname: user.role === 'admin' ? '管理员' : user.role === 'parent' ? '家长' : '学生用户'
  })
  ElMessage.success('登录成功')
  if (user.role === 'admin') {
    router.push('/admin')
  } else if (user.role === 'parent') {
    router.push('/parent')
  } else {
    router.push('/student')
  }
}
</script>

<style scoped lang="scss">
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-title {
  text-align: center;
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: #999;
  margin-bottom: 32px;
}

.login-form {
  margin-bottom: 20px;
}

.login-tips {
  text-align: center;
  font-size: 12px;
  color: #999;
  p {
    margin: 4px 0;
  }
}
</style>
