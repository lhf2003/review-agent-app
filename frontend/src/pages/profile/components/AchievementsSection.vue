<template>
  <div class="achievements-section" :class="{ 'cards-visible': cardsVisible, 'is-dark': isDark }">
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
              class="progress-track"
              stroke-width="2.5"
            />
            <path
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              fill="none"
              class="progress-fill"
              stroke-width="2.5"
              :stroke-dasharray="`${(unlockedCount / totalCount) * 100}, 100`"
            />
          </svg>
          <span class="progress-text">{{ Math.round((unlockedCount / totalCount) * 100) || 0 }}%</span>
        </div>
        <span class="summary-text">已解锁 {{ unlockedCount }}/{{ totalCount }}</span>
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

    <template v-else>
      <div v-if="achievements.length === 0" class="empty-state">
        <el-empty description="暂无成就" :image-size="120" />
      </div>

      <template v-else>
        <!-- Filter Segmented Control -->
        <div class="filter-control">
          <button
            v-for="filter in filters"
            :key="filter.value"
            class="filter-button"
            :class="{ active: activeFilter === filter.value }"
            @click="activeFilter = filter.value"
          >
            {{ filter.label }}
            <span class="filter-count">{{ filter.count }}</span>
          </button>
        </div>

        <div class="achievements-grid">
          <div
            v-for="achievement in filteredAchievements"
            :key="achievement.code"
            class="achievement-card"
            :class="{ locked: !achievement.unlocked }"
          >
        <!-- Icon Section -->
        <div class="card-icon-wrapper">
          <div class="icon-circle" :class="{ 'is-locked': !achievement.unlocked }">
            <el-icon :size="28" class="main-icon">
              <component :is="getAchievementIcon(achievement.icon)" />
            </el-icon>
          </div>
          <div v-if="!achievement.unlocked" class="lock-badge">
            <el-icon :size="12"><Lock /></el-icon>
          </div>
          <div v-else class="check-badge">
            <el-icon :size="12"><Check /></el-icon>
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
          <div v-if="!achievement.unlocked || achievement.progress < achievement.target" class="progress-container">
            <div class="progress-track">
              <div
                class="progress-fill"
                :style="{ width: Math.min((achievement.progress / achievement.target) * 100, 100) + '%' }"
              ></div>
            </div>
            <span class="progress-label">
              {{ achievement.unlocked ? '已完成' : `${achievement.progress}/${achievement.target}` }}
            </span>
          </div>
        </div>
      </div>
    </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { defineProps, computed, ref } from 'vue'
import { useThemeStore } from '../../../stores/theme'
import {
  Check, Clock, Star, Trophy, Medal, Document,
  FolderOpened, Edit, CircleCheck, Calendar, Lock
} from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.isDark)

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

// Filter state
const activeFilter = ref('all')

const unlockedCount = computed(() => props.achievements.filter(a => a.unlocked).length)
const totalCount = computed(() => props.achievements.length)
const lockedCount = computed(() => props.achievements.filter(a => !a.unlocked).length)

// Filter options
const filters = computed(() => [
  { label: '全部', value: 'all', count: totalCount.value },
  { label: '已解锁', value: 'unlocked', count: unlockedCount.value },
  { label: '未解锁', value: 'locked', count: lockedCount.value }
])

// Filtered achievements
const filteredAchievements = computed(() => {
  switch (activeFilter.value) {
    case 'unlocked':
      return props.achievements.filter(a => a.unlocked)
    case 'locked':
      return props.achievements.filter(a => !a.unlocked)
    default:
      return props.achievements
  }
})

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
// AppleStyle Design System
$ease-apple: cubic-bezier(0.25, 1, 0.5, 1);
$ease-smooth: cubic-bezier(0.4, 0, 0.2, 1);

// Light Mode Colors
$glass-bg-light: rgba(255, 255, 255, 0.7);
$glass-border-light: rgba(255, 255, 255, 0.8);
$glass-shadow-light: 0 8px 32px rgba(0, 0, 0, 0.06);
$text-primary-light: #1d1d1f;
$text-secondary-light: #6e6e73;
$icon-bg-light: linear-gradient(135deg, #ff9500 0%, #ff6b00 100%);

// Dark Mode Colors
$glass-bg-dark: rgba(30, 30, 30, 0.6);
$glass-border-dark: rgba(255, 255, 255, 0.1);
$glass-shadow-dark: 0 8px 32px rgba(0, 0, 0, 0.4);
$text-primary-dark: #f5f5f7;
$text-secondary-dark: rgba(255, 255, 255, 0.6);
$icon-bg-dark: linear-gradient(135deg, #ff9500 0%, #ff6b00 100%);
$icon-bg-locked-dark: rgba(255, 255, 255, 0.08);

.achievements-section {
  padding: 28px;
  background: $glass-bg-light;
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid $glass-border-light;
  box-shadow: $glass-shadow-light;
  transition: background 0.3s $ease-smooth, border-color 0.3s $ease-smooth;

  &.is-dark {
    background: $glass-bg-dark;
    border-color: $glass-border-dark;
    box-shadow: $glass-shadow-dark;
  }
}

// Header
.achievements-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);

  .is-dark & {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;

  h3 {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: $text-primary-light;
    letter-spacing: -0.02em;
    line-height: 1.2;

    .is-dark & {
      color: $text-primary-dark;
    }
  }
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: $icon-bg-light;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.25);

  .is-dark & {
    background: $icon-bg-dark;
    box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
  }
}

