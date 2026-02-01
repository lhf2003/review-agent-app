<script setup>
import { ref, onMounted, computed, watch, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import SessionList from '../components/Trace/SessionList.vue'
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
const sessions = ref([])
const activeSessionIndex = ref(-1)

const showSimilarity = ref(false)
const similarIssues = ref([])

const activeSession = computed(() => {
  if (activeSessionIndex.value > -1 && sessions.value[activeSessionIndex.value]) {
    return sessions.value[activeSessionIndex.value]
  }
  return null
})

async function fetchSimilarIssues() {
  if (!activeSession.value) {
    similarIssues.value = []
    return
  }
  try {
    // Pass session ID if available
    const res = await api.getSimilarIssues(activeSession.value.analysisResultId)
    similarIssues.value = res || []
  } catch (e) {
    console.error(e)
    similarIssues.value = []
  }
}

watch(activeSession, () => {
  fetchSimilarIssues()
})

async function loadData() {
  if (!fileId) return
  try {
    loading.value = true
    const res = await api.getSessionTrace(fileId)
    // res should be SessionTraceVo: { content, analysisResultInfoList, description }
    if (res) {
      content.value = res.content || ''
      sessions.value = res.analysisResultInfoList || []
      
      // Initialize active session
      const queryActiveId = route.query.activeId
      if (queryActiveId) {
        const idx = sessions.value.findIndex(s => String(s.analysisResultId) === String(queryActiveId))
        if (idx !== -1) {
          activeSessionIndex.value = idx
        }
      }
      
      if (activeSessionIndex.value === -1) {
        const queryActive = parseInt(route.query.active)
        if (!isNaN(queryActive) && sessions.value[queryActive]) {
          activeSessionIndex.value = queryActive
        } else if (sessions.value.length > 0) {
          activeSessionIndex.value = 0
        }
      }
    }
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function handleSessionSelect(index) {
  activeSessionIndex.value = index
  router.replace({ query: { ...route.query, active: index } })
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="trace-page" v-loading="loading">
    <aside class="left-panel">
      <SessionList 
        :sessions="sessions" 
        :active-session-index="activeSessionIndex"
        @select="handleSessionSelect"
      />
    </aside>
    
    <main class="center-panel">
      <CodeViewer 
        :content="content"
        :sessions="sessions"
        :active-session-index="activeSessionIndex"
        @select-session="handleSessionSelect"
      />
    </main>
    
    <aside class="right-panel">
      <AnalysisCard
        :session="activeSession"
        :file-id="fileId"
        :similarity-count="similarIssues.length"
        @show-similarity="showSimilarity = true"
      />
    </aside>

    <SimilarityModal 
      v-model:visible="showSimilarity"
      :issues="similarIssues"
      :current-content="activeSession?.originContent || ''"
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

.left-panel,
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

:global(html.dark) .left-panel,
:global(html.dark) .center-panel,
:global(html.dark) .right-panel {
  border-color: var(--el-border-color-darker);
  box-shadow: none;
}

.left-panel {
  width: 240px;
  flex-shrink: 0;
}

@media (max-width: 1024px) {
  .left-panel {
    width: 100%;
    height: 240px;
    flex-shrink: 0;
  }
}

.center-panel {
  flex: 1;
  min-width: 0; /* Prevent flex overflow */
}

@media (max-width: 1024px) {
  .center-panel {
    height: 500px;
    flex: none;
  }
}

.right-panel {
  width: 440px;
  flex-shrink: 0;
}

@media (max-width: 1024px) {
  .right-panel {
    width: 100%;
    height: 400px;
    flex: none;
  }
}
</style>
