<script setup>
import { useRouter } from 'vue-router'
import {
  FolderOpened, Document, Notebook, Edit,
  PriceTag, TrendCharts, Clock, Setting, Warning
} from '@element-plus/icons-vue'

const router = useRouter()

const props = defineProps({
  quickActions: {
    type: Array,
    required: true
  },
  cardsVisible: {
    type: Array,
    required: true
  }
})

const getIconComponent = (iconName) => {
  const iconMap = {
    FolderOpened,
    Document,
    Notebook,
    Edit,
    PriceTag,
    TrendCharts,
    Clock,
    Setting,
    Warning
  }
  return iconMap[iconName] || Document
}

const navigateTo = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="actions-grid">
    <div
      v-for="(action, index) in quickActions"
      :key="action.label"
      class="action-card"
      :class="{ visible: cardsVisible[index + 13] }"
      :style="{ '--delay': (index + 13) * 50 + 'ms', '--icon-color': action.color }"
      @click="navigateTo(action.path)"
    >
      <div class="action-icon-wrapper">
        <el-icon class="action-icon" size="32">
          <component :is="getIconComponent(action.icon)" />
        </el-icon>
      </div>
      <span class="action-label">{{ action.label }}</span>
    </div>
  </div>
</template>

<style scoped>
/* Quick Actions Grid */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 8px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 32px 20px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  border: none;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  opacity: 0;
  transform: translateY(20px);
}

.action-card.visible {
  animation: cardFadeIn 0.6s ease forwards;
  animation-delay: var(--delay);
}

@keyframes cardFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.action-card:hover {
  transform: translateY(-6px) scale(1.03);
  box-shadow:
    0 20px 56px rgba(0, 0, 0, 0.14),
    0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.action-icon-wrapper {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: var(--icon-color);
  opacity: 0.1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.action-card:hover .action-icon-wrapper {
  opacity: 0.2;
  transform: scale(1.08);
}

.action-icon {
  color: var(--icon-color);
  transition: all 0.3s ease;
}

.action-card:hover .action-icon {
  transform: scale(1.1);
}

.action-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  text-align: center;
}

/* Dark Mode */
html.dark .action-card {
  background: rgba(30, 30, 30, 0.85);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.24),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}

/* Responsive Design */
@media (max-width: 1400px) {
  .actions-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1200px) {
  .actions-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .actions-grid {
    grid-template-columns: 1fr;
  }

  .action-card {
    padding: 24px 20px;
  }
}
</style>
