<template>
  <div class="knowledge-graph-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <ElButton type="text" @click="$router.back()" class="back-btn">
          <ElIcon><ArrowLeft /></ElIcon>
          返回
        </ElButton>
        <span class="page-title">知识图谱</span>
        <ElTag size="small" type="info" class="beta-tag">测试版</ElTag>

        <!-- 聚焦模式指示器 -->
        <Transition name="fade">
          <ElTag v-if="focusedNodeId" type="warning" effect="dark" class="focus-tag" closable @close="exitFocusMode">
            <ElIcon><ZoomIn /></ElIcon>
            聚焦模式
          </ElTag>
        </Transition>
      </div>

      <div class="header-center">
        <!-- 图层模式切换 -->
        <ElRadioGroup v-model="layerMode" size="small" @change="switchLayerMode">
          <ElRadioButton label="knowledge">
            <ElIcon><Grid /></ElIcon>
            知识层
          </ElRadioButton>
          <ElRadioButton label="paradigm">
            <ElIcon><View /></ElIcon>
            范式层
          </ElRadioButton>
          <ElRadioButton label="mixed">
            <ElIcon><List /></ElIcon>
            混合视图
          </ElRadioButton>
        </ElRadioGroup>

        <span class="stats-text" v-if="layerMode === 'knowledge' || layerMode === 'mixed'">
          节点: <strong>{{ visibleNodes.length }}</strong>
          <span v-if="focusedNodeId">/{{ nodes.length }}</span>
          边: <strong>{{ visibleEdges.length }}</strong>
        </span>
      </div>

      <div class="header-right">
        <!-- 图表控制按钮组 -->
        <div class="chart-controls">
          <ElTooltip content="重置视图" placement="bottom">
            <ElButton type="text" circle class="control-btn" @click="resetView">
              <ElIcon><RefreshRight /></ElIcon>
            </ElButton>
          </ElTooltip>
          <ElTooltip content="放大" placement="bottom">
            <ElButton type="text" circle class="control-btn" @click="zoomIn">
              <ElIcon><ZoomIn /></ElIcon>
            </ElButton>
          </ElTooltip>
          <ElTooltip content="缩小" placement="bottom">
            <ElButton type="text" circle class="control-btn" @click="zoomOut">
              <ElIcon><ZoomOut /></ElIcon>
            </ElButton>
          </ElTooltip>
        </div>

        <!-- 过滤器按钮 -->
        <ElButton
          :type="showFilters ? 'primary' : 'default'"
          size="small"
          @click="showFilters = !showFilters"
          class="filter-btn"
        >
          <ElIcon><Filter /></ElIcon>
          筛选
          <ElBadge v-if="activeFilterCount > 0" :value="activeFilterCount" class="filter-badge" />
        </ElButton>

        <ElTooltip content="节点颜色表示掌握度" placement="bottom">
          <div class="legend">
            <div class="legend-item">
              <span class="legend-dot weak"></span>
              <span class="legend-text">薄弱</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot medium"></span>
              <span class="legend-text">一般</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot strong"></span>
              <span class="legend-text">良好</span>
            </div>
          </div>
        </ElTooltip>
      </div>
    </div>

    <!-- 主体区域：过滤器 + 图表 + 详情面板 -->
    <div class="main-container">
      <!-- 左侧过滤器面板 -->
      <Transition name="slide-left">
        <div v-show="showFilters" class="filter-panel">
          <div class="filter-header">
            <span class="filter-title">筛选器</span>
            <ElButton type="text" size="small" @click="resetFilters" class="reset-btn">
              <ElIcon><RefreshRight /></ElIcon>
              重置
            </ElButton>
          </div>

          <!-- 掌握度筛选 -->
          <div class="filter-section">
            <div class="filter-label">
              <ElIcon><CircleCheck /></ElIcon>
              掌握度
            </div>
            <div class="filter-chips">
              <div
                v-for="level in masteryLevels"
                :key="level.key"
                class="filter-chip"
                :class="{ active: selectedMasteryLevels.includes(level.key) }"
                @click="toggleMasteryLevel(level.key)"
              >
                <span class="chip-dot" :class="level.key"></span>
                <span class="chip-label">{{ level.label }}</span>
              </div>
            </div>
          </div>

          <!-- 标签筛选 -->
          <div class="filter-section" v-if="availableGroups.length > 0">
            <div class="filter-label">
              <ElIcon><Folder /></ElIcon>
              知识分组
            </div>
            <div class="filter-chips">
              <div
                v-for="group in availableGroups"
                :key="group"
                class="filter-chip"
                :class="{ active: selectedGroups.includes(group) }"
                @click="toggleGroup(group)"
              >
                <span class="chip-label">{{ group }}</span>
              </div>
            </div>
          </div>

          <!-- 思维范式筛选 -->
          <div class="filter-section" v-if="(layerMode === 'paradigm' || layerMode === 'mixed') && availableParadigms.length > 0">
            <div class="filter-label">
              <ElIcon><View /></ElIcon>
              思维范式
            </div>
            <div class="filter-chips">
              <div
                v-for="paradigm in availableParadigms"
                :key="paradigm.code"
                class="filter-chip"
                :class="{ active: selectedParadigms.includes(paradigm.code) }"
                :style="{ borderColor: selectedParadigms.includes(paradigm.code) ? paradigm.color : undefined }"
                @click="toggleParadigm(paradigm.code)"
              >
                <span class="chip-dot" :style="{ background: paradigm.color }"></span>
                <span class="chip-label">{{ paradigm.name }}</span>
              </div>
            </div>
          </div>

          <!-- 聚焦模式提示 -->
          <Transition name="fade">
            <div v-if="focusedNodeId" class="filter-hint">
              <ElIcon><InfoFilled /></ElIcon>
              <span>聚焦模式下筛选器仅对邻居节点生效</span>
            </div>
          </Transition>
        </div>
      </Transition>

      <!-- 图表区域 -->
      <div class="chart-container" v-loading="loading" element-loading-text="加载知识图谱中...">
        <div ref="chartRef" class="chart"></div>

        <!-- 空状态 -->
        <Transition name="fade">
          <div v-if="!loading && visibleNodes.length === 0" class="empty-state">
            <ElEmpty :description="emptyDescription">
              <template #image>
                <div class="empty-illustration">
                  <svg viewBox="0 0 200 160" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="100" cy="80" r="60" stroke="currentColor" stroke-width="2" stroke-dasharray="8 4" opacity="0.3"/>
                    <circle cx="70" cy="60" r="15" fill="currentColor" opacity="0.5"/>
                    <circle cx="130" cy="60" r="15" fill="currentColor" opacity="0.3"/>
                    <circle cx="100" cy="110" r="15" fill="currentColor" opacity="0.4"/>
                    <line x1="82" y1="70" x2="115" y2="70" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                    <line x1="85" y1="72" x2="100" y2="95" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                    <line x1="115" y1="72" x2="100" y2="95" stroke="currentColor" stroke-width="2" opacity="0.3"/>
                  </svg>
                </div>
              </template>
              <template #description>
                <p class="empty-title">{{ emptyDescription }}</p>
                <p class="empty-subtitle" v-if="!hasFilters && !focusedNodeId">完成测验后将自动生成你的知识网络</p>
              </template>
              <ElButton v-if="hasFilters" @click="resetFilters" type="primary" plain>
                <ElIcon><RefreshRight /></ElIcon>
                清除筛选
              </ElButton>
              <ElButton v-else type="primary" @click="$router.push('/quiz')">
                <ElIcon><EditPen /></ElIcon>
                去答题
              </ElButton>
            </ElEmpty>
          </div>
        </Transition>

        <!-- 聚焦模式操作提示 -->
        <Transition name="slide-down">
          <div v-if="focusedNodeId" class="focus-hint">
            <ElIcon><InfoFilled /></ElIcon>
            <span>按 ESC 或点击右上角「聚焦模式」标签退出</span>
          </div>
        </Transition>

        <!-- 图例提示 - 底部 -->
        <div class="bottom-hint">
          <ElIcon><Pointer /></ElIcon>
          <span>单击选中节点，双击聚焦，拖拽调整位置</span>
        </div>
      </div>

      <!-- 右侧范式详情面板 -->
      <Transition name="slide-right">
        <div v-if="selectedParadigmNode" class="detail-panel paradigm-detail-panel">
          <div class="detail-header">
            <div class="node-icon" :style="{ background: selectedParadigmNode.color + '20', color: selectedParadigmNode.color }">
              <ElIcon><View /></ElIcon>
            </div>
            <h3 class="detail-title">{{ selectedParadigmNode.label }}</h3>
            <ElButton type="text" circle class="close-btn" @click="selectedParadigmNode = null">
              <ElIcon><Close /></ElIcon>
            </ElButton>
          </div>

          <div class="detail-content">
            <!-- 范式类型 -->
            <div class="detail-section">
              <div class="section-title">范式类型</div>
              <div class="paradigm-type-tag" :style="{ background: selectedParadigmNode.color + '20', color: selectedParadigmNode.color, borderColor: selectedParadigmNode.color }">
                {{ paradigmTypes.find(p => p.code === selectedParadigmNode.paradigmCode)?.name || selectedParadigmNode.paradigmCode }}
              </div>
            </div>

            <!-- 描述 -->
            <div class="detail-section" v-if="selectedParadigmNode.description">
              <div class="section-title">描述</div>
              <p class="description-text">{{ selectedParadigmNode.description }}</p>
            </div>

            <!-- 操作按钮 -->
            <div class="detail-actions">
              <ElButton
                v-if="selectedParadigmNode.flowchartId"
                type="primary"
                class="action-btn primary"
                @click="viewFlowchart(selectedParadigmNode)"
              >
                <ElIcon><View /></ElIcon>
                查看流程图
              </ElButton>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 右侧节点详情面板 -->
      <Transition name="slide-right">
        <div v-if="selectedNode && !selectedParadigmNode" class="detail-panel">
          <div class="detail-header">
            <div class="node-icon" :style="{ background: selectedNode.color + '20', color: selectedNode.color }">
              <ElIcon><Share /></ElIcon>
            </div>
            <h3 class="detail-title">{{ selectedNode.name }}</h3>
            <ElButton type="text" circle class="close-btn" @click="selectedNode = null">
              <ElIcon><Close /></ElIcon>
            </ElButton>
          </div>

          <div class="detail-content">
            <!-- 掌握度 -->
            <div class="detail-section mastery-section">
              <div class="mastery-header">
                <span class="mastery-label">掌握度</span>
                <span class="mastery-badge" :style="{ background: selectedNode.color + '20', color: selectedNode.color }">
                  {{ getMasteryLabel(selectedNode.masteryScore) }}
                </span>
              </div>
              <div class="mastery-value-row">
                <span class="mastery-value" :style="{ color: selectedNode.color }">
                  {{ selectedNode.masteryScore }}%
                </span>
              </div>
              <div class="progress-wrapper">
                <div class="progress-track">
                  <div
                    class="progress-fill"
                    :style="{ width: selectedNode.masteryScore + '%', background: selectedNode.color }"
                  ></div>
                </div>
                <div class="progress-markers">
                  <span class="marker">0%</span>
                  <span class="marker">50%</span>
                  <span class="marker">100%</span>
                </div>
              </div>
            </div>

            <!-- 统计 -->
            <div class="detail-section stats-section">
              <div class="stat-card">
                <div class="stat-icon" style="background: rgba(24, 144, 255, 0.15); color: #1890ff;">
                  <ElIcon><DocumentChecked /></ElIcon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ selectedNode.totalAnswered || 0 }}</div>
                  <div class="stat-label">答题次数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon" style="background: rgba(255, 77, 79, 0.15); color: #ff4d4f;">
                  <ElIcon><CircleClose /></ElIcon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ selectedNode.mistakeCount || 0 }}</div>
                  <div class="stat-label">错误次数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon" style="background: rgba(82, 196, 26, 0.15); color: #52c41a;">
                  <ElIcon><TrendCharts /></ElIcon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ getAccuracyRate(selectedNode) }}%</div>
                  <div class="stat-label">正确率</div>
                </div>
              </div>
            </div>

            <!-- 相邻节点 -->
            <div class="detail-section" v-if="neighborNodes.length > 0">
              <div class="section-header">
                <span class="section-title">相邻知识点</span>
                <span class="section-count">{{ neighborNodes.length }}</span>
              </div>
              <div class="neighbor-list">
                <div
                  v-for="node in neighborNodes.slice(0, 8)"
                  :key="node.id"
                  class="neighbor-item"
                  :class="{ 'is-weak': getMasteryLevel(node.color) === 'weak' }"
                  @click="focusOnNode(node.id)"
                >
                  <span class="neighbor-dot" :style="{ background: node.color }"></span>
                  <span class="neighbor-name">{{ node.name }}</span>
                  <span class="neighbor-score" :style="{ color: node.color }">{{ node.masteryScore }}%</span>
                  <ElIcon class="neighbor-arrow"><ArrowRight /></ElIcon>
                </div>
              </div>
              <ElButton
                v-if="neighborNodes.length > 8"
                type="text"
                size="small"
                class="show-more-btn"
                @click="showAllNeighbors = true"
              >
                查看全部 {{ neighborNodes.length }} 个节点
              </ElButton>
            </div>

            <!-- 知识分组 -->
            <div class="detail-section" v-if="selectedNode.group">
              <div class="section-title">所属分组</div>
              <div class="group-tag">
                <ElIcon><Folder /></ElIcon>
                <span>{{ selectedNode.group }}</span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="detail-actions">
              <ElButton type="primary" class="action-btn primary" @click="goToMistakeBook(selectedNode.id)">
                <ElIcon><Document /></ElIcon>
                查看错题
              </ElButton>
              <ElButton class="action-btn" @click="focusOnNode(selectedNode.id)">
                <ElIcon><ZoomIn /></ElIcon>
                聚焦此节点
              </ElButton>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 节点列表面板 (当没有选中节点时显示) -->
      <Transition name="slide-right">
        <div v-if="!selectedNode && !focusedNodeId && nodes.length > 0" class="list-panel">
          <div class="list-header">
            <h3 class="list-title">知识点列表</h3>
            <span class="list-count">{{ nodes.length }} 个节点</span>
          </div>
          <div class="list-content">
            <div
              v-for="node in sortedNodes.slice(0, 20)"
              :key="node.id"
              class="list-item"
              :class="{ 'is-weak': getMasteryLevel(node.color) === 'weak' }"
              @click="selectNode(node)"
            >
              <span class="list-dot" :style="{ background: node.color }"></span>
              <span class="list-name">{{ node.name }}</span>
              <span class="list-score" :style="{ color: node.color }">{{ node.masteryScore }}%</span>
            </div>
            <div v-if="sortedNodes.length > 20" class="list-more">
              还有 {{ sortedNodes.length - 20 }} 个节点...
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- 邻居节点弹窗 -->
    <ElDialog
      v-model="showAllNeighbors"
      title="相邻知识点"
      width="500px"
      class="neighbors-dialog"
      destroy-on-close
    >
      <div class="neighbors-grid">
        <div
          v-for="node in neighborNodes"
          :key="node.id"
          class="neighbor-card"
          @click="focusOnNode(node.id); showAllNeighbors = false"
        >
          <span class="card-dot" :style="{ background: node.color }"></span>
          <span class="card-name">{{ node.name }}</span>
          <span class="card-score" :style="{ color: node.color }">{{ node.masteryScore }}%</span>
        </div>
      </div>
    </ElDialog>

    <!-- 流程图查看器弹窗 -->
    <ElDialog
      v-model="showFlowchartDialog"
      :title="currentFlowchart?.title || '思维范式流程图'"
      width="90%"
      top="5vh"
      class="flowchart-dialog"
      destroy-on-close
      @close="closeFlowchartDialog"
    >
      <div v-if="currentFlowchart" class="flowchart-container">
        <p v-if="currentFlowchart.description" class="flowchart-description">
          {{ currentFlowchart.description }}
        </p>
        <ParadigmFlowchart
          :data="{ nodes: currentFlowchart.nodes, edges: currentFlowchart.edges }"
          :paradigm-type="currentFlowchart.paradigmType || 'FLOW_CHART'"
          :paradigm-code="currentFlowchart.paradigmCode"
          :paradigm-name="currentFlowchart.paradigmName"
          height="600px"
          :interactive="true"
          @node-click="handleFlowchartNodeClick"
        />
      </div>
      <div v-else class="flowchart-loading">
        <ElIcon class="is-loading"><RefreshRight /></ElIcon>
        <span>加载中...</span>
      </div>
    </ElDialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  ElButton, ElIcon, ElTag, ElTooltip, ElEmpty, ElMessage, ElDialog,
  ElBadge, ElRadioGroup, ElRadioButton
} from 'element-plus'
import {
  ArrowLeft, Filter, ZoomIn, ZoomOut, InfoFilled, Close, RefreshRight,
  EditPen, Pointer, CircleCheck, Folder, Share, DocumentChecked,
  CircleClose, TrendCharts, Document, ArrowRight, View, Grid, List
} from '@element-plus/icons-vue'
import { knowledgeGraphApi } from '../../api/knowledgeGraph.js'
import { paradigmVisualizationApi } from '../../api/paradigmVisualization.js'
import { ParadigmFlowchart } from '../../components/paradigm'

