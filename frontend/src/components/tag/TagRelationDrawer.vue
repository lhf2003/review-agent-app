<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import {
  Connection,
  Link,
  TopRight,
  BottomRight,
  CircleCheck,
  CollectionTag,
  Warning,
  Delete,
  Refresh,
  Close,
  ArrowRight,
  TrendCharts
} from '@element-plus/icons-vue'
import { tagRelationApi } from '../../api/tagRelation'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../CustomScroll.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  tag: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'refresh'])

// 本地状态
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 加载状态
const loading = ref({
  dependencies: false,
  recommended: false,
  similar: false,
  complementary: false,
  relations: false
})

// 关系发现任务状态
const discoveryLoading = ref(false)
const currentTaskId = ref(null)
let pollInterval = null

// 数据
const dependencies = ref([])
const recommendedNext = ref([])
const similarTags = ref([])
const complementaryTags = ref([])
const allRelations = ref([])
const activeTab = ref('overview')

// 关系类型配置
const relationTypeConfig = {
  'DEPENDS_ON': {
    label: '前置依赖',
    color: '#E6A23C',
    bgColor: 'rgba(230, 162, 60, 0.1)',
    icon: Link,
    description: '需要先理解此标签'
  },
  'SIMILAR_TO': {
    label: '概念相似',
    color: '#409EFF',
    bgColor: 'rgba(64, 158, 255, 0.1)',
    icon: CollectionTag,
    description: '两者思路类似'
  },
  'COMPLEMENTS': {
    label: '互补',
    color: '#67C23A',
    bgColor: 'rgba(103, 194, 58, 0.1)',
    icon: CircleCheck,
    description: '结合使用效果更好'
  },
  'CONFLICTS_WITH': {
    label: '冲突',
    color: '#F56C6C',
    bgColor: 'rgba(245, 108, 108, 0.1)',
    icon: Warning,
    description: '两者不能同时使用'
  },
  'EVOLVES_TO': {
    label: '演进',
    color: '#9C27B0',
    bgColor: 'rgba(156, 39, 176, 0.1)',
    icon: TopRight,
    description: '进阶发展方向'
  }
}

// 计算属性
const hasData = computed(() => {
  return dependencies.value.length > 0 ||
    recommendedNext.value.length > 0 ||
    similarTags.value.length > 0 ||
    complementaryTags.value.length > 0
})

const relationStats = computed(() => {
  const stats = {}
  for (const relation of allRelations.value) {
    const type = relation.relationType
    stats[type] = (stats[type] || 0) + 1
  }
  return Object.entries(stats).map(([type, count]) => ({
    type,
    count,
    ...relationTypeConfig[type]
  }))
})

const totalRelations = computed(() => allRelations.value.length)

// 获取关系类型配置
function getRelationConfig(type) {
  return relationTypeConfig[type] || {
    label: type,
    color: '#909399',
    bgColor: 'rgba(144, 147, 153, 0.1)',
    icon: Connection,
    description: ''
  }
}

// 加载数据
async function loadAllData() {
  if (!props.tag?.id) return

  await Promise.all([
    loadDependencies(),
    loadRecommendedNext(),
    loadSimilarTags(),
    loadComplementaryTags(),
    loadAllRelations()
  ])
}

async function loadDependencies() {
  if (!props.tag?.id) return
  loading.value.dependencies = true
  try {
    const data = await tagRelationApi.getTagDependencies(props.tag.id)
    dependencies.value = data || []
  } catch (e) {
    console.error('加载前置依赖失败:', e)
  } finally {
    loading.value.dependencies = false
  }
}

async function loadRecommendedNext() {
  if (!props.tag?.id) return
  loading.value.recommended = true
  try {
    const data = await tagRelationApi.getRecommendedNextTags(props.tag.id)
    recommendedNext.value = data || []
  } catch (e) {
    console.error('加载推荐标签失败:', e)
  } finally {
    loading.value.recommended = false
  }
}

