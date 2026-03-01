<template>
  <Layout>
    <div class="about-container">
      <!-- Warm Gradient Background -->
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>

      <div class="content-wrapper">
        <!-- Hero Section -->
        <section class="hero">
          <div class="hero-inner">
            <div class="avatar-container">
              <div class="avatar-ring"></div>
              <img src="/images/about-avatar.jpg" alt="4iKZ" class="avatar" />
            </div>
            <div class="hero-text">
              <span class="hero-label">HELLO WORLD</span>
              <h1 class="hero-name">4iKZ</h1>
              <p class="hero-tagline">没有天赋，那就反复</p>
            </div>
          </div>
        </section>

        <!-- About Section -->
        <section class="section about">
          <div class="section-head">
            <span class="section-number">01</span>
            <h2>关于我</h2>
          </div>
          <div class="about-content">
            <p class="about-desc">
              热爱技术的后端开发者，专注于 Java 生态与可观测性平台。
              喜欢钻研技术原理，享受从0到1构建系统的过程。
              相信持续学习与分享能让自己和他人共同进步。
            </p>
            <div class="info-list">
              <div class="info-item">
                <div class="info-mark">
                  <span class="mark-line"></span>
                  <span class="mark-dot"></span>
                </div>
                <div class="info-details">
                  <span class="info-key">Identity</span>
                  <span class="info-val">软件工程本科生 · 辽宁科技大学</span>
                </div>
              </div>
              <div class="info-item">
                <div class="info-mark">
                  <span class="mark-line"></span>
                  <span class="mark-dot"></span>
                </div>
                <div class="info-details">
                  <span class="info-key">Location</span>
                  <span class="info-val">北京 · 在读学生</span>
                </div>
              </div>
              <div class="info-item">
                <div class="info-mark">
                  <span class="mark-line"></span>
                  <span class="mark-dot"></span>
                </div>
                <div class="info-details">
                  <span class="info-key">Growth</span>
                  <span class="info-val">2022 — 2026 · 本科在读</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Focus Section -->
        <section class="section focus">
          <div class="section-head">
            <span class="section-number">02</span>
            <h2>技术方向</h2>
          </div>
          <div class="focus-grid">
            <div class="focus-card">
              <div class="focus-icon orange">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <h3>后端开发</h3>
              <p>Java · Spring Boot · MyBatis · 微服务架构</p>
            </div>
            <div class="focus-card">
              <div class="focus-icon yellow">
                <span></span>
                <span></span>
              </div>
              <h3>可观测性</h3>
              <p>OpenTelemetry · Prometheus · Grafana · 监控体系</p>
            </div>
            <div class="focus-card">
              <div class="focus-icon green">
                <span></span>
                <span></span>
                <span></span>
                <span></span>
              </div>
              <h3>数据库</h3>
              <p>MySQL · Redis · 索引优化 · 缓存方案</p>
            </div>
            <div class="focus-card">
              <div class="focus-icon pink">
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
              </div>
              <h3>分布式</h3>
              <p>并发编程 · 分布式锁 · 混沌工程 · 压测</p>
            </div>
          </div>
        </section>

        <!-- Education Section -->
        <section class="section education">
          <div class="section-head">
            <span class="section-number">03</span>
            <h2>教育背景</h2>
          </div>
          <div class="edu-card">
            <div class="edu-time">
              <span class="time-start">2022</span>
              <div class="time-line"></div>
              <span class="time-end">2026</span>
            </div>
            <div class="edu-info">
              <h3>辽宁科技大学</h3>
              <span class="edu-major">软件工程 · 本科</span>
              <p>系统学习软件工程理论与实践，专注于后端开发与分布式系统</p>
            </div>
          </div>
        </section>

        <!-- Achievements Section -->
        <section class="section achievements">
          <div class="section-head">
            <span class="section-number">04</span>
            <h2>荣誉与成就</h2>
          </div>
          <div class="achieve-list">
            <div class="achieve-item">
              <span class="achieve-date">2025</span>
              <div class="achieve-divider"></div>
              <div class="achieve-content">
                <span class="achieve-type">PAPER</span>
                <h4>WISE 会议第一作者论文</h4>
                <p>The Framework for Text to SQL Optimization with Vector Similarity and Prompt Enhancement</p>
              </div>
            </div>
            <div class="achieve-item">
              <span class="achieve-date">2024</span>
              <div class="achieve-divider"></div>
              <div class="achieve-content">
                <span class="achieve-type">AWARD</span>
                <h4>美国大学生数学建模竞赛（MCM/ICM）</h4>
                <p>S奖（Successful Participant）</p>
              </div>
            </div>
            <div class="achieve-item">
              <span class="achieve-date">2023</span>
              <div class="achieve-divider"></div>
              <div class="achieve-content">
                <span class="achieve-type">COMP</span>
                <h4>睿抗机器人开发者大赛</h4>
                <p>全国三等奖</p>
              </div>
            </div>
            <div class="achieve-item">
              <span class="achieve-date">2022-24</span>
              <div class="achieve-divider"></div>
              <div class="achieve-content">
                <span class="achieve-type">HONOR</span>
                <h4>校级奖学金</h4>
                <p>三次获得校级奖学金</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Skills Section -->
        <section class="section skills">
          <div class="section-head">
            <span class="section-number">05</span>
            <h2>技能清单</h2>
          </div>
          <div class="skills-list">
            <div class="skill-block">
              <div class="skill-header">
                <span class="skill-bar"></span>
                <span class="skill-name">Java 基础</span>
              </div>
              <div class="skill-items">
                <span class="skill-item">集合框架（HashMap/ConcurrentHashMap）</span>
                <span class="skill-item">多线程与并发（JUC、线程池、锁机制）</span>
                <span class="skill-item">JVM 内存模型与调优</span>
              </div>
            </div>
            <div class="skill-block">
              <div class="skill-header">
                <span class="skill-bar"></span>
                <span class="skill-name">框架与中间件</span>
              </div>
              <div class="skill-items">
                <span class="skill-item">Spring Boot</span>
                <span class="skill-item">MyBatis / MyBatis-Plus</span>
                <span class="skill-item">Spring Cloud 微服务</span>
              </div>
            </div>
            <div class="skill-block">
              <div class="skill-header">
                <span class="skill-bar"></span>
                <span class="skill-name">数据库</span>
              </div>
              <div class="skill-items">
                <span class="skill-item">MySQL 索引原理与优化</span>
                <span class="skill-item">事务隔离级别</span>
                <span class="skill-item">Redis 缓存场景</span>
                <span class="skill-item">分布式锁</span>
                <span class="skill-item">缓存穿透/击穿解决方案</span>
              </div>
            </div>
            <div class="skill-block">
              <div class="skill-header">
                <span class="skill-bar"></span>
                <span class="skill-name">分布式与监控</span>
              </div>
              <div class="skill-items">
                <span class="skill-item">OpenTelemetry 观测标准</span>
                <span class="skill-item">Prometheus + Grafana</span>
                <span class="skill-item">ChaosBlade 混沌工程</span>
                <span class="skill-item">BenchmarkSQL 压测</span>
              </div>
            </div>
            <div class="skill-block">
              <div class="skill-header">
                <span class="skill-bar"></span>
                <span class="skill-name">工具与环境</span>
              </div>
              <div class="skill-items">
                <span class="skill-item">Git</span>
                <span class="skill-item">Maven</span>
                <span class="skill-item">Linux</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Contact Section -->
        <section class="section contact">
          <div class="section-head">
            <span class="section-number">06</span>
            <h2>联系我</h2>
          </div>
          <div class="contact-list">
            <a href="mailto:syhaox@outlook.com" class="contact-item">
              <div class="contact-left">
                <span class="contact-label">EMAIL</span>
                <span class="contact-value">syhaox@outlook.com</span>
              </div>
              <span class="contact-arrow">
                <span></span>
                <span></span>
              </span>
            </a>
            <a href="https://github.com/4iKZ" target="_blank" class="contact-item">
              <div class="contact-left">
                <span class="contact-label">GITHUB</span>
                <span class="contact-value">github.com/4iKZ</span>
              </div>
              <span class="contact-arrow">
                <span></span>
                <span></span>
              </span>
            </a>
          </div>
          <p class="contact-note">欢迎通过邮件或 GitHub 与我交流技术问题，或分享你的想法</p>
        </section>

        <!-- Footer -->
        <footer class="footer">
          <span class="footer-line"></span>
          <span class="footer-text">© 2025 4iKZ · Lumina</span>
        </footer>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import Layout from '../components/Layout.vue'
