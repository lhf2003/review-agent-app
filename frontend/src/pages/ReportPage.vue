<script setup>
import { ref, onMounted, computed } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import ScrollStack from '../components/ScrollStack/ScrollStack.vue'
import ScrollStackItem from '../components/ScrollStack/ScrollStackItem.vue'
import { Calendar, FullScreen, CopyDocument, Close } from '@element-plus/icons-vue'

const loading = ref(false)
const reportType = ref(1) // 1: Daily, 2: Weekly
const rawReports = ref([])
const expandedReport = ref(null) // Stores the currently expanded report object
const isFullscreen = ref(false)

const reports = computed(() => {
  // Sort by date descending
  return [...rawReports.value].sort((a, b) => new Date(b.startDate) - new Date(a.startDate))
})

const loadReports = async () => {
  loading.value = true
  try {
    const data = await api.getReportList(reportType.value)
    rawReports.value = data || []
  } catch (e) {
    ElMessage.error('Failed to load reports')
  } finally {
    loading.value = false
  }
}

const handleCardClick = (report) => {
  expandedReport.value = report
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const closeExpanded = () => {
  expandedReport.value = null
  isFullscreen.value = false
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <div class="report-page">
    <div class="header-section">
      <div class="title-area">
        <h2>审查报告</h2>
        <p class="subtitle">查看每日和每周的审查结果与建议</p>
      </div>
      <div class="controls">
        <el-radio-group v-model="reportType" @change="loadReports" size="large">
          <el-radio-button :value="1">日报</el-radio-button>
          <el-radio-button :value="2">周报</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="stack-container" v-loading="loading">
      <el-empty v-if="!loading && reports.length === 0" description="暂无报告数据" class="empty-state" />
      
      <ScrollStack 
        v-else
        :itemStackDistance="60"
        stackPosition="20px"
        :itemScale="0.05"
        :blurAmount="1"
      >
        <div class="scroll-stack-wrapper">
          <div style="height: 20px;"></div>

          <ScrollStackItem 
            v-for="(report, index) in reports" 
            :key="report.id" 
            :index="index"
          >
            <div class="stack-card" @click="handleCardClick(report)">
              <div class="card-inner">
                <div class="report-header">
                  <div class="date-badge">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ report.startDate }}</span>
                    <span v-if="report.startDate !== report.endDate"> - {{ report.endDate }}</span>
                  </div>
                  <div class="report-type-tag">
                    {{ report.type === 1 ? '日报' : '周报' }}
                  </div>
                </div>
                
                <div class="preview-content">
                  <div class="report-content markdown-body" v-html="report.reportContent"></div>
                  <div class="read-more-overlay">
                    <span>点击查看详情</span>
                  </div>
                </div>

                <div class="card-footer">
                  <span>生成时间: {{ report.createTime }}</span>
                </div>
              </div>
            </div>
          </ScrollStackItem>

          <div class="stack-hint bottom-hint" v-if="!loading && reports.length > 0">
            已经到底了!
          </div>
        </div>
      </ScrollStack>
    </div>

    <el-dialog
      v-model="expandedReport"
      :fullscreen="isFullscreen"
      :show-close="false"
      width="800px"
      align-center
      class="report-dialog"
      @close="closeExpanded"
    >
      <template #header="{ close, titleId, titleClass }">
        <div class="custom-header">
           <span :id="titleId" :class="titleClass">
             {{ expandedReport ? (expandedReport.type === 1 ? '日报详情' : '周报详情') : '' }}
           </span>
           <div class="header-controls">
             <el-icon class="header-icon" @click="toggleFullscreen">
               <FullScreen v-if="!isFullscreen" />
               <CopyDocument v-else />
             </el-icon>
             <el-icon class="header-icon" @click="close">
               <Close />
             </el-icon>
           </div>
        </div>
      </template>
      <template v-if="expandedReport">
         <div class="dialog-header-info">
            <div class="date-badge large">
              <el-icon><Calendar /></el-icon>
              <span>{{ expandedReport.startDate }}</span>
              <span v-if="expandedReport.startDate !== expandedReport.endDate"> - {{ expandedReport.endDate }}</span>
            </div>
         </div>
         <div class="dialog-content markdown-body" :class="{ 'is-full': isFullscreen }" v-html="expandedReport.reportContent"></div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.report-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-deep);
  overflow: hidden;
}

/* Header is now static and flex-shrink 0 */
.header-section {
  flex-shrink: 0;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 30px 20px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  box-sizing: border-box;
  background-color: var(--bg-deep);
  z-index: 10;
}

.title-area h2 {
  margin: 0;
  font-size: 28px;
  color: var(--text-primary);
}

.subtitle {
  margin: 5px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
}

/* Container for ScrollStack takes remaining height */
.stack-container {
  flex: 1;
  width: 100%;
  position: relative;
  overflow: hidden; /* ScrollStack handles internal scrolling */
}

/* Adjust wrapper inside ScrollStack */
.scroll-stack-wrapper {
  max-width: 700px;
  margin: 0 auto;
  /* padding-bottom: 50px; Removed to prevent overscroll */
  position: relative;
}

.empty-state {
  margin-top: 100px;
}

.stack-card {
  background: var(--glass-surface);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--glass-border);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  cursor: pointer;

  height: 70vh;
  min-height: 400px;
  display: flex;
  flex-direction: column;

  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  transform: translateZ(0);
  outline: 1px solid transparent;
}

.stack-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-5px) scale(1.02);
  border-color: var(--glass-border-hover);
}

.card-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 30px;
  background: transparent;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--glass-border);
  flex-shrink: 0;
}

.date-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--accent-primary);
  background: rgba(204, 102, 51, 0.1);
  padding: 6px 12px;
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

.report-type-tag {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
  background: rgba(255, 248, 245, 0.05);
  padding: 4px 10px;
  border-radius: 4px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden; 
}

.report-content {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-secondary);
}

/* Gradient overlay */
.read-more-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 140px;
  background: linear-gradient(to bottom, transparent, var(--bg-deep) 80%);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 20px;
  color: var(--accent-primary);
  font-weight: 600;
  font-size: 14px;
  pointer-events: none;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.card-footer {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed var(--glass-border);
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
  flex-shrink: 0;
}

/* Hints */
.stack-hint {
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  opacity: 0.5;
  padding: 30px 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.stack-hint.bottom-hint {
  margin-bottom: 20px;
}

/* Dialog Styles (Unchanged mostly) */
.dialog-header-info {
  display: flex;
  justify-content: center;
}

.dialog-content {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  max-height: 60vh;
  overflow-y: auto;
  padding: 0 10px;
}

.dialog-content.is-full {
  max-height: calc(100vh - 140px);
}

.custom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 0; 
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: color 0.2s;
  display: flex;
  align-items: center;
}

.header-icon:hover {
  color: var(--accent-primary);
}

/* Markdown Override */
:deep(.markdown-body) {
  font-size: 15px;
}
:deep(.markdown-body h1), :deep(.markdown-body h2) {
  border-bottom: none;
  margin-bottom: 0.5em;
}

/* Responsive */
@media (max-width: 768px) {
  .header-section {
    padding: 20px 16px 16px;
  }
}
</style>