async function loadSimilarTags() {
  if (!props.tag?.id) return
  loading.value.similar = true
  try {
    const data = await tagRelationApi.getSimilarTags(props.tag.id)
    similarTags.value = data || []
  } catch (e) {
    console.error('加载相似标签失败:', e)
  } finally {
    loading.value.similar = false
  }
}

async function loadComplementaryTags() {
  if (!props.tag?.id) return
  loading.value.complementary = true
  try {
    const data = await tagRelationApi.getComplementaryTags(props.tag.id)
    complementaryTags.value = data || []
  } catch (e) {
    console.error('加载互补标签失败:', e)
  } finally {
    loading.value.complementary = false
  }
}

async function loadAllRelations() {
  if (!props.tag?.id) return
  loading.value.relations = true
  try {
    const data = await tagRelationApi.getTagRelations(props.tag.id)
    allRelations.value = data || []
  } catch (e) {
    console.error('加载标签关系失败:', e)
  } finally {
    loading.value.relations = false
  }
}

// 工具函数
function formatStrength(strength) {
  return `${strength}%`
}

function getStrengthColor(strength) {
  if (strength >= 80) return '#67C23A'
  if (strength >= 60) return '#E6A23C'
  if (strength >= 40) return '#409EFF'
  return '#909399'
}

function getStrengthType(strength) {
  if (strength >= 80) return 'success'
  if (strength >= 60) return 'warning'
  return 'info'
}

// 删除关系
async function deleteRelation(relationId) {
  try {
    await ElMessageBox.confirm('确定要删除这个标签关系吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await tagRelationApi.deleteRelation(relationId)
    ElMessage.success('删除成功')
    await loadAllData()
    emit('refresh')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`删除失败: ${e.message}`)
    }
  }
}

// 触发关系发现
async function triggerDiscovery() {
  try {
    await ElMessageBox.confirm(
      '将基于共现分析重新发现标签关系，这可能需要一些时间。',
      '触发关系发现',
      {
        type: 'info',
        confirmButtonText: '开始',
        cancelButtonText: '取消'
      }
    )

    discoveryLoading.value = true
    const task = await tagRelationApi.discoverRelationsByCoOccurrence()
    currentTaskId.value = task.taskId

    ElMessage.success('关系发现任务已启动')

    // 开始轮询任务状态
    startPolling(task.taskId)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(`触发失败: ${e.message}`)
    }
    discoveryLoading.value = false
  }
}

// 轮询任务状态
function startPolling(taskId) {
  // 清除之前的轮询
  if (pollInterval) {
    clearInterval(pollInterval)
  }

  pollInterval = setInterval(async () => {
    try {
      const task = await tagRelationApi.getDiscoveryTaskStatus(taskId)

      if (task.status === 'COMPLETED') {
        clearInterval(pollInterval)
        pollInterval = null
        discoveryLoading.value = false

        const foundCount = task.result?.foundCount || 0
        ElMessage.success(`发现 ${foundCount} 个新关系`)

        // 刷新关系列表
        await loadAllData()
      } else if (task.status === 'FAILED') {
        clearInterval(pollInterval)
        pollInterval = null
        discoveryLoading.value = false

        ElMessage.error('发现失败: ' + (task.error || '未知错误'))
      }
      // PENDING 或 RUNNING 状态继续轮询
    } catch (error) {
      console.error('轮询失败:', error)
      // 继续轮询，直到显式停止
    }
  }, 2000) // 每2秒查询一次
}

// 组件卸载时清除轮询
onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
})

// 关闭抽屉
function closeDrawer() {
  visible.value = false
}

// 监听标签变化和抽屉打开
watch(() => props.tag, (newTag) => {
  if (newTag?.id && visible.value) {
    loadAllData()
  }
}, { immediate: true })

watch(() => visible.value, (isVisible) => {
  if (isVisible && props.tag?.id) {
    activeTab.value = 'overview'
    loadAllData()
  }
})

// 暴露刷新方法
defineExpose({
  refresh: loadAllData
})
</script>

