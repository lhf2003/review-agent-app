<script setup>
import { ref, computed, onMounted, watch, h, Teleport } from 'vue'
import { Plus, FolderOpened, Folder, Delete, Edit, Grid, Collection, Document, Cpu, Histogram, OfficeBuilding, TrendCharts, List, ScaleToOriginal, HelpFilled, Monitor, ArrowRight, Star, Rank } from '@element-plus/icons-vue'
import { tagApi } from '../../api/tag'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../CustomScroll.vue'
import TagRelationDrawer from './TagRelationDrawer.vue'
import RecommendTagDrawer from './RecommendTagDrawer.vue'

// 图标名称到组件的映射（支持数据库 icon 字段或友好别名）
const iconMap = {
  // Element Plus 标准图标名
  'Document': Document,
  'Cpu': Cpu,
  'Histogram': Histogram,
  'OfficeBuilding': OfficeBuilding,
  'Grid': Grid,
  'Folder': Folder,
  'FolderOpened': FolderOpened,
  'TrendCharts': TrendCharts,
  'Monitor': Monitor,
  'List': List,
  'ScaleToOriginal': ScaleToOriginal,
  'HelpFilled': HelpFilled,
  // 友好别名（向后兼容）
  'Code': Document,
  'Brain': Cpu,
  'Signal': Histogram,
  'UseCase': OfficeBuilding,
  'Collection': Grid
}

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

// 维度相关
const dimensions = ref([])
const selectedDimensionId = ref(null)
const loadingDimensions = ref(false)

// 标签树相关
const tagTree = ref([])
const loadingTags = ref(false)

// 选择的标签
const selectedTag = ref(null)
const drawerVisible = ref(false)
const expandedKeys = ref([])

// 推荐标签抽屉
const recommendDrawerVisible = ref(false)

// 打开推荐标签抽屉
function openRecommendTags() {
  recommendDrawerVisible.value = true
}

// 对话框状态
const createDialog = ref(false)
const createForm = ref({ name: '', parentId: null, dimensionId: null })
const renameDialog = ref(false)
const renameForm = ref({ id: null, name: '' })

// 预定义维度颜色映射（只包含颜色，图标从数据库获取）
const dimensionColors = {
  'TECH_DOMAIN': { color: '#409EFF', bg: '#ecf5ff' },
  'THINKING_PARADIGM': { color: '#9C27B0', bg: '#f3e5f5' },
  'DIFFICULTY': { color: '#F56C6C', bg: '#fef0f0' },
  'SCENARIO': { color: '#67C23A', bg: '#f0f9eb' }
}

// 预定义标签颜色
const tagColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#9C27B0', '#009688', '#3F51B5', '#FF9800', '#795548',
  '#607D8B', '#E91E63'
]

function getTagColor(index) {
  return tagColors[index % tagColors.length]
}

function getDimensionStyle(code) {
  return dimensionColors[code] || { color: '#909399', bg: '#f5f5f5' }
}

// 获取维度图标组件
function getDimensionIcon(iconName) {
  return iconMap[iconName] || Collection
}

// 加载维度列表
async function loadDimensions() {
  loadingDimensions.value = true
  try {
    const resp = await tagApi.getDimensionList()
    dimensions.value = resp || []
    // 默认选择第一个维度
    if (dimensions.value.length > 0 && !selectedDimensionId.value) {
      selectedDimensionId.value = dimensions.value[0].id
    }
  } catch (e) {
    ElMessage.error(`加载维度失败: ${e.message}`)
  } finally {
    loadingDimensions.value = false
  }
}

// 加载标签树
async function loadTagTree() {
  if (!selectedDimensionId.value) {
    tagTree.value = []
    return
  }
  loadingTags.value = true
  try {
    const resp = await tagApi.getTagTree(selectedDimensionId.value)
    tagTree.value = resp || []
    // 默认展开第一级
    if (tagTree.value.length > 0) {
      expandedKeys.value = tagTree.value.map(t => t.id)
    } else {
      expandedKeys.value = []
    }
  } catch (e) {
    ElMessage.error(`加载标签失败: ${e.message}`)
  } finally {
    loadingTags.value = false
  }
}

// 监听维度变化，重新加载标签
watch(selectedDimensionId, () => {
  selectedTag.value = null
  loadTagTree()
})

