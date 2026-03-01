<template>
  <footer class="footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-brand">
          <p class="brand-name">Lumina<span class="brand-dot">.</span></p>
        </div>
        <div class="footer-social">
          <a href="#" class="social-link" aria-label="Twitter">
            <i class="fab fa-twitter"></i>
          </a>
          <a href="#" class="social-link" aria-label="GitHub">
            <i class="fab fa-github"></i>
          </a>
          <a href="#" class="social-link" aria-label="Instagram">
            <i class="fab fa-instagram"></i>
          </a>
        </div>
      </div>
      <div class="footer-icp">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer" class="icp-link">
          豫ICP备2025121891号-2
        </a>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
// Minimalist Footer Component
</script>

<style scoped>
.footer {
  background-color: var(--bg-primary);
  border-top: 1px solid var(--border-color);
  padding: var(--space-12) 0;
  margin-top: auto;
  transition: all var(--duration-normal) var(--ease-default);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-6);
}

.footer-brand {
  text-align: left;
}

.brand-name {
  font-family: var(--font-serif);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 var(--space-1) 0;
}

.brand-dot {
  color: var(--color-blue-500);
}

.footer-social {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.social-link {
  color: var(--text-tertiary);
  font-size: var(--text-lg);
  transition: all var(--duration-fast) var(--ease-default);
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.social-link:hover {
  color: var(--text-primary);
  transform: translateY(-2px);
}

.dark .social-link:hover {
  color: var(--text-primary);
}

.footer-icp {
  text-align: center;
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-color);
}

.icp-link {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-default);
}

.icp-link:hover {
  color: var(--color-blue-500);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer {
    padding: var(--space-8) 0;
  }
  
  .container {
    padding: 0 var(--space-4);
  }
  
  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: var(--space-4);
  }
  
  .footer-brand {
    text-align: center;
  }
  
  .footer-social {
    justify-content: center;
  }
}
</style>