const router = useRouter()
const chartRef = ref(null)
const loading = ref(false)
const nodes = ref([])
const edges = ref([])
let chart = null

// 聚焦模式状态
const focusedNodeId = ref(null)
const selectedNode = ref(null)

// 拖拽状态 - 用于禁用拖拽时的 emphasis 高亮
const isDragging = ref(false)
let dragStartTime = 0

// 过滤器状态
const showFilters = ref(false)
const selectedMasteryLevels = ref(['weak', 'medium', 'strong'])
const selectedGroups = ref([])

// 弹窗状态
const showAllNeighbors = ref(false)

// ========== 思维范式层状态 ==========
// 图层模式: 'knowledge' | 'paradigm' | 'mixed'
const layerMode = ref('knowledge')
// 范式层数据
const paradigmNodes = ref([])
const paradigmEdges = ref([])
// 选中的范式
const selectedParadigms = ref([])
// 选中的范式节点（用于显示详情）
const selectedParadigmNode = ref(null)
// 流程图查看器弹窗
const showFlowchartDialog = ref(false)
const currentFlowchart = ref(null)
const loadingParadigm = ref(false)

// 范式类型配置
const paradigmTypes = [
  { code: 'SYSTEMATIC_DEBUGGING', name: '系统调试', color: '#3B82F6' },
  { code: 'TRADE_OFF_ANALYSIS', name: '权衡分析', color: '#10B981' },
  { code: 'DECOMPOSITION', name: '问题分解', color: '#F59E0B' },
  { code: 'FIRST_PRINCIPLES', name: '第一性原理', color: '#EF4444' },
  { code: 'PATTERN_RECOGNITION', name: '模式识别', color: '#8B5CF6' },
  { code: 'ABSTRACTION_MODELING', name: '抽象建模', color: '#EC4899' },
  { code: 'ANALOGY', name: '类比推理', color: '#06B6D4' }
]

