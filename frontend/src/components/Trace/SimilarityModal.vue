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

<style scoped>
.similarity-list {
  max-height: 400px;
  overflow-y: auto;
}

.similarity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  margin-bottom: 12px;
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
}

.compare-container {
  display: flex;
  height: 500px;
  border: 1px solid var(--el-border-color-lighter);
}

.compare-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.col-header {
  padding: 10px;
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 600;
  text-align: center;
}

.col-content {
  flex: 1;
  padding: 10px;
  overflow-y: auto;
}

.compare-divider {
  width: 1px;
  background-color: var(--el-border-color-lighter);
}
</style>
