<script setup>
import { ref, onMounted, watch, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import CodeViewer from '../components/Trace/CodeViewer.vue'
import AnalysisCard from '../components/Trace/AnalysisCard.vue'
import SimilarityModal from '../components/Trace/SimilarityModal.vue'

const router = useRouter()
const route = useRoute()
const fileId = route.params.fileId

// Provide router to child components
provide('router', router)

const loading = ref(false)
const content = ref('')
const session = ref(null)

const showSimilarity = ref(false)
const similarIssues = ref([])

async function fetchSimilarIssues() {
  if (!session.value?.analysisResultId) {
    similarIssues.value = []
    return
  }
  try {
    const res = await api.getSimilarIssues(session.value.analysisResultId)
    similarIssues.value = res || []
  } catch (e) {
    similarIssues.value = []
  }
}

watch(session, () => {
  fetchSimilarIssues()
})

async function loadData() {
  if (!fileId) return
  try {
    loading.value = true
    const res = await api.getSessionTrace(fileId)
    // res is SessionTraceVo: { content, analysisResultId, problemStatement, solution }
    if (res) {
      content.value = res.content || ''
      if (res.analysisResultId) {
        session.value = {
          analysisResultId: res.analysisResultId,
          problemStatement: res.problemStatement,
          solution: res.solution,
          originContent: res.content
        }
      } else {
        session.value = null
      }
    }
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="trace-page" v-loading="loading">
    <main class="center-panel">
      <CodeViewer 
        :content="content"
      />
    </main>
    
    <aside class="right-panel">
      <AnalysisCard
        :session="session"
        :file-id="fileId"
        :similarity-count="similarIssues.length"
        @show-similarity="showSimilarity = true"
        @trigger-analysis="router.push('/data')"
      />
    </aside>

    <SimilarityModal 
      v-model:visible="showSimilarity"
      :issues="similarIssues"
      :current-content="session?.originContent || ''"
    />
  </div>
</template>

<style scoped>
.trace-page {
  height: 100%;
  display: flex;
  overflow: hidden;
  background-color: var(--el-bg-color-page);
  gap: 16px;
  padding: 16px;
  box-sizing: border-box;
  transition: background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@media (max-width: 1024px) {
  .trace-page {
    flex-direction: column;
    padding: 12px;
    gap: 12px;
    overflow-y: auto;
  }
}

.center-panel,
.right-panel {
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--el-border-color-light);
  box-shadow: var(--el-box-shadow-light);
}

:global(html.dark) .center-panel,
:global(html.dark) .right-panel {
  border-color: var(--el-border-color-darker);
  box-shadow: none;
}

.center-panel {
  flex: 1;
  min-width: 0;
  width: 50%;
}

.right-panel {
  flex: 1;
  min-width: 0;
  width: 50%;
}

@media (max-width: 1024px) {
  .center-panel,
  .right-panel {
    width: 100%;
    height: 400px;
    flex: none;
  }

  .center-panel {
    height: 500px;
  }
}
</style>
