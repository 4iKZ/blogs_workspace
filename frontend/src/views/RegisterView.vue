<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <h2>注册</h2>
      </div>
      <div class="register-form">
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="registerForm.username" 
              placeholder="请输入用户名" 
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-model="registerForm.email" 
              type="email" 
              placeholder="请输入邮箱" 
              autocomplete="email"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="请输入密码" 
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input 
              v-model="registerForm.confirmPassword" 
              type="password" 
              placeholder="请确认密码" 
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input 
              v-model="registerForm.nickname" 
              placeholder="请输入昵称" 
              autocomplete="nickname"
            />
          </el-form-item>
          <el-form-item label="职位" prop="position">
            <el-input 
              v-model="registerForm.position" 
              placeholder="请输入职位（选填）" 
            />
          </el-form-item>
          <el-form-item label="公司" prop="company">
            <el-input 
              v-model="registerForm.company" 
              placeholder="请输入公司/单位/学校（选填）" 
            />
          </el-form-item>
          <el-form-item label="简介" prop="bio">
            <el-input 
              v-model="registerForm.bio" 
              type="textarea"
              placeholder="请输入个性签名（选填）" 
              :rows="2"
            />
          </el-form-item>
          <el-form-item label="头像" prop="avatar">
            <el-upload
              class="avatar-uploader"
              action="/api/user/avatar/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              :headers="uploadHeaders"
            >
              <img v-if="registerForm.avatar" :src="registerForm.avatar" class="avatar-preview" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持JPG/PNG格式，不超过2MB</div>
          </el-form-item>
          <el-form-item label="验证码" prop="captcha">
            <div class="captcha-container">
              <el-input 
                v-model="registerForm.captcha" 
                placeholder="请输入验证码"
                maxlength="4"
              />
              <div class="captcha-image" @click="refreshCaptcha">
                <el-image 
                  v-if="captchaImage" 
                  :src="captchaImage" 
                  fit="contain"
                  style="cursor: pointer"
                />
                <el-skeleton v-else animated />
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleRegister" :loading="loading" class="register-btn" size="large">
              注册
            </el-button>
            <div class="register-footer">
              <el-button @click="navigateToLogin" class="login-link-btn">
                已有账号？去登录
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/composables/useLuminaToast'
import { Plus } from '@element-plus/icons-vue'
import { authService, type RegisterRequest } from '../services/authService'

const router = useRouter()
const registerFormRef = ref()
const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

// 注册表单
const registerForm = reactive<RegisterRequest>({
  username: '',
  email: '',
  password: '',
  nickname: '',
  position: '',
  company: '',
  bio: '',
  avatar: '',
  captcha: '',
  captchaKey: '',
  confirmPassword: '' // 添加确认密码字段到表单对象中
})

// 表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (!value) {
          callback(new Error('请再次输入密码'))
        } else if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  nickname: [
    { max: 20, message: '昵称长度不超过 20 个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码长度为4位', trigger: 'blur' }
  ]
}

// 获取验证码
const getCaptcha = async () => {
  try {
    const response = await authService.getCaptcha()
    captchaKey.value = response.captchaKey
    captchaImage.value = response.captchaImage
  } catch (error) {
    console.error('获取验证码失败:', error)
    toast.error('获取验证码失败')
  }
}

// 刷新验证码
const refreshCaptcha = () => {
  registerForm.captcha = ''
  getCaptcha()
}

// 处理注册
const handleRegister = async () => {
  try {
    // 表单验证
    await (registerFormRef.value as any).validate()
    
    // 额外验证确认密码（已由表单验证规则处理，此处可删除）
    // Element Plus 表单验证已经包含了这些检查
    
    loading.value = true
    
    // 设置验证码key
    registerForm.captchaKey = captchaKey.value
    
    // 发送注册请求（包含confirmPassword，后端也会验证）
    await authService.register(registerForm)
    
    toast.success('注册成功，请登录')
    
    // 跳转到登录页
    router.push('/login')
  } catch (error: any) {
    console.error('注册失败:', error)
    toast.error(error.response?.data?.message || '注册失败，请稍后重试')
    // 刷新验证码
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 跳转到登录页面
const navigateToLogin = () => {
  router.push('/login')
}

// 组件挂载时获取验证码
onMounted(() => {
  getCaptcha()
})

// 头像上传成功回调
const handleAvatarSuccess = (response: any) => {
  registerForm.avatar = response.data
  toast.success('头像上传成功')
}

// 头像上传前验证
const beforeAvatarUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    toast.error('只能上传图片文件！')
    return false
  }
  if (!isLt2M) {
    toast.error('头像文件大小不能超过2MB！')
    return false
  }
  return true
}

// 上传请求头
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  if (token) {
    return {
      'Authorization': `Bearer ${token}`
    }
  }
  return {}
})
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--bg-secondary);
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 500px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  padding: 40px;
  transition: var(--transition);
  max-height: 90vh;
  overflow-y: auto;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
  font-family: var(--font-serif);
}

.register-form {
  width: 100%;
}

.register-btn {
  width: 100%;
  margin-bottom: 0;
}

.register-footer {
  width: 100%;
  margin-top: 12px;
}

.login-link-btn {
  width: 100%;
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.captcha-container {
  display: flex;
  gap: 16px;
  align-items: center;
}

.captcha-container .el-input {
  flex: 1;
}

.captcha-image {
  width: 140px;
  height: 48px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition);
}

.captcha-image:hover {
  border-color: var(--color-blue-500);
  transform: translateY(-1px);
}

.avatar-uploader {
  display: flex;
  justify-content: center;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: var(--text-tertiary);
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
}

.avatar-uploader-icon:hover {
  border-color: var(--color-blue-500);
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  cursor: pointer;
}

.upload-tip {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 8px;
  text-align: center;
}

/* 表单样式覆盖 */
:deep(.el-form-item__label) {
  color: var(--text-primary);
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  background-color: var(--bg-primary);
  border-color: var(--border-color);
  transition: var(--transition);
  border-radius: 8px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-hover);
  border-color: var(--border-hover);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  border-color: var(--color-blue-500);
}

:deep(.el-input__inner) {
  color: var(--text-primary);
  font-family: var(--font-sans);
}

:deep(.el-textarea__inner) {
  background-color: var(--bg-primary);
  border-color: var(--border-color);
  border-radius: 8px;
  transition: var(--transition);
}

:deep(.el-textarea__inner:hover) {
  border-color: var(--border-hover);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-blue-500);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-container {
    padding: 24px;
    max-height: none;
  }

  .register-header h2 {
    font-size: 24px;
  }

  .captcha-image {
    width: 120px;
    height: 44px;
  }
}
</style>