<template>
  <Layout>
    <div class="about-container">
      <!-- Hero Section -->
      <div class="hero-section">
        <div class="hero-content">
          <div class="avatar-wrapper">
            <el-avatar :size="120" class="hero-avatar" :src="'/images/about-avatar.jpg'">4iKZ</el-avatar>
            <div class="avatar-ring"></div>
          </div>
          <h1 class="hero-name">4iKZ</h1>
          <p class="hero-tagline">梦想是成为 Coding 大师~</p>
          <div class="hero-badges">
            <el-tag type="primary" effect="light" size="large">
              <el-icon><School /></el-icon>
              <span>软件工程学生</span>
            </el-tag>
            <el-tag type="success" effect="light" size="large">
              <el-icon><Location /></el-icon>
              <span>北京</span>
            </el-tag>
          </div>
        </div>
      </div>

      <div class="content-wrapper">
        <!-- About Me -->
        <section class="section-block">
          <div class="section-header">
            <h2>关于我</h2>
            <p class="section-subtitle">热爱技术，持续成长</p>
          </div>
          <el-card class="about-card" shadow="never">
            <div class="about-grid">
              <div class="about-item">
                <div class="about-icon">
                  <el-icon><User /></el-icon>
                </div>
                <div class="about-text">
                  <h4>身份</h4>
                  <p>软件工程本科生 | 辽宁科技大学</p>
                </div>
              </div>
              <div class="about-item">
                <div class="about-icon">
                  <el-icon><Compass /></el-icon>
                </div>
                <div class="about-text">
                  <h4>坐标</h4>
                  <p>北京 | 在读学生</p>
                </div>
              </div>
              <div class="about-item">
                <div class="about-icon">
                  <el-icon><TrendCharts /></el-icon>
                </div>
                <div class="about-text">
                  <h4>成长</h4>
                  <p>2022 - 2026 | 本科在读</p>
                </div>
              </div>
            </div>
            <div class="about-intro">
              <p>
                热爱技术的后端开发者，专注于 Java 生态与可观测性平台。
                喜欢钻研技术原理，享受从0到1构建系统的过程。
                相信持续学习与分享能让自己和他人共同进步。
              </p>
            </div>
          </el-card>
        </section>

        <!-- Technical Focus -->
        <section class="section-block">
          <div class="section-header">
            <h2>技术方向</h2>
            <p class="section-subtitle">专注领域与兴趣所在</p>
          </div>
          <div class="focus-grid">
            <div class="focus-card">
              <div class="focus-icon java">
                <el-icon><Cpu /></el-icon>
              </div>
              <h3>后端开发</h3>
              <p>Java、Spring Boot、MyBatis、微服务架构</p>
            </div>
            <div class="focus-card">
              <div class="focus-icon observability">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <h3>可观测性</h3>
              <p>OpenTelemetry、Prometheus、Grafana、监控体系</p>
            </div>
            <div class="focus-card">
              <div class="focus-icon database">
                <el-icon><Coin /></el-icon>
              </div>
              <h3>数据库</h3>
              <p>MySQL、Redis、索引优化、缓存方案</p>
            </div>
            <div class="focus-card">
              <div class="focus-icon distributed">
                <el-icon><Connection /></el-icon>
              </div>
              <h3>分布式</h3>
              <p>并发编程、分布式锁、混沌工程、压测</p>
            </div>
          </div>
        </section>

        <!-- Education -->
        <section class="section-block">
          <div class="section-header">
            <h2>教育背景</h2>
            <p class="section-subtitle">Education & Academic</p>
          </div>
          <el-card class="education-card" shadow="never">
            <div class="education-timeline">
              <div class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-period">2022.09 - 2026.06</div>
                  <h3>辽宁科技大学</h3>
                  <p class="timeline-major">软件工程 · 本科</p>
                  <p class="timeline-desc">系统学习软件工程理论与实践，专注于后端开发与分布式系统</p>
                </div>
              </div>
            </div>
          </el-card>
        </section>

        <!-- Achievements -->
        <section class="section-block">
          <div class="section-header">
            <h2>荣誉与成就</h2>
            <p class="section-subtitle">Achievements & Awards</p>
          </div>
          <div class="achievements-grid">
            <div class="achievement-card paper">
              <div class="achievement-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="achievement-content">
                <h3>学术论文</h3>
                <p class="achievement-title">2025 WISE 会议第一作者论文</p>
                <p class="achievement-desc">《The Framework for Text to SQL Optimization with Vector Similarity and Prompt Enhancement》</p>
              </div>
            </div>

            <div class="achievement-card award">
              <div class="achievement-icon">
                <el-icon><Trophy /></el-icon>
              </div>
              <div class="achievement-content">
                <h3>数学建模竞赛</h3>
                <p class="achievement-title">2024 美国大学生数学建模竞赛（MCM/ICM）</p>
                <p class="achievement-desc">S奖（Successful Participant）</p>
              </div>
            </div>

            <div class="achievement-card competition">
              <div class="achievement-icon">
                <el-icon><Medal /></el-icon>
              </div>
              <div class="achievement-content">
                <h3>机器人开发大赛</h3>
                <p class="achievement-title">2023 睿抗机器人开发者大赛</p>
                <p class="achievement-desc">全国三等奖</p>
              </div>
            </div>

            <div class="achievement-card scholarship">
              <div class="achievement-icon">
                <el-icon><Star /></el-icon>
              </div>
              <div class="achievement-content">
                <h3>学业优秀</h3>
                <p class="achievement-title">校级奖学金</p>
                <p class="achievement-desc">2022-2024 三次获得校级奖学金</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Skills -->
        <section class="section-block">
          <div class="section-header">
            <h2>技能清单</h2>
            <p class="section-subtitle">Technical Skills</p>
          </div>
          <div class="skills-container">
            <div class="skill-group">
              <h3 class="skill-category">
                <el-icon><Cpu /></el-icon>
                Java 基础
              </h3>
              <div class="skill-items">
                <span class="skill-tag">集合框架（HashMap/ConcurrentHashMap）</span>
                <span class="skill-tag">多线程与并发（JUC、线程池、锁机制）</span>
                <span class="skill-tag">JVM 内存模型与调优</span>
              </div>
            </div>

            <div class="skill-group">
              <h3 class="skill-category">
                <el-icon><Box /></el-icon>
                框架与中间件
              </h3>
              <div class="skill-items">
                <span class="skill-tag">Spring Boot</span>
                <span class="skill-tag">MyBatis / MyBatis-Plus</span>
                <span class="skill-tag">Spring Cloud 微服务</span>
              </div>
            </div>

            <div class="skill-group">
              <h3 class="skill-category">
                <el-icon><Coin /></el-icon>
                数据库
              </h3>
              <div class="skill-items">
                <span class="skill-tag">MySQL 索引原理与优化</span>
                <span class="skill-tag">事务隔离级别</span>
                <span class="skill-tag">Redis 缓存场景</span>
                <span class="skill-tag">分布式锁</span>
                <span class="skill-tag">缓存穿透/击穿解决方案</span>
              </div>
            </div>

            <div class="skill-group">
              <h3 class="skill-category">
                <el-icon><Monitor /></el-icon>
                分布式与监控
              </h3>
              <div class="skill-items">
                <span class="skill-tag">OpenTelemetry 观测标准</span>
                <span class="skill-tag">Prometheus + Grafana</span>
                <span class="skill-tag">ChaosBlade 混沌工程</span>
                <span class="skill-tag">BenchmarkSQL 压测</span>
              </div>
            </div>

            <div class="skill-group">
              <h3 class="skill-category">
                <el-icon><Tools /></el-icon>
                工具与环境
              </h3>
              <div class="skill-items">
                <span class="skill-tag">Git</span>
                <span class="skill-tag">Maven</span>
                <span class="skill-tag">Linux</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Contact -->
        <section class="section-block">
          <div class="section-header">
            <h2>联系我</h2>
            <p class="section-subtitle">Get in Touch</p>
          </div>
          <div class="contact-grid">
            <a href="mailto:syhaox@outlook.com" class="contact-card email">
              <div class="contact-icon">
                <el-icon><Message /></el-icon>
              </div>
              <div class="contact-info">
                <h3>Email</h3>
                <p>syhaox@outlook.com</p>
              </div>
            </a>

            <a href="https://github.com/4iKZ" target="_blank" class="contact-card github">
              <div class="contact-icon">
                <svg height="32" viewBox="0 0 16 16" version="1.1" width="32">
                  <path fill="currentColor" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
                </svg>
              </div>
              <div class="contact-info">
                <h3>GitHub</h3>
                <p>github.com/4iKZ</p>
              </div>
              <el-icon class="contact-arrow"><ArrowRight /></el-icon>
            </a>
          </div>

          <div class="contact-footer">
            <p>欢迎通过邮件或 GitHub 与我交流技术问题，或分享你的想法！</p>
          </div>
        </section>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import Layout from '../components/Layout.vue'
