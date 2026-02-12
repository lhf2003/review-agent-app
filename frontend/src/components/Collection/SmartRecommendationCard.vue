<template>
  <div class="smart-recommendation-card" :class="{ expanded: isExpanded }">
    <!-- 卡片头部 -->
    <div class="card-header" @click="toggleExpand">
      <div class="header-left">
        <div class="card-icon">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-title">{{ recommendation.suggestedName }}</div>
          <div class="card-meta">
            <span>{{ recommendation.analysisCount }} 个相关分析</span>
            <span class="confidence" v-if="recommendation.confidence">
              推荐度 {{ (recommendation.confidence * 100).toFixed(0) }}%
            </span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-icon class="expand-icon" :class="{ rotated: isExpanded }">
          <ArrowDown />
        </el-icon>
      </div>
    </div>

    <!-- 展开内容 -->
    <el-collapse-transition>
      <div v-show="isExpanded" class="card-content">
        <div class="content-header">
          <span class="label">将包含的分析结果</span>
          <el-checkbox
            v-model="selectAll"
            :indeterminate="isIndeterminate"
            @change="handleSelectAllChange"
          >
            全选
          </el-checkbox>
        </div>

        <div class="analysis-list">
          <el-checkbox-group v-model="selectedIds">
            <div
              v-for="item in recommendation.analysisResults"
              :key="item.id"
              class="analysis-item"
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

        <!-- 编辑合集名称和描述 -->
        <div class="edit-section">
          <el-form label-position="top" size="small">
            <el-form-item label="合集名称">
              <el-input v-model="editForm.name" placeholder="输入合集名称" />
            </el-form-item>
            <el-form-item label="描述（可选）">
              <el-input
                v-model="editForm.description"
                type="textarea"
                :rows="2"
                placeholder="输入合集描述"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 操作按钮 -->
        <div class="card-actions">
          <el-button @click="handleDismiss">忽略</el-button>
          <el-button
            type="primary"
            :disabled="selectedIds.length === 0"
            :loading="creating"
            @click="handleCreate"
          >
            创建合集 ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>
    </el-collapse-transition>

    <!-- 未展开时的快捷按钮 -->
    <div v-if="!isExpanded" class="quick-actions">
      <el-button size="small" @click.stop="handleDismiss">忽略</el-button>
      <el-button size="small" type="primary" @click.stop="handleQuickCreate" :loading="creating">
        创建
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Folder, ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  recommendation: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['create', 'dismiss'])

const isExpanded = ref(false)
const creating = ref(false)
const selectedIds = ref([])
const selectAll = ref(true)
const editForm = ref({
  name: '',
  description: ''
})

// 是否部分选中
const isIndeterminate = computed(() => {
  const len = selectedIds.value.length
  const total = props.recommendation.analysisResults?.length || 0
  return len > 0 && len < total
})

// 初始化选中状态和编辑表单
watch(() => props.recommendation, (newVal) => {
  if (newVal) {
    selectedIds.value = newVal.analysisResults?.map(item => item.id) || []
    editForm.value.name = newVal.suggestedName || ''
    editForm.value.description = newVal.suggestedDescription || ''
  }
}, { immediate: true })

// 监听全选状态
watch(selectedIds, (newVal) => {
  const total = props.recommendation.analysisResults?.length || 0
  selectAll.value = newVal.length === total
})

function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

function handleSelectAllChange(val) {
  if (val) {
    selectedIds.value = props.recommendation.analysisResults?.map(item => item.id) || []
  } else {
    selectedIds.value = []
  }
}

function handleDismiss() {
  emit('dismiss', props.recommendation.recommendationId)
}

async function handleCreate() {
  if (selectedIds.value.length === 0) return

  creating.value = true
  try {
    emit('create', {
      name: editForm.value.name,
      description: editForm.value.description,
      analysisIds: [...selectedIds.value]
    })
  } finally {
    creating.value = false
  }
}

async function handleQuickCreate() {
  creating.value = true
  try {
    emit('create', {
      name: editForm.value.name || props.recommendation.suggestedName,
      description: editForm.value.description || props.recommendation.suggestedDescription,
      analysisIds: props.recommendation.analysisResults?.map(item => item.id) || []
    })
  } finally {
    creating.value = false
  }
}
</script>

<style scoped lang="scss">
.smart-recommendation-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

html.dark .smart-recommendation-card {
  background: rgba(28, 28, 30, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.smart-recommendation-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

html.dark .smart-recommendation-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  width: 40px;
  height: 40px;
  background: var(--el-color-primary-light-9);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  font-size: 20px;
}

html.dark .card-icon {
  background: rgba(64, 158, 255, 0.15);
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.confidence {
  color: var(--el-color-success);
}

.expand-icon {
  color: var(--el-text-color-secondary);
  transition: transform 0.3s ease;
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.quick-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 20px 16px;
}

.card-content {
  padding: 0 20px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

html.dark .card-content {
  border-top-color: rgba(255, 255, 255, 0.08);
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 12px;
}

.content-header .label {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.analysis-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;
  margin-bottom: 16px;
}

html.dark .analysis-list {
  background: rgba(0, 0, 0, 0.2);
}

.analysis-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

html.dark .analysis-item {
  border-bottom-color: rgba(255, 255, 255, 0.05);
}

.analysis-item:last-child {
  border-bottom: none;
}

.item-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-date {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.edit-section {
  margin-bottom: 16px;
}

.card-actions {
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
