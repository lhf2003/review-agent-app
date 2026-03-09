<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  School,
  TrendCharts,
  RefreshRight,
  Share,
  ArrowRight,
  Connection,
  Check,
  CircleCheck,
  Timer,
  Notebook
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { learningPathApi } from '../../api/learningPath'
import CustomScroll from '../CustomScroll.vue'

/**
 * 学习路径推荐引擎组件（基于标签关系）
 * 根据用户的薄弱知识点和标签依赖关系智能推荐学习路径
 */
const props = defineProps({
  // 限制推荐数量
  limit: {
    type: Number,
    default: 5
  },
  // 是否自动加载
  autoLoad: {
    type: Boolean,
    default: true
  },
  // 是否显示进度概览
  showOverview: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['recommendations-loaded', 'path-selected'])

const router = useRouter()

// 跳转到知识图谱
function goToKnowledgeGraph() {
  router.push('/knowledge-graph')
}

// 数据状态
const loading = ref(false)
const recommendations = ref([])
const progressOverview = ref(null)
const selectedPath = ref(null)

// 加载推荐
async function loadRecommendations() {
  loading.value = true
  try {
    const [pathsData, overviewData] = await Promise.all([
      learningPathApi.getRecommendations(props.limit),
      props.showOverview ? learningPathApi.getLearningProgressOverview() : Promise.resolve(null)
    ])

    recommendations.value = pathsData || []
    progressOverview.value = overviewData

    emit('recommendations-loaded', recommendations.value)
  } catch (error) {
    console.error('加载学习路径失败:', error)
    ElMessage.error('加载推荐失败')
  } finally {
    loading.value = false
  }
}

// 选择学习路径
function selectPath(path) {
  selectedPath.value = selectedPath.value?.id === path.id ? null : path
  emit('path-selected', selectedPath.value)
}

// 开始学习
function handleStartLearning(path) {
  // 跳转到该标签相关的测验或合集
  if (path.steps?.length > 0) {
    const firstUncompleted = path.steps.find(s => !s.completed)
    if (firstUncompleted) {
      router.push({
        path: '/quiz',
        query: { tagId: firstUncompleted.tagId, mode: 'practice' }
      })
    } else {
      // 都完成了，跳转到核心目标
      router.push({
        path: '/quiz',
        query: { tagId: path.id, mode: 'practice' }
      })
    }
  }
}

// 刷新推荐
async function refresh() {
  await loadRecommendations()
  ElMessage.success('推荐已更新')
}

// 获取难度标签
function getDifficultyLabel(difficulty) {
  const labels = {
    'beginner': { text: '初级', type: 'success' },
    'intermediate': { text: '中级', type: 'warning' },
    'advanced': { text: '高级', type: 'danger' },
    'expert': { text: '专家', type: 'info' }
  }
  return labels[difficulty] || { text: '未知', type: 'info' }
}

// 获取路径类型标签
function getPathTypeLabel(pathType) {
  const labels = {
    'sequential': { text: '顺序学习', icon: 'ArrowRight' },
    'parallel': { text: '并行学习', icon: 'Connection' },
    'beginner': { text: '入门路线', icon: 'School' }
  }
  return labels[pathType] || { text: '标准路线', icon: 'Notebook' }
}

// 格式化时间
function formatTime(minutes) {
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`
}

// 计算进度颜色
function getProgressColor(rate) {
  if (rate >= 80) return '#67C23A'
  if (rate >= 60) return '#E6A23C'
  if (rate >= 40) return '#409EFF'
  return '#F56C6C'
}

// 组件挂载时加载
onMounted(() => {
  if (props.autoLoad) {
    loadRecommendations()
  }
})

// 暴露刷新方法
defineExpose({
  refresh,
  loadRecommendations
})
</script>

<template>
  <div class="learning-path-recommender">
    <!-- 头部 -->
    <div class="recommender-header">
      <div class="header-left">
        <div class="header-icon-wrapper">
          <el-icon :size="20" class="header-icon"><Connection /></el-icon>
        </div>
        <div class="header-text">
          <h3 class="header-title">智能学习路径</h3>
          <p class="header-subtitle">基于标签关系构建的个性化学习路线</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button
          :icon="RefreshRight"
          :loading="loading"
          @click="refresh"
          circle
          class="refresh-btn"
          plain
        />
      </div>
    </div>

    <!-- 学习进度概览 -->
    <div v-if="showOverview && progressOverview" class="progress-overview">
      <div class="overview-card">
        <div class="overview-stats">
          <div class="stat-item">
            <div class="stat-value" :style="{ color: getProgressColor(progressOverview.overallProgress) }">
              {{ progressOverview.overallProgress }}%
            </div>
            <div class="stat-label">总体进度</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value text-success">{{ progressOverview.masteredCount }}</div>
            <div class="stat-label">已掌握</div>
          </div>
          <div class="stat-item">
            <div class="stat-value text-warning">{{ progressOverview.learningCount }}</div>
            <div class="stat-label">学习中</div>
          </div>
          <div class="stat-item">
            <div class="stat-value text-danger">{{ progressOverview.weakCount }}</div>
            <div class="stat-label">待加强</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <div style="display: flex; gap: 16px; align-items: center;">
              <el-skeleton-item variant="rect" style="width: 48px; height: 48px; border-radius: 12px" />
              <div style="flex: 1">
                <el-skeleton-item variant="text" style="width: 60%" />
                <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="recommendations.length === 0" class="empty-state">
      <div class="empty-icon-wrapper">
        <el-icon :size="32"><TrendCharts /></el-icon>
      </div>
      <div class="empty-text">
        <h4>暂无推荐</h4>
        <p>继续学习后，系统将基于标签关系为你智能推荐学习路径</p>
      </div>
    </div>

    <!-- 推荐列表 -->
    <div v-else class="recommendations-list">
      <div
        v-for="path in recommendations"
        :key="path.id"
        class="path-card"
        :class="{ 'is-expanded': selectedPath?.id === path.id }"
        @click="selectPath(path)"
      >
        <!-- 卡片头部 -->
        <div class="path-card-header">
          <div class="path-info">
            <div class="path-title-row">
              <span class="path-title">{{ path.title }}</span>
              <el-tag size="small" :type="getDifficultyLabel(path.difficulty).type">
                {{ getDifficultyLabel(path.difficulty).text }}
              </el-tag>
            </div>
            <div class="path-meta">
              <span class="meta-item">
                <el-icon><Timer /></el-icon>
                {{ formatTime(path.estimatedMinutes) }}
              </span>
              <span class="meta-item" v-if="path.questionCount > 0">
                <el-icon><Notebook /></el-icon>
                {{ path.questionCount }}题
              </span>
              <span class="meta-item">
                <el-icon><Connection /></el-icon>
                {{ getPathTypeLabel(path.pathType).text }}
              </span>
            </div>
          </div>

          <div class="path-progress">
            <el-progress
              type="circle"
              :percentage="path.completionRate"
              :width="50"
              :stroke-width="4"
              :color="getProgressColor(path.completionRate)"
            />
          </div>
        </div>

        <!-- 推荐原因 -->
        <div class="path-reason" v-if="path.reason">
          <el-icon><School /></el-icon>
          <span>{{ path.reason }}</span>
        </div>

        <!-- 展开详情 -->
        <div v-if="selectedPath?.id === path.id" class="path-details">
          <CustomScroll class="details-scroll">
            <!-- 学习步骤 -->
            <div v-if="path.steps?.length > 0" class="detail-section">
              <div class="section-title">
                <el-icon><ArrowRight /></el-icon>
                学习步骤
              </div>
              <div class="steps-list">
                <div
                  v-for="step in path.steps"
                  :key="step.order"
                  class="step-item"
                  :class="{ 'is-completed': step.completed, 'is-core': step.stepType === 'core' }"
                >
                  <div class="step-number">{{ step.order }}</div>
                  <div class="step-content">
                    <div class="step-header">
                      <span class="step-name">{{ step.tagName }}</span>
                      <el-tag v-if="step.stepType === 'core'" size="small" type="primary">核心</el-tag>
                      <el-tag v-else-if="step.stepType === 'prerequisite'" size="small" type="info">前置</el-tag>
                    </div>
                    <div class="step-progress">
                      <el-progress
                        :percentage="step.masteryLevel"
                        :stroke-width="4"
                        :color="getProgressColor(step.masteryLevel)"
                        :show-text="false"
                      />
                      <span class="progress-text">{{ step.masteryLevel }}%</span>
                    </div>
                  </div>

                  <div class="step-status">
                    <el-icon v-if="step.completed" class="status-completed"><CircleCheck /></el-icon>
                    <el-icon v-else class="status-pending"><Check /></el-icon>
                  </div>
                </div>
              </div>
            </div>

            <!-- 前置依赖 -->
            <div v-if="path.prerequisites?.length > 0" class="detail-section">
              <div class="section-title">
                <el-icon><Connection /></el-icon>
                前置依赖
              </div>
              <div class="tag-chips">
                <el-tag
                  v-for="pre in path.prerequisites"
                  :key="pre.tagId"
                  size="small"
                  :type="pre.masteryLevel >= 60 ? 'success' : 'warning'"
                  class="tag-chip"
                >
                  {{ pre.tagName }}
                  <span v-if="pre.masteryLevel < 60" class="chip-hint">（待学习）</span>
                </el-tag>
              </div>
            </div>

            <!-- 进阶方向 -->
            <div v-if="path.nextSteps?.length > 0" class="detail-section">
              <div class="section-title">
                <el-icon><ArrowRight /></el-icon>
                进阶方向
              </div>
              <div class="tag-chips">
                <el-tag
                  v-for="next in path.nextSteps"
                  :key="next.tagId"
                  size="small"
                  type="info"
                  effect="plain"
                  class="tag-chip"
                >
                  {{ next.tagName }}
                </el-tag>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="path-actions">
              <el-button
                type="primary"
                size="large"
                @click.stop="handleStartLearning(path)"
              >
                <template v-if="path.completionRate >= 80">
                  继续巩固
                </template>
                <template v-else-if="path.completionRate >= 40">
                  继续学习
                </template>
                <template v-else>
                  开始学习
                </template>
              </el-button>
            </div>
          </CustomScroll>
        </div>
      </div>
    </div>

    <!-- 知识图谱入口 -->
    <div class="knowledge-graph-entry" @click="goToKnowledgeGraph">
      <div class="kg-entry-content">
        <el-icon :size="20"><Share /></el-icon>
        <span class="kg-entry-text">查看知识图谱，发现更多学习关系</span>
        <el-icon :size="16"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.learning-path-recommender {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

// 头部
.recommender-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, var(--el-color-primary-light-3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  letter-spacing: -0.01em;
}

.header-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.header-actions {
  display: flex;
  gap: 8px;

  .refresh-btn {
    border: none;
    background: rgba(0, 0, 0, 0.04);
    color: var(--el-text-color-secondary);
    transition: all 0.3s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.08);
      color: var(--el-color-primary);
      transform: rotate(180deg);
    }
  }
}

// 进度概览
.progress-overview {
  flex-shrink: 0;
}

.overview-card {
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  border-radius: 12px;
  padding: 5px 10px;
}

.overview-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background-color: var(--el-border-color-light);
}

.text-success { color: var(--el-color-success); }
.text-warning { color: var(--el-color-warning); }
.text-danger { color: var(--el-color-danger); }

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0;
}

.skeleton-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  border: 1px dashed rgba(0, 0, 0, 0.1);
  margin-top: 12px;
}

.empty-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  margin-bottom: 8px;
}

.empty-text {
  text-align: center;

  h4 {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    max-width: 200px;
    line-height: 1.5;
  }
}

// 推荐列表
.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 20px;
  overflow-y: auto;
}

// 路径卡片
.path-card {
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  border-radius: 16px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: var(--glass-border-hover);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }

  &.is-expanded {
    border-color: var(--accent-primary);
    box-shadow: 0 4px 20px var(--accent-glow-soft);
  }
}

.path-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.path-info {
  flex: 1;
  min-width: 0;
}

.path-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.path-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.path-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);

  .el-icon {
    font-size: 14px;
  }
}

.path-progress {
  flex-shrink: 0;
}

.path-reason {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;

  .el-icon {
    flex-shrink: 0;
    margin-top: 2px;
    color: var(--el-color-primary);
  }
}

// 详情区域
.path-details {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.details-scroll {
  max-height: 400px;
}

.detail-section {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;

  .el-icon {
    color: var(--el-color-primary);
  }
}

// 学习步骤
.steps-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;
  transition: all 0.2s ease;

  &.is-core {
    background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-fill-color-lighter));
    border: 1px solid var(--el-color-primary-light-7);
  }

  &.is-completed {
    opacity: 0.7;
  }
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.step-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.step-progress {
  display: flex;
  align-items: center;
  gap: 8px;

  .el-progress {
    flex: 1;
  }
}

.progress-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  width: 36px;
  text-align: right;
}

.step-status {
  flex-shrink: 0;

  .status-completed {
    font-size: 20px;
    color: var(--el-color-success);
  }

  .status-pending {
    font-size: 20px;
    color: var(--el-text-color-placeholder);
  }
}

// 标签组
.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  .chip-hint {
    margin-left: 4px;
    opacity: 0.7;
  }
}

// 操作按钮
.path-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

// 知识图谱入口
.knowledge-graph-entry {
  margin-top: 8px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.08), rgba(82, 196, 26, 0.04));
  border: 1px dashed rgba(24, 144, 255, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.knowledge-graph-entry:hover {
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.12), rgba(82, 196, 26, 0.08));
  border-color: rgba(24, 144, 255, 0.5);
  transform: translateY(-1px);
}

.kg-entry-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-color-primary);
  font-size: 14px;
}

.kg-entry-text {
  flex: 1;
  font-weight: 500;
}

</style>
