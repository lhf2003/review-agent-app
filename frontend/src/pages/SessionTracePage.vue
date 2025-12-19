<script setup>
import { ref, onMounted, computed } from 'vue'
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

const loading = ref(false)
const content = ref('')
const sessions = ref([])
const activeSessionIndex = ref(-1)

const showSimilarity = ref(false)

const activeSession = computed(() => {
  if (activeSessionIndex.value > -1 && sessions.value[activeSessionIndex.value]) {
    return sessions.value[activeSessionIndex.value]
  }
  return null
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
      
      // Initialize active session from query or default to 0
      const queryActive = parseInt(route.query.active)
      if (!isNaN(queryActive) && sessions.value[queryActive]) {
        activeSessionIndex.value = queryActive
      } else if (sessions.value.length > 0) {
        activeSessionIndex.value = 0
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
    <div class="left-panel">
      <SessionList 
        :sessions="sessions" 
        :active-session-index="activeSessionIndex"
        @select="handleSessionSelect"
      />
    </div>
    
    <div class="center-panel">
      <CodeViewer 
        :content="content"
        :sessions="sessions"
        :active-session-index="activeSessionIndex"
        @select-session="handleSessionSelect"
      />
    </div>
    
    <div class="right-panel">
      <AnalysisCard 
        :session="activeSession"
        @show-similarity="showSimilarity = true"
      />
    </div>

    <SimilarityModal 
      v-model:visible="showSimilarity"
    />
  </div>
</template>

<style scoped>
.trace-page {
  height: 100%;
  display: flex;
  overflow: hidden;
  background-color: var(--el-bg-color-page);
}

.left-panel {
  width: 250px; /* Approx 15% of 1920, but fixed is better for sidebar */
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.center-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.right-panel {
  width: 400px; /* Approx 30% */
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
</style>
