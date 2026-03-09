<template>
  <div class="achievements-section" :class="{ 'cards-visible': cardsVisible, 'is-expanded': isExpanded }">
    <!-- Collapsible Header -->
    <div class="achievements-header" @click="toggleExpand">
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
        <div class="expand-arrow" :class="{ 'is-expanded': isExpanded }">
          <el-icon><ArrowDown /></el-icon>
        </div>
      </div>
    </div>

    <!-- Expandable Content -->
    <div class="achievements-content" :class="{ 'is-expanded': isExpanded }">

    <div v-if="loading.achievements" class="loading-grid">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <div style="display: flex; gap: 12px; align-items: center;">
              <el-skeleton-item variant="circle" style="width: 40px; height: 40px" />
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
  </div>
</template>

<script setup>
import { defineProps, computed, ref } from 'vue'
import {
  Check, Clock, Star, Trophy, Medal, Document,
  FolderOpened, Edit, CircleCheck, Calendar, Lock, ArrowDown
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

// Filter state
const activeFilter = ref('all')

// Expand/Collapse state
const isExpanded = ref(false)

function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

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

// Dark Mode Colors - Fixed for single theme
$glass-bg-dark: rgba(30, 30, 30, 0.6);
$glass-border-dark: rgba(255, 255, 255, 0.1);
$glass-shadow-dark: 0 8px 32px rgba(0, 0, 0, 0.4);
$text-primary-dark: #f5f5f7;
$text-secondary-dark: rgba(255, 255, 255, 0.6);
$icon-bg-dark: linear-gradient(135deg, #5CADFF 0%, #0A84FF 100%);
$icon-bg-locked-dark: rgba(255, 255, 255, 0.08);

.achievements-section {
  padding: 28px;
  background: $glass-bg-dark;
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid $glass-border-dark;
  box-shadow: $glass-shadow-dark;
}

// Header
.achievements-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.8;
  }

  .is-expanded & {
    border-bottom-color: rgba(255, 255, 255, 0.12);
    margin-bottom: 24px;
  }
}

// Expandable Content with Animation
.achievements-content {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transform: translateY(-10px);
  transition: max-height 0.4s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.3s ease,
              transform 0.3s ease;

  &.is-expanded {
    max-height: 2000px;
    opacity: 1;
    transform: translateY(0);
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
    color: $text-primary-dark;
    letter-spacing: -0.02em;
    line-height: 1.2;
  }
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: $icon-bg-dark;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(10, 132, 255, 0.3);
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
  stroke: rgba(255, 255, 255, 0.1);
}

.progress-fill {
  stroke: #30D158;
  stroke-linecap: round;
  transition: stroke-dasharray 0.8s $ease-apple;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: 700;
  color: #30D158;
}

.summary-text {
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary-dark;
}

// Expand Arrow
.expand-arrow {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), background 0.2s ease;

  .el-icon {
    font-size: 14px;
    color: $text-secondary-dark;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.15);
  }

  &.is-expanded {
    transform: rotate(180deg);
  }
}

// Filter Segmented Control
.filter-control {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  width: fit-content;
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
  color: $text-secondary-dark;
  cursor: pointer;
  transition: all 0.2s $ease-smooth;
  position: relative;

  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.05);
  }

  &.active {
    background: rgba(60, 60, 60, 0.9);
    color: $text-primary-dark;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}

.filter-count {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  min-width: 20px;
  text-align: center;

  .filter-button.active & {
    background: rgba(10, 132, 255, 0.2);
    color: #0A84FF;
  }
}

// Grid - Compact cards (260px min width)
.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.skeleton-card {
  padding: 16px;
  background: $glass-bg-dark;
  border-radius: 14px;
  border: 1px solid $glass-border-dark;
}

// Card - Modern AppleStyle (Compact)
.achievement-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: $glass-bg-dark;
  border-radius: 14px;
  border: 1px solid $glass-border-dark;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s $ease-apple;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    background: rgba(255, 255, 255, 0.08);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    border-color: rgba(255, 255, 255, 0.12);
  }

  // Locked state
  &.locked {
    opacity: 0.75;

    &:hover {
      transform: translateY(-1px);
    }
  }
}

// Icon - Compact size (40px)
.card-icon-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
}

.icon-circle {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $icon-bg-dark;
  color: white;
  box-shadow: 0 4px 12px rgba(10, 132, 255, 0.25);
  transition: all 0.3s $ease-apple;

  .el-icon {
    font-size: 20px;
  }

  &.is-locked {
    background: $icon-bg-locked-dark;
    box-shadow: none;
  }

  .main-icon {
    color: white;
  }
}

// Badges - Compact (20px)
.lock-badge,
.check-badge {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  transition: all 0.2s $ease-smooth;

  .el-icon {
    font-size: 10px;
  }
}

.lock-badge {
  background: #86868b;
  color: white;
}

.check-badge {
  background: #30D158;
  color: white;
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
  color: $text-primary-dark;
  letter-spacing: -0.01em;
  line-height: 1.3;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  .achievement-card.locked & {
    font-weight: 500;
  }
}

.unlock-date {
  font-size: 11px;
  color: #0A84FF;
  font-weight: 600;
  background: rgba(10, 132, 255, 0.15);
  padding: 3px 8px;
  border-radius: 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.achievement-desc {
  margin: 0;
  font-size: 13px;
  color: $text-secondary-dark;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #30D158 0%, #30D158 100%);
  border-radius: 3px;
  transition: width 0.6s $ease-apple;
}

.progress-label {
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary-dark;
  min-width: 45px;
  text-align: right;
}

// Empty state
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: $text-secondary-dark;
}
</style>
