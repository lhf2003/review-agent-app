<template>
  <div class="achievements-section" :class="{ 'cards-visible': cardsVisible }">
    <div class="achievements-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon><Trophy /></el-icon>
        </div>
        <h3>成就徽章</h3>
      </div>
      <div class="achievement-summary">
        <div class="progress-ring">
          <svg viewBox="0 0 36 36">
            <path
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              fill="none"
              stroke="var(--el-fill-color-darker)"
              stroke-width="3"
            />
            <path
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              fill="none"
              stroke="url(#gradient-teal)"
              stroke-width="3"
              :stroke-dasharray="`${(unlockedCount / totalCount) * 100}, 100`"
              class="progress-path"
            />
            <defs>
              <linearGradient id="gradient-teal" x1="0%" y1="0%" x2="100%" y2="0%">
    <stop offset="0%" stop-color="var(--el-color-primary)" />
    <stop offset="100%" stop-color="var(--el-color-primary-light-3)" />
  </linearGradient>
</defs>
          </svg>
          <span class="progress-text">{{ Math.round((unlockedCount / totalCount) * 100) || 0 }}%</span>
        </div>
        <span class="summary-text">已解锁 {{ unlockedCount }} / {{ totalCount }}</span>
      </div>
    </div>

    <div v-if="loading.achievements" class="loading-grid">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <div style="display: flex; gap: 16px; align-items: center;">
              <el-skeleton-item variant="circle" style="width: 56px; height: 56px" />
              <div style="flex: 1">
                <el-skeleton-item variant="text" style="width: 50%" />
                <el-skeleton-item variant="text" style="width: 80%; margin-top: 8px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>
    
    <div v-else-if="achievements.length === 0" class="empty-state">
      <el-empty description="暂无成就" :image-size="120" />
    </div>

    <div v-else class="achievements-grid">
      <div
        v-for="achievement in achievements"
        :key="achievement.code"
        class="achievement-card"
        :class="{ locked: !achievement.unlocked }"
      >
        <!-- Icon Section -->
        <div class="card-icon-wrapper">
          <div class="icon-ring">
            <div class="icon-circle">
              <el-icon :size="24" class="main-icon">
                <component :is="getAchievementIcon(achievement.icon)" />
              </el-icon>
            </div>
          </div>
          <div v-if="!achievement.unlocked" class="lock-badge">
            <el-icon><Lock /></el-icon>
          </div>
          <div v-else class="check-badge">
            <el-icon><Check /></el-icon>
          </div>
        </div>

        <!-- Content Section -->
        <div class="card-content">
          <div class="text-header">
            <h4 class="achievement-name">{{ achievement.name }}</h4>
            <span v-if="achievement.unlocked" class="unlock-date">{{ formatDate(achievement.unlockedTime) }}</span>
          </div>
          
          <p class="achievement-desc">{{ achievement.description }}</p>
          
          <!-- Progress Bar (Only for locked or partially completed) -->
          <div class="progress-container">
            <div class="progress-track">
              <div 
                class="progress-fill" 
                :style="{ width: Math.min((achievement.progress / achievement.target) * 100, 100) + '%' }"
                :class="{ completed: achievement.unlocked }"
              ></div>
            </div>
            <span class="progress-label">
              {{ achievement.unlocked ? '已完成' : `${achievement.progress}/${achievement.target}` }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, computed } from 'vue'
import { 
  Check, Clock, Star, Trophy, Medal, Document, 
  FolderOpened, Edit, CircleCheck, Calendar, Lock 
} from '@element-plus/icons-vue'

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

function formatDate(dateStr) {
  if (!dateStr) return ''
  // 简单格式化，假设输入是标准日期字符串
  return dateStr.split(' ')[0]
}
</script>

<style scoped lang="scss">
// Variables consistent with AchievementNotification
$color-primary: var(--el-color-primary); // Use theme primary
$color-accent: var(--el-color-warning); // Use theme warning
$ease-spring: cubic-bezier(0.25, 1, 0.5, 1);

