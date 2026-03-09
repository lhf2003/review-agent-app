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
          <!-- 合集内容预览 -->
          <div class="preview-section">
            <div class="section-header">
              <div class="header-title">
                <el-icon><View /></el-icon>
                <span>合集内容</span>
                <span class="count">{{ previewSelectedIds.length }}/{{ selectedRecommendation.analysisResults?.length }}</span>
              </div>
              <el-checkbox
                v-model="previewSelectAll"
                :indeterminate="previewIsIndeterminate"
                @change="handlePreviewSelectAllChange"
                size="small"
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
                      <div class="item-date">{{ item.createdTime }}</div>
                    </div>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>

          <!-- 编辑区域 -->
          <div class="edit-section">
            <div class="section-header compact">
              <el-icon><EditPen /></el-icon>
              <span>编辑信息</span>
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>名称</label>
                <el-input
                  v-model="createForm.name"
                  placeholder="合集名称"
                  maxlength="50"
                  size="small"
                />
              </div>
              <div class="form-field">
                <label>描述 <span class="optional">可选</span></label>
                <el-input
                  v-model="createForm.description"
                  placeholder="描述"
                  maxlength="200"
                  size="small"
                />
              </div>
            </div>
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
        <el-button @click="handleClose" size="small">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canCreate"
          :loading="creating"
          @click="handleCreate"
          size="small"
        >
          <el-icon v-if="!creating"><Check /></el-icon>
          <span>创建合集 ({{ previewSelectedIds.length }})</span>
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
  } catch {
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
@use '../../styles/nebula-theme.scss' as *;

// ========================================
// Design System: Smart Create Collection
// Pattern: Lead Magnet + Form (≤3 fields)
// Style: Exaggerated Minimalism + Glassmorphism
// ========================================

// 动画曲线
$ease-out: cubic-bezier(0.16, 1, 0.3, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// 间距系统 (8px 基数)
$space-1: 4px;
$space-2: 8px;
$space-3: 12px;
$space-4: 16px;
$space-5: 20px;
$space-6: 24px;

// 对话框容器
.quick-create-dialog {
  :deep(.el-dialog) {
    border-radius: var(--radius-card);
    overflow: hidden;
    background: var(--glass-surface);
    backdrop-filter: blur(var(--glass-blur-strong)) saturate(180%);
    -webkit-backdrop-filter: blur(var(--glass-blur-strong)) saturate(180%);
    border: 1px solid var(--glass-border);
    box-shadow:
      0 25px 50px -12px rgba(0, 0, 0, 0.5),
      inset 0 1px 0 var(--glass-highlight);
  }

  // Header - 极简设计
  :deep(.el-dialog__header) {
    padding: $space-2 $space-4;
    margin: 0;
    height: 40px;
    min-height: 40px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--glass-border);
    background: linear-gradient(135deg, rgba(204, 102, 51, 0.08) 0%, transparent 100%);
  }

  :deep(.el-dialog__title) {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.01em;
  }

  :deep(.el-dialog__headerbtn) {
    width: 28px;
    height: 28px;
    top: 6px;
    right: 12px;

    .el-icon { font-size: 14px; }
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: $space-3 $space-4;
    border-top: 1px solid var(--glass-border);
  }
}

// 主内容区
.dialog-content {
  min-height: 460px;
}

.loading-area,
.empty-area {
  padding: $space-6 $space-5;
}

// 选择区域 - 双栏布局
.selection-area {
  display: flex;
  height: 480px;
}

// ========================================
// 左侧边栏 - 推荐列表
// ========================================
.recommendation-sidebar {
  width: 200px;
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.2);
  border-right: 1px solid var(--glass-border);
}

.sidebar-header {
  padding: $space-3 $space-4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--glass-border);

  .header-title {
    display: flex;
    align-items: center;
    gap: $space-2;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);

    .header-icon {
      font-size: 16px;
      color: var(--accent-tertiary);
    }
  }
}

.count-badge {
  background: var(--gradient-warm);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: $space-2;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: $space-3;
  padding: $space-3;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s $ease-out;
  margin-bottom: $space-1;
  border: 1px solid transparent;
  min-height: 56px; // 触摸目标尺寸

  .item-icon {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    background: var(--glass-surface);
    border: 1px solid var(--glass-border);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-tertiary);
    transition: all 0.2s $ease-out;
    flex-shrink: 0;

    .el-icon { font-size: 16px; }
  }

  .item-info {
    flex: 1;
    min-width: 0;
  }

  .item-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .item-meta {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .item-count {
    font-size: 11px;
    color: var(--text-secondary);
  }

  .confidence-badge {
    font-size: 10px;
    padding: 1px 5px;
    border-radius: 4px;
    font-weight: 500;

    &.high {
      background: rgba(34, 197, 94, 0.12);
      color: var(--mastery-high);
    }
  }

  &:hover {
    background: rgba(204, 102, 51, 0.08);

    .item-icon {
      background: rgba(204, 102, 51, 0.12);
      border-color: rgba(204, 102, 51, 0.2);
      color: var(--accent-tertiary);
    }
  }

  &.active {
    background: rgba(204, 102, 51, 0.12);
    border-color: rgba(204, 102, 51, 0.3);

    .item-icon {
      background: var(--accent-primary);
      border-color: var(--accent-primary);
      color: #fff;
    }

    .item-name {
      color: var(--accent-tertiary);
      font-weight: 600;
    }
  }
}

