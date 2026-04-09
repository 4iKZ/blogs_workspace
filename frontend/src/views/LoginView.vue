<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <!-- 网站 Icon -->
        <div class="site-icon-wrapper" v-if="siteIcon">
          <img :src="siteIcon" alt="Site Icon" class="site-icon" />
        </div>
        <h2>登录</h2>
      </div>
      <div class="login-form">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="80px"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名或邮箱"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item label="验证码" prop="captcha">
            <div class="captcha-container">
              <el-input
                v-model="loginForm.captcha"
                placeholder="请输入验证码"
                maxlength="4"
                @keyup.enter="handleLogin"
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
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              class="login-btn"
              size="large"
            >
              登录
            </el-button>
            <div class="login-footer">
              <el-button @click="navigateToRegister" class="footer-btn">
                注册账号
              </el-button>
              <el-button
                type="text"
                @click="navigateToResetPassword"
                class="forgot-btn"
              >
                忘记密码？
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <div class="oauth-divider">
              <span class="divider-text">其他登录方式</span>
            </div>
            <el-button
              @click="handleGithubLogin"
              class="github-btn"
              size="large"
            >
              <svg
                class="github-icon"
                viewBox="0 0 24 24"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"
                />
              </svg>
              GitHub 登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { toast } from "@/composables/useLuminaToast";
import { useUserStore } from "../store/user";
import { useSiteConfigStore } from "../store/siteConfig";
import { authService, type LoginRequest } from "../services/authService";

const router = useRouter();
const userStore = useUserStore();
const siteConfigStore = useSiteConfigStore();
const loginFormRef = ref();
const loading = ref(false);
const captchaImage = ref("");
const captchaKey = ref("");

// 获取网站图标（优先使用 favicon，其次使用 logo）
const siteIcon = computed(() => {
  return (
    siteConfigStore.config?.websiteFavicon ||
    siteConfigStore.config?.websiteLogo ||
    "/favicon.png"
  );
});

// 登录表单
const loginForm = ref<LoginRequest>({
  username: "",
  password: "",
  captcha: "",
  captchaKey: "",
});

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: "请输入用户名或邮箱", trigger: "blur" },
  ],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  captcha: [
    { required: true, message: "请输入验证码", trigger: "blur" },
    { min: 4, max: 4, message: "验证码长度为4位", trigger: "blur" },
  ],
};

// 获取验证码
const getCaptcha = async () => {
  try {
    const response = await authService.getCaptcha();
    captchaKey.value = response.captchaKey;
    captchaImage.value = response.captchaImage;
  } catch (error) {
    console.error("获取验证码失败:", error);
    toast.error("获取验证码失败");
  }
};

// 刷新验证码
const refreshCaptcha = () => {
  loginForm.value.captcha = "";
  getCaptcha();
};

// 处理登录
const handleLogin = async () => {
  try {
    // 表单验证
    await loginFormRef.value.validate();

    loading.value = true;

    // 设置验证码key
    loginForm.value.captchaKey = captchaKey.value;

    // 发送登录请求
    const response = await authService.login(loginForm.value);

    // 保存用户信息和token
    // 响应拦截器已经解包，response 就是 UserDTO
    const userInfo = {
      id: response.id,
      username: response.username,
      email: response.email || "",
      phone: response.phone || null,
      nickname: response.nickname || response.username,
      avatar: response.avatar || null,
      bio: response.bio || null,
      website: response.website || null,
      status: response.status || 1,
      role: response.role || "user",
      position: (response as any).position || null,
      company: (response as any).company || null,
      createTime: response.createTime || new Date().toISOString(),
      lastLoginTime: response.lastLoginTime || new Date().toISOString(),
      lastLoginIp: response.lastLoginIp || null,
      articleCount: response.articleCount || 0,
      likeCount: 0, // Login response might not have these, default to 0
      viewCount: 0,
      commentCount: response.commentCount || 0,
    };
    userStore.setUserInfo(userInfo);
    userStore.setTokens(response.accessToken, response.refreshToken);

    toast.success("登录成功");

    // 跳转到首页
    router.push("/");
  } catch (error: any) {
    console.error("登录失败:", error);
    if (!error._handled) {
      toast.error(
        error.response?.data?.message || "登录失败，请检查用户名和密码"
      );
    }
    // 刷新验证码
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

// 跳转到注册页面
const navigateToRegister = () => {
  router.push("/register");
};

// GitHub 登录
const handleGithubLogin = () => {
  const authUrl = authService.getGithubAuthUrl();
  window.location.href = authUrl;
};

// 跳转到重置密码页面
const navigateToResetPassword = () => {
  router.push("/reset-password");
};

// 组件挂载时获取验证码
onMounted(() => {
  getCaptcha();
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--bg-secondary);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 450px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1),
    0 4px 6px -2px rgba(0, 0, 0, 0.05);
  padding: 40px;
  transition: var(--transition);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

/* 网站 Icon 样式 */
.site-icon-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  width: 100%;
}

.site-icon {
  width: 80px;
  height: 80px;
  max-width: 80px;
  max-height: 80px;
  object-fit: contain;
  background: transparent;
  display: block;
}

.login-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
  font-family: var(--font-serif);
}

.login-form {
  width: 100%;
}

.login-btn {
  width: 100%;
  margin-bottom: 0;
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

.login-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-top: 12px;
  gap: 12px;
}

.footer-btn {
  flex: 1;
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.forgot-btn {
  color: var(--text-secondary);
  padding: 12px 16px;
}

.oauth-divider {
  width: 100%;
  text-align: center;
  position: relative;
  margin-bottom: 16px;
}

.oauth-divider::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background-color: var(--border-color);
}

.divider-text {
  position: relative;
  background-color: var(--bg-primary);
  padding: 0 16px;
  color: var(--text-secondary);
  font-size: 14px;
}

.github-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  transition: var(--transition);
}

.github-btn:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--border-hover);
  transform: translateY(-1px);
}

.github-icon {
  width: 20px;
  height: 20px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .login-page {
    padding: 16px;
    align-items: flex-start;
    padding-top: 60px;
  }

  .login-container {
    padding: 24px;
    max-width: 100%;
  }

  .login-header h2 {
    font-size: 24px;
  }

  .site-icon {
    width: 64px;
    height: 64px;
    max-width: 64px;
    max-height: 64px;
  }

  .captcha-image {
    width: 120px;
    height: 44px;
    min-width: 100px;
  }

  /* 移动端输入框优化 */
  :deep(.el-input__inner) {
    font-size: 16px; /* 防止 iOS 自动缩放 */
  }

  :deep(.el-input__wrapper) {
    min-height: 48px;
  }

  .login-btn {
    height: 48px;
    font-size: 16px;
  }

  .footer-btn,
  .forgot-btn {
    min-height: 44px;
  }
}

/* iOS Safari 键盘弹出适配 */
@supports (-webkit-touch-callout: none) {
  .login-page {
    min-height: -webkit-fill-available;
  }
}

/* 小屏手机优化 */
@media (max-width: 480px) {
  .login-page {
    padding: 12px;
    padding-top: 40px;
  }

  .login-container {
    padding: 20px;
  }

  .captcha-container {
    flex-direction: column;
    align-items: stretch;
  }

  .captcha-image {
    width: 100%;
    height: 48px;
  }
}
</style>