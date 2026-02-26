<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="智能创建合集"
    width="720px"
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
            <div class="header-title">
              <el-icon class="header-icon"><Collection /></el-icon>
              <span>推荐合集</span>
            </div>
            <span class="count-badge">{{ recommendations.length }}</span>
          </div>
          <div class="sidebar-list custom-scroll">
            <div
              v-for="rec in recommendations"
              :key="rec.recommendationId"
              class="sidebar-item"
              :class="{ active: selectedRecommendation?.recommendationId === rec.recommendationId }"
              @click="selectRecommendation(rec)"
            >
              <div class="item-icon">
                <el-icon><Folder /></el-icon>
              </div>
              <div class="item-info">
                <div class="item-name">{{ rec.suggestedName }}</div>
                <div class="item-meta">
                  <span class="item-count">{{ rec.analysisCount }} 个分析</span>
                  <span v-if="rec.confidence >= 0.8" class="confidence-badge high">高匹配</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：预览和编辑 -->
        <div class="preview-area" v-if="selectedRecommendation">
          <div class="preview-header">
            <div class="header-left">
              <el-icon class="header-icon"><View /></el-icon>
              <h4>合集内容预览</h4>
            </div>
            <el-checkbox
              v-model="previewSelectAll"
              :indeterminate="previewIsIndeterminate"
              @change="handlePreviewSelectAllChange"
            >
              全选
            </el-checkbox>
          </div>

          <!-- 分析结果列表 -->
          <div class="preview-list custom-scroll">
            <el-checkbox-group v-model="previewSelectedIds">
              <div
                v-for="item in selectedRecommendation.analysisResults"
                :key="item.id"
                class="preview-item"
                :class="{ selected: previewSelectedIds.includes(item.id) }"
              >
                <el-checkbox :label="item.id">
                  <div class="item-content">
                    <div class="item-title">{{ item.problemStatement }}</div>
                    <div class="item-date">
                      <el-icon><Clock /></el-icon>
                      {{ item.createdTime }}
                    </div>
                  </div>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>

          <!-- 编辑区域 -->
          <div class="edit-area">
            <div class="edit-header">
              <el-icon><EditPen /></el-icon>
              <span>编辑合集信息</span>
            </div>
            <el-form label-position="top" size="default">
              <el-form-item label="合集名称">
                <el-input
                  v-model="createForm.name"
                  placeholder="输入合集名称"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="描述（可选）">
                <el-input
                  v-model="createForm.description"
                  type="textarea"
                  :rows="2"
                  placeholder="输入合集描述"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 未选择推荐时的占位 -->
        <div v-else class="preview-placeholder">
          <div class="placeholder-content">
            <el-icon class="placeholder-icon"><Collection /></el-icon>
            <p>请从左侧选择一个推荐</p>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" class="cancel-btn">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canCreate"
          :loading="creating"
          @click="handleCreate"
          class="create-btn"
        >
          <el-icon v-if="!creating"><Check /></el-icon>
          <span>创建合集</span>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { collectionApi } from '../../api'
import {
  Collection,
  Folder,
  View,
  Clock,
  EditPen,
  Check
} from '@element-plus/icons-vue'

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
// 动画曲线
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
$ease-smooth: cubic-bezier(0.25, 0.8, 0.5, 1);

.quick-create-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px) saturate(180%);
    box-shadow:
      0 25px 50px -12px rgba(0, 0, 0, 0.25),
      0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  }

  :deep(.el-dialog__header) {
    padding: 20px 24px;
    margin: 0;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    background: linear-gradient(135deg, rgba(var(--el-color-primary-rgb), 0.05) 0%, transparent 100%);
    border-radius: 24px 24px 0 0;
  }

  :deep(.el-dialog__title) {
    font-size: 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    letter-spacing: -0.02em;
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    background: rgba(var(--el-color-primary-rgb), 0.02);
    border-radius: 0 0 24px 24px;
  }
}

.dialog-content {
  min-height: 480px;
}

.loading-area,
.empty-area {
  padding: 48px 32px;
}

.empty-area {
  :deep(.el-empty__description) {
    margin-top: 16px;
  }

  p {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-top: 8px;
    text-align: center;
  }
}

.selection-area {
  display: flex;
  height: 520px;
}

// 左侧边栏
.recommendation-sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  background: rgba(var(--el-color-primary-rgb), 0.03);
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 0 0 0 24px;
}

.sidebar-header {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.5);

  .header-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);

    .header-icon {
      font-size: 18px;
      color: var(--el-color-primary);
    }
  }
}

.count-badge {
  background: linear-gradient(135deg, var(--el-color-primary) 0%, var(--el-color-primary-light-3) 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(var(--el-color-primary-rgb), 0.3);
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.sidebar-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s $ease-smooth;
  margin-bottom: 4px;
  border: 1px solid transparent;

  .item-icon {
    width: 36px;
    height: 36px;
    border-radius: 12px;
    background: var(--el-fill-color);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-secondary);
    transition: all 0.3s $ease-smooth;
    flex-shrink: 0;

    .el-icon {
      font-size: 18px;
    }
  }

  .item-info {
    flex: 1;
    min-width: 0;
  }

  .item-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.2s ease;
  }

  .item-meta {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .item-count {
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  .confidence-badge {
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 6px;
    font-weight: 500;

    &.high {
      background: rgba(103, 194, 58, 0.15);
      color: #67c23a;
    }
  }

  &:hover {
    background: rgba(var(--el-color-primary-rgb), 0.08);
    transform: translateX(4px);

    .item-icon {
      background: rgba(var(--el-color-primary-rgb), 0.15);
      color: var(--el-color-primary);
    }
  }

  &.active {
    background: rgba(var(--el-color-primary-rgb), 0.12);
    border-color: rgba(var(--el-color-primary-rgb), 0.2);
    box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb), 0.1);

    .item-icon {
      background: var(--el-color-primary);
      color: #fff;
      box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
    }

    .item-name {
      color: var(--el-color-primary);
      font-weight: 600;
    }
  }
}

