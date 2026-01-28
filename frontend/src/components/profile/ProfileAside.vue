<template>
  <div class="profile-aside">
    <a class="follow-item" href="#" @click.prevent="$emit('show-followings')">
      <div class="item-title">关注了</div>
      <div class="item-count">{{ followingCount }}</div>
    </a>
    <a class="follow-item" href="#" rel="nofollow" @click.prevent="$emit('show-followers')">
      <div class="item-title">关注者</div>
      <div class="item-count">{{ followerCount }}</div>
    </a>
    <a class="more-item" href="#" rel="nofollow" @click.prevent="$emit('show-favorites')">
      <div class="item-title">收藏集</div>
      <div class="item-count">{{ favoriteCount }}</div>
    </a>
  </div>
</template>

<script setup lang="ts">
defineProps<{ followingCount: number; followerCount: number; favoriteCount: number }>()
defineEmits(['show-followings', 'show-followers', 'show-favorites'])
</script>

<style scoped>
.profile-aside {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.follow-item,
.more-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-primary);
  color: var(--text-primary);
  text-decoration: none;
}

.follow-item:hover,
.more-item:hover {
  border-color: var(--border-hover);
}

.item-title {
  font-weight: 600;
}

.item-count {
  color: var(--text-secondary);
}
</style>