.achievement-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-ring {
  position: relative;
  width: 44px;
  height: 44px;

  svg {
    transform: rotate(-90deg);
    width: 100%;
    height: 100%;
  }
}

.progress-track {
  stroke: rgba(0, 0, 0, 0.1);

  .is-dark & {
    stroke: rgba(255, 255, 255, 0.1);
  }
}

.progress-fill {
  stroke: #0d9488;
  stroke-linecap: round;
  transition: stroke-dasharray 0.8s $ease-apple;

  .is-dark & {
    stroke: #14b8a6;
  }
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 700;
  color: #0d9488;

  .is-dark & {
    color: #14b8a6;
  }
}

.summary-text {
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary-light;

  .is-dark & {
    color: $text-secondary-dark;
  }
}

// Filter Segmented Control
.filter-control {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  padding: 4px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  width: fit-content;

  .is-dark & {
    background: rgba(255, 255, 255, 0.08);
  }
}

.filter-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary-light;
  cursor: pointer;
  transition: all 0.2s $ease-smooth;
  position: relative;

  .is-dark & {
    color: $text-secondary-dark;
  }

  &:hover:not(.active) {
    background: rgba(0, 0, 0, 0.05);

    .is-dark & {
      background: rgba(255, 255, 255, 0.05);
    }
  }

  &.active {
    background: white;
    color: $text-primary-light;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    .is-dark & {
      background: rgba(60, 60, 60, 0.9);
      color: $text-primary-dark;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    }
  }
}

.filter-count {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.08);
  min-width: 20px;
  text-align: center;

  .is-dark & {
    background: rgba(255, 255, 255, 0.1);
  }

  .filter-button.active & {
    background: rgba(13, 148, 136, 0.15);
    color: #0d9488;

    .is-dark & {
      background: rgba(20, 184, 166, 0.2);
      color: #14b8a6;
    }
  }
}

// Grid
.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.skeleton-card {
  padding: 20px;
  background: $glass-bg-light;
  border-radius: 16px;
  border: 1px solid $glass-border-light;

  .is-dark & {
    background: $glass-bg-dark;
    border-color: $glass-border-dark;
  }
}

// Card - Modern AppleStyle
.achievement-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: $glass-bg-light;
  border-radius: 16px;
  border: 1px solid $glass-border-light;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s $ease-apple;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  .is-dark & {
    background: $glass-bg-dark;
    border-color: $glass-border-dark;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }

  &:hover {
    transform: translateY(-3px);
    background: rgba(255, 255, 255, 0.85);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
    border-color: rgba(255, 255, 255, 0.9);

    .is-dark & {
      background: rgba(255, 255, 255, 0.08);
      box-shadow: 0 12px 28px rgba(0, 0, 0, 0.3);
      border-color: rgba(255, 255, 255, 0.12);
    }
  }

  // Locked state
  &.locked {
    opacity: 0.75;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

// Icon - Simplified, no white ring
.card-icon-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 56px;
  height: 56px;
}

.icon-circle {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $icon-bg-light;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.2);
  transition: all 0.3s $ease-apple;

  .is-dark & {
    background: $icon-bg-dark;
    box-shadow: 0 4px 12px rgba(255, 149, 0, 0.25);
  }

  &.is-locked {
    background: linear-gradient(135deg, #86868b 0%, #636366 100%);
    box-shadow: none;

    .is-dark & {
      background: $icon-bg-locked-dark;
    }
  }

  .main-icon {
    color: white;
  }
}

// Badges - No white border
.lock-badge,
.check-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s $ease-smooth;

  .is-dark & {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  }
}

.lock-badge {
  background: #86868b;
  color: white;
}

.check-badge {
  background: #0d9488;
  color: white;

  .is-dark & {
    background: #14b8a6;
  }
}

// Content
.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-width: 0;
}

.text-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.achievement-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary-light;
  letter-spacing: -0.01em;
  line-height: 1.3;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  .is-dark & {
    color: $text-primary-dark;
  }

  .achievement-card.locked & {
    font-weight: 500;
  }
}

.unlock-date {
  font-size: 11px;
  color: #0d9488;
  font-weight: 600;
  background: rgba(13, 148, 136, 0.1);
  padding: 3px 8px;
  border-radius: 6px;
  white-space: nowrap;
  flex-shrink: 0;

  .is-dark & {
    color: #14b8a6;
    background: rgba(20, 184, 166, 0.15);
  }
}

.achievement-desc {
  margin: 0;
  font-size: 13px;
  color: $text-secondary-light;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;

  .is-dark & {
    color: $text-secondary-dark;
  }
}

// Progress Bar
.progress-container {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-track {
  flex: 1;
  height: 5px;
  background: rgba(0, 0, 0, 0.08);
  border-radius: 3px;
  overflow: hidden;

  .is-dark & {
    background: rgba(255, 255, 255, 0.1);
  }
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #0d9488 0%, #14b8a6 100%);
  border-radius: 3px;
  transition: width 0.6s $ease-apple;
}

.progress-label {
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary-light;
  min-width: 45px;
  text-align: right;

  .is-dark & {
    color: $text-secondary-dark;
  }
}

// Empty state
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: $text-secondary-light;

  .is-dark & {
    color: $text-secondary-dark;
  }
}
</style>
