<template>
  <div class="learning-heatmap-chart">
    <div v-if="loading" v-loading="true" class="chart-loading">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="!hasData" class="chart-empty">
      <el-empty description="暂无学习记录" :image-size="100" />
    </div>
    <div v-else class="heatmap-wrapper">
      <!-- Month Navigation Header -->
      <div class="month-header">
        <button class="nav-btn" @click="goToPrevMonth">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <span class="month-title">{{ monthTitle }}</span>
        <button class="nav-btn" @click="goToNextMonth" :disabled="isCurrentMonth">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <!-- Weekday Headers -->
      <div class="weekday-headers">
        <div v-for="day in weekdays" :key="day" class="weekday-label">{{ day }}</div>
      </div>

      <!-- Month Grid -->
      <div class="month-grid">
        <div
          v-for="(day, index) in monthDays"
          :key="index"
          class="day-cell"
          :class="{
            'is-today': day.isToday,
            'is-other-month': day.isOtherMonth,
            'is-empty': !day.date
          }"
          @click="day.date && handleDayClick(day)"
          @mouseenter="day.date && handleDayHover(day)"
          @mouseleave="day.date && handleDayLeave()"
        >
          <template v-if="day.date">
            <div
              class="day-block"
              :style="{ backgroundColor: getColorByValue(day.value) }"
            >
              <span class="day-number">{{ day.dayOfMonth }}</span>
            </div>
          </template>
          <template v-else>
            <div class="day-block empty"></div>
          </template>
        </div>
      </div>

      <!-- Custom Legend -->
      <div class="custom-legend">
        <span class="legend-label">少</span>
        <div class="legend-bar">
          <div
            v-for="(color, index) in legendColors"
            :key="index"
            class="legend-item"
            :class="{ 'is-highlighted': getColorIndex(hoveredValue) === index }"
            :style="{ backgroundColor: color }"
          ></div>
        </div>
        <span class="legend-label">多</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  theme: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['date-click'])

const monthOffset = ref(0)
const hoveredValue = ref(null)

const hasData = computed(() => props.data !== null && props.data !== undefined)

const legendColors = computed(() => {
  if (props.theme === 'nebula') {
    return [
      'rgba(204, 102, 51, 0.15)',
      'rgba(204, 102, 51, 0.35)',
      'rgba(204, 102, 51, 0.55)',
      'rgba(204, 102, 51, 0.75)',
      'rgba(204, 102, 51, 0.95)'
    ]
  }
  return ['#2c2c2e', '#3d6b59', '#4ade80', '#22c55e', '#16a34a']
})

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const isCurrentMonth = computed(() => monthOffset.value >= 0)

const monthTitle = computed(() => {
  const { year, month } = getMonthRange(monthOffset.value)
  return `${year}年${month + 1}月`
})

const monthDays = computed(() => {
  const { year, month } = getMonthRange(monthOffset.value)
  const days = []

  // First day of the month
  const firstDay = new Date(year, month, 1)
  // Last day of the month
  const lastDay = new Date(year, month + 1, 0)

  // Day of week for first day (0 = Sunday)
  const firstDayOfWeek = firstDay.getDay()
  // Total days in month
  const totalDays = lastDay.getDate()

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // Empty cells for days before the first day of month
  for (let i = 0; i < firstDayOfWeek; i++) {
    days.push({ date: null })
  }

  // Days of the month
  for (let day = 1; day <= totalDays; day++) {
    const currentDate = new Date(year, month, day)
    const dateStr = formatDateToStr(currentDate)
    const value = props.data[dateStr] || 0

    days.push({
      date: dateStr,
      dayOfMonth: day,
      value: value,
      isToday: currentDate.getTime() === today.getTime(),
      isOtherMonth: false
    })
  }

  // Calculate how many rows we need (each row has 7 cells)
  const totalCellsNeeded = Math.ceil(days.length / 7) * 7
  const remainingCells = totalCellsNeeded - days.length

  // Only fill necessary empty cells to complete the last row
  for (let i = 0; i < remainingCells; i++) {
    days.push({ date: null })
  }

  return days
})

