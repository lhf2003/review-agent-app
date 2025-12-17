<script setup>
import { inject, computed, ref, onMounted, watchEffect } from 'vue'

const props = defineProps({
  index: { type: Number, required: true }
})

const stackProps = inject('scrollStackProps')
const scrollTop = inject('scrollTop')
const scrollContainer = inject('scrollContainer')
const itemRef = ref(null)
const offsetTop = ref(0)

onMounted(() => {
  if (itemRef.value && scrollContainer?.value) {
    const itemRect = itemRef.value.getBoundingClientRect()
    const containerRect = scrollContainer.value.getBoundingClientRect()
    const currentScroll = scrollContainer.value.scrollTop
    
    offsetTop.value = itemRect.top - containerRect.top + currentScroll
  }
})

// Recalculate offset if needed (e.g. window resize)
// For now, keep it simple.

const itemStyle = computed(() => {
  if (!stackProps) return {}
  
  const { 
    itemStackDistance, 
    stackPosition, 
    itemScale, 
    baseScale, 
    blurAmount,
  } = stackProps

  // Extract numeric value from stackPosition (assuming '20px' format)
  const stackPosVal = parseInt(stackPosition) || 20
  
  // Calculate the sticky top position for this specific item
  const stickyTop = stackPosVal + props.index * itemStackDistance
  
  // Calculate how far the scroll has passed the point where this item sticks
  // Item sticks when scrollTop >= offsetTop - stickyTop
  const stickThreshold = offsetTop.value - stickyTop
  const distancePastStick = Math.max(0, scrollTop.value - stickThreshold)
  
  // Calculate scale: decreases as we scroll past (item goes deeper into stack)
  // We clamp it so it doesn't get too small
  const scale = Math.max(0.8, baseScale - (distancePastStick / 500) * itemScale)
  
  // Calculate blur
  const blur = Math.min(blurAmount, (distancePastStick / 200) * blurAmount)

  return {
    position: 'sticky',
    top: `${stickyTop}px`,
    zIndex: props.index + 1,
    transform: `scale(${scale})`,
    filter: `blur(${blur}px)`,
    transformOrigin: 'top center',
    transition: 'all 0.1s linear', // smooth transitions
    marginBottom: `${stackPosVal}px` // Add some spacing
  }
})
</script>

<template>
  <div class="scroll-stack-item" ref="itemRef" :style="itemStyle">
    <slot></slot>
  </div>
</template>

<style scoped>
.scroll-stack-item {
  width: 100%;
  will-change: transform, filter;
}
</style>
