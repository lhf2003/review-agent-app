<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder, Plus, Check, Collection } from '@element-plus/icons-vue'
import { api } from '../../api/http'

const props = defineProps({
  sessionId: {
    type: [String, Number],
    required: true
  }
})

const visible = ref(false)
const loading = ref(false)
const mode = ref('select') // 'select' | 'create'
const collections = ref([])
const selectedCollectionId = ref(null)

const newCollectionForm = ref({
  name: '',
  description: ''
})

// 计算属性：当前合集名称
const selectedCollectionName = computed(() => {
  const item = collections.value.find(c => c.id === selectedCollectionId.value)
  return item?.name || ''
})

function open() {
  if (!props.sessionId) {
    ElMessage.error('无法添加：会话ID缺失')
    return
  }
  visible.value = true
  fetchCollections()
}

async function fetchCollections() {
  loading.value = true
  try {
    const res = await api.getCollectionList()
    collections.value = res.list || []
  } catch {
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  if (mode.value === 'select' && !selectedCollectionId.value) {
    ElMessage.warning('请选择一个合集')
    return
  }
  if (mode.value === 'create' && !newCollectionForm.value.name) {
    ElMessage.warning('请输入合集名称')
    return
  }

  let targetId = selectedCollectionId.value

  try {
    loading.value = true
    
    // 如果是新建模式，先创建合集
    if (mode.value === 'create') {
      const res = await api.createCollection(newCollectionForm.value)
      if (res && res.id) {
        targetId = res.id
      } else {
        throw new Error('创建合集失败：未返回有效ID')
      }
    }

    // 加入合集
    await api.addSessionToCollection({
      collectionId: targetId,
      sessionId: props.sessionId
    })

    ElMessage.success('已成功加入合集')
    visible.value = false
    // 重置表单
    newCollectionForm.value = { name: '', description: '' }
    mode.value = 'select'
    selectedCollectionId.value = null
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    title="添加到合集"
    width="440px"
    destroy-on-close
    align-center
    modal-class="blur-backdrop"
    class="add-to-collection-dialog"
  >
    <div class="dialog-body">
      <!-- 模式切换 -->
      <div class="mode-tabs">
        <div 
          class="mode-tab" 
          :class="{ active: mode === 'select' }"
          @click="mode = 'select'"
        >
          <el-icon><Folder /></el-icon>
          <span>选择已有</span>
        </div>
        <div 
          class="mode-tab" 
          :class="{ active: mode === 'create' }"
          @click="mode = 'create'"
        >
          <el-icon><Plus /></el-icon>
          <span>新建合集</span>
        </div>
      </div>

      <!-- 选择已有模式 -->
      <div v-if="mode === 'select'" class="form-section">
        <div class="section-label">选择目标合集</div>
        
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        
        <div v-else-if="collections.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Collection /></el-icon>
          <div class="empty-text">暂无合集</div>
          <div class="empty-hint">点击上方"新建合集"创建第一个合集</div>
        </div>
        
        <div v-else class="collection-list">
          <div
            v-for="item in collections"
            :key="item.id"
            class="collection-item"
            :class="{ active: selectedCollectionId === item.id }"
            @click="selectedCollectionId = item.id"
          >
            <div class="item-icon">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <div v-if="item.description" class="item-desc">{{ item.description }}</div>
            </div>
            <div v-if="selectedCollectionId === item.id" class="item-check">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 新建合集模式 -->
      <div v-else class="form-section">
        <div class="section-label">创建新合集</div>
        
        <div class="form-group">
          <label class="form-label">合集名称 <span class="required">*</span></label>
          <el-input 
            v-model="newCollectionForm.name" 
            placeholder="输入合集名称"
            maxlength="50"
            show-word-limit
          />
        </div>
        
        <div class="form-group">
          <label class="form-label">描述 <span class="optional">（可选）</span></label>
          <el-input 
            v-model="newCollectionForm.description" 
            type="textarea" 
            placeholder="简要描述这个合集的用途"
            :rows="3"
            maxlength="200"
            show-word-limit
            resize="none"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" class="cancel-btn">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleConfirm" 
          :loading="loading"
          :disabled="mode === 'select' && !selectedCollectionId"
          class="confirm-btn"
        >
          <el-icon v-if="!loading"><Check /></el-icon>
          <span>确定</span>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
// 动画曲线
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
$ease-smooth: cubic-bezier(0.25, 0.8, 0.5, 1);

/* 背景模糊效果 */
:global(.el-overlay.blur-backdrop) {
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  background-color: rgba(0, 0, 0, 0.4);
}

.add-to-collection-dialog {
  :deep(.el-dialog) {
    border-radius: 20px;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px) saturate(180%);
    box-shadow:
      0 25px 50px -12px rgba(0, 0, 0, 0.25),
      0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  }

  :deep(.el-dialog__header) {
    padding: 20px 24px 16px;
    margin: 0;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    background: linear-gradient(135deg, rgba(var(--el-color-primary-rgb), 0.05) 0%, transparent 100%);
  }

  :deep(.el-dialog__title) {
    font-size: 17px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    letter-spacing: -0.01em;
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px 20px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    background: rgba(var(--el-color-primary-rgb), 0.02);
  }
}

.dialog-body {
  padding: 20px 24px;
}

// 模式切换标签
.mode-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 4px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
}

.mode-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  transition: all 0.25s $ease-smooth;
  border: 1px solid transparent;

  .el-icon {
    font-size: 16px;
    transition: all 0.2s ease;
  }

  &:hover {
    color: var(--el-text-color-primary);
    background: rgba(255, 255, 255, 0.5);
  }

  &.active {
    background: #fff;
    color: var(--el-color-primary);
    border-color: rgba(var(--el-color-primary-rgb), 0.2);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

    .el-icon {
      transform: scale(1.1);
    }
  }
}

