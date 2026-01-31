<template>
  <div class="achievements-section" :class="{ 'cards-visible': cardsVisible }">
    <div class="achievements-header">
      <h3>成就徽章</h3>
      <div class="achievement-summary">
        <span class="unlocked-count">已解锁 {{ unlockedCount }}/{{ totalCount }}</span>
      </div>
    </div>

    <div v-if="loading.achievements" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>
    <div v-else-if="achievements.length === 0" class="empty-state">
      <el-empty description="暂无成就" />
    </div>
    <div v-else class="achievements-grid">
      <div
        v-for="achievement in achievements"
        :key="achievement.code"
        class="achievement-card"
        :class="{ locked: !achievement.unlocked }"
      >
        <div class="achievement-icon-wrapper">
          <el-icon size="32">
            <component :is="getAchievementIcon(achievement.icon)" />
          </el-icon>
          <div v-if="achievement.unlocked" class="unlocked-badge">
            <el-icon><Check /></el-icon>
          </div>
        </div>

        <div class="achievement-content">
          <h4 class="achievement-name">{{ achievement.name }}</h4>
          <p class="achievement-desc">{{ achievement.description }}</p>
          
          <div v-if="!achievement.unlocked" class="progress-section">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: achievement.progress / achievement.target * 100 + '%' }"></div>
            </div>
            <span class="progress-text">{{ achievement.progress }}/{{ achievement.target }}</span>
          </div>
          <div v-else class="unlocked-time">
            <el-icon><Clock /></el-icon>
            <span>{{ achievement.unlockedTime }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, computed } from 'vue'
import { Check, Clock, Star, Trophy, Medal, Document, FolderOpened, Edit, CircleCheck, Calendar } from '@element-plus/icons-vue'

const props = defineProps({
  achievements: {
    type: Array,
    required: true
  },
  loading: {
    type: Object,
    default: () => ({ achievements: false, trends: false, progress: false })
  },
  cardsVisible: {
    type: Boolean,
    default: true
  }
})

const unlockedCount = computed(() => props.achievements.filter(a => a.unlocked).length)
const totalCount = computed(() => props.achievements.length)

function getAchievementIcon(iconName) {
  const iconMap = {
    'Document': Document,
    'FolderOpened': FolderOpened,
    'Edit': Edit,
    'CircleCheck': CircleCheck,
    'Calendar': Calendar,
    'Star': Star,
    'Medal': Medal,
    'Trophy': Trophy
  }
  return iconMap[iconName] || Star
}
</script>

<style scoped>
.achievements-section {
  padding: 32px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  border: none;
}

.achievements-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.achievements-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.achievement-summary {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.unlocked-count {
  color: var(--el-color-success);
  font-weight: 600;
  margin-left: 8px;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.achievement-card {
  display: flex;
  gap: 20px;
  padding: 24px;
  background: var(--el-fill-color-light);
  border-radius: 16px;
  border: none;
  transition: all 0.3s ease;
  position: relative;
}

.achievement-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.achievement-card.locked {
  opacity: 0.6;
  filter: grayscale(0.8);
  cursor: not-allowed;
}

.achievement-icon-wrapper {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--icon-color);
  border-radius: 16px;
  opacity: 0.1;
  transition: all 0.3s ease;
}

.achievement-card:hover .achievement-icon-wrapper {
  opacity: 0.2;
  transform: scale(1.05);
}

.achievement-card.locked .achievement-icon-wrapper {
  background: var(--el-text-color-placeholder);
}

.unlocked-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 20px;
  height: 20px;
  background: var(--el-color-success);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.achievement-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.achievement-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.achievement-desc {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  flex: 1;
}

.progress-section,
.unlocked-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--el-fill-color-blank);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--el-color-primary) 0%, var(--el-color-primary) 100%);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.progress-text {
  flex-shrink: 0;
  font-weight: 500;
  white-space: nowrap;
}

.unlocked-time .el-icon {
  color: var(--el-text-color-regular);
  margin-right: 4px;
}

/* Dark Mode */
html.dark .achievements-section {
  background: rgba(30, 30, 30, 0.85);
}

html.dark .achievement-card {
  background: rgba(255, 255, 255, 0.05);
}

html.dark .achievement-name {
  color: var(--el-text-color-primary);
}

html.dark .achievement-desc {
  color: var(--el-text-color-secondary);
}
</style>
