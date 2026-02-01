import { ref, onMounted, nextTick } from 'vue'

export function useAnimations() {
  const showContent = ref(false)
  const cardsVisible = ref([])

  function initAnimations() {
    nextTick(() => {
      showContent.value = true
      setTimeout(() => {
        cardsVisible.value = Array(22).fill(false).map((_, i) => true)
      }, 100)
    })
  }

  return {
    showContent,
    cardsVisible,
    initAnimations
  }
}
