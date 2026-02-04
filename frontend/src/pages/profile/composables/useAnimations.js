import { ref, onMounted, nextTick } from 'vue'

export function useAnimations() {
  const showContent = ref(false)
  // Initialize as an array with 20 boolean values (indices 0-19)
  // Used by: StatisticsGrid (7-12), RecentActivity (6), QuickActions (13-16)
  const cardsVisible = ref(new Array(20).fill(false))
  // Boolean for AchievementsSection
  const achievementsVisible = ref(false)

  function initAnimations() {
    nextTick(() => {
      showContent.value = true
      setTimeout(() => {
        // Set all cards to visible
        cardsVisible.value = cardsVisible.value.map(() => true)
        achievementsVisible.value = true
      }, 100)
    })
  }

  return {
    showContent,
    cardsVisible,
    achievementsVisible,
    initAnimations
  }
}
