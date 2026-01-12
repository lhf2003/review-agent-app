<script setup>
import { ref } from 'vue'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownRenderer from '../MarkdownRenderer.vue'
import AddToCollectionDialog from '../Collection/AddToCollectionDialog.vue'

const props = defineProps({
  session: {
    type: Object,
    default: null
  },
  similarityCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['show-similarity'])
const collectionDialogRef = ref(null)

function addToCollection() {
  if (props.session && collectionDialogRef.value) {
    collectionDialogRef.value.open()
  }
}

function showSimilarity() {
  emit('show-similarity')
}
</script>

<template>
  <div class="analysis-card" v-if="session">
    <AddToCollectionDialog 
      ref="collectionDialogRef" 
      :session-id="session.analysisResultId || session.id" 
    />
    
    <div class="card-header">
      <div class="card-title">分析详情</div>
      <div class="card-actions">
        <!-- Future actions -->
      </div>
    </div>
    
    <div class="card-content">
      <div class="section">
        <div class="section-title">Problem Statement</div>
        <div class="section-body">{{ session.problemStatement }}</div>
      </div>
      
      <div class="section">
        <div class="section-title">Solution</div>
        <div class="section-body">
          <MarkdownRenderer :content="session.solution" />
        </div>
      </div>
    </div>

    <div class="card-footer">
      <el-button type="primary" plain @click="addToCollection">加入合集</el-button>
      <el-button @click="showSimilarity">查看相似问题 ({{ similarityCount }})</el-button>
    </div>
  </div>
  <div class="empty-state" v-else>
    <el-empty description="请选择一个会话查看详情" />
  </div>
</template>

<style scoped>
.analysis-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
  border-left: 1px solid var(--el-border-color);
}

.card-header {
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color);
  background-color: var(--el-fill-color-light);
}

.card-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-body {
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
}

.card-footer {
  padding: 16px;
  border-top: 1px solid var(--el-border-color);
  display: flex;
  gap: 12px;
  background-color: var(--el-fill-color-light);
}

.empty-state {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border-left: 1px solid var(--el-border-color);
}
</style>