function getMonthRange(offset) {
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth()

  let targetMonth = currentMonth + offset
  let targetYear = currentYear

  // Handle year wrap-around
  while (targetMonth < 0) {
    targetMonth += 12
    targetYear--
  }
  while (targetMonth > 11) {
    targetMonth -= 12
    targetYear++
  }

  return { year: targetYear, month: targetMonth }
}

function formatDateToStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getMaxValue() {
  const allValues = monthDays.value
    .filter(d => d.date)
    .map(d => d.value)
  return Math.max(...allValues, 1)
}

function getColorIndex(value) {
  if (value === 0) return -1
  const maxValue = getMaxValue()
  const ratio = value / maxValue
  return Math.min(Math.floor(ratio * 5), 4)
}

function getColorByValue(value) {
  if (value === 0) return 'rgba(255, 248, 245, 0.03)'

  const index = getColorIndex(value)

  if (props.theme === 'nebula') {
    const colors = [
      { r: 204, g: 102, b: 51, a: 0.25 },
      { r: 204, g: 102, b: 51, a: 0.45 },
      { r: 204, g: 102, b: 51, a: 0.65 },
      { r: 204, g: 102, b: 51, a: 0.85 },
      { r: 204, g: 102, b: 51, a: 1.0 }
    ]
    const c = colors[index]
    return `rgba(${c.r}, ${c.g}, ${c.b}, ${c.a})`
  }

  const colors = ['#2c2c2e', '#3d6b59', '#4ade80', '#22c55e', '#16a34a']
  return colors[index]
}

function handleDayHover(day) {
  if (day.date) {
    hoveredValue.value = day.value
  }
}

function handleDayLeave() {
  hoveredValue.value = null
}

function goToPrevMonth() {
  monthOffset.value--
}

function goToNextMonth() {
  if (!isCurrentMonth.value) {
    monthOffset.value++
  }
}

function handleDayClick(day) {
  emit('date-click', day.date)
}

watch(() => props.data, () => {
  monthOffset.value = 0
}, { deep: true })
</script>

<style scoped lang="scss">
.learning-heatmap-chart {
  width: 100%;
  height: 100%;
  min-height: 160px;
}

.chart-loading,
.chart-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
}

.heatmap-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// Month Navigation Header
.month-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
}

.nav-btn {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid var(--glass-border);
  background: var(--glass-surface);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: var(--glass-surface-hover);
    color: var(--text-primary);
    border-color: var(--glass-border-hover);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .el-icon {
    font-size: 12px;
  }
}

.month-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

// Weekday Headers
.weekday-headers {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  padding: 0 4px;
}

.weekday-label {
  text-align: center;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
  padding: 4px 0;
}

// Month Grid
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  padding: 0 4px;
}

.day-cell {
  aspect-ratio: 1;
  min-height: 28px;

  &.is-empty {
    pointer-events: none;
  }

  &:not(.is-empty) {
    cursor: pointer;

    &:hover .day-block:not(.empty) {
      transform: scale(1.1);
      border-color: var(--glass-border-hover);
    }
  }

  &.is-today .day-block:not(.empty) {
    box-shadow: 0 0 0 2px var(--accent-primary);
  }
}

.day-block {
  width: 100%;
  height: 100%;
  min-height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &.empty {
    background: transparent;
  }
}

.day-number {
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
}

// Custom Legend
.custom-legend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--glass-border);
}

.legend-label {
  font-size: 10px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.legend-bar {
  display: flex;
  gap: 2px;
  border-radius: 3px;
  overflow: hidden;
}

.legend-item {
  width: 16px;
  height: 8px;
  transition: all 0.2s ease;

  &.is-highlighted {
    transform: scale(1.3);
    box-shadow: 0 0 8px rgba(204, 102, 51, 0.6);
    z-index: 1;
  }

  &:not(.is-highlighted) {
    opacity: 0.5;
  }
}
</style>
