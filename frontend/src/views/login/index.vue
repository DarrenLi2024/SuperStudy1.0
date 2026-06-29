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
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" style="flex:1" maxlength="4" />
            <div class="captcha-img" @click="refreshCaptcha" :title="'点击刷新验证码'">
              <canvas ref="captchaCanvasRef" width="120" height="44"></canvas>
            </div>
          </div>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { setToken, setUserInfo } from '@/utils/auth'
import { login } from '@/api/user'

const router = useRouter()
const loginForm = ref<FormInstance>()
const loading = ref(false)
const captchaCanvasRef = ref<HTMLCanvasElement>()
const captchaText = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value && value.toUpperCase() !== captchaText.value.toUpperCase()) {
          callback(new Error('验证码错误'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 生成随机验证码
const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars[Math.floor(Math.random() * chars.length)]
  }
  captchaText.value = code
  return code
}

// 绘制验证码到Canvas
const drawCaptcha = (code: string) => {
  nextTick(() => {
    const canvas = captchaCanvasRef.value
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // 背景
    ctx.fillStyle = '#f0f2f5'
    ctx.fillRect(0, 0, 120, 44)

    // 干扰线
    for (let i = 0; i < 4; i++) {
      ctx.strokeStyle = `rgba(${Math.random() * 200},${Math.random() * 200},${Math.random() * 200},0.5)`
      ctx.beginPath()
      ctx.moveTo(Math.random() * 120, Math.random() * 44)
      ctx.lineTo(Math.random() * 120, Math.random() * 44)
      ctx.stroke()
    }

    // 干扰点
    for (let i = 0; i < 30; i++) {
      ctx.fillStyle = `rgba(${Math.random() * 255},${Math.random() * 255},${Math.random() * 255},0.5)`
      ctx.fillRect(Math.random() * 120, Math.random() * 44, 1, 1)
    }

    // 文字
    for (let i = 0; i < code.length; i++) {
      ctx.font = `${20 + Math.random() * 6}px bold Arial`
      ctx.fillStyle = `rgb(${Math.random() * 100},${Math.random() * 100},${Math.random() * 150})`
      ctx.save()
      ctx.translate(20 + i * 24, 28 + Math.random() * 6 - 3)
      ctx.rotate((Math.random() - 0.5) * 0.4)
      ctx.fillText(code[i], 0, 0)
      ctx.restore()
    }
  })
}

const refreshCaptcha = () => {
  const code = generateCaptcha()
  drawCaptcha(code)
  form.captchaCode = ''
}

onMounted(() => {
  refreshCaptcha()
})

const handleLogin = async () => {
  if (!loginForm.value) return
  await loginForm.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login({
          username: form.username,
          password: form.password
        })
        const { token, user } = res.data
        doLogin(token, user)
      } catch (error: any) {
        refreshCaptcha()
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

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-img {
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  transition: border-color 0.2s;
  &:hover { border-color: #409eff; }
  canvas { display: block; }
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
