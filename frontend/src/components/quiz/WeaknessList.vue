<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts, WarningFilled, ArrowRight, PriceTag, Document } from '@element-plus/icons-vue'
import { ElMessage, ElProgress } from 'element-plus'

/**
 * 薄弱知识点列表组件
 * 展示掌握度较低的知识点，提供针对性复习
 */
const props = defineProps({
  // 知识点掌握度数据
  knowledgeData: {
    type: Array,
    default: () => []
  },
  // 显示数量限制
  limit: {
    type: Number,
    default: 10
  },
  // 掌握度阈值（低于此值视为薄弱）
  threshold: {
    type: Number,
    default: 60
  },
  // 紧凑模式
  compact: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['start-practice'])

const router = useRouter()

// 筛选薄弱知识点
const weakPoints = computed(() => {
  if (!props.knowledgeData || props.knowledgeData.length === 0) {
    return []
  }

  return props.knowledgeData
    .filter(item => (item.masteryRate || 0) < props.threshold)
    .sort((a, b) => (a.masteryRate || 0) - (b.masteryRate || 0))
    .slice(0, props.limit)
})

// 是否为空
const isEmpty = computed(() => weakPoints.value.length === 0)

// 获取掌握度等级
const getMasteryLevel = (rate) => {
  if (rate < 30) return { label: '薄弱', color: '#f56c6c', level: 'critical' }
  if (rate < 50) return { label: '待提高', color: '#e6a23c', level: 'warning' }
  return { label: '一般', color: '#409eff', level: 'normal' }
}

// 获取进度条颜色
const getProgressColor = (rate) => {
  if (rate < 30) return '#f56c6c'
  if (rate < 50) return '#e6a23c'
  return '#409eff'
}

// 开始练习
function handleStartPractice(point) {
  emit('start-practice', point)
}

// 查看相关合集
function viewRelatedCollections(point) {
  // 跳转到合集页面，并传递知识点筛选
  router.push({
    path: '/collections',
    query: { tag: point.knowledgePoint }
  })
}

// 格式化百分比
function formatPercent(value) {
  return `${Math.round(value)}%`
}

// 获取建议文案
const getSuggestion = (rate) => {
  if (rate < 30) return '建议立即复习'
  if (rate < 50) return '需要加强练习'
  return '建议定期回顾'
}
</script>

<template>
  <div class="weakness-list" :class="{ 'compact-mode': compact }">
    <!-- 空状态 -->
    <div v-if="isEmpty" class="empty-state">
      <el-icon class="empty-icon" :size="64">
        <TrendCharts />
      </el-icon>
      <div class="empty-text">
        <h4>暂无薄弱知识点</h4>
        <p>你的知识掌握情况良好，继续保持！</p>
      </div>
    </div>

    <!-- 薄弱知识点列表 -->
    <div v-else class="weakness-items">
      <div
        v-for="(point, index) in weakPoints"
        :key="point.knowledgePoint || index"
        class="weakness-item"
        :class="`level-${getMasteryLevel(point.masteryRate).level}`"
      >
        <!-- 左侧：序号和图标 -->
        <div class="item-left">
          <div class="rank-badge" :class="`rank-${Math.min(index, 2)}`">
            {{ index + 1 }}
          </div>
          <div class="point-icon">
            <el-icon :size="24" :color="getMasteryLevel(point.masteryRate).color">
              <WarningFilled v-if="point.masteryRate < 30" />
              <TrendCharts v-else />
            </el-icon>
          </div>
        </div>

        <!-- 中间：知识点信息 -->
        <div class="item-content">
          <div class="content-header">
            <h4 class="point-name">{{ point.knowledgePoint || '未知知识点' }}</h4>
            <el-tag
              :type="getMasteryLevel(point.masteryRate).level === 'critical' ? 'danger' :
                     getMasteryLevel(point.masteryRate).level === 'warning' ? 'warning' : 'primary'"
              size="small"
              effect="plain"
            >
              {{ getMasteryLevel(point.masteryRate).label }}
            </el-tag>
          </div>

          <!-- 掌握度进度条 -->
          <div class="mastery-progress">
            <div class="progress-info">
              <span class="progress-label">掌握度</span>
              <span class="progress-value" :style="{ color: getProgressColor(point.masteryRate) }">
                {{ formatPercent(point.masteryRate) }}
              </span>
            </div>
            <el-progress
              :percentage="point.masteryRate"
              :color="getProgressColor(point.masteryRate)"
              :show-text="false"
              :stroke-width="8"
            />
          </div>

          <!-- 统计信息 -->
          <div class="point-stats" v-if="!compact">
            <div class="stat-item">
              <el-icon><Document /></el-icon>
              <span>题目数: {{ point.questionCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <el-icon><WarningFilled /></el-icon>
              <span>错误率: {{ formatPercent(100 - (point.masteryRate || 0)) }}</span>
            </div>
          </div>

          <!-- 建议文案 -->
          <div class="suggestion" v-if="!compact">
            <el-icon class="suggestion-icon"><ArrowRight /></el-icon>
            <span class="suggestion-text">{{ getSuggestion(point.masteryRate) }}</span>
          </div>
        </div>

        <!-- 右侧：操作按钮 -->
        <div class="item-actions">
          <el-button
            type="primary"
            size="small"
            @click="handleStartPractice(point)"
            class="practice-button"
          >
            <el-icon><Document /></el-icon>
            练习
          </el-button>
          <el-button
            v-if="!compact"
            text
            size="small"
            @click="viewRelatedCollections(point)"
            class="collection-button"
          >
            <el-icon><PriceTag /></el-icon>
            关联合集
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部提示 -->
    <div v-if="!isEmpty" class="list-footer">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          <span>建议优先练习掌握度低于 {{ threshold }}% 的知识点</span>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.weakness-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.empty-icon {
  color: var(--el-color-success);
  opacity: 0.6;
}

.empty-text {
  text-align: center;

  h4 {
    margin: 0 0 8px 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 0;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

// 薄弱知识点列表
.weakness-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weakness-item {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 2px solid var(--el-border-color-light);
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
    border-color: var(--el-color-primary-light-5);
  }

  // 等级样式
  &.level-critical {
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.05) 0%, var(--el-bg-color) 100%);
    border-color: rgba(245, 108, 108, 0.3);
  }

  &.level-warning {
    background: linear-gradient(135deg, rgba(230, 162, 60, 0.05) 0%, var(--el-bg-color) 100%);
    border-color: rgba(230, 162, 60, 0.3);
  }

  .compact-mode & {
    padding: 14px 16px;
    gap: 12px;
  }
}

// 左侧序号和图标
.item-left {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.rank-badge {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: white;
  background: var(--el-text-color-secondary);

  &.rank-0 {
    background: linear-gradient(135deg, #f56c6c 0%, #ff8787 100%);
    box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
  }

  &.rank-1 {
    background: linear-gradient(135deg, #e6a23c 0%, #f0b857 100%);
    box-shadow: 0 4px 12px rgba(230, 162, 60, 0.4);
  }

  &.rank-2 {
    background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  }
}

.point-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

// 内容区域
.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.point-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 掌握度进度条
.mastery-progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.progress-label {
  color: var(--el-text-color-secondary);
}

.progress-value {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

// 统计信息
.point-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);

  .el-icon {
    font-size: 16px;
  }
}

// 建议文案
.suggestion {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.suggestion-icon {
  font-size: 16px;
  color: var(--el-color-primary);
}

.suggestion-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
}

// 操作按钮
.item-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.practice-button {
  white-space: nowrap;

  &:hover {
    transform: translateX(2px);
  }
}

.collection-button {
  &:hover {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

// 底部提示
.list-footer {
  margin-top: 8px;
}

// 深色模式适配
:global(.dark) .weakness-item {
  &.level-critical {
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.15) 0%, rgba(40, 40, 40, 0.8) 100%);
  }

  &.level-warning {
    background: linear-gradient(135deg, rgba(230, 162, 60, 0.15) 0%, rgba(40, 40, 40, 0.8) 100%);
  }

  .suggestion {
    background: rgba(255, 255, 255, 0.05);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .weakness-item {
    flex-direction: column;
    padding: 16px;
  }

  .item-left {
    flex-direction: row;
    width: 100%;
    justify-content: flex-start;
  }

  .item-actions {
    width: 100%;
    flex-direction: row;

    .practice-button {
      flex: 1;
    }
  }

  .compact-mode .weakness-item {
    flex-direction: row;
  }

  .compact-mode .item-actions {
    width: auto;
  }
}
</style>
