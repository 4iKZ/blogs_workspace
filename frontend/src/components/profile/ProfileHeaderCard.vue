<template>
  <div class="profile-header-card">
    <div class="profile-main">
      <div class="avatar-section">
        <el-avatar :size="avatarSize" :src="user.avatar || ''" class="avatar">
          {{ displayInitial }}
        </el-avatar>
      </div>

      <div class="profile-content">
        <div class="profile-top">
          <div class="identity-block">
            <div class="title-row">
              <h1 class="username">{{ displayName }}</h1>
              <span v-if="roleLabel" class="role-badge">{{ roleLabel }}</span>
            </div>
          </div>

          <div v-if="$slots.action" class="action-box">
            <slot name="action" />
          </div>
        </div>

        <div class="position-info">
          <SvgIcon name="user" size="14px" />
          <span>{{ positionText }}</span>
          <span class="divider">|</span>
          <span>{{ companyText }}</span>
        </div>

        <div class="intro">{{ bioText }}</div>

        <div class="stats-row">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <span class="stat-count">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from '../SvgIcon.vue'

interface ProfileHeaderUser {
  username?: string | null
  nickname?: string | null
  avatar?: string | null
  bio?: string | null
  website?: string | null
  position?: string | null
  company?: string | null
  role?: string | null
  articleCount?: number | null
  followerCount?: number | null
  followingCount?: number | null
}

const props = withDefaults(
  defineProps<{
    user: ProfileHeaderUser
    avatarSize?: number
  }>(),
  {
    avatarSize: 96,
  },
)

const displayName = computed(() => props.user.nickname || props.user.username || '未命名用户')

const displayInitial = computed(() => {
  const source = props.user.nickname || props.user.username || 'U'
  return source.charAt(0).toUpperCase()
})

const positionText = computed(() => props.user.position || '职位未填写')
const companyText = computed(() => props.user.company || '公司未填写')
const bioText = computed(() => props.user.bio || '这个用户很懒，什么都没写')

const roleLabel = computed(() => {
  if (props.user.role === 'admin') return '管理员'
  return ''
})

const stats = computed(() => [
  { label: '关注', value: props.user.followingCount ?? 0 },
  { label: '粉丝', value: props.user.followerCount ?? 0 },
  { label: '文章', value: props.user.articleCount ?? 0 },
])
</script>

<style scoped>
.profile-header-card {
  margin-bottom: 24px;
  padding: 28px 32px;
  border-radius: 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #e5e6eb);
  box-shadow: 0 12px 32px rgba(31, 35, 41, 0.08);
}

.profile-main {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar {
  border: 4px solid #f4f6fa;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.avatar :deep(img) {
  object-fit: cover;
}

.profile-content {
  flex: 1;
  min-width: 0;
}

.profile-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.identity-block {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.username {
  margin: 0;
  color: var(--text-primary, #1f2329);
  font-size: clamp(1.75rem, 2.8vw, 2.25rem);
  font-weight: 700;
  line-height: 1.2;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(30, 128, 255, 0.1);
  color: #1e80ff;
  font-size: 0.8125rem;
  font-weight: 600;
}

.position-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  color: var(--text-secondary, #515767);
  font-size: 0.9375rem;
}

.divider {
  color: #c9cdd4;
}

.intro {
  margin-top: 12px;
  color: var(--text-secondary, #515767);
  font-size: 0.95rem;
  line-height: 1.75;
  white-space: pre-wrap;
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 28px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eef0f3;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 72px;
}

.stat-count {
  color: var(--text-primary, #1f2329);
  font-size: 1.375rem;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  color: #86909c;
  font-size: 0.875rem;
}

.action-box {
  display: flex;
  flex-shrink: 0;
  align-items: flex-start;
}

@media (max-width: 900px) {
  .profile-header-card {
    padding: 24px;
  }

  .profile-main,
  .profile-top,
  .identity-block {
    flex-direction: column;
  }

  .action-box {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .profile-header-card {
    margin-bottom: 16px;
    padding: 18px;
    border-radius: 14px;
  }

  .profile-main {
    gap: 16px;
  }

  .username {
    font-size: 1.5rem;
  }

  .position-info,
  .intro {
    font-size: 0.875rem;
  }

  .stats-row {
    gap: 16px;
  }

  .stat-item {
    min-width: calc(33.333% - 12px);
  }
}

@media (max-width: 480px) {
  .profile-header-card {
    padding: 16px;
  }

  .title-row {
    gap: 8px;
  }

  .username {
    font-size: 1.25rem;
  }

  .role-badge,
  .stat-label {
    font-size: 0.75rem;
  }

  .position-info {
    gap: 4px;
  }

  .divider {
    display: none;
  }

  .stat-item {
    min-width: calc(50% - 8px);
  }

  .stat-count {
    font-size: 1.125rem;
  }
}
</style>