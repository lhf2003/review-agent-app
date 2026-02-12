<script setup>
import { ref, inject } from 'vue'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Service, Connection, Star, ArrowLeft, Download } from '@element-plus/icons-vue'
import MarkdownRenderer from '../MarkdownRenderer.vue'
import AddToCollectionDialog from '../Collection/AddToCollectionDialog.vue'
import { api } from '../../api/http'

const props = defineProps({
  session: {
    type: Object,
    default: null
  },
  fileId: {
    type: Number,
    default: null
  },
  similarityCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['show-similarity'])
const collectionDialogRef = ref(null)
const router = inject('router')
const exporting = ref(false)

async function addToCollection() {
  if (props.session && collectionDialogRef.value) {
    collectionDialogRef.value.open()
  }
}

function showSimilarity() {
  emit('show-similarity')
}

async function goToAnalysisResult() {
  if (!props.session || !props.fileId) {
    ElMessage.error('缺少必要信息，无法跳转')
    return
  }

  try {
    const resp = await api.getAnalysisResultByIds(props.fileId, props.session.analysisResultId)
    const data = resp?.data || resp

    const mainTag = data?.mainTagName

    router.push({
      path: '/analysis',
      query: {
        mainTag,
        dataId: props.fileId,
        highlightId: props.session.analysisResultId
      }
    })
  } catch (e) {
    ElMessage.error(`跳转失败: ${e.message}`)
  }
}

async function exportToMarkdown() {
  if (!props.session?.analysisResultId) {
    ElMessage.warning('无法导出：缺少分析结果ID')
    return
  }

  try {
    exporting.value = true
    const blob = await api.exportAnalysis(props.session.analysisResultId)
    const filename = `analysis_${props.session.analysisResultId}.md`
    api.downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(`导出失败: ${e.message}`)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="analysis-card" v-if="session">
    <AddToCollectionDialog 
      ref="collectionDialogRef" 
      :session-id="session.analysisResultId || session.id" 
    />
    
    <div class="chat-header">
      <div class="header-info">
        <span class="header-title">AI Analysis</span>
      </div>
      <div class="header-actions">
        <!-- Future: Export, Share, etc. -->
      </div>
    </div>
    
    <div class="chat-content custom-scrollbar">
      <!-- User Question Bubble -->
      <div class="message-group user">
        <div class="avatar-container">
          <el-avatar :size="32" :icon="UserFilled" class="user-avatar" />
        </div>
        <div class="bubble-container">
          <div class="message-bubble user-bubble">
            <div class="bubble-content">{{ session.problemStatement }}</div>
          </div>
          <div class="message-meta">Problem Statement</div>
        </div>
      </div>
      
      <!-- AI Answer Bubble -->
      <div class="message-group ai">
        <div class="avatar-container">
          <el-avatar :size="32" :icon="Service" class="ai-avatar" />
        </div>
        <div class="bubble-container">
          <div class="message-bubble ai-bubble">
            <div class="bubble-content">
              <MarkdownRenderer :content="session.solution" />
            </div>
          </div>
          <div class="message-meta">Analysis Result</div>
        </div>
      </div>
      
      <div class="chat-padding-bottom"></div>
    </div>

    <div class="chat-footer">
      <div class="footer-actions">
        <el-button class="action-btn" @click="showSimilarity">
          <el-icon><Connection /></el-icon>
          <span>相似问题 ({{ similarityCount }})</span>
        </el-button>
        <el-button class="action-btn" @click="goToAnalysisResult">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回结果</span>
        </el-button>
        <el-button class="action-btn" :loading="exporting" @click="exportToMarkdown">
          <el-icon><Download /></el-icon>
          <span>导出</span>
        </el-button>
        <el-button class="action-btn primary" @click="addToCollection">
          <el-icon><Star /></el-icon>
          <span>加入合集</span>
        </el-button>
      </div>
    </div>
  </div>
  <div class="empty-state" v-else>
    <div class="empty-content">
      <el-icon class="empty-icon"><Service /></el-icon>
      <div class="empty-text">选择一个会话以查看详细分析</div>
    </div>
  </div>
</template>

<style scoped>
.analysis-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
  position: relative;
}

.chat-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-light);
  z-index: 10;
}

.header-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary);
  display: block;
}

.header-subtitle {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  display: block;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message-group {
  display: flex;
  gap: 12px;
  max-width: 100%;
}

.message-group.user {
  flex-direction: row-reverse;
}

.avatar-container {
  flex-shrink: 0;
  margin-top: 4px;
}

.user-avatar {
  background-color: var(--el-color-primary);
  color: white;
}

.ai-avatar {
  background-color: var(--el-color-success);
  color: white;
}

.bubble-container {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}

.message-group.user .bubble-container {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: var(--el-box-shadow-light);
}

.user-bubble {
  background-color: var(--el-color-primary);
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background-color: var(--el-fill-color);
  color: var(--el-text-color-primary);
  border-bottom-left-radius: 4px;
}

.message-meta {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 6px;
  margin-left: 4px;
  margin-right: 4px;
}

.chat-padding-bottom {
  height: 20px;
}

.chat-footer {
  padding: 16px 20px;
  background-color: var(--el-bg-color-overlay);
  border-top: 1px solid var(--el-border-color-light);
  z-index: 10;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  border-radius: 8px !important;
  height: 40px !important;
  display: flex !important;
  justify-content: center;
  align-items: center;
  border: 1px solid var(--el-border-color) !important;
  background-color: var(--el-fill-color-light) !important;
  color: var(--el-text-color-primary) !important;
  font-weight: 500 !important;
  transition: all 0.2s !important;
}

.action-btn:hover {
  background-color: var(--el-fill-color) !important;
  border-color: var(--el-border-color-darker) !important;
  transform: translateY(-1px);
}

.action-btn.primary {
  background-color: var(--el-color-primary-light-9) !important;
  color: var(--el-color-primary) !important;
  border-color: var(--el-color-primary-light-5) !important;
}

.action-btn.primary:hover {
  background-color: var(--el-color-primary-light-8) !important;
}

:global(html.dark) .action-btn.primary {
  background-color: rgba(var(--el-color-primary-rgb), 0.1) !important;
  border-color: rgba(var(--el-color-primary-rgb), 0.2) !important;
}

:global(html.dark) .action-btn.primary:hover {
  background-color: rgba(var(--el-color-primary-rgb), 0.2) !important;
}

.empty-state {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--el-text-color-placeholder);
}

.empty-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-icon {
  font-size: 48px;
  color: var(--el-border-color);
}

.empty-text {
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: var(--el-text-color-secondary);
}
</style>