// 计算扁平化的标签列表（用于展示）
const flatTags = computed(() => {
  const result = []
  function traverse(list, level = 0, parentColor = null, parentId = null) {
    for (const node of list || []) {
      const color = level === 0 ? getTagColor(result.filter(r => r.level === 0).length) : parentColor
      result.push({
        ...node,
        level,
        color,
        parentId,
        isSystemTag: node.isSystemTag,
        children: node.children
      })
      if (node.children) {
        traverse(node.children, level + 1, color, node.id)
      }
    }
  }
  traverse(tagTree.value)
  return result
})

// 获取当前选中维度的信息
const currentDimension = computed(() => {
  return dimensions.value.find(d => d.id === selectedDimensionId.value)
})

// 获取子标签数量
function getChildrenCount(tag) {
  let count = 0
  function traverse(list) {
    for (const node of list || []) {
      count++
      traverse(node.children)
    }
  }
  traverse(tag.children)
  return count
}

// ========== 维度选择 ==========

function selectDimension(dimensionId) {
  selectedDimensionId.value = dimensionId
}

// ========== 标签操作 ==========

function openCreateTag(parentId = null) {
  createForm.value = { name: '', parentId, dimensionId: selectedDimensionId.value }
  createDialog.value = true
}

async function createTag() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  try {
    await tagApi.addTag({
      name: createForm.value.name.trim(),
      parentId: createForm.value.parentId,
      dimensionId: createForm.value.dimensionId
    })
    ElMessage.success('创建成功')
    createDialog.value = false
    await loadTagTree()
  } catch (e) {
    ElMessage.error(`创建失败: ${e.message}`)
  }
}

function openRenameTag(tag) {
  renameForm.value = { id: tag.id, name: tag.name }
  renameDialog.value = true
}

async function doRenameTag() {
  if (!renameForm.value.name.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }
  try {
    await tagApi.updateTag({
      id: renameForm.value.id,
      name: renameForm.value.name.trim()
    })
    ElMessage.success('已重命名')
    renameDialog.value = false
    await loadTagTree()
  } catch (e) {
    ElMessage.error(`重命名失败: ${e.message}`)
  }
}