</script>

<style scoped>
/* Import fonts */
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@400;500;600&display=swap');

/* Design System */
:root {
  --color-bg: #F0F7FF;
  --color-surface: #FFFFFF;
  --color-primary: #409EFF;
  --color-primary-light: #66B1FF;
  --color-primary-soft: #ECF5FF;
  --color-secondary: #5DADE2;
  --color-accent: #58D68D;
  --color-accent-green: #58D68D;
  --color-accent-blue: #5DADE2;
  --color-accent-pink: #F5B7B1;
  --color-text: #1A1A1A;
  --color-text-secondary: #6B6B6B;
  --color-text-muted: #9B9B9B;
  --color-border: #E1EEFF;
  --color-border-light: #ECF5FF;

  --font-display: 'Space Grotesk', sans-serif;
  --font-body: 'Inter', sans-serif;

  --shadow-soft: 0 4px 20px rgba(64, 158, 255, 0.08);
  --shadow-medium: 0 8px 30px rgba(64, 158, 255, 0.12);
  --shadow-strong: 0 12px 40px rgba(64, 158, 255, 0.16);
}

.about-container {
  position: relative;
  min-height: 100vh;
  background: var(--color-bg);
  overflow-x: hidden;
}

/* Background Pattern */
.bg-gradient {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(64, 158, 255, 0.2), transparent),
    radial-gradient(ellipse 60% 40% at 100% 50%, rgba(93, 173, 226, 0.12), transparent),
    radial-gradient(ellipse 50% 30% at 0% 80%, rgba(88, 214, 141, 0.08), transparent);
  pointer-events: none;
  z-index: 0;
}

