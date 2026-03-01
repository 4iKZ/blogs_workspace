<template>
  <div class="reset-password-page">
    <div class="reset-password-container">
      <div class="reset-password-header">
        <h2>重置密码</h2>
      </div>
      <div class="reset-password-form">
        <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="100px">
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-model="resetForm.email" 
              placeholder="请输入注册邮箱"
              type="email"
            />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-container">
              <el-input 
                v-model="resetForm.code" 
                placeholder="请输入邮箱验证码"
                maxlength="6"
              />
              <el-button 
                type="primary" 
                :disabled="codeSendDisabled"
                @click="sendCode"
                :loading="sendingCode"
              >
                {{ codeButtonText }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input 
              v-model="resetForm.newPassword" 
              type="password" 
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input 
              v-model="resetForm.confirmPassword" 
              type="password" 
              placeholder="请再次输入新密码"
              show-password
              @keyup.enter="handleResetPassword"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleResetPassword" :loading="loading" class="reset-btn">
              重置密码
            </el-button>
            <el-button type="default" @click="navigateToLogin">
              返回登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/composables/useLuminaToast'
import { authService } from '../services/authService'

const router = useRouter()
const resetFormRef = ref()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

// 重置密码表单
const resetForm = ref({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== resetForm.value.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const resetRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 发送验证码按钮文本
const codeButtonText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}秒后重试` : '发送验证码'
})

// 发送验证码按钮是否禁用
const codeSendDisabled = computed(() => {
  return countdown.value > 0 || !resetForm.value.email
})

// 发送验证码
const sendCode = async () => {
  // 验证邮箱
  try {
    await resetFormRef.value.validateField('email')
  } catch (error) {
    return
  }

  try {
    sendingCode.value = true
    await authService.sendResetCode({ email: resetForm.value.email })
    toast.success('验证码已发送到您的邮箱')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    console.error('发送验证码失败:', error)
    toast.error(error.response?.data?.message || '发送验证码失败')
  } finally {
    sendingCode.value = false
  }
}

// 处理重置密码
const handleResetPassword = async () => {
  try {
    // 表单验证
    await resetFormRef.value.validate()
    
    loading.value = true
    
    // 发送重置密码请求
    await authService.resetPassword({
      email: resetForm.value.email,
      code: resetForm.value.code,
      newPassword: resetForm.value.newPassword
    })
    
    toast.success('密码重置成功，请使用新密码登录')
    
    // 跳转到登录页面
    router.push('/login')
  } catch (error: any) {
    console.error('重置密码失败:', error)
    toast.error(error.response?.data?.message || '重置密码失败')
  } finally {
    loading.value = false
  }
}

// 跳转到登录页面
const navigateToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.reset-password-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

.reset-password-container {
  width: 100%;
  max-width: 500px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 30px;
}

.reset-password-header {
  text-align: center;
  margin-bottom: 30px;
}

.reset-password-header h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.reset-password-form {
  width: 100%;
}

.reset-btn {
  width: 100%;
  margin-bottom: 12px;
}

.code-container {
  display: flex;
  gap: 12px;
}

.code-container .el-input {
  flex: 1;
}

.code-container .el-button {
  width: 120px;
  flex-shrink: 0;
}
</style>