async function doDeleteTag(tag) {
  try {
    const childCount = getChildrenCount(tag)
    const confirmMsg = childCount > 0
      ? `标签「${tag.name}」包含 ${childCount} 个子标签，删除将同时删除所有子标签。确认删除？`
      : `确认删除标签「${tag.name}」？`

    await ElMessageBox.confirm(confirmMsg, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await tagApi.deleteTag(tag.id)
    ElMessage.success('删除成功')
    if (selectedTag.value?.id === tag.id) {
      selectedTag.value = null
    }
    await loadTagTree()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

function onSelectTag(tag) {
  selectedTag.value = tag
  drawerVisible.value = true
}

function toggleExpand(tag) {
  const index = expandedKeys.value.indexOf(tag.id)
  if (index > -1) {
    expandedKeys.value.splice(index, 1)
  } else {
    expandedKeys.value.push(tag.id)
  }
}

onMounted(async () => {
  await loadDimensions()
  await loadTagTree()
})

// ========== 拖拽相关 ==========

const draggingTag = ref(null)
const dragOverTag = ref(null)
const dragOverPosition = ref(null) // 'before', 'after', 'inside'
const dragOverFloat = ref(false) // 是否悬停在浮动放置面板

// 判断是否允许拖拽
function isDraggable(tag) {
  return !tag.isSystemTag
}

// 判断是否可以作为放置目标
function isValidDropTarget(sourceTag, targetTag) {
  if (!sourceTag || !targetTag) return false
  if (sourceTag.id === targetTag.id) return false
  // 不能放到自己的子标签中
  if (isDescendant(targetTag, sourceTag)) return false
  // 系统标签不能作为放置目标
  if (targetTag.isSystemTag) return false
  return true
}

// 判断是否为后代
function isDescendant(possibleChild, possibleParent) {
  function findInChildren(tag, parentId) {
    if (tag.id === parentId) return true
    if (tag.children) {
      for (const child of tag.children) {
        if (findInChildren(child, parentId)) return true
      }
    }
    return false
  }
  return findInChildren(possibleChild, possibleParent.id)
}

// 开始拖拽
function handleDragStart(event, tag) {
  if (!isDraggable(tag)) {
    event.preventDefault()
    return
  }
  draggingTag.value = tag
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', tag.id)
}

// 拖拽经过
function handleDragOver(event, tag, position) {
  event.preventDefault()
  if (!isValidDropTarget(draggingTag.value, tag)) {
    event.dataTransfer.dropEffect = 'none'
    return
  }
  dragOverTag.value = tag
  dragOverPosition.value = position
  event.dataTransfer.dropEffect = 'move'
}

// 拖拽离开
function handleDragLeave() {
  dragOverTag.value = null
  dragOverPosition.value = null
}

// 放置
async function handleDrop(event, targetTag, position) {
  event.preventDefault()
  event.stopPropagation()

  if (!isValidDropTarget(draggingTag.value, targetTag)) {
    handleDragLeave()
    return
  }

  const sourceTag = draggingTag.value
  let newParentId = 0

  // 根据放置位置确定新的 parentId
  if (position === 'inside') {
    // 放到目标标签内部
    newParentId = targetTag.id
  } else if (position === 'before' || position === 'after') {
    // 放到目标标签前后，使用目标的 parentId（null 转为 0）
    newParentId = targetTag.parentId || 0
  }

  // 如果 parentId 没有变化，不执行操作
  if (sourceTag.parentId === newParentId || (sourceTag.parentId === null && newParentId === 0)) {
    handleDragLeave()
    return
  }

  // 执行移动
  try {
    await tagApi.updateTag({
      id: sourceTag.id,
      parentId: newParentId
    })
    ElMessage.success('标签移动成功')
    await loadTagTree()
  } catch (e) {
    ElMessage.error(`移动失败: ${e.message}`)
  } finally {
    handleDragLeave()
    draggingTag.value = null
  }
}

// 拖拽结束
function handleDragEnd() {
  draggingTag.value = null
  dragOverTag.value = null
  dragOverPosition.value = null
  dragOverFloat.value = false
}

// ========== 浮动放置面板相关 ==========

function handleFloatDragOver(event) {
  event.preventDefault()
  if (!draggingTag.value) return
  // 只有当前有父级的标签才能成为根级标签（parentId 为 null 或 0 表示顶级）
  if (draggingTag.value.parentId !== null && draggingTag.value.parentId !== 0) {
    event.dataTransfer.dropEffect = 'move'
    dragOverFloat.value = true
  }
}

function handleFloatDragLeave() {
  dragOverFloat.value = false
}

async function handleFloatDrop(event) {
  event.preventDefault()
  event.stopPropagation()

  if (!draggingTag.value || draggingTag.value.parentId === null || draggingTag.value.parentId === 0) {
    handleFloatDragLeave()
    return
  }

  const sourceTag = draggingTag.value

  try {
    await tagApi.updateTag({
      id: sourceTag.id,
      parentId: 0
    })
    ElMessage.success('标签已移动到顶级')
    await loadTagTree()
  } catch (e) {
    ElMessage.error(`移动失败: ${e.message}`)
  } finally {
    handleFloatDragLeave()
    draggingTag.value = null
  }
}
</script>

<template>
  <div class="tag-management-pane">
    <!-- 左侧区域：维度 + 标签列表 -->
    <div class="left-panel">
      <!-- 维度选择区域 -->
      <div class="dimension-section">
        <div class="section-header">
          <el-icon class="section-icon"><Grid /></el-icon>
          <span class="section-title">标签维度</span>
        </div>
        <div v-if="loadingDimensions" class="dimension-loading">
          <el-skeleton :rows="2" animated />
        </div>
        <div v-else class="dimension-list">
          <div
            v-for="dim in dimensions"
            :key="dim.id"
            class="dimension-card"
            :class="{ 'is-active': selectedDimensionId === dim.id }"
            :style="{
              '--dim-color': getDimensionStyle(dim.code).color,
              '--dim-bg': getDimensionStyle(dim.code).bg
            }"
            @click="selectDimension(dim.id)"
          >
            <div class="dimension-icon">
              <el-icon :size="24" :color="getDimensionStyle(dim.code).color">
                <component :is="getDimensionIcon(dim.icon)" />
              </el-icon>
            </div>
            <div class="dimension-info">
              <div class="dimension-name">{{ dim.name }}</div>
              <div class="dimension-desc" v-if="dim.description">{{ dim.description }}</div>
            </div>
            <div v-if="selectedDimensionId === dim.id" class="dimension-indicator"></div>
          </div>
        </div>
      </div>

      <!-- 标签管理区域 -->
      <div class="tag-section">
        <div class="section-header">
          <el-icon class="section-icon"><Collection /></el-icon>
          <span class="section-title">{{ currentDimension?.name || '标签' }}管理</span>
          <span class="drag-hint" title="拖拽标签可调整层级关系">
            <el-icon><Rank /></el-icon>
            <span>拖拽移动</span>
          </span>
          <div class="header-actions">
            <el-button
              type="warning"
              :icon="Star"
              size="small"
              @click="openRecommendTags"
            >
              推荐标签
            </el-button>
            <el-button
              type="primary"
              :icon="Plus"
              size="small"
              @click="openCreateTag(null)"
              :disabled="!selectedDimensionId"
            >
              创建标签
            </el-button>
          </div>
        </div>

        <div class="tag-tree-container">
          <CustomScroll>
            <div v-if="loadingTags" class="loading-wrapper">
              <el-skeleton :rows="6" animated />
            </div>

            <div v-else-if="!selectedDimensionId" class="empty-wrapper">
              <el-empty description="请选择标签维度" :image-size="80" />
            </div>

            <div v-else-if="tagTree.length === 0" class="empty-wrapper">
              <el-empty description="该维度下暂无标签，点击上方按钮创建" :image-size="80" />
            </div>

            <div v-else class="tag-tree">
              <div
                v-for="tag in flatTags"
                :key="tag.id"
                class="tag-item"
                :class="{
                  'is-selected': selectedTag?.id === tag.id,
                  'is-expanded': expandedKeys.includes(tag.id),
                  [`level-${tag.level}`]: true,
                  'is-dragging': draggingTag?.id === tag.id,
                  'is-drag-over': dragOverTag?.id === tag.id,
                  'drag-over-inside': dragOverTag?.id === tag.id && dragOverPosition === 'inside',
                  'is-draggable': isDraggable(tag),
                  'is-system': tag.isSystemTag
                }"
                :style="{ '--tag-color': tag.color }"
                :draggable="isDraggable(tag)"
                @dragstart="handleDragStart($event, tag)"
                @dragend="handleDragEnd"
              >
                <!-- 拖拽指示器 - 前 -->
                <div
                  v-if="!tag.isSystemTag"
                  class="drop-indicator drop-before"
                  :class="{ 'is-active': dragOverTag?.id === tag.id && dragOverPosition === 'before' }"
                  @dragover.prevent="handleDragOver($event, tag, 'before')"
                  @dragleave="handleDragLeave"
                  @drop="handleDrop($event, tag, 'before')"
                ></div>

                <!-- 内部放置区域 -->
                <div
                  class="tag-inner-area"
                  :class="{ 'is-drag-target': dragOverTag?.id === tag.id && dragOverPosition === 'inside' }"
                  @dragover.prevent="handleDragOver($event, tag, 'inside')"
                  @dragleave="handleDragLeave"
                  @drop="handleDrop($event, tag, 'inside')"
                >
                  <!-- 缩进占位 -->
                  <div class="indent-spacer" :style="{ width: `${tag.level * 24}px` }"></div>

                  <!-- 展开/折叠按钮 -->
                  <div
                    v-if="tag.children?.length"
                    class="expand-btn"
                    @click.stop="toggleExpand(tag)"
                  >
                    <el-icon><FolderOpened v-if="expandedKeys.includes(tag.id)" /><Folder v-else /></el-icon>
                  </div>
                  <div v-else class="expand-placeholder"></div>

                  <!-- 标签内容 -->
                  <div class="tag-content" @click="onSelectTag(tag)" title="点击查看标签关系">
                    <div class="tag-indicator" :style="{ backgroundColor: tag.color }"></div>
                    <span class="tag-name">{{ tag.name }}</span>
                    <span v-if="tag.children?.length" class="tag-count">
                      ({{ tag.children.length }})
                    </span>
                    <el-icon class="relation-hint" :size="14"><ArrowRight /></el-icon>
                  </div>

                  <!-- 拖拽手柄（仅可拖拽标签显示） -->
                  <el-icon
                    v-if="isDraggable(tag)"
                    class="drag-handle"
                    :size="14"
                    title="拖拽移动标签"
                  >
                    <Rank />
                  </el-icon>

                  <!-- 操作按钮 -->
                  <div class="tag-actions">
                    <el-button
                      text
                      size="small"
                      :icon="Plus"
                      @click.stop="openCreateTag(tag.id)"
                      title="添加子标签"
                    />
                    <el-button
                      text
                      size="small"
                      :icon="Edit"
                      @click.stop="openRenameTag(tag)"
                      title="重命名"
                    />
                    <el-button
                      text
                      size="small"
                      type="danger"
                      :icon="Delete"
                      @click.stop="doDeleteTag(tag)"
                      title="删除"
                    />
                  </div>
                </div>

                <!-- 拖拽指示器 - 后 -->
                <div
                  v-if="!tag.isSystemTag"
                  class="drop-indicator drop-after"
                  :class="{ 'is-active': dragOverTag?.id === tag.id && dragOverPosition === 'after' }"
                  @dragover.prevent="handleDragOver($event, tag, 'after')"
                  @dragleave="handleDragLeave"
                  @drop="handleDrop($event, tag, 'after')"
                ></div>
              </div>
            </div>
          </CustomScroll>
        </div>
      </div>
    </div>

    <!-- 拖拽浮动放置面板 -->
    <Teleport to="body">
      <div
        v-if="draggingTag"
        class="drag-float-panel"
        :class="{ 'is-active': dragOverFloat }"
      >
        <div class="float-panel-content">
          <el-icon :size="24"><Rank /></el-icon>
          <span class="panel-title">拖放到此处</span>
          <span class="panel-desc">成为顶级标签</span>
        </div>
        <div
          class="float-drop-zone"
          @dragover.prevent="handleFloatDragOver"
          @dragleave="handleFloatDragLeave"
          @drop="handleFloatDrop"
        ></div>
      </div>
    </Teleport>

    <!-- 标签关系抽屉 -->
    <TagRelationDrawer
      v-model="drawerVisible"
      :tag="selectedTag"
      @refresh="loadTagTree"
    />

    <!-- 推荐标签抽屉 -->
    <RecommendTagDrawer
      v-model="recommendDrawerVisible"
      @refresh="loadTagTree"
    />

    <!-- 创建标签对话框 -->
    <el-dialog
      v-model="createDialog"
      :title="createForm.parentId ? '创建子标签' : '创建标签'"
      width="420px"
      align-center
    >
      <el-form label-width="80px">
        <el-form-item label="所属维度">
          <el-input :model-value="currentDimension?.name" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item>
          <div style="flex:1"></div>
          <el-button @click="createDialog = false">取消</el-button>
          <el-button type="primary" @click="createTag">创建</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialog" title="重命名标签" width="380px" align-center>
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="renameForm.name" placeholder="请输入新名称" />
        </el-form-item>
        <el-form-item>
          <div style="flex:1"></div>
          <el-button @click="renameDialog = false">取消</el-button>
          <el-button type="primary" @click="doRenameTag">保存</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<style scoped>
.tag-management-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}

