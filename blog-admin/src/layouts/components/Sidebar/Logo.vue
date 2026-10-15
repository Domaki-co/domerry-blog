<template>
  <img
    class="logo-img"
    :src="currentLogo"
    :width="size"
    :height="size"
    alt="Logo"
    @error="onError"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useSettingsStore } from '@/store/modules/settings'

defineProps({
  size: {
    type: [Number, String],
    default: 32
  },
})

const defaultLogo = import.meta.env.VITE_APP_LOGO || '/logo.svg'
const settingsStore = useSettingsStore()
const imgError = ref(false)

// 当 store 中的 logo 更新时，重置错误状态
watch(
  () => settingsStore.logo,
  () => {
    imgError.value = false
  }
)

const currentLogo = computed(() => {
  if (imgError.value) {
    return defaultLogo
  }
  return settingsStore.logo || defaultLogo
})

const onError = () => {
  imgError.value = true
}
</script>

<style scoped>
.logo-img {
  object-fit: contain;
  transition: transform 0.3s ease;
}

.logo-img:hover {
  transform: scale(1.08);
}
</style> 