<template>
  <div class="profile-nav">
    <div
      v-for="(nav, index) in navItems"
      :key="nav.key"
      class="nav-item"
      :class="{ active: activeSection === nav.key }"
      @click="handleNavClick(nav.key)"
    >
      <el-icon>
        <component :is="nav.icon" />
      </el-icon>
      <span>{{ nav.label }}</span>
      <div v-if="nav.count" class="nav-count">{{ nav.count }}</div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import { TrendCharts, Star, FolderOpened, DataLine } from '@element-plus/icons-vue'

const props = defineProps({
  activeSection: {
    type: String,
    default: 'overview'
  }
})

const emit = defineEmits(['section-change'])

const navItems = [
  { key: 'overview', label: '数据概览', icon: DataLine, count: null },
  { key: 'achievements', label: '学习成就', icon: Star, count: null },
  { key: 'trends', label: '趋势分析', icon: TrendCharts, count: null }
]

function handleNavClick(section) {
  emit('section-change', section)
}
</script>

<style scoped>
.profile-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: var(--el-text-color-secondary);
  position: relative;
}

.nav-item:hover {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  transform: translateX(4px);
}

.nav-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight:  500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.nav-item .el-icon {
  font-size: 20px;
}

.nav-item span {
  font-size: 14px;
  font-weight: 500;
}

.nav-count {
  position: absolute;
  top: 8px;
  right: 12px;
  background: var(--el-color-danger);
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1;
}

</style>
