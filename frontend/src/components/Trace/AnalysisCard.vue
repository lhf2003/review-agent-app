<script setup>
import { ref } from 'vue'

import { ElMessage } from 'element-plus'
import { UserFilled, Service, Connection, Star, Download } from '@element-plus/icons-vue'
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

const emit = defineEmits(['show-similarity', 'trigger-analysis'])
const collectionDialogRef = ref(null)
const exporting = ref(false)

async function addToCollection() {
  if (props.session && collectionDialogRef.value) {
    collectionDialogRef.value.open()
  }
}

function showSimilarity() {
  emit('show-similarity')
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
        <div class="icon-btn" :class="{ loading: exporting }" @click="exportToMarkdown" title="导出为 Markdown">
          <el-icon><Download /></el-icon>
        </div>
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
      <div class="empty-title">暂无分析结果</div>
      <div class="empty-desc">该文件尚未进行分析，请先触发分析流程</div>
      <el-button type="primary" @click="$emit('trigger-analysis')">
        开始分析
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '../../styles/nebula-theme.scss' as *;

.analysis-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-card);
  position: relative;
  overflow: hidden;
}

.chat-header {
  height: 44px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--glass-surface);
  border-bottom: 1px solid var(--glass-border);
  z-index: 10;
  flex-shrink: 0;
}

.header-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
  display: block;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 6px;
  color: var(--text-secondary);
  transition: all 0.2s;
  font-size: 16px;
}

.icon-btn:hover {
  background-color: var(--glass-surface-hover);
  color: var(--text-primary);
}

.icon-btn.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.header-subtitle {
  font-size: 11px;
  color: var(--text-secondary);
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
  color: var(--text-tertiary);
  margin-top: 6px;
  margin-left: 4px;
  margin-right: 4px;
}

.chat-padding-bottom {
  height: 20px;
}

.chat-footer {
  padding: 16px 20px;
  background: var(--glass-surface);
  border-top: 1px solid var(--glass-border);
  z-index: 10;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  border-radius: var(--radius-md) !important;
  height: 40px !important;
  display: flex !important;
  justify-content: center;
  align-items: center;
  border: 1px solid var(--glass-border) !important;
  background: var(--glass-surface) !important;
  color: var(--text-primary) !important;
  font-weight: 500 !important;
  transition: all 0.2s !important;
}

.action-btn:hover {
  background: var(--glass-surface-hover) !important;
  border-color: var(--glass-border-hover) !important;
  transform: translateY(-1px);
}

.action-btn.primary {
  background: var(--accent-primary) !important;
  color: #fff !important;
  border-color: var(--accent-primary) !important;
}

.action-btn.primary:hover {
  background: var(--accent-secondary) !important;
}


.empty-state {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--text-tertiary);
}

.empty-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-icon {
  font-size: 64px;
  color: var(--glass-border);
  margin-bottom: 8px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  text-align: center;
  max-width: 280px;
  line-height: 1.5;
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: var(--glass-border);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: var(--glass-border-hover);
}
</style>


