<template>
  <div class="login-container">
    <div class="login-left">
      <div class="brand">
        <div class="logo">
          <el-icon size="48"><ChatDotRound /></el-icon>
        </div>
        <h1>SmartAgent</h1>
        <p>Intelligent Customer Service Platform</p>
      </div>
      <div class="features">
        <div class="feature-item">
          <el-icon><Service /></el-icon>
          <span>AI-Powered Chat</span>
        </div>
        <div class="feature-item">
          <el-icon><User /></el-icon>
          <span>Human Handoff</span>
        </div>
        <div class="feature-item">
          <el-icon><Document /></el-icon>
          <span>Knowledge Base</span>
        </div>
        <div class="feature-item">
          <el-icon><DataAnalysis /></el-icon>
          <span>Analytics Dashboard</span>
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="login-form-container">
        <h2>Welcome Back</h2>
        <p class="subtitle">Sign in to your account</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="Username"
              prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="Password"
              prefix-icon="Lock"
              show-password
              @keydown.enter="handleLogin"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="form.rememberMe">Remember me</el-checkbox>
            <a href="#" class="forgot-link">Forgot password?</a>
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            Sign In
          </el-button>
        </el-form>

        <div class="demo-accounts">
          <p>Demo Accounts:</p>
          <div class="demo-buttons">
            <el-button size="small" @click="fillDemo('admin')">Admin</el-button>
            <el-button size="small" @click="fillDemo('agent')">Agent</el-button>
            <el-button size="small" @click="fillDemo('customer')">Customer</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ChatDotRound, Service, User, Document, DataAnalysis } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  rememberMe: false,
})

const rules = {
  username: [{ required: true, message: 'Please enter username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter password', trigger: 'blur' }],
}

function fillDemo(type: string) {
  const accounts: Record<string, { username: string; password: string }> = {
    admin: { username: 'admin', password: 'admin123' },
    agent: { username: 'agent', password: 'agent123' },
    customer: { username: 'customer', password: 'customer123' },
  }
  form.username = accounts[type].username
  form.password = accounts[type].password
}

async function handleLogin() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.login(form.username, form.password)
      ElMessage.success('Login successful')
      router.push('/admin')
    } catch (error: any) {
      ElMessage.error(error.message || 'Login failed')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  min-height: 100vh;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  color: #fff;

  .brand {
    text-align: center;
    margin-bottom: 60px;

    .logo {
      width: 80px;
      height: 80px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 20px;
    }

    h1 {
      font-size: 36px;
      margin: 0 0 8px;
    }

    p {
      font-size: 18px;
      opacity: 0.9;
      margin: 0;
    }
  }

  .features {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
    max-width: 400px;

    .feature-item {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 16px;
      background: rgba(255, 255, 255, 0.1);
      padding: 16px 20px;
      border-radius: 12px;

      .el-icon {
        font-size: 24px;
      }
    }
  }
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.login-form-container {
  width: 100%;
  max-width: 400px;
  padding: 40px;

  h2 {
    font-size: 28px;
    margin: 0 0 8px;
    color: #303133;
  }

  .subtitle {
    color: #909399;
    margin: 0 0 32px;
  }

  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .forgot-link {
      color: #409eff;
      text-decoration: none;
      font-size: 14px;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .login-button {
    width: 100%;
    height: 48px;
    font-size: 16px;
  }

  .demo-accounts {
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid #f0f0f0;
    text-align: center;

    p {
      color: #909399;
      font-size: 13px;
      margin: 0 0 12px;
    }

    .demo-buttons {
      display: flex;
      gap: 8px;
      justify-content: center;
    }
  }
}
</style>
