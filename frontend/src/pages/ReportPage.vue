<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import ScrollStack from '../components/ScrollStack/ScrollStack.vue'
import ScrollStackItem from '../components/ScrollStack/ScrollStackItem.vue'

const loading = ref(false)
const reportType = ref(1) // 1: Daily, 2: Weekly
const rawReports = ref([])
const expandedReport = ref(null) // Stores the currently expanded report object
const scrollContainer = ref(null)

const reports = computed(() => {
  // Sort by date descending
  return [...rawReports.value].sort((a, b) => new Date(b.startDate) - new Date(a.startDate))
})

const loadReports = async () => {
  loading.value = true
  try {
    const data= await api.getReportList(reportType.value)
    rawReports.value = data || []
  } catch (e) {
    ElMessage.error('Failed to load reports')
    if (rawReports.value.length === 0) {
      // Mock handled or empty
    }
  } finally {
    loading.value = false
  }
}

const handleCardClick = (report) => {
  expandedReport.value = report
}

const closeExpanded = () => {
  expandedReport.value = null
}

onMounted(() => {
  loadReports()
})
</script>

<template>
  <div class="report-page">
    <ScrollStack 
      :itemStackDistance="60"
      stackPosition="20px"
      :itemScale="0.05"
      :blurAmount="4"
    >
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
      
      <div class="scroll-stack-wrapper" v-loading="loading">
        <el-empty v-if="!loading && reports.length === 0" description="暂无报告数据" />
        
        <!-- Top Hint -->
        <div class="stack-hint top-hint" v-if="!loading && reports.length > 0">
          已经到顶了!
        </div>

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
                <!-- Overlay to indicate there is more content -->
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

        <!-- Bottom Hint -->
        <div class="stack-hint bottom-hint" v-if="!loading && reports.length > 0">
          已经到底了!
        </div>
      </div>
    </ScrollStack>

    <!-- Expanded Dialog -->
    <el-dialog
      v-model="expandedReport"
      :title="expandedReport ? (expandedReport.type === 1 ? '日报详情' : '周报详情') : ''"
      width="800px"
      align-center
      class="report-dialog"
      :show-close="true"
      @close="closeExpanded"
    >
      <template v-if="expandedReport">
         <div class="dialog-header-info">
            <div class="date-badge large">
              <el-icon><Calendar /></el-icon>
              <span>{{ expandedReport.startDate }}</span>
              <span v-if="expandedReport.startDate !== expandedReport.endDate"> - {{ expandedReport.endDate }}</span>
            </div>
         </div>
         <div class="dialog-content markdown-body" v-html="expandedReport.reportContent"></div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.report-page {
  height: 100%;
  overflow: hidden; /* ScrollStack handles scrolling */
  background-color: var(--el-bg-color-page);
}

/* Make ScrollStack container full height and adjust padding */
:deep(.scroll-stack-container) {
  padding: 40px 20px;
  box-sizing: border-box;
}

.header-section {
  max-width: 800px;
  margin: 0 auto 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.title-area h2 {
  margin: 0;
  font-size: 28px;
  color: var(--el-text-color-primary);
}

.subtitle {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.scroll-stack-wrapper {
  max-width: 700px;
  margin: 0 auto;
  padding-bottom: 300px; 
  position: relative;
}

.stack-card {
  /* Removed sticky positioning as it is handled by ScrollStackItem */
  /* Margin is handled by ScrollStackItem spacing */
  
  background: var(--el-bg-color-overlay);
  border-radius: 16px;
  box-shadow: var(--el-box-shadow);
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  
  /* Fixed height for preview */
  height: 300px;
  display: flex;
  flex-direction: column;
}

/* Hover effect on the card itself */
.stack-card:hover {
  box-shadow: var(--el-box-shadow-light);
  border-color: var(--el-color-primary-light-5);
  /* z-index is handled by ScrollStackItem */
}

.card-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  background: var(--el-bg-color-overlay);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
  height: 25px; 
}

.date-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 6px 12px;
  border-radius: 8px;
}

.date-badge.large {
  font-size: 18px;
  padding: 8px 16px;
  margin-bottom: 20px;
  width: fit-content;
}

.report-type-tag {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
  background: var(--el-fill-color);
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
  color: var(--el-text-color-regular);
}

/* Gradient overlay for "Read More" effect */
.read-more-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 80px;
  background: linear-gradient(to bottom, transparent, var(--el-bg-color-overlay));
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 0;
  color: var(--el-color-primary);
  font-weight: 500;
  font-size: 14px;
}

.card-footer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
  flex-shrink: 0;
}

/* Dialog Styles */
.dialog-header-info {
  display: flex;
  justify-content: center;
}

.dialog-content {
  font-size: 16px;
  line-height: 1.8;
  color: var(--el-text-color-primary);
  max-height: 60vh;
  overflow-y: auto;
  padding: 0 10px;
}

/* Stack Hints */
.stack-hint {
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
  opacity: 0.6;
  padding: 20px;
  /* margin-top: -20px;  Removed as it might conflict with new layout */
  position: relative;
  z-index: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.stack-hint.top-hint {
  margin-bottom: 40px;
  margin-top: 0;
}

.stack-hint.bottom-hint {
  margin-top: 40px; 
  margin-bottom: 40px;
}

/* Markdown styles override */
:deep(.markdown-body) {
  font-size: 15px;
}
:deep(.markdown-body h1), :deep(.markdown-body h2) {
  border-bottom: none;
  margin-bottom: 0.5em;
}
</style>