/* ========== 主内容面板 ========== */
.left-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

/* ========== 维度区域 ========== */
.dimension-section {
  flex-shrink: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-icon {
  font-size: 18px;
  color: var(--accent-primary);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drag-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-left: 12px;
  padding: 4px 10px;
  background: var(--glass-surface);
  border-radius: 12px;
  border: 1px solid var(--glass-border);
}

.dimension-loading {
  padding: 20px;
}

.dimension-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.dimension-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: var(--glass-surface);
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 160px;
}

.dimension-card:hover {
  transform: translateY(-2px);
  background: var(--glass-surface-hover);
}

.dimension-card.is-active {
  background: rgba(204, 102, 51, 0.1);
  box-shadow: 0 4px 16px rgba(204, 102, 51, 0.15);
}

.dimension-indicator {
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 40%;
  height: 3px;
  background: var(--dim-color, #909399);
  border-radius: 2px;
}

.dimension-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.dimension-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dimension-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.dimension-desc {
  font-size: 12px;
  color: var(--text-secondary);
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== 标签区域 ========== */
.tag-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tag-tree-container {
  flex: 1;
  min-height: 0;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background-color: var(--glass-surface);
  overflow: hidden;
}

.loading-wrapper,
.empty-wrapper {
  padding: 40px 20px;
}

.tag-tree {
  padding: 8px 0;
}

.tag-item {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.tag-item:hover {
  background-color: var(--glass-surface-hover);
}

.tag-item.is-selected {
  background-color: rgba(204, 102, 51, 0.1);
  border-left-color: var(--tag-color);
}

.indent-spacer {
  flex-shrink: 0;
}

.expand-btn,
.expand-placeholder {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.expand-btn {
  cursor: pointer;
  border-radius: 4px;
  color: var(--text-secondary);
}

.expand-btn:hover {
  background-color: var(--glass-surface-hover);
  color: var(--text-primary);
}

.tag-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  margin-left: 4px;
}

.tag-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tag-name {
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-count {
  font-size: 12px;
  color: var(--text-secondary);
}

.relation-hint {
  margin-left: auto;
  color: var(--text-secondary);
  opacity: 0;
  transition: all 0.2s ease;
}

.tag-item:hover .relation-hint {
  opacity: 1;
  color: var(--accent-primary);
}

.tag-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tag-item:hover .tag-actions {
  opacity: 1;
}

/* 层级样式 */
.level-0 .tag-name {
  font-weight: 600;
  font-size: 15px;
}

.level-1 .tag-name {
  color: var(--text-secondary);
}

.level-2 .tag-name {
  color: var(--text-tertiary);
  font-size: 13px;
}

/* ========== 拖拽样式 ========== */
.tag-item {
  position: relative;
}

.tag-item.is-dragging {
  opacity: 0.5;
}

.tag-item.is-system {
  opacity: 0.8;
}

.tag-item.is-draggable {
  cursor: grab;
}

.tag-item.is-draggable:active {
  cursor: grabbing;
}

.tag-inner-area {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 10px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.tag-inner-area.is-drag-target {
  background-color: rgba(204, 102, 51, 0.2);
  box-shadow: inset 0 0 0 2px var(--accent-primary);
}

.drag-handle {
  color: var(--text-tertiary);
  margin-right: 8px;
  cursor: grab;
  opacity: 0;
  transition: opacity 0.2s;
}

.tag-item:hover .drag-handle {
  opacity: 0.6;
}

.drag-handle:hover {
  opacity: 1 !important;
  color: var(--accent-primary);
}

/* 放置指示器 */
.drop-indicator {
  height: 2px;
  position: absolute;
  left: 0;
  right: 0;
  z-index: 10;
  pointer-events: all;
  transition: all 0.15s ease;
}

.drop-before {
  top: -1px;
}

.drop-after {
  bottom: -1px;
}

.drop-indicator.is-active {
  height: 3px;
  background-color: var(--accent-primary);
  box-shadow: 0 0 6px var(--accent-glow);
}

.drop-indicator.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: -3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--accent-primary);
}

/* 响应式 */
@media (max-width: 1024px) {
  .tag-management-pane {
    padding: 12px;
  }
}

@media (max-width: 768px) {
  .tag-management-pane {
    padding: 8px;
  }

  .dimension-list {
    gap: 8px;
  }

  .dimension-card {
    min-width: 140px;
    padding: 12px 14px;
  }

  .tag-actions {
    opacity: 1;
  }
}

/* ========== 浮动放置面板样式 ========== */
.drag-float-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;
  pointer-events: none;
}

.float-panel-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 48px;
  background: var(--glass-surface);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 2px dashed var(--glass-border);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  color: var(--text-secondary);
  transition: all 0.2s ease;
  pointer-events: auto;
}

.drag-float-panel.is-active .float-panel-content {
  border-color: var(--accent-primary);
  background: rgba(204, 102, 51, 0.15);
  color: var(--accent-primary);
  transform: scale(1.05);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
}

.panel-desc {
  font-size: 13px;
  opacity: 0.8;
}

.float-drop-zone {
  position: absolute;
  inset: 0;
  pointer-events: auto;
}
</style>