// 计算可用的范式类型（基于返回的数据）
const availableParadigms = computed(() => {
  const codes = new Set(paradigmNodes.value.map(n => n.paradigmCode).filter(Boolean))
  return paradigmTypes.filter(p => codes.has(p.code))
})

// 掌握度级别配置
const masteryLevels = [
  { key: 'weak', label: '薄弱', range: '0-40%', color: '#ff4d4f' },
  { key: 'medium', label: '一般', range: '41-70%', color: '#faad14' },
  { key: 'strong', label: '良好', range: '71-100%', color: '#52c41a' }
]

// 计算可用的知识分组
const availableGroups = computed(() => {
  const groups = new Set(nodes.value.map(n => n.group).filter(Boolean))
  return Array.from(groups).sort()
})

// 计算激活的过滤器数量
const activeFilterCount = computed(() => {
  let count = 0
  if (selectedMasteryLevels.value.length < 3) count++
  if (selectedGroups.value.length > 0 && selectedGroups.value.length < availableGroups.value.length) count++
  if (selectedParadigms.value.length > 0 && selectedParadigms.value.length < availableParadigms.value.length) count++
  return count
})

// 是否有过滤器
const hasFilters = computed(() => activeFilterCount.value > 0)

// 按掌握度排序的节点（薄弱优先）
const sortedNodes = computed(() => {
  return [...nodes.value].sort((a, b) => {
    const scoreA = parseFloat(a.masteryScore) || 0
    const scoreB = parseFloat(b.masteryScore) || 0
    return scoreA - scoreB
  })
})