<template>
  <el-drawer
    v-model="visible"
    :size="480"
    :with-header="false"
    :destroy-on-close="false"
    class="tag-relation-drawer"
  >
    <div class="drawer-container">
      <!-- 头部 -->
      <div class="drawer-header">
        <div class="header-top">
          <div class="header-brand">
            <div class="header-icon">
              <el-icon :size="18">
                <Connection />
              </el-icon>
            </div>
            <span class="header-brand-text">标签关系</span>
          </div>
          <div class="header-actions">
            <el-tooltip content="刷新数据" placement="bottom">
              <el-button
                circle
                size="small"
                :icon="Refresh"
                :loading="loading.relations"
                @click="loadAllData"
              />
            </el-tooltip>
            <el-tooltip content="关闭" placement="bottom">
              <el-button
                circle
                size="small"
                :icon="Close"
                @click="closeDrawer"
              />
            </el-tooltip>
          </div>
        </div>

        <!-- 标签信息卡片 -->
        <div v-if="tag" class="header-tag-card">
          <div class="tag-color-indicator" :style="{ backgroundColor: tag.color }"></div>
          <div class="tag-info">
            <h3 class="tag-name">{{ tag.name }}</h3>
            <span v-if="tag.dimensionName" class="tag-dimension">{{ tag.dimensionName }}</span>
          </div>
          <div v-if="totalRelations > 0" class="tag-stat">
            <el-icon :size="14"><Connection /></el-icon>
            <span>{{ totalRelations }} 个关系</span>
          </div>
        </div>
      </div>

      <!-- 标签页切换 -->
      <div class="drawer-tabs">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'overview' }"
          @click="activeTab = 'overview'"
        >
          <el-icon><TrendCharts /></el-icon>
          概览
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'relations' }"
          @click="activeTab = 'relations'"
        >
          <el-icon><Connection /></el-icon>
          全部关系
          <span v-if="totalRelations > 0" class="tab-badge">{{ totalRelations }}</span>
        </div>
      </div>

      <!-- 内容区域 -->
      <CustomScroll class="drawer-content">
        <!-- 概览标签页 -->
        <div v-show="activeTab === 'overview'" class="tab-panel">
          <!-- 统计卡片 -->
          <div v-if="relationStats.length > 0" class="stats-section">
            <div class="section-title">关系统计</div>
            <div class="stats-grid">
              <div
                v-for="stat in relationStats"
                :key="stat.type"
                class="stat-card"
                :style="{
                  '--stat-color': stat.color,
                  '--stat-bg': stat.bgColor
                }"
              >
                <div class="stat-icon">
                  <el-icon :size="18">
                    <component :is="stat.icon" />
                  </el-icon>
                </div>
                <div class="stat-info">
                  <span class="stat-label">{{ stat.label }}</span>
                  <span class="stat-count">{{ stat.count }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 前置依赖 -->
          <div class="relation-section">
            <div class="section-header">
              <div class="section-title">
                <el-icon :size="16"><Link /></el-icon>
                前置依赖
                <span v-if="dependencies.length > 0" class="count-badge">{{ dependencies.length }}</span>
              </div>
            </div>

            <div v-if="loading.dependencies" class="section-loading">
              <el-skeleton :rows="2" animated />
            </div>

            <div v-else-if="dependencies.length === 0" class="section-empty">
              <el-icon :size="24" color="var(--el-text-color-secondary)"><Connection /></el-icon>
              <span>暂无前置依赖</span>
            </div>

            <div v-else class="relation-list">
              <div
                v-for="dep in dependencies"
                :key="dep.id"
                class="relation-card"
                :style="{ '--relation-color': getRelationConfig('DEPENDS_ON').color }"
              >
                <div class="relation-main">
                  <span class="relation-name">{{ dep.name }}</span>
                  <el-tag size="small" :type="getStrengthType(dep.strength)">
                    {{ formatStrength(dep.strength) }}
                  </el-tag>
                </div>
                <div class="relation-desc">需要先掌握此知识点</div>
              </div>
            </div>
          </div>

          <!-- 推荐进阶 -->
          <div class="relation-section">
            <div class="section-header">
              <div class="section-title">
                <el-icon :size="16"><BottomRight /></el-icon>
                推荐进阶
                <span v-if="recommendedNext.length > 0" class="count-badge">{{ recommendedNext.length }}</span>
              </div>
            </div>

            <div v-if="loading.recommended" class="section-loading">
              <el-skeleton :rows="2" animated />
            </div>

            <div v-else-if="recommendedNext.length === 0" class="section-empty">
              <el-icon :size="24" color="var(--el-text-color-secondary)"><ArrowRight /></el-icon>
              <span>暂无推荐</span>
            </div>

            <div v-else class="relation-list">
              <div
                v-for="rec in recommendedNext"
                :key="rec.id"
                class="relation-card primary"
                :style="{ '--relation-color': getRelationConfig('DEPENDS_ON').color }"
              >
                <div class="relation-main">
                  <span class="relation-name">{{ rec.name }}</span>
                  <el-tag size="small" :type="getStrengthType(rec.strength)">
                    {{ formatStrength(rec.strength) }}
                  </el-tag>
                </div>
                <div class="relation-desc">基于此标签的进阶内容</div>
              </div>
            </div>
          </div>

          <!-- 相似标签 -->
          <div class="relation-section">
            <div class="section-header">
              <div class="section-title">
                <el-icon :size="16"><CollectionTag /></el-icon>
                相似标签
                <span v-if="similarTags.length > 0" class="count-badge">{{ similarTags.length }}</span>
              </div>
            </div>

            <div v-if="loading.similar" class="section-loading">
              <el-skeleton :rows="2" animated />
            </div>

            <div v-else-if="similarTags.length === 0" class="section-empty">
              <el-icon :size="24" color="var(--el-text-color-secondary)"><CollectionTag /></el-icon>
              <span>暂无相似标签</span>
            </div>

            <div v-else class="relation-list">
              <div
                v-for="sim in similarTags"
                :key="sim.id"
                class="relation-card"
                :style="{ '--relation-color': getRelationConfig('SIMILAR_TO').color }"
              >
                <div class="relation-main">
                  <span class="relation-name">{{ sim.name }}</span>
                  <el-tag size="small" :type="getStrengthType(sim.strength)">
                    {{ formatStrength(sim.strength) }}
                  </el-tag>
                </div>
                <div class="relation-desc">思路类似的可选方案</div>
              </div>
            </div>
          </div>

          <!-- 互补标签 -->
          <div class="relation-section">
            <div class="section-header">
              <div class="section-title">
                <el-icon :size="16"><CircleCheck /></el-icon>
                互补标签
                <span v-if="complementaryTags.length > 0" class="count-badge">{{ complementaryTags.length }}</span>
              </div>
            </div>

            <div v-if="loading.complementary" class="section-loading">
              <el-skeleton :rows="2" animated />
            </div>

            <div v-else-if="complementaryTags.length === 0" class="section-empty">
              <el-icon :size="24" color="var(--el-text-color-secondary)"><CircleCheck /></el-icon>
              <span>暂无互补标签</span>
            </div>

            <div v-else class="relation-list">
              <div
                v-for="comp in complementaryTags"
                :key="comp.id"
                class="relation-card"
                :style="{ '--relation-color': getRelationConfig('COMPLEMENTS').color }"
              >
                <div class="relation-main">
                  <span class="relation-name">{{ comp.name }}</span>
                  <el-tag size="small" :type="getStrengthType(comp.strength)">
                    {{ formatStrength(comp.strength) }}
                  </el-tag>
                </div>
                <div class="relation-desc">结合使用效果更好</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 全部关系标签页 -->
        <div v-show="activeTab === 'relations'" class="tab-panel">
          <div v-if="loading.relations" class="section-loading">
            <el-skeleton :rows="8" animated />
          </div>

          <div v-else-if="allRelations.length === 0" class="section-empty large">
            <el-icon :size="48" color="var(--el-text-color-secondary)"><Connection /></el-icon>
            <span>暂无标签关系</span>
            <el-button
              type="primary"
              size="small"
              :loading="discoveryLoading"
              @click="triggerDiscovery"
            >
              <el-icon v-if="!discoveryLoading"><Refresh /></el-icon>
              {{ discoveryLoading ? '发现中...' : '触发关系发现' }}
            </el-button>
          </div>

          <div v-else class="relations-timeline">
            <div
              v-for="relation in allRelations"
              :key="relation.id"
              class="relation-timeline-item"
              :class="{ 'is-auto': relation.autoDetected }"
            >
              <div class="timeline-marker" :style="{
                backgroundColor: getRelationConfig(relation.relationType).color
              }">
                <el-icon :size="12">
                  <component :is="getRelationConfig(relation.relationType).icon" />
                </el-icon>
              </div>

              <div class="timeline-content">
                <div class="relation-header">
                  <span class="relation-type-label" :style="{
                    color: getRelationConfig(relation.relationType).color
                  }">
                    {{ getRelationConfig(relation.relationType).label }}
                  </span>
                  <div class="relation-meta">
                    <el-tag size="small" effect="plain">
                      {{ formatStrength(relation.strength) }}
                    </el-tag>
                    <el-tag v-if="relation.autoDetected" size="small" type="info" effect="plain">
                      AI发现
                    </el-tag>
                  </div>
                </div>

                <div class="relation-path">
                  <span class="tag-pill source">{{ relation.sourceTagName }}</span>
                  <el-icon class="path-arrow"><ArrowRight /></el-icon>
                  <span class="tag-pill target">{{ relation.targetTagName }}</span>
                </div>

                <div v-if="relation.evidence" class="relation-evidence">
                  {{ relation.evidence }}
                </div>
              </div>

              <div class="timeline-actions">
                <el-button
                  text
                  size="small"
                  type="danger"
                  :icon="Delete"
                  @click="deleteRelation(relation.id)"
                />
              </div>
            </div>
          </div>
        </div>
      </CustomScroll>
    </div>
  </el-drawer>
