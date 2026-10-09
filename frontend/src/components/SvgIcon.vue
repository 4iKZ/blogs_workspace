<template>
  <!-- 用 CSS mask 渲染：图标颜色跟随 currentColor，深色模式下才能看清 -->
  <span
    role="img"
    :aria-label="name"
    :class="['svg-icon', sizeClass]"
    :style="{
      width: customSize,
      height: customSize,
      maskImage: maskUrl,
      WebkitMaskImage: maskUrl,
    }"
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
const maskUrl = computed(() => `url("${iconPath.value}")`)

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
  flex-shrink: 0;
  /* 颜色继承父元素的 color，由使用方决定 */
  background-color: currentColor;
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: contain;
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