import {
  School, Location, User, Compass, TrendCharts, Cpu, DataAnalysis,
  Coin, Connection, Document, Trophy, Medal, Star, Box, Monitor,
  Tools, Message, ArrowRight
} from '@element-plus/icons-vue'
</script>

<style scoped>
.about-container {
  min-height: 100%;
  padding-bottom: 60px;
}

/* Hero Section */
.hero-section {
  text-align: center;
  padding: 80px 20px 60px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, rgba(103, 194, 58, 0.05) 100%);
  margin-bottom: 40px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 24px;
}

.hero-avatar {
  background: linear-gradient(135deg, #409eff, #67c23a);
  font-size: 48px;
  font-weight: 700;
  color: white;
  border: 4px solid white;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.3);
}

.avatar-ring {
  position: absolute;
  top: -8px;
  left: -8px;
  right: -8px;
  bottom: -8px;
  border: 2px dashed #409eff;
  border-radius: 50%;
  animation: spin 20s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.hero-name {
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 12px;
  letter-spacing: -0.02em;
}

.hero-tagline {
  font-size: 1.25rem;
  color: var(--text-secondary);
  margin-bottom: 24px;
  font-weight: 500;
}

.hero-badges {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.hero-badges .el-tag {
  padding: 8px 20px;
  font-size: 0.95rem;
  border-radius: 20px;
}

.hero-badges .el-icon {
  margin-right: 6px;
}

/* Content Wrapper */
.content-wrapper {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 24px;
}

/* Section Common */
.section-block {
  margin-bottom: 60px;
}

.section-header {
  text-align: center;
  margin-bottom: 32px;
}

.section-header h2 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.section-subtitle {
  font-size: 1rem;
  color: var(--text-tertiary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* About Card */
.about-card {
  border: none;
  box-shadow: var(--shadow-sm);
}

.about-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.about-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
}

.about-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.about-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  border-radius: var(--radius-md);
  color: white;
  font-size: 24px;
  flex-shrink: 0;
}

.about-text h4 {
  font-size: 0.875rem;
  color: var(--text-tertiary);
  margin-bottom: 4px;
  font-weight: 500;
}

.about-text p {
  font-size: 1rem;
  color: var(--text-primary);
  font-weight: 600;
  margin: 0;
}

.about-intro {
  text-align: center;
  padding: 24px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05), rgba(103, 194, 58, 0.05));
  border-radius: var(--radius-lg);
}

