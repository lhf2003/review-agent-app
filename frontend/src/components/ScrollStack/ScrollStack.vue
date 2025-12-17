<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  itemDistance: { type: Number, default: 100 },
  itemScale: { type: Number, default: 0.05 },
  itemStackDistance: { type: Number, default: 30 },
  stackPosition: { type: String, default: "20px" },
  baseScale: { type: Number, default: 1 },
  rotationAmount: { type: Number, default: 0 },
  blurAmount: { type: Number, default: 0 },
})

const scrollTop = ref(0)
const containerRef = ref(null)

const handleScroll = (e) => {
  scrollTop.value = e.target.scrollTop
}

provide('scrollStackProps', props)
provide('scrollTop', scrollTop)
provide('scrollContainer', containerRef)

defineExpose({
  containerRef
})
</script>

<template>
  <div class="scroll-stack-container" ref="containerRef" @scroll="handleScroll">
    <div class="scroll-stack-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.scroll-stack-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  position: relative;
  scroll-behavior: smooth;
}

.scroll-stack-content {
  position: relative;
  padding-bottom: 100px; /* Extra space for scrolling */
}
</style>
