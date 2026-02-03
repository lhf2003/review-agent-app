import { ref, onMounted, nextTick } from 'vue'

export function useAnimations() {
  const showContent = ref(false)
  const cardsVisible = ref(false)

  function initAnimations() {
    nextTick(() => {
      showContent.value = true
      setTimeout(() => {
        cardsVisible.value = true
      }, 100)
    })
  }

  return {
    showContent,
    cardsVisible,
    initAnimations
  }
}
