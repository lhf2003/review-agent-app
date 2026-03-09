<script setup>
import { ref, computed, onMounted } from 'vue'
import { Star, Check, Close, Document, Clock, CircleCheck, Hide } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tagRecommendApi } from '../../api/tagRecommend'
import CustomScroll from '../CustomScroll.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'refresh'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 推荐标签列表（按状态分组）
const recommendations = ref({
  PENDING: [],
  ADOPTED: [],
  IGNORED: []
})
const loading = ref(false)
const activeTab = ref('PENDING')

// 统计信息
const stats = ref({
  pending: 0,
  adopted: 0,
  ignored: 0,
  total: 0
})

// 选中的标签（用于批量操作）
const selectedIds = ref([])

// 过滤后的推荐标签
const filteredRecommendations = computed(() => {
  return recommendations.value[activeTab.value] || []
})

// 是否全选（仅针对当前显示的PENDING标签）
const isAllSelected = computed(() => {
  const pendingItems = filteredRecommendations.value
  if (pendingItems.length === 0) return false
  return pendingItems.every(item => selectedIds.value.includes(item.id))
})

// 加载推荐标签列表
async function loadRecommendations() {
  loading.value = true
  try {
    const data = await tagRecommendApi.getRecommendations()
    recommendations.value = {
      PENDING: data?.PENDING || [],
      ADOPTED: data?.ADOPTED || [],
      IGNORED: data?.IGNORED || []
    }
    // 清空选中状态
    selectedIds.value = []
  } catch (e) {
    ElMessage.error(`加载推荐标签失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

// 加载统计信息
async function loadStats() {
  try {
    const data = await tagRecommendApi.getStats()
    stats.value = data || { pending: 0, adopted: 0, ignored: 0, total: 0 }
  } catch (e) {
    console.warn('加载统计信息失败:', e)
  }
}

// 采纳推荐标签
async function adoptTag(tag) {
  try {
    await tagRecommendApi.adoptRecommendation(tag.id)
    ElMessage.success(`已采纳标签「${tag.tagName}」`)
    // 从PENDING列表移除
    recommendations.value.PENDING = recommendations.value.PENDING.filter(item => item.id !== tag.id)
    // 添加到ADOPTED列表
    tag.userAction = 'ADOPTED'
    tag.userActionText = '已采纳'
    recommendations.value.ADOPTED.unshift(tag)
    // 刷新统计
    loadStats()
    // 通知父组件刷新标签树
    emit('refresh')
  } catch (e) {
    ElMessage.error(`采纳失败: ${e.message}`)
  }
}

// 忽略推荐标签
async function ignoreTag(tag) {
  try {
    await ElMessageBox.confirm(
      `确定忽略标签「${tag.tagName}」吗？`,
      '提示',
      { confirmButtonText: '忽略', cancelButtonText: '取消', type: 'warning' }
    )
    await tagRecommendApi.ignoreRecommendation(tag.id)
    ElMessage.success(`已忽略标签「${tag.tagName}」`)
    // 从PENDING列表移除
    recommendations.value.PENDING = recommendations.value.PENDING.filter(item => item.id !== tag.id)
    // 添加到IGNORED列表（保持最多5条）
    tag.userAction = 'IGNORED'
    tag.userActionText = '已忽略'
    recommendations.value.IGNORED.unshift(tag)
    if (recommendations.value.IGNORED.length > 5) {
      recommendations.value.IGNORED = recommendations.value.IGNORED.slice(0, 5)
    }
    // 从选中列表移除
    selectedIds.value = selectedIds.value.filter(id => id !== tag.id)
    // 刷新统计
    loadStats()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`忽略失败: ${e.message}`)
    }
  }
}

// 批量采纳
async function adoptSelected() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要采纳的标签')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定采纳选中的 ${selectedIds.value.length} 个标签吗？`,
      '批量采纳',
      { confirmButtonText: '采纳', cancelButtonText: '取消', type: 'info' }
    )
    const newTags = await tagRecommendApi.adoptAll(selectedIds.value)
    ElMessage.success(`已成功采纳 ${selectedIds.value.length} 个标签`)
    // 从PENDING列表移除已采纳的标签
    const adoptedIds = new Set(selectedIds.value)
    recommendations.value.PENDING = recommendations.value.PENDING.filter(item => !adoptedIds.has(item.id))
    // 添加到ADOPTED列表
    if (Array.isArray(newTags)) {
      newTags.forEach(tag => {
        recommendations.value.ADOPTED.unshift({
          ...tag,
          userAction: 'ADOPTED',
          userActionText: '已采纳'
        })
      })
    }
    selectedIds.value = []
    loadStats()
    emit('refresh')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`批量采纳失败: ${e.message}`)
    }
  }
}