.about-intro p {
  font-size: 1.05rem;
  line-height: 1.8;
  color: var(--text-secondary);
  margin: 0;
}

/* Focus Grid */
.focus-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.focus-card {
  background: var(--bg-card);
  padding: 32px 24px;
  border-radius: var(--radius-lg);
  text-align: center;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.focus-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: #409eff;
}

.focus-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

.focus-icon.java {
  background: linear-gradient(135deg, #f89820, #5382a1);
}

.focus-icon.observability {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.focus-icon.database {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.focus-icon.distributed {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}

.focus-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.focus-card p {
  font-size: 0.9rem;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* Education Card */
.education-card {
  border: none;
  box-shadow: var(--shadow-sm);
}

.education-timeline {
  position: relative;
  padding-left: 40px;
}

.timeline-item {
  position: relative;
  padding-bottom: 32px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -40px;
  top: 4px;
  width: 16px;
  height: 16px;
  background: #409eff;
  border: 4px solid white;
  border-radius: 50%;
  box-shadow: 0 0 0 2px #409eff;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: -34px;
  top: 20px;
  width: 2px;
  height: calc(100% + 12px);
  background: var(--border-color);
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-period {
  font-size: 0.875rem;
  color: #409eff;
  font-weight: 600;
  margin-bottom: 8px;
}

.timeline-content h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.timeline-major {
  font-size: 1rem;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.timeline-desc {
  font-size: 0.95rem;
  color: var(--text-tertiary);
  line-height: 1.6;
  margin: 0;
}

/* Achievements Grid */
.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.achievement-card {
  background: var(--bg-card);
  padding: 24px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  display: flex;
  gap: 16px;
  transition: all 0.3s ease;
}

.achievement-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.achievement-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.achievement-card.paper .achievement-icon {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.achievement-card.award .achievement-icon {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}

.achievement-card.competition .achievement-icon {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.achievement-card.scholarship .achievement-icon {
  background: linear-gradient(135deg, #f56c6c, #f89898);
}

.achievement-content h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.achievement-title {
  font-size: 0.95rem;
  color: var(--text-secondary);
  margin-bottom: 4px;
  font-weight: 500;
}

.achievement-desc {
  font-size: 0.875rem;
  color: var(--text-tertiary);
  line-height: 1.5;
  margin: 0;
}

/* Skills */
.skills-container {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 32px;
  border: 1px solid var(--border-color);
}

.skill-group {
  margin-bottom: 32px;
}

.skill-group:last-child {
  margin-bottom: 0;
}

.skill-category {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.skill-category .el-icon {
  color: #409eff;
}

.skill-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.skill-tag {
  padding: 8px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  font-size: 0.9rem;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.skill-tag:hover {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

/* Contact */
.contact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.contact-card {
  background: var(--bg-card);
  padding: 32px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 20px;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
}

.contact-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.contact-card.email:hover {
  border-color: #409eff;
}

.contact-card.github:hover {
  border-color: #24292e;
}

.contact-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  flex-shrink: 0;
}

.contact-card.email .contact-icon {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.contact-card.github .contact-icon {
  background: #24292e;
}

.contact-info h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.contact-info p {
  font-size: 0.95rem;
  color: var(--text-secondary);
  margin: 0;
}

.contact-arrow {
  position: absolute;
  right: 24px;
  font-size: 20px;
  color: var(--text-tertiary);
  transition: transform 0.3s ease;
}

.contact-card:hover .contact-arrow {
  transform: translateX(4px);
}

.contact-footer {
  text-align: center;
  padding: 24px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05), rgba(103, 194, 58, 0.05));
  border-radius: var(--radius-lg);
}

.contact-footer p {
  font-size: 1rem;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-name {
    font-size: 2rem;
  }

  .about-grid {
    grid-template-columns: 1fr;
  }

  .focus-grid {
    grid-template-columns: 1fr;
  }

  .achievements-grid {
    grid-template-columns: 1fr;
  }

  .contact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