// 空状态描述
const emptyDescription = computed(() => {
  if (hasFilters.value) return '没有符合筛选条件的节点'
  if (focusedNodeId.value) return '该节点没有连接其他知识点'
  return '暂无知识图谱数据'
})

// 根据颜色判断掌握度等级
const getMasteryLevel = (color) => {
  const c = color?.trim() || ''
  if (c === '#ff4d4f') return 'weak'
  if (c === '#faad14') return 'medium'
  if (c === '#52c41a') return 'strong'
  return 'unknown'
}

// 获取掌握度标签
const getMasteryLabel = (score) => {
  const s = parseFloat(score) || 0
  if (s <= 40) return '薄弱'
  if (s <= 70) return '一般'
  return '良好'
}

// 计算可见节点（考虑聚焦模式和过滤器）
const visibleNodes = computed(() => {
  let result = nodes.value

  // 聚焦模式：只显示选中节点及其邻居
  if (focusedNodeId.value) {
    const neighborIds = new Set([focusedNodeId.value])
    edges.value.forEach(edge => {
      if (edge.source === focusedNodeId.value) neighborIds.add(edge.target)
      if (edge.target === focusedNodeId.value) neighborIds.add(edge.source)
    })
    result = result.filter(n => neighborIds.has(n.id))
  }

  // 掌握度筛选
  if (selectedMasteryLevels.value.length < 3) {
    result = result.filter(n => {
      const level = getMasteryLevel(n.color)
      return selectedMasteryLevels.value.includes(level)
    })
  }

  // 分组筛选
  if (selectedGroups.value.length > 0) {
    result = result.filter(n => selectedGroups.value.includes(n.group))
  }

  return result
})

// 计算可见边
const visibleEdges = computed(() => {
  const nodeIds = new Set(visibleNodes.value.map(n => n.id))
  return edges.value.filter(e => nodeIds.has(e.source) && nodeIds.has(e.target))
})

// 计算选中节点的邻居
const neighborNodes = computed(() => {
  if (!selectedNode.value) return []

  const neighborIds = new Set()
  edges.value.forEach(edge => {
    if (edge.source === selectedNode.value.id) neighborIds.add(edge.target)
    if (edge.target === selectedNode.value.id) neighborIds.add(edge.source)
  })

  return nodes.value.filter(n => neighborIds.has(n.id))
})

// 获取正确率
const getAccuracyRate = (node) => {
  const total = node.totalAnswered || 0
  const mistakes = node.mistakeCount || 0
  if (total === 0) return 0
  return Math.round(((total - mistakes) / total) * 100)
}

// 切换掌握度筛选
const toggleMasteryLevel = (level) => {
  const index = selectedMasteryLevels.value.indexOf(level)
  if (index > -1) {
    if (selectedMasteryLevels.value.length > 1) {
      selectedMasteryLevels.value.splice(index, 1)
    }
  } else {
    selectedMasteryLevels.value.push(level)
  }
}

// 切换分组筛选
const toggleGroup = (group) => {
  const index = selectedGroups.value.indexOf(group)
  if (index > -1) {
    selectedGroups.value.splice(index, 1)
  } else {
    selectedGroups.value.push(group)
  }
}

