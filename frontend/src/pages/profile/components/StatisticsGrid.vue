<script setup>
import {
  Document, DataLine, Collection, CollectionTag,
  CircleCheck, Calendar, ArrowUp, ArrowDown
} from '@element-plus/icons-vue'

const props = defineProps({
  animatedStats: {
    type: Object,
    required: true
  },
  statCards: {
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
    Document,
    DataLine,
    Collection,
    CollectionTag,
    CircleCheck,
    Calendar
  }
  return iconMap[iconName] || Document
}
</script>

<template>
  <div class="stats-grid">
    <div
      v-for="(card, index) in statCards"
      :key="card.key"
      class="stat-card"
      :class="{ visible: cardsVisible[index + 7] }"
      :style="{ '--delay': (index + 7) * 50 + 'ms', '--icon-color': card.color }"
    >
      <div class="stat-icon-wrapper">
        <el-icon class="stat-icon">
          <component :is="getIconComponent(card.icon)" />
        </el-icon>
      </div>
      <div class="stat-content">
        <div class="stat-number">{{ animatedStats[card.key] }}</div>
        <div class="stat-label">{{ card.label }}</div>
        <div v-if="card.trend" class="stat-trend" :class="card.trend > 0 ? 'up' : 'down'">
          <el-icon>
            <component :is="card.trend > 0 ? ArrowUp : ArrowDown" />
          </el-icon>
          <span>{{ Math.abs(card.trend) }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Statistics Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 8px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px;
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

.stat-card.visible {
  animation: cardFadeIn 0.6s ease forwards;
  animation-delay: var(--delay);
}

@keyframes cardFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow:
    0 16px 48px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.stat-card .stat-icon-wrapper {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: var(--icon-color);
  opacity: 0.1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.stat-card:hover .stat-icon-wrapper {
  opacity: 0.2;
  transform: scale(1.05);
}

.stat-card .stat-icon {
  font-size: 32px;
  color: var(--icon-color);
  transition: all 0.3s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
}

.stat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
}

.stat-trend.up {
  color: #67C23A;
}

.stat-trend.down {
  color: #F56C6C;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: 24px 20px;
  }
}
</style>