// ========================================
// 右侧预览区域
// ========================================
.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: $space-3;
  gap: $space-3;
  overflow: hidden;
}

// 预览区块
.preview-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
}

// 区块头部 - 统一样式
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $space-3 $space-4;
  border-bottom: 1px solid var(--glass-border);
  background: rgba(255, 248, 245, 0.02);

  &.compact {
    padding: $space-2 $space-3;
    gap: $space-2;
    justify-content: flex-start;

    .el-icon {
      font-size: 14px;
      color: var(--accent-tertiary);
    }

    span {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }

  .header-title {
    display: flex;
    align-items: center;
    gap: $space-2;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);

    .el-icon {
      font-size: 16px;
      color: var(--accent-tertiary);
    }

    .count {
      font-size: 11px;
      font-weight: 500;
      color: var(--text-secondary);
      background: var(--glass-surface);
      padding: 1px 8px;
      border-radius: 10px;
      border: 1px solid var(--glass-border);
      margin-left: $space-1;
    }
  }
}

// 预览列表
.preview-list {
  flex: 1;
  overflow-y: auto;
  padding: $space-2 $space-3;
}

.preview-item {
  padding: $space-2 $space-3;
  border-radius: 8px;
  margin-bottom: $space-1;
  transition: all 0.15s $ease-out;
  border: 1px solid transparent;
  background: transparent;

  &:last-child { margin-bottom: 0; }

  &.selected {
    background: rgba(204, 102, 51, 0.08);
    border-color: rgba(204, 102, 51, 0.15);
  }

  &:hover {
    background: rgba(255, 248, 245, 0.04);
  }

  :deep(.el-checkbox) {
    align-items: flex-start;
    width: 100%;
    height: auto;
    margin-right: 0;
  }

  :deep(.el-checkbox__label) {
    padding-left: $space-2;
    flex: 1;
    min-width: 0;
  }

  :deep(.el-checkbox__input) {
    margin-top: 2px;
  }

  .item-content {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .item-title {
    font-size: 13px;
    color: var(--text-primary);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .item-date {
    font-size: 11px;
    color: var(--text-tertiary);
  }
}

// ========================================
// 编辑区域 - 极简表单
// ========================================
.edit-section {
  flex-shrink: 0;
  padding: $space-2 $space-3 $space-3;
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
}

.form-row {
  display: flex;
  gap: $space-3;
}

.form-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &:first-child { flex: 0.9; }
  &:last-child { flex: 1.1; }

  label {
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    gap: 4px;

    .optional {
      font-size: 11px;
      color: var(--text-tertiary);
      font-weight: 400;
    }
  }

  :deep(.el-input__wrapper) {
    background: rgba(255, 248, 245, 0.05);
    border: 1px solid var(--glass-border);
    border-radius: 8px;
    box-shadow: none;
    transition: all 0.15s ease;
    padding: 0 10px;

    &:hover {
      background: rgba(255, 248, 245, 0.08);
      border-color: rgba(255, 248, 245, 0.12);
    }

    &.is-focus {
      border-color: var(--accent-primary);
      box-shadow: 0 0 0 2px rgba(204, 102, 51, 0.12);
    }

    .el-input__inner {
      height: 32px;
      font-size: 13px;
    }
  }
}

// ========================================
// 底部按钮
// ========================================
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: $space-3;

  :deep(.el-button) {
    border-radius: 8px;
    font-weight: 500;
    transition: all 0.2s $ease-out;

    &.el-button--primary {
      background: var(--gradient-warm);
      border: none;
      box-shadow: 0 2px 8px rgba(204, 102, 51, 0.3);

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(204, 102, 51, 0.4);
      }

      &:disabled {
        opacity: 0.5;
        box-shadow: none;
      }
    }

    &:not(.el-button--primary) {
      background: rgba(255, 248, 245, 0.05);
      border: 1px solid var(--glass-border);
      color: var(--text-secondary);

      &:hover {
        background: rgba(255, 248, 245, 0.08);
        color: var(--text-primary);
      }
    }
  }
}

// ========================================
// 占位区域
// ========================================
.preview-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  .placeholder-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-4;
    color: var(--text-tertiary);

    .placeholder-icon {
      font-size: 48px;
      opacity: 0.3;
    }

    p {
      font-size: 14px;
      margin: 0;
      color: var(--text-secondary);
    }
  }
}

// ========================================
// 自定义滚动条
// ========================================
.custom-scroll {
  &::-webkit-scrollbar {
    width: 4px;
    height: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 248, 245, 0.08);
    border-radius: 2px;

    &:hover {
      background: rgba(255, 248, 245, 0.15);
    }
  }
}
</style>