// 选中节点
const selectNode = (node) => {
  selectedNode.value = node
}

// 获取知识图谱数据
const loadGraphData = async () => {
  loading.value = true
  try {
    const data = await knowledgeGraphApi.getSimpleGraph()
    if (data && Array.isArray(data.nodes)) {
      nodes.value = data.nodes || []
      edges.value = data.edges || []

      if (nodes.value.length > 0) {
        // 默认选中所有分组
        selectedGroups.value = availableGroups.value

        await nextTick()
        renderChart()
      }
    } else {
      nodes.value = []
      edges.value = []
    }
  } catch (error) {
    console.error('加载知识图谱失败:', error)
    ElMessage.error('加载知识图谱失败')
    nodes.value = []
    edges.value = []
  } finally {
    loading.value = false
  }
}

// 获取范式层数据
const loadParadigmLayer = async () => {
  loadingParadigm.value = true
  try {
    const data = await paradigmVisualizationApi.getGraphLayer(selectedParadigms.value.length > 0 ? selectedParadigms.value : null)
    if (data) {
      paradigmNodes.value = data.nodes || []
      paradigmEdges.value = data.edges || []
    }
  } catch (error) {
    console.error('加载范式层数据失败:', error)
    ElMessage.error('加载范式层数据失败')
    paradigmNodes.value = []
    paradigmEdges.value = []
  } finally {
    loadingParadigm.value = false
  }
}

// 切换图层模式
const switchLayerMode = async (mode) => {
  layerMode.value = mode
  if (mode === 'paradigm' || mode === 'mixed') {
    await loadParadigmLayer()
  }
  nextTick(() => {
    renderChart()
  })
}

// 切换范式筛选
const toggleParadigm = (code) => {
  const index = selectedParadigms.value.indexOf(code)
  if (index > -1) {
    selectedParadigms.value.splice(index, 1)
  } else {
    selectedParadigms.value.push(code)
  }
  // 重新加载范式层数据
  if (layerMode.value === 'paradigm' || layerMode.value === 'mixed') {
    loadParadigmLayer().then(() => {
      renderChart()
    })
  }
}

// 选中范式节点
const selectParadigmNode = (node) => {
  selectedParadigmNode.value = node
}

// 查看流程图详情
const viewFlowchart = async (node) => {
  if (!node || !node.flowchartId) return
  try {
    const data = await paradigmVisualizationApi.getFlowchartDetail(node.flowchartId)
    currentFlowchart.value = data
    showFlowchartDialog.value = true
  } catch (error) {
    console.error('加载流程图失败:', error)
    ElMessage.error('加载流程图失败')
  }
}

// 关闭流程图弹窗
const closeFlowchartDialog = () => {
  showFlowchartDialog.value = false
  currentFlowchart.value = null
}

// 处理流程图节点点击
const handleFlowchartNodeClick = (node) => {
  console.log('流程图节点点击:', node)
  // 可以在这里扩展更多功能，如显示节点详情等
}

// 渲染图表
const renderChart = () => {
  if (!chartRef.value) return

  if (chart) {
    chart.dispose()
  }

  chart = echarts.init(chartRef.value)

  // 根据图层模式准备数据
  let seriesData = []
  let seriesLinks = []

  if (layerMode.value === 'knowledge') {
    // 知识层：只显示知识节点
    seriesData = visibleNodes.value.map(node => ({
      ...node,
      category: 'knowledge'
    }))
    seriesLinks = visibleEdges.value
  } else if (layerMode.value === 'paradigm') {
    // 范式层：只显示范式节点
    seriesData = paradigmNodes.value.map(node => ({
      ...node,
      id: node.id,
      name: node.label,
      symbolSize: node.size || 40,
      itemStyle: {
        color: node.color,
        borderColor: selectedParadigmNode.value?.id === node.id ? '#fff' : 'transparent',
        borderWidth: selectedParadigmNode.value?.id === node.id ? 3 : 0
      },
      label: {
        show: true,
        position: 'bottom',
        color: '#cbd5e1',
        fontSize: 12,
        fontWeight: 500,
        formatter: (params) => {
          const name = params.data.name
          return name.length > 8 ? name.substring(0, 8) + '...' : name
        }
      },
      category: 'paradigm'
    }))
    seriesLinks = paradigmEdges.value.map(edge => ({
      source: edge.source,
      target: edge.target,
      label: edge.label ? { show: true, formatter: edge.label } : undefined
    }))
  } else if (layerMode.value === 'mixed') {
    // 混合视图：显示知识节点和范式节点
    const knowledgeData = visibleNodes.value.map(node => ({
      ...node,
      category: 'knowledge'
    }))
    const paradigmData = paradigmNodes.value.map(node => ({
      ...node,
      id: node.id,
      name: node.label,
      symbolSize: (node.size || 40) * 0.8,
      itemStyle: {
        color: node.color,
        opacity: 0.8
      },
      label: {
        show: true,
        position: 'bottom',
        color: '#cbd5e1',
        fontSize: 11
      },
      category: 'paradigm'
    }))
    seriesData = [...knowledgeData, ...paradigmData]
    seriesLinks = [...visibleEdges.value, ...paradigmEdges.value]
  }

  // 最终数据处理 - 知识节点需要额外处理
  const finalData = seriesData.map(node => {
    if (node.category === 'knowledge') {
      return {
        ...node,
        symbolSize: focusedNodeId.value === node.id ? 45 : Math.max(20, Math.min(40, 20 + (node.totalAnswered || 0) * 2)),
        itemStyle: {
          color: node.color ? node.color.trim() : '#64748b',
          borderColor: selectedNode.value?.id === node.id ? '#fff' : 'transparent',
          borderWidth: selectedNode.value?.id === node.id ? 3 : 0,
          shadowBlur: focusedNodeId.value === node.id ? 20 : 0,
          shadowColor: node.color ? node.color.trim() : '#64748b'
        },
        label: {
          show: true,
          position: 'bottom',
          color: '#cbd5e1',
          fontSize: 12,
          fontWeight: 500,
          formatter: (params) => {
            const name = params.data.name
            return name.length > 8 ? name.substring(0, 8) + '...' : name
          }
        },
        emphasis: { disabled: true }
      }
    }
    // 范式节点保持原有配置
    return node
  })

  const option = {
    backgroundColor: 'transparent',
    series: [{
      type: 'graph',
      layout: 'force',
      data: finalData,
      links: seriesLinks,
      roam: true,
      draggable: true,
      // 禁用悬停动画，防止拖拽时闪烁
      hoverAnimation: false,
      force: {
        repulsion: focusedNodeId.value ? 400 : 250,
        gravity: 0.08,
        edgeLength: focusedNodeId.value ? [50, 90] : [70, 130],
        layoutAnimation: true
      },
      lineStyle: {
        color: 'rgba(148, 163, 184, 0.35)',
        width: 1.5,
        curveness: 0.2
      },
      // 禁用 emphasis 的邻接高亮
      emphasis: {
        disabled: true
      },
      zoom: 1,
      scaleLimit: { min: 0.3, max: 3 }
    }]
  }

  chart.setOption(option)

  // 单击事件 - 选中节点
  chart.on('click', (params) => {
    if (params.dataType === 'node') {
      const clickedNode = params.data
      if (clickedNode.category === 'paradigm') {
        // 范式节点
        const paradigmNode = paradigmNodes.value.find(n => n.id === clickedNode.id)
        selectParadigmNode(paradigmNode)
      } else {
        // 知识节点
        const node = nodes.value.find(n => n.id === clickedNode.id)
        selectedNode.value = node
        selectedParadigmNode.value = null
      }
    }
  })

  // 双击事件 - 进入聚焦模式
  chart.on('dblclick', (params) => {
    if (params.dataType === 'node') {
      focusOnNode(params.data.id)
    }
  })

  // 拖拽开始 - 禁用高亮交互
  chart.on('mousedown', (params) => {
    if (params.dataType === 'node') {
      isDragging.value = true
      dragStartTime = Date.now()
    }
  })

  // 拖拽结束 - 延迟恢复高亮交互（区分点击和拖拽）
  chart.on('mouseup', () => {
    const dragDuration = Date.now() - dragStartTime
    // 如果拖拽时间小于 200ms，认为是点击而非拖拽
    if (dragDuration < 200) {
      isDragging.value = false
    } else {
      // 延迟恢复，避免拖拽结束时的闪烁
      setTimeout(() => {
        isDragging.value = false
      }, 100)
    }
  })

  // 鼠标移出图表区域时重置拖拽状态
  chart.getZr().on('mouseout', () => {
    isDragging.value = false
  })

  // 窗口大小变化时重新调整
  const handleResize = () => chart?.resize()
  window.addEventListener('resize', handleResize)

  chart._cleanup = () => {
    window.removeEventListener('resize', handleResize)
    chart.off('mousedown')
    chart.off('mouseup')
    chart.getZr().off('mouseout')
  }
}