</template>

<style scoped>
/* 抽屉容器 */
.drawer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 抽屉圆角 */
:deep(.el-drawer) {
  border-radius: var(--radius-card) 0 0 var(--radius-card);
  overflow: hidden;
  background: var(--bg-overlay);
  border-left: 1px solid var(--glass-border);
}

/* 头部样式 */
.drawer-header {
  padding: 16px 20px;
  background: var(--glass-surface);
  border-bottom: 1px solid var(--glass-border);
  border-radius: var(--radius-card) 0 0 0;
  flex-shrink: 0;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--accent-primary);
  color: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px var(--accent-glow-soft);
}

.header-brand-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-actions :deep(.el-button) {
  color: var(--text-secondary);
  background-color: var(--glass-surface);
  border: 1px solid var(--glass-border);
  transition: all 0.2s ease;
}

.header-actions :deep(.el-button:hover) {
  color: var(--accent-primary);
  border-color: var(--accent-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

/* 标签信息卡片 */
.header-tag-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: var(--glass-surface);
  border-radius: 16px;
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-sm);
}

.tag-color-indicator {
  width: 12px;
  height: 40px;
  border-radius: 6px;
  flex-shrink: 0;
}

.tag-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tag-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-dimension {
  font-size: 12px;
  color: var(--text-secondary);
  padding: 2px 8px;
  background-color: rgba(255, 248, 245, 0.05);
  border-radius: 6px;
  width: fit-content;
}

