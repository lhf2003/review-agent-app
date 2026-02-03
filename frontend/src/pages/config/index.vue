<template>
  <div class="config-layout">
    <div class="sidebar-wrapper">
      <ConfigSidebar />
    </div>
    <div class="content-wrapper">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import ConfigSidebar from './components/ConfigSidebar.vue'
</script>

<style scoped>
.config-layout {
  display: flex;
  gap: 40px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
  align-items: flex-start;
}

.sidebar-wrapper {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
}

.content-wrapper {
  flex: 1;
  min-width: 0;
  padding-bottom: 80px;
}
</style>