.achievements-section {
  padding: 32px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 4px 24px -1px rgba(0, 0, 0, 0.05),
    0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  transition: opacity 0.4s ease;
  
  // Dark mode
  :global(.dark) & {
    background: rgba(0, 0, 0, 0.4);
    box-shadow:
      0 4px 24px -1px rgba(0, 0, 0, 0.4),
      0 0 0 1px rgba(255, 255, 255, 0.08) inset;
  }
}

// Header
.achievements-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  
  :global(.dark) & {
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  
  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    letter-spacing: -0.01em;
  }
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--el-color-warning-light-3), var(--el-color-warning-light-5));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-warning-dark-2);
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.2);
  
  :global(.dark) & {
     background: linear-gradient(135deg, var(--el-color-warning-dark-2), var(--el-color-warning));
     color: #FFF;
     box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }
}

.achievement-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-ring {
  position: relative;
  width: 40px;
  height: 40px;
  
  svg {
    transform: rotate(-90deg);
    width: 100%;
    height: 100%;
  }
  
  .progress-path {
    transition: stroke-dasharray 0.6s ease;
    stroke-linecap: round;
  }
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  font-weight: 700;
  color: $color-primary;
}

.summary-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

// Grid
.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.skeleton-card {
  padding: 24px;
  background: var(--el-fill-color-light);
  border-radius: 20px;
}

// Card
.achievement-card {
  display: flex;
  gap: 20px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: all 0.3s $ease-spring;
  position: relative;
  overflow: hidden;
  cursor: pointer;

  :global(.dark) & {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.05);
  }

  &:hover {
    transform: translateY(-4px) scale(1.01);
    background: rgba(255, 255, 255, 0.7);
    box-shadow: 
      0 12px 32px -8px rgba(0, 0, 0, 0.08),
      0 0 0 1px rgba(255, 255, 255, 0.8) inset;
    
    :global(.dark) & {
      background: rgba(255, 255, 255, 0.06);
      box-shadow: 0 12px 32px -8px rgba(0, 0, 0, 0.3);
    }
  }
}

// Icon Styling
.card-icon-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 56px;
  height: 56px;
}

.icon-ring {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, #FFF, #F0F0F0);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  
  :global(.dark) & {
    background: linear-gradient(135deg, #444, #333);
  }
}

.icon-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 30% 30%, var(--el-color-warning-light-8) 0%, var(--el-color-warning-light-5) 100%);
  color: var(--el-color-warning-dark-2);
  
  :global(.dark) & {
    background: radial-gradient(circle at 30% 30%, var(--el-color-warning-dark-2) 0%, var(--el-color-warning) 100%);
    color: #FFFbeb;
  }
}

// Locked State Styling
.achievement-card.locked {
  .icon-circle {
    background: var(--el-fill-color-dark);
    color: var(--el-text-color-placeholder);
  }
  
  .achievement-name {
    color: var(--el-text-color-regular);
  }
  
  .achievement-desc {
    color: var(--el-text-color-placeholder);
  }
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  }
}

// Badges
.lock-badge, .check-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #FFF;
  font-size: 12px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  
  :global(.dark) & {
    border-color: #333;
  }
}

.lock-badge {
  background: var(--el-text-color-secondary);
  color: white;
}

.check-badge {
  background: $color-primary;
  color: white;
}

// Content
.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.text-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.achievement-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.3;
}

.unlock-date {
  font-size: 12px;
  color: var(--el-color-success);
  font-weight: 500;
}

.achievement-desc {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// Progress Bar
.progress-container {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: var(--el-fill-color-dark);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--el-color-primary), var(--el-color-primary-light-3));
  border-radius: 3px;
  transition: width 0.6s ease;
  
  &.completed {
    background: var(--el-color-primary);
  }
}

.progress-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  min-width: 40px;
  text-align: right;
}
</style>