.tag-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: rgba(255, 248, 245, 0.05);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
}

/* 标签页 */
.drawer-tabs {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--glass-border);
  background-color: var(--glass-surface);
  flex-shrink: 0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background-color: transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-item:hover {
  background-color: var(--glass-surface-hover);
  color: var(--text-primary);
}

.tab-item.active {
  background-color: rgba(204, 102, 51, 0.15);
  color: var(--accent-primary);
}

.tab-badge {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-primary);
  background-color: rgba(204, 102, 51, 0.1);
  border-radius: 10px;
}

/* 内容区域 */
.drawer-content {
  flex: 1;
  min-height: 0;
  padding: 16px 20px;
}

.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 区块标题 */
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background-color: var(--stat-bg);
  border-radius: 16px;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: var(--stat-color);
}

.stat-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--stat-color);
  color: white;
  border-radius: 12px;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.stat-count {
  font-size: 18px;
  font-weight: 700;
  color: var(--stat-color);
  line-height: 1.2;
}

/* 关系区块 */
.relation-section {
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.count-badge {
  font-size: 11px;
  padding: 2px 10px;
  background-color: rgba(204, 102, 51, 0.1);
  color: var(--accent-primary);
  border-radius: 10px;
  font-weight: 600;
}

/* 关系卡片列表 */
.relation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.relation-card {
  padding: 14px 16px;
  background-color: rgba(255, 248, 245, 0.03);
  border-radius: 16px;
  border-left: 3px solid var(--relation-color);
  transition: all 0.2s ease;
}

.relation-card:hover {
  background-color: rgba(255, 248, 245, 0.06);
  transform: translateX(4px);
}

.relation-card.primary {
  background: linear-gradient(135deg, rgba(204, 102, 51, 0.1) 0%, rgba(255, 248, 245, 0.03) 100%);
}

.relation-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.relation-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.relation-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 空状态 */
.section-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 20px;
  background-color: rgba(255, 248, 245, 0.03);
  border-radius: 16px;
  color: var(--text-secondary);
  font-size: 13px;
}

