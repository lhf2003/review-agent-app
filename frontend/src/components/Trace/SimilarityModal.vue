<script setup>
import { ref } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

function handleClose() {
  emit('update:visible', false)
}

// Mock data
const similarIssues = ref([
  { id: 1, title: 'BPS vs KB conversion confusion', similarity: '95%' },
  { id: 2, title: 'Network bandwidth units misunderstanding', similarity: '82%' },
  { id: 3, title: 'Storage vs Transmission units', similarity: '78%' }
])
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="相似问题推荐"
    width="600px"
    @close="handleClose"
  >
    <div class="similarity-list">
      <div v-for="issue in similarIssues" :key="issue.id" class="similarity-item">
        <div class="issue-main">
          <div class="issue-title">{{ issue.title }}</div>
          <div class="issue-meta">
            <el-tag size="small" type="success">相似度: {{ issue.similarity }}</el-tag>
          </div>
        </div>
        <el-button link type="primary">查看对比</el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.similarity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  margin-bottom: 12px;
}

.issue-title {
  font-weight: 500;
  margin-bottom: 4px;
}
</style>
