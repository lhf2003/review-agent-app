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
@use '../../styles/nebula-theme.scss' as *;

.smart-recommendation-card {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: var(--radius-card);
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: var(--transition-smooth);
}

.smart-recommendation-card:hover {
  transform: translateY(-4px);
  background: var(--glass-surface-hover);
  box-shadow: var(--shadow-md);
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
  background: rgba(204, 102, 51, 0.1);
  border: 1px solid rgba(204, 102, 51, 0.2);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-tertiary);
  font-size: 20px;
}


.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.confidence {
  color: var(--mastery-high);
}

.expand-icon {
  color: var(--text-tertiary);
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
  border-top: 1px solid var(--glass-border);
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
  color: var(--text-primary);
}

.analysis-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px 12px;
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.analysis-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--glass-border);
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
  color: var(--text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-date {
  font-size: 11px;
  color: var(--text-tertiary);
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
