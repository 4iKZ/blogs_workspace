<template>
  <div
    class="empty-state"
    :class="`empty-state--${size}`"
  >
    <div class="empty-state__icon">
      <i :class="icon" />
    </div>
    <h3 class="empty-state__title">
      {{ title }}
    </h3>
    <p
      v-if="description"
      class="empty-state__description"
    >
      {{ description }}
    </p>
    <div
      v-if="showAction"
      class="empty-state__action"
    >
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon?: string
  title?: string
  description?: string
  showAction?: boolean
  size?: 'small' | 'medium' | 'large'
}>(), {
  icon: 'fas fa-inbox',
  title: '暂无内容',
  description: '',
  showAction: false,
  size: 'medium',
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  animation: fade-in-up 0.5s cubic-bezier(0.4, 0, 0.2, 1) both;
}

.empty-state--small {
  padding: 40px 20px;
}

.empty-state--small .empty-state__icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.empty-state--small .empty-state__title {
  font-size: var(--text-sm);
}

.empty-state--medium {
  padding: 80px 20px;
}

.empty-state--medium .empty-state__icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state--medium .empty-state__title {
  font-size: var(--text-base);
}

.empty-state--large {
  padding: 120px 20px;
}

.empty-state--large .empty-state__icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.empty-state--large .empty-state__title {
  font-size: var(--text-lg);
}

.empty-state__icon {
  color: var(--text-disabled);
  opacity: 0.5;
}

.empty-state__title {
  font-family: var(--font-sans);
  font-weight: 600;
  color: var(--text-tertiary);
  margin: 0 0 8px;
}

.empty-state__description {
  font-size: var(--text-sm);
  color: var(--text-disabled);
  margin: 0 0 20px;
  max-width: 320px;
  line-height: var(--leading-relaxed);
}

.empty-state__action {
  margin-top: 8px;
}
</style>