// 批量忽略
async function ignoreSelected() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要忽略的标签')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定忽略选中的 ${selectedIds.value.length} 个标签吗？`,
      '批量忽略',
      { confirmButtonText: '忽略', cancelButtonText: '取消', type: 'warning' }
    )
    await tagRecommendApi.ignoreAll(selectedIds.value)
    ElMessage.success(`已忽略 ${selectedIds.value.length} 个推荐标签`)
    // 从PENDING列表移除并添加到IGNORED列表
    const ignoredIds = new Set(selectedIds.value)
    const ignoredTags = recommendations.value.PENDING.filter(item => ignoredIds.has(item.id))
    recommendations.value.PENDING = recommendations.value.PENDING.filter(item => !ignoredIds.has(item.id))
    ignoredTags.forEach(tag => {
      recommendations.value.IGNORED.unshift({
        ...tag,
        userAction: 'IGNORED',
        userActionText: '已忽略'
      })
    })
    if (recommendations.value.IGNORED.length > 5) {
      recommendations.value.IGNORED = recommendations.value.IGNORED.slice(0, 5)
    }
    selectedIds.value = []
    loadStats()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`批量忽略失败: ${e.message}`)
    }
  }
}

// 切换全选
function toggleSelectAll() {
  const pendingItems = filteredRecommendations.value
  if (isAllSelected.value) {
    // 取消全选
    pendingItems.forEach(item => {
      const index = selectedIds.value.indexOf(item.id)
      if (index > -1) {
        selectedIds.value.splice(index, 1)
      }
    })
  } else {
    // 全选
    pendingItems.forEach(item => {
      if (!selectedIds.value.includes(item.id)) {
        selectedIds.value.push(item.id)
      }
    })
  }
}

// 获取状态标签样式
function getStatusType(action) {
  switch (action) {
    case 'PENDING': return 'warning'
    case 'ADOPTED': return 'success'
    case 'IGNORED': return 'info'
    default: return 'info'
  }
}

// 监听抽屉打开
function onOpen() {
  loadRecommendations()
  loadStats()
}

onMounted(() => {
  if (visible.value) {
    loadRecommendations()
    loadStats()
  }
})
</script>

<template>
  <el-drawer
    v-model="visible"
    size="520px"
    :close-on-click-modal="true"
    @open="onOpen"
  >
    <template #header>
      <div class="drawer-header-custom">
        <div class="header-main">
          <el-icon :size="18"><Star /></el-icon>
          <div class="header-titles">
            <span class="header-title">推荐标签</span>
            <span class="header-subtitle">AI 根据会话分析自动推荐标签，您可选择采纳或忽略</span>
          </div>
        </div>
      </div>
    </template>
    <div class="recommend-tag-drawer">
      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card pending" :class="{ active: activeTab === 'PENDING' }" @click="activeTab = 'PENDING'">
          <div class="stat-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.pending }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
        <div class="stat-card adopted" :class="{ active: activeTab === 'ADOPTED' }" @click="activeTab = 'ADOPTED'">
          <div class="stat-icon">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.adopted }}</div>
            <div class="stat-label">已采纳</div>
          </div>
        </div>
        <div class="stat-card ignored" :class="{ active: activeTab === 'IGNORED' }" @click="activeTab = 'IGNORED'">
          <div class="stat-icon">
            <el-icon><Hide /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.ignored }}</div>
            <div class="stat-label">已忽略</div>
          </div>
        </div>
      </div>

      <!-- 推荐标签列表 -->
      <div class="recommendation-list">
        <CustomScroll>
          <div v-if="loading" class="loading-wrapper">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="filteredRecommendations.length === 0" class="empty-wrapper">
            <el-empty :image-size="80">
              <template #image>
                <div class="empty-state">
                  <el-icon :size="48" class="empty-icon"><Star /></el-icon>
                </div>
              </template>
              <template #description>
                <div class="empty-text">
                  <p class="empty-title">
                    <span v-if="activeTab === 'PENDING'">暂无待处理的推荐标签</span>
                    <span v-else-if="activeTab === 'ADOPTED'">暂无已采纳的标签</span>
                    <span v-else>暂无已忽略的标签</span>
                  </p>
                  <p v-if="activeTab === 'PENDING'" class="empty-subtitle">AI 会在分析会话时自动推荐相关标签</p>
                </div>
              </template>
            </el-empty>
          </div>

          <div v-else class="tag-items">
            <div
              v-for="item in filteredRecommendations"
              :key="item.id"
              class="tag-item"
              :class="{ 'is-pending': item.userAction === 'PENDING', 'is-adopted': item.userAction === 'ADOPTED', 'is-ignored': item.userAction === 'IGNORED' }"
            >
              <!-- 左侧状态指示 -->
              <div class="item-status-indicator">
                <div v-if="item.userAction === 'PENDING'" class="status-dot pending"></div>
                <div v-else-if="item.userAction === 'ADOPTED'" class="status-dot adopted"></div>
                <div v-else class="status-dot ignored"></div>
                <!-- 复选框（仅待处理状态） -->
                <el-checkbox
                  v-if="item.userAction === 'PENDING'"
                  v-model="selectedIds"
                  :label="item.id"
                  class="item-checkbox"
                >
                  {{ '' }}
                </el-checkbox>
              </div>

              <!-- 标签内容 -->
              <div class="item-content">
                <div class="item-header">
                  <div class="tag-name-wrapper">
                    <span class="tag-name">{{ item.tagName }}</span>
                    <el-tag :type="getStatusType(item.userAction)" size="small" effect="light" class="status-tag">
                      {{ item.userActionText }}
                    </el-tag>
                  </div>
                  <div class="item-time">{{ item.createdTime }}</div>
                </div>

                <div v-if="item.problemStatement" class="item-problem">
                  <el-icon :size="14"><Document /></el-icon>
                  <span class="problem-text">{{ item.problemStatement }}</span>
                </div>

                <div v-if="item.fileName" class="item-meta">
                  <span class="meta-label">来源</span>
                  <span class="meta-value">{{ item.fileName }}</span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div v-if="item.userAction === 'PENDING'" class="item-actions">
                <el-button
                  type="primary"
                  size="small"
                  :icon="Check"
                  @click="adoptTag(item)"
                >
                  采纳
                </el-button>
                <el-button
                  size="small"
                  :icon="Close"
                  @click="ignoreTag(item)"
                >
                  忽略
                </el-button>
              </div>
            </div>
          </div>
        </CustomScroll>
      </div>

    </div>
  </el-drawer>
