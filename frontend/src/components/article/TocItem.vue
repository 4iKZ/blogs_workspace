<template>
  <div class="toc-item">
    <div
      :class="['toc-link', { active: activeId === item.id }]"
      :style="{ paddingLeft: (item.level - 1) * 12 + 'px' }"
    >
      <span class="toc-text" @click="handleNavigate(item)">{{ item.title }}</span>
      <span
        v-if="item.children.length > 0"
        class="expand-btn"
        @click="toggleExpand(item, $event)"
      >
        <svg
          class="expand-icon"
          :class="{ collapsed: !item.expanded }"
          viewBox="0 0 24 24"
          fill="none"
        >
          <path
            d="M6 9l6 6 6-6"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </span>
    </div>
    <div v-if="item.children.length > 0" v-show="item.expanded" class="toc-children">
      <TocItem
        v-for="child in item.children"
        :key="child.id"
        :item="child"
        :active-id="activeId"
        @navigate="(i: TocItemData) => emit('navigate', i)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
interface TocItemData {
  id: string
  title: string
  level: number
  index: number
  children: TocItemData[]
  expanded: boolean
}

defineProps<{
  item: TocItemData
  activeId: string
}>()

const emit = defineEmits<{
  (e: 'navigate', item: TocItemData): void
}>()

const toggleExpand = (item: TocItemData, event: MouseEvent) => {
  event.stopPropagation()
  item.expanded = !item.expanded
}

const handleNavigate = (item: TocItemData) => {
  emit('navigate', item)
}
</script>

<style scoped>
.toc-item {
  width: 100%;
}

.toc-link {
  display: flex;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
  line-height: 1.5;
  width: 100%;
}

.toc-link:hover {
  color: var(--color-blue-500);
  background-color: var(--bg-secondary);
}

.toc-link.active {
  color: var(--color-blue-500);
  background-color: rgba(59, 130, 246, 0.1);
  font-weight: 500;
}

.toc-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.expand-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-left: var(--space-1);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.expand-btn:hover {
  background-color: var(--bg-tertiary, rgba(0, 0, 0, 0.05));
}

.expand-icon {
  width: 14px;
  height: 14px;
  opacity: 0.6;
  transition: transform var(--duration-normal) var(--ease-default);
}

.expand-icon.collapsed {
  transform: rotate(-90deg);
}

.toc-children {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

/* 暗色模式支持 */
.dark .toc-link {
  color: var(--text-secondary);
}

.dark .toc-link:hover {
  color: var(--color-blue-500);
  background-color: var(--bg-secondary);
}

.dark .toc-link.active {
  color: var(--color-blue-500);
  background-color: rgba(59, 130, 246, 0.15);
}
</style>