// 聚焦到指定节点
const focusOnNode = (nodeId) => {
  focusedNodeId.value = nodeId
  selectedNode.value = null

  // 重新渲染图表
  nextTick(() => {
    renderChart()
  })
}

// 退出聚焦模式
const exitFocusMode = () => {
  focusedNodeId.value = null
  selectedNode.value = null
  nextTick(() => {
    renderChart()
  })
}

// 重置过滤器
const resetFilters = () => {
  selectedMasteryLevels.value = ['weak', 'medium', 'strong']
  selectedGroups.value = availableGroups.value
}

// 重置视图
const resetView = () => {
  if (chart) {
    chart.dispatchAction({
      type: 'restore'
    })
  }
  exitFocusMode()
}

// 放大
const zoomIn = () => {
  if (chart) {
    const currentZoom = chart.getOption().series[0].zoom || 1
    chart.setOption({
      series: [{ zoom: Math.min(currentZoom * 1.2, 3) }]
    })
  }
}

// 缩小
const zoomOut = () => {
  if (chart) {
    const currentZoom = chart.getOption().series[0].zoom || 1
    chart.setOption({
      series: [{ zoom: Math.max(currentZoom / 1.2, 0.3) }]
    })
  }
}

// 跳转到错题本
const goToMistakeBook = (knowledgePoint) => {
  router.push({
    path: '/mistake-book',
    query: { keyword: knowledgePoint }
  })
}

// ESC 键退出聚焦模式
const handleKeyDown = (e) => {
  if (e.key === 'Escape' && focusedNodeId.value) {
    exitFocusMode()
  }
}

// 监听可见节点变化，自动重新渲染
watch([visibleNodes, visibleEdges], () => {
  if (chart && !loading.value) {
    renderChart()
  }
}, { deep: true })