// 右侧预览区域
.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-icon {
      font-size: 18px;
      color: var(--el-color-primary);
    }
  }

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0;
  }

  :deep(.el-checkbox__label) {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.preview-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: rgba(var(--el-color-primary-rgb), 0.03);
  border-radius: 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.preview-item {
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 8px;
  transition: all 0.25s $ease-smooth;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.6);

  &:last-child {
    margin-bottom: 0;
  }

  &.selected {
    background: rgba(var(--el-color-primary-rgb), 0.08);
    border-color: rgba(var(--el-color-primary-rgb), 0.15);
  }

  &:hover {
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transform: translateY(-1px);
  }

  :deep(.el-checkbox) {
    align-items: flex-start;
    width: 100%;
  }

  :deep(.el-checkbox__label) {
    padding-left: 10px;
    flex: 1;
    min-width: 0;
  }

  .item-content {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .item-title {
    font-size: 13px;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-weight: 500;
  }

  .item-date {
    font-size: 11px;
    color: var(--el-text-color-placeholder);
    display: flex;
    align-items: center;
    gap: 4px;

    .el-icon {
      font-size: 12px;
    }
  }
}

// 编辑区域
.edit-area {
  flex-shrink: 0;
  padding: 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);

  .edit-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);

    .el-icon {
      font-size: 16px;
      color: var(--el-color-primary);
    }
  }

  :deep(.el-form-item) {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(.el-form-item__label) {
    font-size: 12px;
    font-weight: 500;
    color: var(--el-text-color-secondary);
    padding-bottom: 6px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    transition: all 0.2s ease;

    &:hover {
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
    }

    &:focus {
      box-shadow: 0 0 0 2px rgba(var(--el-color-primary-rgb), 0.15);
    }
  }

  :deep(.el-textarea__inner) {
    border-radius: 12px;
    resize: none;
  }
}

// 占位区域
.preview-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  .placeholder-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    color: var(--el-text-color-placeholder);

    .placeholder-icon {
      font-size: 48px;
      opacity: 0.4;
    }

    p {
      font-size: 14px;
      margin: 0;
    }
  }
}

// 底部按钮
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .cancel-btn {
    border-radius: 12px;
    padding: 10px 20px;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.05);
      transform: translateY(-1px);
    }
  }

  .create-btn {
    border-radius: 12px;
    padding: 10px 24px;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.3s $ease-spring;
    background: linear-gradient(135deg, var(--el-color-primary) 0%, var(--el-color-primary-light-3) 100%);
    border: none;
    box-shadow: 0 4px 14px rgba(var(--el-color-primary-rgb), 0.35);
    color: #ffffff !important;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(var(--el-color-primary-rgb), 0.45);
      color: #ffffff !important;
    }

    &:active:not(:disabled) {
      transform: translateY(0);
      color: #ffffff !important;
    }

    &:disabled {
      opacity: 0.6;
      box-shadow: none;
      color: rgba(255, 255, 255, 0.7) !important;
    }

    .el-icon {
      font-size: 16px;
      color: #ffffff !important;
    }

    span {
      color: #ffffff !important;
    }
  }
}

// 自定义滚动条
.custom-scroll {
  &::-webkit-scrollbar {
    width: 5px;
    height: 5px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.15);
    border-radius: 3px;

    &:hover {
      background: rgba(0, 0, 0, 0.25);
    }
  }
}

// 深色模式适配
html.dark {
  .quick-create-dialog {
    :deep(.el-dialog) {
      background: rgba(30, 30, 30, 0.95);
      box-shadow:
        0 25px 50px -12px rgba(0, 0, 0, 0.5),
        0 0 0 1px rgba(255, 255, 255, 0.1) inset;
    }

    :deep(.el-dialog__header) {
      border-bottom-color: rgba(255, 255, 255, 0.08);
      background: linear-gradient(135deg, rgba(var(--el-color-primary-rgb), 0.1) 0%, transparent 100%);
    }

    :deep(.el-dialog__footer) {
      border-top-color: rgba(255, 255, 255, 0.08);
      background: rgba(0, 0, 0, 0.2);
    }
  }

  .recommendation-sidebar {
    background: rgba(0, 0, 0, 0.2);
    border-right-color: rgba(255, 255, 255, 0.08);
  }

  .sidebar-header {
    background: rgba(30, 30, 30, 0.5);
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .sidebar-item {
    &:hover {
      background: rgba(var(--el-color-primary-rgb), 0.12);
    }

    &.active {
      background: rgba(var(--el-color-primary-rgb), 0.18);
      border-color: rgba(var(--el-color-primary-rgb), 0.3);
    }
  }

  .preview-list {
    background: rgba(0, 0, 0, 0.2);
    border-color: rgba(255, 255, 255, 0.06);
  }

  .preview-item {
    background: rgba(30, 30, 30, 0.6);

    &.selected {
      background: rgba(var(--el-color-primary-rgb), 0.15);
      border-color: rgba(var(--el-color-primary-rgb), 0.25);
    }

    &:hover {
      background: rgba(30, 30, 30, 0.8);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }
  }

  .edit-area {
    background: rgba(30, 30, 30, 0.6);
    border-color: rgba(255, 255, 255, 0.08);
  }

  .custom-scroll {
    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.15);

      &:hover {
        background: rgba(255, 255, 255, 0.25);
      }
    }
  }
}
</style>
