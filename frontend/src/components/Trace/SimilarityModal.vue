<script setup>
import { ref } from 'vue'
import MarkdownRenderer from '../MarkdownRenderer.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  issues: {
    type: Array,
    default: () => []
  },
  currentContent: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible'])

const compareVisible = ref(false)
const selectedIssue = ref(null)

function handleClose() {
  emit('update:visible', false)
}

function truncate(text) {
  if (!text) return ''
  return text.length > 50 ? text.slice(0, 50) + '...' : text
}

function handleCompare(issue) {
  selectedIssue.value = issue
  compareVisible.value = true
}

function closeCompare() {
  compareVisible.value = false
  selectedIssue.value = null
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="相似问题推荐"
    width="600px"
    modal-class="blur-backdrop"
    @close="handleClose"
  >
    <div class="similarity-list">
      <div v-if="issues.length === 0" style="text-align:center;color:var(--el-text-color-secondary);padding:20px;">
        暂无相似问题
      </div>
      <div v-else v-for="issue in issues" :key="issue.vectorId" class="similarity-item">
        <div class="issue-main">
          <el-tooltip 
            v-if="issue.problemStatement && issue.problemStatement.length > 50"
            :content="issue.problemStatement" 
            placement="top"
            :show-after="500"
          >
            <div class="issue-title">{{ truncate(issue.problemStatement) }}</div>
          </el-tooltip>
          <div v-else class="issue-title">{{ issue.problemStatement }}</div>
          
          <div class="issue-meta">
            <el-tag size="small" type="success">相似度: {{ issue.score }} %</el-tag>
          </div>
        </div>
        <el-button class="action-btn" link type="primary" @click="handleCompare(issue)">查看对比</el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 对比弹窗 -->
  <el-dialog
    v-model="compareVisible"
    title="内容对比"
    width="800px"
    append-to-body
    modal-class="blur-backdrop"
    @close="closeCompare"
  >
    <div class="compare-container">
      <div class="compare-col">
        <div class="col-header">当前会话内容</div>
        <div class="col-content">
          <MarkdownRenderer :content="currentContent || '暂无内容'" />
        </div>
      </div>
      <div class="compare-divider"></div>
      <div class="compare-col">
        <div class="col-header">相似结果原始内容</div>
        <div class="col-content">
           <MarkdownRenderer :content="selectedIssue?.originContent || '暂无内容'" />
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
@use '../../styles/nebula-theme.scss' as *;

.similarity-list {
  max-height: 400px;
  overflow-y: auto;
}

.similarity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  background: var(--glass-surface);
  transition: all 0.2s ease;

  &:hover {
    background: var(--glass-surface-hover);
    border-color: var(--glass-border-hover);
  }
}

.issue-main {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.action-btn {
  flex-shrink: 0;
}

.issue-title {
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.compare-container {
  display: flex;
  height: 500px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.compare-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.col-header {
  padding: 10px;
  background: var(--glass-surface);
  border-bottom: 1px solid var(--glass-border);
  font-weight: 600;
  text-align: center;
  color: var(--text-primary);
}

.col-content {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
  background: var(--bg-deep);
}

.compare-divider {
  width: 1px;
  background-color: var(--glass-border);
}

/* 背景模糊效果 */
:global(.el-overlay.blur-backdrop) {
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  background-color: rgba(26, 15, 8, 0.7);
}

/* 弹窗内容样式优化 */
:deep(.el-dialog) {
  border-radius: var(--radius-card);
  overflow: hidden;
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur-strong));
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-lg);
}

:deep(.el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
  border-bottom: 1px solid var(--glass-border);
  background: linear-gradient(135deg, rgba(204, 102, 51, 0.1) 0%, transparent 100%);
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.el-dialog__body) {
  padding: 16px 20px;
  color: var(--text-secondary);
}

:deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--glass-border);
}
</style>