.section-empty.large {
  padding: 60px 20px;
  gap: 16px;
}

.section-empty.large span {
  font-size: 15px;
}

/* 加载状态 */
.section-loading {
  padding: 20px;
}

/* 时间线样式 - 全部关系 */
.relations-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.relation-timeline-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background-color: rgba(255, 248, 245, 0.03);
  border-radius: 16px;
  transition: all 0.2s ease;
}

.relation-timeline-item:hover {
  background-color: rgba(255, 248, 245, 0.06);
}

.relation-timeline-item.is-auto {
  background-image: linear-gradient(45deg, transparent 46%, var(--glass-border) 49%, var(--glass-border) 51%, transparent 55%);
  background-size: 8px 8px;
}

.timeline-marker {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: white;
  flex-shrink: 0;
  margin-top: 2px;
}

.timeline-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.relation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.relation-type-label {
  font-size: 13px;
  font-weight: 600;
}

.relation-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.relation-path {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-pill {
  padding: 4px 12px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  background-color: rgba(255, 248, 245, 0.05);
}

.tag-pill.source {
  color: var(--text-secondary);
}

.tag-pill.target {
  color: var(--accent-primary);
  background-color: rgba(204, 102, 51, 0.1);
}

.path-arrow {
  color: var(--text-tertiary);
}

.relation-evidence {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  padding: 8px 12px;
  background-color: var(--glass-surface);
  border-radius: 10px;
}

.timeline-actions {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.relation-timeline-item:hover .timeline-actions {
  opacity: 1;
}

/* 响应式 */
@media (max-width: 768px) {
  :deep(.el-drawer) {
    width: 100% !important;
  }

  .drawer-header {
    padding: 12px 16px;
  }

  .header-top {
    margin-bottom: 8px;
  }

  .header-tag-card {
    padding: 10px 12px;
    gap: 10px;
  }

  .tag-color-indicator {
    width: 8px;
    height: 32px;
  }

  .tag-name {
    font-size: 14px;
  }

  .tag-stat {
    display: none;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .relation-path {
    flex-direction: column;
    align-items: flex-start;
  }

  .path-arrow {
    transform: rotate(90deg);
  }

  .timeline-actions {
    opacity: 1;
  }
}
</style>
