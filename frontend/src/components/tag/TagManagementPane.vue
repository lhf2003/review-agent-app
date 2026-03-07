<script setup>
import { ref, computed, onMounted, watch, h } from 'vue'
import { Plus, FolderOpened, Folder, Delete, Edit, Grid, Collection, Document, Cpu, Histogram, OfficeBuilding, TrendCharts, List, ScaleToOriginal, HelpFilled, Monitor, ArrowRight } from '@element-plus/icons-vue'
import { tagApi } from '../../api/tag'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../CustomScroll.vue'
import TagRelationDrawer from './TagRelationDrawer.vue'

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
  function traverse(list, level = 0, parentColor = null) {
    for (const node of list || []) {
      const color = level === 0 ? getTagColor(result.filter(r => r.level === 0).length) : parentColor
      result.push({ ...node, level, color })
      if (node.children) {
        traverse(node.children, level + 1, color)
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
                  [`level-${tag.level}`]: true
                }"
                :style="{ '--tag-color': tag.color }"
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
            </div>
          </CustomScroll>
        </div>
      </div>
    </div>

    <!-- 标签关系抽屉 -->
    <TagRelationDrawer
      v-model="drawerVisible"
      :tag="selectedTag"
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
  color: var(--el-color-primary);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex: 1;
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
  background: var(--dim-bg, #f5f5f5);
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 160px;
}

.dimension-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: var(--dim-color, #909399);
}

.dimension-card.is-active {
  border-color: var(--dim-color, #909399);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
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
  color: var(--el-text-color-primary);
}

.dimension-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
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
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background-color: var(--el-bg-color-overlay);
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
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.tag-item:hover {
  background-color: var(--el-fill-color-light);
}

.tag-item.is-selected {
  background-color: var(--el-color-primary-light-9);
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
  color: var(--el-text-color-secondary);
}

.expand-btn:hover {
  background-color: var(--el-fill-color);
  color: var(--el-text-color-primary);
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
  color: var(--el-text-color-secondary);
}

.relation-hint {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  opacity: 0;
  transition: all 0.2s ease;
}

.tag-row:hover .relation-hint {
  opacity: 1;
  color: var(--el-color-primary);
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
  color: var(--el-text-color-regular);
}

.level-2 .tag-name {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

/* 暗黑模式 */
html.dark .tag-tree-container {
  background-color: rgba(28, 28, 30, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
}

html.dark .dimension-card {
  background: rgba(255, 255, 255, 0.05);
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
</style>
