<template>
  <div class="config-layout">
    <div class="sidebar-wrapper">
      <ConfigSidebar />
    </div>
    <CustomScroll class="content-scroll">
      <div class="content-inner">
        <router-view />
      </div>
    </CustomScroll>
  </div>
</template>

<script setup>
import ConfigSidebar from './components/ConfigSidebar.vue'
import CustomScroll from '../../components/CustomScroll.vue'
</script>

<style scoped>
.config-layout {
  display: flex;
  gap: 40px;
  max-width: 1600px;
  width: 100%;
  height: 100%;
  margin: 0 auto;
  padding: 20px;
  align-items: flex-start;
}

@media (max-width: 768px) {
  .config-layout {
    padding: 16px;
  }
}

.sidebar-wrapper {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
}

.content-scroll {
  flex: 1;
  height: 100%;
  min-width: 0;
}

.content-inner {
  padding: 0 20px 80px 0;
}
</style>