// 生命周期
onMounted(() => {
  loadGraphData()
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  if (chart) {
    chart._cleanup?.()
    chart.dispose()
    chart = null
  }
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped lang="scss">
.knowledge-graph-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #161616;
  border-radius: 24px;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

// 页面头部
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(30, 41, 59, 0.5);
  border-bottom: 1px solid rgba(100, 116, 139, 0.2);
  flex-shrink: 0;
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;

  .back-btn {
    color: #94a3b8;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: color 150ms ease;
    cursor: pointer;

    &:hover {
      color: #f1f5f9;
    }
  }

  .page-title {
    font-size: 17px;
    font-weight: 600;
    color: #f1f5f9;
    letter-spacing: -0.3px;
  }

  .beta-tag {
    background: rgba(59, 130, 246, 0.15) !important;
    color: #3b82f6 !important;
    border-color: rgba(59, 130, 246, 0.3) !important;
    font-weight: 500;
  }

  .focus-tag {
    margin-left: 8px;
    animation: pulse-tag 2s ease-in-out infinite;
  }
}

@keyframes pulse-tag {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.header-center {
  .stats-text {
    color: #64748b;
    font-size: 13px;
    font-weight: 500;

    strong {
      color: #f1f5f9;
      margin: 0 4px;
      font-weight: 600;
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;

  .chart-controls {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px;
    background: rgba(51, 65, 85, 0.3);
    border-radius: 8px;

    .control-btn {
      color: #94a3b8;
      transition: all 150ms ease;
      cursor: pointer;

      &:hover {
        color: #f1f5f9;
        background: rgba(30, 41, 59, 0.5);
      }
    }
  }

  .filter-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
    transition: all 150ms ease;
    cursor: pointer;
  }

  .filter-badge {
    margin-left: 4px;
  }

  .legend {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 6px 12px;
    background: rgba(51, 65, 85, 0.3);
    border-radius: 8px;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    transition: opacity 150ms ease;

    &:hover {
      opacity: 0.8;
    }
  }

  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    box-shadow: 0 0 8px currentColor;

    &.weak { background: #ef4444; color: #ef4444; }
    &.medium { background: #f59e0b; color: #f59e0b; }
    &.strong { background: #22c55e; color: #22c55e; }
  }

  .legend-text {
    color: #94a3b8;
    font-size: 12px;
    font-weight: 500;
  }
}

// 主体容器
.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

// 过滤器面板
.filter-panel {
  width: 260px;
  background: rgba(30, 41, 59, 0.5);
  border-right: 1px solid rgba(100, 116, 139, 0.2);
  padding: 20px;
  overflow-y: auto;
  flex-shrink: 0;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .filter-title {
    font-size: 15px;
    font-weight: 600;
    color: #f1f5f9;
  }

  .reset-btn {
    color: #64748b;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: color 150ms ease;
    cursor: pointer;

    &:hover {
      color: #3b82f6;
    }
  }
}

.filter-section {
  margin-bottom: 28px;

  .filter-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    font-weight: 600;
    color: #94a3b8;
    margin-bottom: 14px;
    text-transform: uppercase;
    letter-spacing: 0.5px;

    .el-icon {
      font-size: 14px;
    }
  }
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(51, 65, 85, 0.3);
  border: 1px solid transparent;
  border-radius: 20px;
  font-size: 12px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 150ms ease;
  user-select: none;

  &:hover {
    background: rgba(100, 116, 139, 0.4);
    color: #f1f5f9;
  }

  &.active {
    background: rgba(59, 130, 246, 0.15);
    border-color: rgba(59, 130, 246, 0.4);
    color: #3b82f6;
  }

  .chip-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.weak { background: #ef4444; }
    &.medium { background: #f59e0b; }
    &.strong { background: #22c55e; }
  }
}

.filter-hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 10px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;

  .el-icon {
    color: #f59e0b;
    margin-top: 2px;
    flex-shrink: 0;
  }
}

// 图表区域
.chart-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: radial-gradient(ellipse at center, rgba(30, 41, 59, 0.3) 0%, transparent 70%);
}

.chart {
  width: 100%;
  height: 100%;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;

  .empty-illustration {
    width: 180px;
    height: 140px;
    margin: 0 auto 20px;
    color: #64748b;
    opacity: 0.6;

    svg {
      width: 100%;
      height: 100%;
    }
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #94a3b8;
    margin-bottom: 8px;
  }

  .empty-subtitle {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 20px;
  }

  :deep(.el-empty__description) {
    color: #94a3b8;
  }
}

.focus-hint {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid rgba(100, 116, 139, 0.2);
  border-radius: 20px;
  font-size: 13px;
  color: #94a3b8;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5);

  .el-icon {
    color: #3b82f6;
  }
}

.bottom-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(100, 116, 139, 0.2);
  border-radius: 16px;
  font-size: 12px;
  color: #64748b;
  backdrop-filter: blur(10px);

  .el-icon {
    color: #3b82f6;
  }
}

// 详情面板
.detail-panel {
  width: 340px;
  background: rgba(30, 41, 59, 0.5);
  border-left: 1px solid rgba(100, 116, 139, 0.2);
  padding: 20px;
  overflow-y: auto;
  flex-shrink: 0;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(100, 116, 139, 0.2);

  .node-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  .detail-title {
    flex: 1;
    font-size: 17px;
    font-weight: 600;
    color: #f1f5f9;
    margin: 0;
    line-height: 1.4;
    padding-top: 2px;
    word-break: break-word;
  }

  .close-btn {
    color: #64748b;
    transition: color 150ms ease;
    cursor: pointer;
    margin-top: 4px;

    &:hover {
      color: #f1f5f9;
    }
  }
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-section {
  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .section-title {
    font-size: 12px;
    font-weight: 600;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .section-count {
    padding: 2px 8px;
    background: rgba(51, 65, 85, 0.3);
    border-radius: 10px;
    font-size: 11px;
    font-weight: 600;
    color: #94a3b8;
  }
}

// 掌握度区块
.mastery-section {
  background: rgba(51, 65, 85, 0.3);
  border-radius: 12px;
  padding: 16px;
}

.mastery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .mastery-label {
    font-size: 13px;
    color: #94a3b8;
    font-weight: 500;
  }

  .mastery-badge {
    padding: 4px 10px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
  }
}

.mastery-value-row {
  margin-bottom: 12px;

  .mastery-value {
    font-size: 36px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -1px;
  }
}

.progress-wrapper {
  .progress-track {
    height: 8px;
    background: rgba(100, 116, 139, 0.2);
    border-radius: 4px;
    overflow: hidden;
  }

  .progress-fill {
    height: 100%;
    border-radius: 4px;
    transition: width 0.5s ease;
  }

  .progress-markers {
    display: flex;
    justify-content: space-between;
    margin-top: 6px;

    .marker {
      font-size: 11px;
      color: #64748b;
    }
  }
}

// 统计区块
.stats-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: rgba(51, 65, 85, 0.3);
  border-radius: 10px;
  transition: transform 150ms ease;

  &:hover {
    transform: translateX(4px);
  }

  .stat-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
  }

  .stat-info {
    flex: 1;
  }

  .stat-value {
    font-size: 20px;
    font-weight: 700;
    color: #f1f5f9;
    line-height: 1;
    margin-bottom: 4px;
  }

  .stat-label {
    font-size: 12px;
    color: #94a3b8;
  }
}

// 邻居列表
.neighbor-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.neighbor-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(51, 65, 85, 0.3);
  border-radius: 8px;
  cursor: pointer;
  transition: all 150ms ease;
  border: 1px solid transparent;

  &:hover {
    background: rgba(100, 116, 139, 0.4);
    transform: translateX(4px);

    .neighbor-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.is-weak {
    border-color: rgba(239, 68, 68, 0.3);
    background: rgba(239, 68, 68, 0.08);
  }

  .neighbor-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .neighbor-name {
    flex: 1;
    font-size: 13px;
    color: #f1f5f9;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .neighbor-score {
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
  }

  .neighbor-arrow {
    font-size: 12px;
    color: #64748b;
    opacity: 0;
    transform: translateX(-4px);
    transition: all 150ms ease;
  }
}

.show-more-btn {
  margin-top: 8px;
  color: #3b82f6;
  font-weight: 500;
}

// 分组标签
.group-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: rgba(51, 65, 85, 0.3);
  border-radius: 8px;
  font-size: 13px;
  color: #94a3b8;

  .el-icon {
    font-size: 14px;
  }
}

// 操作按钮
.detail-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid rgba(100, 116, 139, 0.2);

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    border-radius: 10px;
    font-weight: 500;
    transition: all 150ms ease;
    cursor: pointer;

    &.primary {
      background: #3b82f6;
      border-color: #3b82f6;

      &:hover {
        background: #2563eb;
        border-color: #2563eb;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
      }
    }

    &:not(.primary) {
      background: rgba(51, 65, 85, 0.3);
      border-color: rgba(100, 116, 139, 0.2);
      color: #94a3b8;

      &:hover {
        background: rgba(100, 116, 139, 0.4);
        color: #f1f5f9;
      }
    }
  }
}

