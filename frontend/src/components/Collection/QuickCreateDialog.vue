<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="智能创建合集"
    width="700px"
    align-center
    class="quick-create-dialog"
    @close="handleClose"
  >
    <div class="dialog-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-area">
        <el-skeleton :rows="4" animated />
      </div>

      <!-- 空状态 -->
      <div v-else-if="recommendations.length === 0" class="empty-area">
        <el-empty description="暂无推荐">
          <template #description>
            <p>有更多未归档的分析结果时，会自动推荐创建合集</p>
          </template>
        </el-empty>
      </div>

      <!-- 推荐选择区域 -->
      <div v-else class="selection-area">
        <!-- 左侧：推荐列表 -->
        <div class="recommendation-sidebar">
          <div class="sidebar-header">
            <span>推荐合集</span>
            <span class="count-badge">{{ recommendations.length }}</span>
          </div>
          <div class="sidebar-list">
            <div
              v-for="rec in recommendations"
              :key="rec.recommendationId"
              class="sidebar-item"
              :class="{ active: selectedRecommendation?.recommendationId === rec.recommendationId }"
              @click="selectRecommendation(rec)"
            >
              <div class="item-name">{{ rec.suggestedName }}</div>
              <div class="item-count">{{ rec.analysisCount }} 个分析</div>
            </div>
          </div>
        </div>

        <!-- 右侧：预览和编辑 -->
        <div class="preview-area" v-if="selectedRecommendation">
          <div class="preview-header">
            <h4>合集内容预览</h4>
            <el-checkbox
              v-model="previewSelectAll"
              :indeterminate="previewIsIndeterminate"
              @change="handlePreviewSelectAllChange"
            >
              全选
            </el-checkbox>
          </div>

          <!-- 分析结果列表 -->
          <div class="preview-list">
            <el-checkbox-group v-model="previewSelectedIds">
              <div
                v-for="item in selectedRecommendation.analysisResults"
                :key="item.id"
                class="preview-item"
              >
                <el-checkbox :label="item.id">
                  <div class="item-content">
                    <div class="item-title">{{ item.problemStatement }}</div>
                    <div class="item-date">{{ item.createdTime }}</div>
                  </div>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>

          <!-- 编辑区域 -->
          <div class="edit-area">
            <el-form label-position="top" size="small">
              <el-form-item label="合集名称">
                <el-input v-model="createForm.name" placeholder="输入合集名称" />
              </el-form-item>
              <el-form-item label="描述（可选）">
                <el-input
                  v-model="createForm.description"
                  type="textarea"
                  :rows="2"
                  placeholder="输入合集描述"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 未选择推荐时的占位 -->
        <div v-else class="preview-placeholder">
          <el-empty description="请从左侧选择一个推荐" :image-size="80" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canCreate"
          :loading="creating"
          @click="handleCreate"
        >
          创建合集
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { collectionApi } from '../../api'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'created'])

const loading = ref(false)
const creating = ref(false)
const recommendations = ref([])
const selectedRecommendation = ref(null)
const previewSelectedIds = ref([])
const previewSelectAll = ref(true)
const createForm = ref({
  name: '',
  description: ''
})

// 是否部分选中
const previewIsIndeterminate = computed(() => {
  if (!selectedRecommendation.value) return false
  const len = previewSelectedIds.value.length
  const total = selectedRecommendation.value.analysisResults?.length || 0
  return len > 0 && len < total
})

// 是否可以创建
const canCreate = computed(() => {
  return selectedRecommendation.value && previewSelectedIds.value.length > 0 && createForm.value.name
})

// 监听对话框打开
watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadRecommendations()
  }
})

// 监听选中推荐变化
watch(selectedRecommendation, (newVal) => {
  if (newVal) {
    previewSelectedIds.value = newVal.analysisResults?.map(item => item.id) || []
    createForm.value.name = newVal.suggestedName || ''
    createForm.value.description = newVal.suggestedDescription || ''
    previewSelectAll.value = true
  }
})

// 监听预览选择变化
watch(previewSelectedIds, (newVal) => {
  if (selectedRecommendation.value) {
    const total = selectedRecommendation.value.analysisResults?.length || 0
    previewSelectAll.value = newVal.length === total
  }
})

// 加载推荐数据
async function loadRecommendations() {
  loading.value = true
  try {
    const data = await collectionApi.getSmartRecommendations()
    if (data) {
      recommendations.value = data.recommendations || []
      // 默认选中第一个
      if (recommendations.value.length > 0) {
        selectRecommendation(recommendations.value[0])
      }
    }
  } catch (e) {
    console.error('加载推荐失败:', e)
  } finally {
    loading.value = false
  }
}

// 选择推荐
function selectRecommendation(rec) {
  selectedRecommendation.value = rec
}

// 预览全选/取消全选
function handlePreviewSelectAllChange(val) {
  if (val && selectedRecommendation.value) {
    previewSelectedIds.value = selectedRecommendation.value.analysisResults?.map(item => item.id) || []
  } else {
    previewSelectedIds.value = []
  }
}

// 创建合集
async function handleCreate() {
  if (!canCreate.value) return

  creating.value = true
  try {
    const result = await collectionApi.quickCreateCollection({
      name: createForm.value.name,
      description: createForm.value.description,
      analysisIds: [...previewSelectedIds.value]
    })
    ElMessage.success(`成功创建合集，包含 ${result.createdCount} 个分析结果`)
    emit('created', result)
    handleClose()
  } catch (e) {
    ElMessage.error('创建合集失败: ' + e.message)
  } finally {
    creating.value = false
  }
}

// 关闭对话框
function handleClose() {
  emit('update:visible', false)
  selectedRecommendation.value = null
  previewSelectedIds.value = []
  createForm.value = { name: '', description: '' }
}
</script>

<style scoped lang="scss">
.quick-create-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.dialog-content {
  min-height: 400px;
}

.loading-area,
.empty-area {
  padding: 40px;
}

.empty-area p {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  margin-top: 8px;
}

.selection-area {
  display: flex;
  height: 450px;
}

.recommendation-sidebar {
  width: 200px;
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
}

html.dark .recommendation-sidebar {
  border-right-color: rgba(255, 255, 255, 0.08);
}

.sidebar-header {
  padding: 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  gap: 8px;
}

html.dark .sidebar-header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.count-badge {
  background: var(--el-color-primary);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
}

.sidebar-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background-color 0.2s;
}

html.dark .sidebar-item {
  border-bottom-color: rgba(255, 255, 255, 0.05);
}

.sidebar-item:hover {
  background: var(--el-fill-color-light);
}

html.dark .sidebar-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.sidebar-item.active {
  background: var(--el-color-primary-light-9);
  border-left: 3px solid var(--el-color-primary);
}

html.dark .sidebar-item.active {
  background: rgba(64, 158, 255, 0.15);
}

.item-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.item-count {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.preview-header h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.preview-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;
  margin-bottom: 16px;
}

html.dark .preview-list {
  background: rgba(0, 0, 0, 0.2);
}

.preview-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

html.dark .preview-item {
  border-bottom-color: rgba(255, 255, 255, 0.05);
}

.preview-item:last-child {
  border-bottom: none;
}

.preview-item .item-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preview-item .item-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-item .item-date {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.edit-area {
  flex-shrink: 0;
}

.preview-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 覆盖 checkbox 样式 */
:deep(.el-checkbox) {
  align-items: flex-start;
}

:deep(.el-checkbox__label) {
  padding-left: 8px;
}
</style>