.bg-pattern {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    repeating-linear-gradient(90deg, transparent, transparent 100px, rgba(64, 158, 255, 0.02) 100px, rgba(64, 158, 255, 0.02) 101px),
    repeating-linear-gradient(0deg, transparent, transparent 100px, rgba(64, 158, 255, 0.02) 100px, rgba(64, 158, 255, 0.02) 101px);
  pointer-events: none;
  z-index: 0;
}

.content-wrapper {
  position: relative;
  z-index: 1;
  max-width: 880px;
  margin: 0 auto;
  padding: 80px 32px 100px;
}

/* Hero Section */
.hero {
  display: flex;
  justify-content: center;
  padding: 60px 0 100px;
}

.hero-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.avatar-container {
  position: relative;
}

.avatar-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 220px;
  height: 220px;
  border: 3px dashed var(--color-primary);
  border-radius: 50%;
  animation: rotate 30s linear infinite;
}

@keyframes rotate {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.avatar {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  object-fit: cover;
  border: 6px solid var(--color-surface);
  box-shadow:
    0 0 0 1px var(--color-border),
    var(--shadow-medium);
  transition: transform 0.4s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.hero-text {
  text-align: center;
}

.hero-label {
  display: inline-block;
  font-family: var(--font-display);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-primary);
  margin-bottom: 16px;
}

.hero-name {
  font-family: var(--font-display);
  font-size: clamp(3.5rem, 12vw, 6rem);
  font-weight: 700;
  line-height: 1;
  color: var(--color-primary);
  margin-bottom: 16px;
  /* Fallback: solid color */
  background: linear-gradient(135deg, #409EFF 0%, #66B1FF 50%, #5DADE2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-tagline {
  font-family: var(--font-body);
  font-size: 1.125rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

/* Section Common */
.section {
  margin-bottom: 80px;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid var(--color-border);
}

.section-number {
  font-family: var(--font-display);
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-primary);
}

.section-head h2 {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

/* About Section */
.about-content {
  background: var(--color-surface);
  border-radius: 24px;
  padding: 40px;
  box-shadow: var(--shadow-soft);
}

.about-desc {
  font-family: var(--font-body);
  font-size: 1.0625rem;
  line-height: 1.8;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.info-mark {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.mark-line {
  width: 24px;
  height: 2px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
}

.mark-dot {
  width: 8px;
  height: 8px;
  background: var(--color-primary);
  border-radius: 50%;
}

.info-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-key {
  font-family: var(--font-display);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  color: var(--color-text-muted);
  text-transform: uppercase;
}

.info-val {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text);
}

/* Focus Grid */
.focus-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.focus-card {
  background: var(--color-surface);
  border-radius: 20px;
  padding: 32px 28px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
}

.focus-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-medium);
}

.focus-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-content: flex-start;
}

.focus-icon span {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: currentColor;
}

.focus-icon.orange { color: var(--color-primary); }
.focus-icon.yellow { color: var(--color-secondary); }
.focus-icon.green { color: var(--color-accent-green); }
.focus-icon.pink { color: var(--color-accent-pink); }

.focus-card h3 {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px;
}

.focus-card p {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* Education Card */
.edu-card {
  display: flex;
  gap: 32px;
  background: var(--color-surface);
  border-radius: 24px;
  padding: 40px;
  box-shadow: var(--shadow-soft);
  align-items: flex-start;
}

.edu-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.time-start,
.time-end {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-primary);
}

.time-line {
  width: 2px;
  height: 40px;
  background: linear-gradient(180deg, var(--color-primary), var(--color-secondary));
}

.edu-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edu-info h3 {
  font-family: var(--font-display);
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.edu-major {
  font-family: var(--font-display);
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-secondary);
}

.edu-info p {
  font-family: var(--font-body);
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* Achievements List */
.achieve-list {
  display: flex;
  flex-direction: column;
}

.achieve-item {
  display: grid;
  grid-template-columns: 80px 1px 1fr;
  gap: 24px;
  padding: 24px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.achieve-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.achieve-date {
  font-family: var(--font-display);
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-primary);
  text-align: right;
  padding-top: 2px;
}

.achieve-divider {
  background: var(--color-border);
}

.achieve-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.achieve-type {
  font-family: var(--font-display);
  font-size: 0.625rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--color-text-muted);
}

.achieve-content h4 {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.achieve-content p {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0;
}

/* Skills List */
.skills-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skill-block {
  background: var(--color-surface);
  border-radius: 20px;
  padding: 28px 32px;
  box-shadow: var(--shadow-soft);
  transition: box-shadow 0.3s ease;
}

.skill-block:hover {
  box-shadow: var(--shadow-medium);
}

.skill-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.skill-bar {
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
  border-radius: 2px;
}

.skill-name {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.skill-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.skill-item {
  font-family: var(--font-body);
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  padding: 8px 16px;
  background: var(--color-bg);
  border-radius: 8px;
  border: 1px solid var(--color-border);
  transition: all 0.2s ease;
}

.skill-item:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

/* Contact Section */
.contact-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.contact-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  border-radius: 20px;
  padding: 28px 32px;
  box-shadow: var(--shadow-soft);
  text-decoration: none;
  transition: all 0.3s ease;
}

.contact-item:hover {
  transform: translateX(8px);
  box-shadow: var(--shadow-medium);
}

.contact-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.contact-label {
  font-family: var(--font-display);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  color: var(--color-text-muted);
}

.contact-value {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text);
}

.contact-arrow {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 32px;
  height: 32px;
  background: var(--color-primary-soft);
  border-radius: 50%;
  padding: 10px 12px;
}

.contact-arrow span {
  flex: 1;
  height: 2px;
  background: var(--color-primary);
  border-radius: 1px;
  transition: transform 0.3s ease;
}

.contact-item:hover .contact-arrow span:first-child {
  transform: translateX(2px);
}

.contact-item:hover .contact-arrow span:last-child {
  transform: translateX(-2px);
}

.contact-note {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--color-text-muted);
  text-align: center;
  max-width: 480px;
  margin: 0 auto;
  line-height: 1.6;
}

/* Footer */
.footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 48px;
  margin-top: 80px;
  border-top: 1px solid var(--color-border);
}

.footer-line {
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
}

.footer-text {
  font-family: var(--font-body);
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

/* Responsive */
@media (max-width: 768px) {
  .content-wrapper {
    padding: 60px 24px 80px;
  }

  .hero {
    padding: 40px 0 60px;
  }

  .avatar {
    width: 140px;
    height: 140px;
  }

  .avatar-ring {
    width: 170px;
    height: 170px;
  }

  .section-head {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .focus-grid {
    grid-template-columns: 1fr;
  }

  .edu-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .edu-time {
    flex-direction: row;
    width: fit-content;
  }

  .time-line {
    width: 40px;
    height: 2px;
  }

  .achieve-item {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .achieve-date {
    text-align: left;
  }

  .achieve-divider {
    display: none;
  }
}

@media (max-width: 480px) {
  .about-content,
  .edu-card {
    padding: 28px 24px;
  }

  .hero-name {
    font-size: 2.75rem;
  }

  .contact-item {
    padding: 24px;
  }
}
</style>
