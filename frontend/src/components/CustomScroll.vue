<template>
  <div class="custom-scroll" :class="{ 'hide-scrollbar': hideScrollbar }">
    <slot></slot>
  </div>
</template>

<script setup>
defineProps({
  hideScrollbar: {
    type: Boolean,
    default: false
  }
})
</script>

<style>
/* Global scrollbar styles for CustomScroll component */
.custom-scroll {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  /* Firefox */
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color) transparent; 
}

/* Chrome/Safari/Edge */
.custom-scroll::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.custom-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scroll::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
  transition: background-color 0.3s;
}

.custom-scroll::-webkit-scrollbar-thumb:hover {
  background-color: var(--el-text-color-secondary);
}

.custom-scroll.hide-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.custom-scroll.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