// 列表面板
.list-panel {
  width: 280px;
  background: rgba(30, 41, 59, 0.5);
  border-left: 1px solid rgba(100, 116, 139, 0.2);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.list-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(100, 116, 139, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;

  .list-title {
    font-size: 15px;
    font-weight: 600;
    color: #f1f5f9;
    margin: 0;
  }

  .list-count {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 150ms ease;
  margin-bottom: 4px;

  &:hover {
    background: rgba(51, 65, 85, 0.3);
  }

  &.is-weak {
    background: rgba(239, 68, 68, 0.08);

    &:hover {
      background: rgba(239, 68, 68, 0.15);
    }
  }

  .list-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .list-name {
    flex: 1;
    font-size: 13px;
    color: #f1f5f9;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .list-score {
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
  }
}

.list-more {
  text-align: center;
  padding: 12px;
  font-size: 12px;
  color: #64748b;
}

// 邻居弹窗
:deep(.neighbors-dialog) {
  .el-dialog__header {
    border-bottom: 1px solid rgba(100, 116, 139, 0.2);
    margin-right: 0;
    padding: 16px 20px;
  }

  .el-dialog__body {
    padding: 20px;
  }
}

.neighbors-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.neighbor-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(100, 116, 139, 0.2);
  border-radius: 10px;
  cursor: pointer;
  transition: all 150ms ease;

  &:hover {
    border-color: #3b82f6;
    background: rgba(59, 130, 246, 0.08);
    transform: translateY(-2px);
  }

  .card-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .card-name {
    flex: 1;
    font-size: 13px;
    color: #f1f5f9;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-score {
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
  }
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 200ms ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 250ms ease;
}

.slide-left-enter-from,
.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 250ms ease;
}

.slide-right-enter-from,
.slide-right-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 250ms ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}


// 响应式适配
@media (max-width: 1200px) {
  .detail-panel,
  .list-panel {
    width: 280px;
  }
}

@media (max-width: 992px) {
  .filter-panel {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 10;
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
  }

  .detail-panel,
  .list-panel {
    width: 260px;
  }
}

@media (max-width: 768px) {
  .knowledge-graph-page {
    border-radius: 16px;
  }

  .page-header {
    padding: 10px 16px;
    flex-wrap: wrap;
    gap: 10px;

    .header-center {
      order: 3;
      width: 100%;
      text-align: center;
    }
  }

  .legend {
    display: none !important;
  }

  .detail-panel,
  .list-panel {
    position: absolute;
    right: 0;
    top: 0;
    bottom: 0;
    z-index: 10;
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
  }

  .neighbors-grid {
    grid-template-columns: 1fr;
  }
}

// ========== 范式详情面板样式 ==========
.paradigm-detail-panel {
  .paradigm-type-tag {
    display: inline-block;
    padding: 6px 12px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    border: 1px solid;
  }

  .description-text {
    font-size: 14px;
    line-height: 1.6;
    color: #94a3b8;
    margin: 0;
  }
}

// ========== 流程图弹窗样式 ==========
.flowchart-dialog {
  .el-dialog__body {
    padding: 20px;
    max-height: calc(90vh - 120px);
    overflow-y: auto;
  }

  .flowchart-container {
    .flowchart-description {
      font-size: 14px;
      color: #94a3b8;
      margin-bottom: 16px;
      line-height: 1.6;
    }
  }

  .flowchart-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 60px;
    color: #94a3b8;
    font-size: 14px;

    .el-icon {
      font-size: 20px;
    }
  }
}

// 图层切换按钮样式
:deep(.el-radio-group) {
  .el-radio-button__inner {
    display: flex;
    align-items: center;
    gap: 4px;
    background: rgba(51, 65, 85, 0.5);
    border-color: rgba(100, 116, 139, 0.3);
    color: #94a3b8;

    &:hover {
      color: #f1f5f9;
    }
  }

  .el-radio-button__original-radio:checked + .el-radio-button__inner {
    background: #3b82f6;
    border-color: #3b82f6;
    color: #fff;
    box-shadow: none;
  }
}

// 深色模式适配
html.dark {
  .paradigm-detail-panel {
    background: rgba(30, 41, 59, 0.8);
  }
}
</style>