// 表单区域
.form-section {
  animation: fadeIn 0.3s $ease-smooth;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}

// 加载状态
.loading-state {
  padding: 16px 0;
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: var(--el-border-color);
    margin-bottom: 12px;
  }

  .empty-text {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-secondary);
    margin-bottom: 4px;
  }

  .empty-hint {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

// 合集列表
.collection-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.15);
    border-radius: 2px;

    &:hover {
      background: rgba(0, 0, 0, 0.25);
    }
  }
}

.collection-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s $ease-smooth;
  border: 1px solid transparent;
  background: rgba(var(--el-fill-color-light), 0.5);

  .item-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: var(--el-fill-color);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-secondary);
    transition: all 0.25s $ease-smooth;
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
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .item-desc {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .item-check {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--el-color-primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: scaleIn 0.2s $ease-spring;

    .el-icon {
      font-size: 12px;
    }
  }

  &:hover {
    background: rgba(var(--el-color-primary-rgb), 0.06);
    border-color: rgba(var(--el-color-primary-rgb), 0.15);

    .item-icon {
      background: rgba(var(--el-color-primary-rgb), 0.12);
      color: var(--el-color-primary);
    }
  }

  &.active {
    background: rgba(var(--el-color-primary-rgb), 0.1);
    border-color: rgba(var(--el-color-primary-rgb), 0.25);
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

@keyframes scaleIn {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

// 表单组
.form-group {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;

  .required {
    color: var(--el-color-danger);
  }

  .optional {
    color: var(--el-text-color-placeholder);
    font-weight: 400;
  }
}

// Element Plus 组件样式覆盖
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  }

  &:focus-within {
    box-shadow: 0 0 0 2px rgba(var(--el-color-primary-rgb), 0.15);
  }
}

:deep(.el-textarea__inner) {
  border-radius: 10px;
  resize: none;
}

:deep(.el-input__count) {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

// 底部按钮
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .cancel-btn {
    border-radius: 10px;
    padding: 9px 20px;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.05);
      transform: translateY(-1px);
    }
  }

  .confirm-btn {
    border-radius: 10px;
    padding: 9px 24px;
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
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }

    &:disabled {
      opacity: 0.6;
      box-shadow: none;
      cursor: not-allowed;
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

// 深色模式适配
html.dark {
  .add-to-collection-dialog {
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

  .mode-tabs {
    background: rgba(255, 255, 255, 0.05);
  }

  .mode-tab {
    &:hover {
      background: rgba(255, 255, 255, 0.08);
    }

    &.active {
      background: rgba(30, 30, 30, 0.8);
      border-color: rgba(var(--el-color-primary-rgb), 0.3);
    }
  }

  .collection-item {
    background: rgba(255, 255, 255, 0.03);

    &:hover {
      background: rgba(var(--el-color-primary-rgb), 0.1);
    }

    &.active {
      background: rgba(var(--el-color-primary-rgb), 0.15);
      border-color: rgba(var(--el-color-primary-rgb), 0.3);
    }
  }

  .collection-list {
    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.15);

      &:hover {
        background: rgba(255, 255, 255, 0.25);
      }
    }
  }

  .empty-state {
    .empty-icon {
      color: rgba(255, 255, 255, 0.2);
    }
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    background: rgba(255, 255, 255, 0.05);

    &:hover {
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    }
  }
}
</style>
