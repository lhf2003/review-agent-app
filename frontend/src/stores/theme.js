import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 主题 Store - 固定深色主题
 * 系统只有一套主题，无切换功能
 */
export const useThemeStore = defineStore('theme', () => {
  // 固定为深色主题
  const isDark = ref(true)

  function initTheme() {
    // 主题已固定，无需初始化
  }

  return { isDark, initTheme }
})
