<template>
  <img 
    :src="iconPath" 
    :alt="name"
    :class="['svg-icon', sizeClass]"
    :style="{ width: customSize, height: customSize }"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  size?: 'small' | 'medium' | 'large' | string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium'
})

const iconPath = computed(() => `/images/icons/${props.name}.svg`)

const sizeClass = computed(() => {
  if (props.size === 'small' || props.size === 'medium' || props.size === 'large') {
    return `svg-icon--${props.size}`
  }
  return ''
})

const customSize = computed(() => {
  if (!['small', 'medium', 'large'].includes(props.size)) {
    return props.size
  }
  return undefined
})
</script>

<style scoped>
.svg-icon {
  display: inline-block;
  vertical-align: middle;
  transition: var(--transition);
}

.svg-icon--small {
  width: 14px;
  height: 14px;
}

.svg-icon--medium {
  width: 18px;
  height: 18px;
}

.svg-icon--large {
  width: 24px;
  height: 24px;
}

.svg-icon:hover {
  transform: scale(1.1);
}
</style>