</template>

<style scoped>
.recommend-tag-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 20px 20px 20px;
}

/* 移除 drawer header 的底边框和底部 margin，实现与 body 零间距 */
:deep(.el-drawer__header) {
  border-bottom: none !important;
  margin-bottom: 0 !important;
  padding: 20px 24px 12px 24px !important;
}

/* ========== 统计卡片 ========== */
.stats-row {
  display: flex;
  gap: 10px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 12px;
  border-radius: 12px;
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  cursor: pointer;
  transition: all 0.2s ease;
}

.stat-card:hover {
  background: var(--glass-surface-hover);
  transform: translateY(-1px);
}

.stat-card.active {
  background: rgba(204, 102, 51, 0.12);
  border-color: var(--accent-primary);
  box-shadow: 0 4px 12px rgba(204, 102, 51, 0.15);
}

.stat-card.pending .stat-icon {
  color: #E6A23C;
}

.stat-card.adopted .stat-icon {
  color: #67C23A;
}

.stat-card.ignored .stat-icon {
  color: #909399;
}

.stat-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 20px;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-number {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

/* ========== 列表区域 ========== */
.recommendation-list {
  flex: 1;
  min-height: 0;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-surface);
  overflow: hidden;
}

.loading-wrapper,
.empty-wrapper {
  padding: 48px 20px;
}

.empty-state {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--glass-surface-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.empty-icon {
  color: var(--accent-primary);
}

.empty-text {
  text-align: center;
}

.empty-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.empty-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ========== 标签项 ========== */
.tag-items {
  padding: 12px;
}

.tag-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 10px;
  background: var(--bg-primary);
  border: 1px solid var(--glass-border);
  transition: all 0.2s ease;
}

.tag-item:last-child {
  margin-bottom: 0;
}

.tag-item.is-pending {
  border-left: 1px solid var(--glass-border);
}

.tag-item.is-pending:hover {
  background: var(--glass-surface-hover);
  box-shadow: var(--shadow-sm);
}

.tag-item.is-adopted {
  border-left: 1px solid var(--glass-border);
  opacity: 0.85;
}

.tag-item.is-ignored {
  border-left: 1px solid var(--glass-border);
  opacity: 0.7;
}

/* 状态指示器 */
.item-status-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.pending {
  background: #E6A23C;
  box-shadow: 0 0 6px #E6A23C;
}

.status-dot.adopted {
  background: #67C23A;
}

.status-dot.ignored {
  background: #909399;
}

.item-checkbox {
  margin-left: -4px;
}

/* 内容区 */
.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.tag-name-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.tag-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag {
  flex-shrink: 0;
}

.item-time {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.item-problem {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: var(--glass-surface);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.item-problem .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--accent-primary);
}

.problem-text {
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.meta-label {
  color: var(--text-tertiary);
}

.meta-value {
  color: var(--text-secondary);
  font-weight: 500;
}

/* 操作按钮 */
.item-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 2px;
}

/* ========== 自定义 Header ========== */
.drawer-header-custom {
  display: flex;
  align-items: center;
  width: 100%;
}

.header-main {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.header-main .el-icon {
  color: var(--accent-primary);
  margin-top: 2px;
}

.header-titles {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.header-subtitle {
  font-size: 11px;
  color: var(--text-tertiary);
  font-weight: normal;
  line-height: 1.2;
}
</style>
