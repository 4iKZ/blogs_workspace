<template>
  <Layout :show-left-sidebar="false">
    <div class="not-found">
      <div class="not-found-content">
        <div class="error-code">404</div>
        <div class="error-illustration">
          <div class="ghost">
            <div class="ghost-body">
              <div class="eyes">
                <div class="eye left"></div>
                <div class="eye right"></div>
              </div>
              <div class="mouth"></div>
            </div>
            <div class="ghost-tail">
              <div class="tail-segment"></div>
              <div class="tail-segment"></div>
              <div class="tail-segment"></div>
            </div>
          </div>
          <div class="floating-elements">
            <div class="float-item float-1"></div>
            <div class="float-item float-2"></div>
            <div class="float-item float-3"></div>
          </div>
        </div>
        <h1 class="error-title">页面走丢了</h1>
        <p class="error-message">
          抱歉，您访问的页面不存在或已被移除。<br />
          让我们帮您回到正轨吧！
        </p>
        <div class="error-actions">
          <router-link to="/" class="btn btn-primary">
            <i class="fas fa-home"></i>
            返回首页
          </router-link>
          <button @click="goBack" class="btn btn-secondary">
            <i class="fas fa-arrow-left"></i>
            返回上一页
          </button>
        </div>
        <div class="helpful-links">
          <span class="helpful-label">您可能在寻找：</span>
          <div class="link-tags">
            <router-link to="/category" class="link-tag">
              <i class="fas fa-folder"></i>
              文章分类
            </router-link>
            <router-link to="/search" class="link-tag">
              <i class="fas fa-search"></i>
              搜索文章
            </router-link>
            <router-link to="/about" class="link-tag">
              <i class="fas fa-user"></i>
              关于我们
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'Home' })
  }
}
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 500px;
  padding: var(--space-12) var(--space-6);
}

.not-found-content {
  text-align: center;
  max-width: 500px;
}

.error-code {
  font-size: 120px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, var(--color-blue-500) 0%, var(--color-blue-600) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: var(--space-4);
  animation: bounce-in 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.error-illustration {
  position: relative;
  height: 120px;
  margin-bottom: var(--space-6);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Ghost Animation */
.ghost {
  position: relative;
  animation: float 3s ease-in-out infinite;
}

.ghost-body {
  width: 80px;
  height: 90px;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.05) 100%);
  border-radius: 40px 40px 20px 20px;
  position: relative;
  border: 2px solid var(--color-blue-400);
}

.eyes {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 30px;
}

.eye {
  width: 12px;
  height: 16px;
  background-color: var(--text-primary);
  border-radius: 50%;
  animation: blink 4s infinite;
}

.mouth {
  width: 20px;
  height: 10px;
  border: 2px solid var(--color-blue-400);
  border-top: none;
  border-radius: 0 0 20px 20px;
  margin: 12px auto 0;
}

.ghost-tail {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-top: -2px;
}

.tail-segment {
  width: 24px;
  height: 16px;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.05) 100%);
  border: 2px solid var(--color-blue-400);
  border-top: none;
  border-radius: 0 0 12px 12px;
  animation: wave 1.5s ease-in-out infinite;
}

.tail-segment:nth-child(2) {
  animation-delay: 0.2s;
}

.tail-segment:nth-child(3) {
  animation-delay: 0.4s;
}

/* Floating Elements */
.floating-elements {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
}

.float-item {
  position: absolute;
  width: 8px;
  height: 8px;
  background-color: var(--color-blue-400);
  border-radius: 50%;
  opacity: 0.6;
}

.float-1 {
  top: 20%;
  left: 20%;
  animation: float-around 4s ease-in-out infinite;
}

.float-2 {
  top: 60%;
  right: 15%;
  animation: float-around 4s ease-in-out infinite 1s;
}

.float-3 {
  bottom: 20%;
  left: 25%;
  animation: float-around 4s ease-in-out infinite 2s;
}

.error-title {
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--space-3);
  animation: fade-in-up 0.6s ease-out 0.2s both;
}

.error-message {
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  margin-bottom: var(--space-8);
  animation: fade-in-up 0.6s ease-out 0.3s both;
}

.error-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  justify-content: center;
  margin-bottom: var(--space-12);
  animation: fade-in-up 0.6s ease-out 0.4s both;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-sm);
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  text-decoration: none;
  border: none;
}

.btn-primary {
  background-color: var(--color-blue-600);
  color: white;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.btn-primary:hover {
  background-color: var(--color-blue-500);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
}

.btn-secondary {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--color-slate-100);
  border-color: var(--border-hover);
  transform: translateY(-2px);
}

.helpful-links {
  animation: fade-in-up 0.6s ease-out 0.5s both;
}

.helpful-label {
  display: block;
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--space-3);
}

.link-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: center;
}

.link-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-secondary);
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  text-decoration: none;
  transition: all var(--duration-fast) var(--ease-default);
}

.link-tag:hover {
  color: var(--color-blue-600);
  border-color: var(--color-blue-400);
  background-color: rgba(59, 130, 246, 0.05);
  transform: translateY(-1px);
}

/* Animations */
@keyframes bounce-in {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes blink {
  0%,
  48%,
  52%,
  100% {
    transform: scaleY(1);
  }
  50% {
    transform: scaleY(0.1);
  }
}

@keyframes wave {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(4px);
  }
}

@keyframes float-around {
  0%,
  100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(10px, -10px);
  }
  50% {
    transform: translate(0, -20px);
  }
  75% {
    transform: translate(-10px, -10px);
  }
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .not-found {
    padding: var(--space-8) var(--space-4);
  }

  .error-code {
    font-size: 80px;
  }

  .error-illustration {
    height: 90px;
  }

  .ghost-body {
    width: 60px;
    height: 70px;
    border-radius: 30px 30px 15px 15px;
  }

  .eyes {
    padding-top: 22px;
    gap: 12px;
  }

  .eye {
    width: 10px;
    height: 12px;
  }

  .mouth {
    width: 16px;
    height: 8px;
    margin: 10px auto 0;
  }

  .tail-segment {
    width: 18px;
    height: 12px;
    border-radius: 0 0 9px 9px;
  }

  .error-title {
    font-size: var(--text-2xl);
  }

  .error-message {
    font-size: var(--text-sm);
  }

  .error-actions {
    flex-direction: column;
    width: 100%;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }

  .link-tags {
    justify-content: center;
  }
}

/* Dark mode adjustments */
.dark .ghost-body {
  background: linear-gradient(180deg, rgba(96, 165, 250, 0.15) 0%, rgba(96, 165, 250, 0.05) 100%);
  border-color: var(--color-blue-500);
}

.dark .tail-segment {
  background: linear-gradient(180deg, rgba(96, 165, 250, 0.15) 0%, rgba(96, 165, 250, 0.05) 100%);
  border-color: var(--color-blue-500);
}

.dark .mouth {
  border-color: var(--color-blue-500);
}

.dark .float-item {
  background-color: var(--color-blue-500);
}

.dark .btn-secondary {
  background-color: var(--bg-card);
  border-color: var(--border-color);
}

.dark .btn-secondary:hover {
  background-color: var(--color-slate-700);
}

.dark .link-tag {
  background-color: var(--bg-card);
  border-color: var(--border-color);
}

.dark .link-tag:hover {
  background-color: rgba(96, 165, 250, 0.1);
}
</style>
