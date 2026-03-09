<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Document, Select, UploadFilled, Close, Refresh, Back } from '@element-plus/icons-vue'
import AnalysisLoadingModal from '../components/AnalysisLoadingModal.vue'
import DataFileGrid from '../components/data/DataFileGrid.vue'
import CustomScroll from '../components/CustomScroll.vue'
import GeminiIcon from '../../public/icons/gemini-color.svg'
import OpenAIIcon from '../../public/icons/openai.svg'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const searchName = ref('')
const statusFilter = ref(null)

const importDialog = ref(false)
const importFile = ref(null)
const uploadRef = ref(null)
const importSource = ref(0)

const sourceFilter = ref(0)

const statusOptions = [
  { value: null, label: '全部', color: '#6B7280' },
  { value: 0, label: '未分析', color: '#6B7280' },
  { value: 1, label: '分析中', color: '#3B82F6' },
  { value: 2, label: '已分析', color: '#10B981' },
  { value: 3, label: '有更新', color: '#F59E0B' },
  { value: 4, label: '失败', color: '#EF4444' }
]

const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })

const showLogs = ref(false)
const currentAnalysisFile = ref(null)
const analysisStage = ref(1)
const analysisError = ref(false)
let logStream = null

function openContent(row) {
  router.push({ path: `/trace/${row.id}` })
}

async function load() {
  try {
    loading.value = true
    const resp = await api.dataPage({
      userId: auth.userId,
      page: page.value - 1,
      size: pageSize.value,
      fileName: searchName.value || null,
      processedStatus: statusFilter.value,
      source: sourceFilter.value
    })
    const pageData = resp?.data || resp
    const content = pageData?.content || []
    tableData.value = content
    total.value = pageData?.page?.totalElements || 0
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function openImport() {
  importDialog.value = true
  importSource.value = 0
  importFile.value = null
}

async function doImport() {
  try {
    if (!importFile.value) { ElMessage.warning('请选择文件'); return }
    await api.dataImport(auth.userId, importFile.value, importSource.value)

    ElMessage.success('操作成功')
    importDialog.value = false
    sourceFilter.value = importSource.value
    await load()
  } catch (e) { ElMessage.error(`操作失败: ${e.message}`) }
}

function handleAnalyze(row) {
  if (row.processedStatus === 1) return

  currentAnalysisFile.value = row
  showLogs.value = true

  analysisStage.value = 1
  analysisError.value = false

  if (logStream && typeof logStream.cancel === 'function') {
    logStream.cancel()
  }

  logStream = api.analysisLogStream({
    onStage: (data) => {
      const stage = parseInt(data)
      if ([1, 2, 3].includes(stage)) {
        analysisStage.value = stage
      }
    },
    onErrorEvent: (errorMessage) => {
      analysisError.value = true
    },
    onError: () => {
      analysisError.value = true
    },
    onDone: () => {}
  })

  api.startAnalysis(row.id)
    .then(() => {
      ElMessage.success('已触发分析')
      row.processedStatus = 1
      load()
    })
    .catch(e => {
      ElMessage.error(`触发失败: ${e.message}`)
      analysisError.value = true
      if (logStream && typeof logStream.cancel === 'function') {
        logStream.cancel()
      }
    })
}

function handleViewResult(row) {
  router.push({ path: '/data', query: { dataId: row.id } })
}

async function handleDelete(row) {
  try {
    await api.dataDelete(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    ElMessage.error(`删除失败: ${e.message}`)
  }
}

function handleRetry(data) {
  // 关闭弹窗并重置状态
  showLogs.value = false
  analysisError.value = false
  analysisStage.value = 1
  if (logStream && typeof logStream.cancel === 'function') {
    logStream.cancel()
  }

  if (typeof data === 'number' || typeof data === 'string') {
    const row = tableData.value.find(item => item.id === data)
    if (row) {
      // 延迟一下再打开弹窗，确保状态已重置
      setTimeout(() => handleAnalyze(row), 100)
    } else {
      api.startAnalysis(data)
        .then(() => {
          ElMessage.success('已重新触发分析')
          load()
        })
        .catch(e => {
          ElMessage.error(`触发失败: ${e.message}`)
        })
    }
  } else {
    // 延迟一下再打开弹窗，确保状态已重置
    setTimeout(() => handleAnalyze(data), 100)
  }
}


onMounted(() => {
  load()
  // 引导功能已禁用
})

function handleCloseModal() {
  if (logStream && typeof logStream.cancel === 'function') {
    logStream.cancel()
  }
  showLogs.value = false
  currentAnalysisFile.value = null
  load()
}
</script>

<template>
  <div class="data-page">
    <CustomScroll class="page-scroll">
      <!-- 顶部工具栏 -->
      <div class="toolbar glass">
        <el-button type="plain" link class="back-btn" @click="router.push('/dashboard')">
          <el-icon><Back /></el-icon>
        </el-button>

        <el-radio-group v-model="sourceFilter" @change="() => { page = 1; load() }" class="nav-radio-group">
          <el-radio-button :label="0">本地文件</el-radio-button>
          <el-radio-button :label="1">Gemini</el-radio-button>
          <el-radio-button :label="2">ChatGPT</el-radio-button>
        </el-radio-group>

        <div class="spacer"></div>

        <div class="filter-group" id="filter-group">
          <el-input
            v-model="searchName"
            placeholder="输入文件名..."
            prefix-icon="Search"
            clearable
            @change="() => { page = 1; load() }"
            class="search-input"
          />
          <el-select
            v-model="statusFilter"
            placeholder="状态筛选"
            clearable
            @change="() => { page = 1; load() }"
            class="status-select"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value ?? 'all'"
              :value="opt.value"
              :label="opt.label"
            >
              <span class="status-dot" :style="{ backgroundColor: opt.color }"></span>
              {{ opt.label }}
            </el-option>
          </el-select>
        </div>

        <el-button type="primary" id="import-btn" @click="openImport" class="import-btn">
          <el-icon><UploadFilled /></el-icon>
          导入
        </el-button>
        <el-button type="plain" link class="refresh-btn" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <!-- 数据展示区域 -->
      <div class="content-card glass" id="data-grid">
        <DataFileGrid
          :data="tableData"
          :loading="loading"
          empty-text="暂无数据"
          empty-description="点击右上角「导入」按钮添加文件"
          @view-content="openContent"
          @analyze="handleAnalyze"
          @view-result="handleViewResult"
          @delete="handleDelete"
          @retry="handleRetry"
        />
      </div>

      <!-- 分页导航 -->
      <div class="pagination-card glass">
        <el-config-provider :locale="zhCn">
          <el-pagination
            small
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 48, 96]"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @current-change="load"
            @size-change="() => { page = 1; load() }"
          />
        </el-config-provider>
      </div>
    </CustomScroll>

    <!-- 分析加载弹窗 -->
    <AnalysisLoadingModal
      v-model="showLogs"
      :fileName="currentAnalysisFile?.fileName || ''"
      :currentStage="analysisStage"
      :hasError="analysisError"
      :fileId="currentAnalysisFile?.id"
      @close="handleCloseModal"
      @retry="handleRetry"
    />

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialog" title="导入/新建数据" width="640px" align-center class="glass-dialog">
      <div class="import-container">
        <!-- 数据来源 -->
        <div class="section-block">
          <div class="section-label">数据来源</div>
          <div class="source-cards">
            <div class="source-card" :class="{ active: importSource === 0 }" @click="importSource = 0">
              <div class="card-icon-wrapper local">
                <el-icon><Document /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-title">本地文件</div>
                <div class="card-desc">上传本地文档</div>
              </div>
              <div class="check-mark" v-if="importSource === 0">
                <el-icon><Select /></el-icon>
              </div>
            </div>
            <div class="source-card" :class="{ active: importSource === 1 }" @click="importSource = 1">
              <div class="card-icon-wrapper gemini">
                <img :src="GeminiIcon" class="svg-icon" alt="Gemini" />
              </div>
              <div class="card-info">
                <div class="card-title">Gemini</div>
                <div class="card-desc">对话记录导入</div>
              </div>
              <div class="check-mark" v-if="importSource === 1">
                <el-icon><Select /></el-icon>
              </div>
            </div>
            <div class="source-card" :class="{ active: importSource === 2 }" @click="importSource = 2">
              <div class="card-icon-wrapper chatgpt">
                <img :src="OpenAIIcon" class="svg-icon" alt="ChatGPT" />
              </div>
              <div class="card-info">
                <div class="card-title">ChatGPT</div>
                <div class="card-desc">对话记录导入</div>
              </div>
              <div class="check-mark" v-if="importSource === 2">
                <el-icon><Select /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 统一文件上传 -->
        <div class="section-block">
          <div class="section-label">文件上传</div>
          <el-upload
            class="upload-area"
            drag
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="(file) => importFile = file.raw"
            accept=".md,.txt,.json,.html"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .txt, .md, .json, .html 格式文件
              </div>
            </template>
          </el-upload>

          <transition name="fade">
            <div v-if="importFile" class="file-preview">
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name">{{ importFile.name }}</span>
              <el-icon class="remove-file" @click.stop="importFile = null"><Close /></el-icon>
            </div>
          </transition>
        </div>

        <!-- 按钮组 -->
        <div class="dialog-footer">
          <el-button size="large" @click="importDialog=false" class="btn-cancel">取消</el-button>
          <el-button size="large" type="primary" @click="doImport" :disabled="!importFile" class="btn-primary">
            开始导入
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 分析结果 -->
    <el-dialog v-model="resultDialog" title="分析结果" width="620px" align-center class="glass-dialog">
      <div class="result-card">
        <div class="result-title">{{ result.title }}</div>
        <div class="result-section">
          <div class="result-label">Problem:</div>
          <div class="result-content">{{ result.problemStatement }}</div>
        </div>
        <div class="result-section">
          <div class="result-label">Solution:</div>
          <div class="result-content pre-wrap">{{ result.solution }}</div>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<style scoped lang="scss">
// Glass Effect Base
.glass {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
}

// Page Container
.data-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

// Scroll Area
.page-scroll {
  flex: 1;
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// Toolbar
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  border-radius: 16px;
  flex-shrink: 0;
}

.nav-radio-group {
  background: transparent;
  border: none;
  padding: 0;
}

.nav-radio-group :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  border-radius: 10px;
  padding: 8px 16px;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.nav-radio-group :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 248, 245, 0.05);
  color: var(--text-primary);
}

.nav-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--accent-primary);
  color: white;
  box-shadow: 0 4px 12px var(--accent-glow-soft) !important;
}

.spacer {
  flex: 1;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 200px;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 248, 245, 0.05);
  box-shadow: 0 0 0 1px var(--glass-border) inset;
  border-radius: 10px;
}

.search-input :deep(.el-input__wrapper:hover) {
  background: rgba(255, 248, 245, 0.08);
}

.search-input :deep(.el-input__inner) {
  color: var(--text-primary);
}

.search-input :deep(.el-input__inner::placeholder) {
  color: var(--text-tertiary);
}

.status-select {
  width: 130px;
}

.status-select :deep(.el-input__wrapper) {
  background: rgba(255, 248, 245, 0.05);
  box-shadow: 0 0 0 1px var(--glass-border) inset;
  border-radius: 10px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.import-btn {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  color: white;
  border-radius: 10px;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.import-btn:hover {
  background: var(--accent-secondary);
  border-color: var(--accent-secondary);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--accent-glow-soft);
}

.refresh-btn {
  color: var(--text-secondary);
  font-size: 18px;
}

.refresh-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 248, 245, 0.05);
}

.back-btn {
  color: var(--text-secondary);
  font-size: 18px;
}

.back-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 248, 245, 0.05);
}

// Content Card
.content-card {
  flex: 1;
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

// Pagination Card
.pagination-card {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 12px 20px;
  border-radius: 16px;
}

.pagination-card :deep(.el-pagination) {
  color: var(--text-secondary);
}

.pagination-card :deep(.el-pagination__total),
.pagination-card :deep(.el-pagination__jump) {
  color: var(--text-tertiary);
}

.pagination-card :deep(.btn-prev),
.pagination-card :deep(.btn-next),
.pagination-card :deep(.el-pager li) {
  background: rgba(255, 248, 245, 0.05);
  border: 1px solid var(--glass-border);
  color: var(--text-secondary);
  border-radius: 8px;
}

.pagination-card :deep(.btn-prev:hover),
.pagination-card :deep(.btn-next:hover),
.pagination-card :deep(.el-pager li:hover) {
  background: rgba(255, 248, 245, 0.08);
  color: var(--text-primary);
}

.pagination-card :deep(.el-pager li.is-active) {
  background: var(--accent-primary);
  color: white;
  border-color: var(--accent-primary);
}

// Glass Dialog Overrides
:deep(.glass-dialog) {
  background: var(--bg-overlay);
  backdrop-filter: blur(var(--glass-blur-strong));
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-lg);
}

:deep(.glass-dialog .el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid var(--glass-border);
  margin-right: 0;
}

:deep(.glass-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 16px;
}

:deep(.glass-dialog .el-dialog__body) {
  padding: 24px;
  color: var(--text-secondary);
}

// Import Dialog Styles
.import-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.source-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.source-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 12px;
  background: rgba(255, 248, 245, 0.03);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
  text-align: center;
  gap: 8px;
  overflow: hidden;
}

.source-card:hover {
  border-color: rgba(204, 102, 51, 0.4);
  transform: translateY(-2px);
  background: rgba(255, 248, 245, 0.05);
}

.source-card.active {
  border-color: var(--accent-primary);
  background: rgba(204, 102, 51, 0.1);
  box-shadow: 0 0 0 1px var(--accent-primary), 0 4px 12px var(--accent-glow-soft);
}

.card-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 4px;
  background: rgba(255, 248, 245, 0.05);
  color: var(--text-secondary);
  transition: all 0.2s;
  border: 1px solid var(--glass-border);
}

.source-card.active .card-icon-wrapper.local {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  color: white;
}

.source-card.active .card-icon-wrapper.gemini,
.source-card.active .card-icon-wrapper.chatgpt {
  background: rgba(255, 248, 245, 0.05);
  border-color: var(--accent-primary);
}

.svg-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-desc {
  font-size: 12px;
  color: var(--text-tertiary);
}

.check-mark {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--accent-primary);
  font-size: 16px;
}

// Upload Area
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 160px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-style: dashed;
  border-color: var(--glass-border);
  background: rgba(255, 248, 245, 0.03);
  transition: all 0.2s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: rgba(204, 102, 51, 0.5);
  background: rgba(204, 102, 51, 0.05);
}

.upload-area :deep(.el-icon--upload) {
  font-size: 48px;
  color: var(--accent-primary);
  margin-bottom: 8px;
}

.upload-area :deep(.el-upload__text) {
  color: var(--text-secondary);
  font-size: 14px;
}

.upload-area :deep(.el-upload__text em) {
  color: var(--accent-primary);
  font-style: normal;
  font-weight: 500;
}

.upload-area :deep(.el-upload__tip) {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 8px;
}

// File Preview
.file-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(255, 248, 245, 0.05);
  border-radius: 12px;
  border: 1px solid var(--glass-border);
  margin-top: 12px;
}

.file-icon {
  font-size: 20px;
  color: var(--accent-primary);
}

.file-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-file {
  cursor: pointer;
  color: var(--text-tertiary);
  transition: color 0.2s;
  font-size: 16px;
}

.remove-file:hover {
  color: var(--mastery-low);
}

// Dialog Footer
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--glass-border);
}

.btn-cancel {
  background: rgba(255, 248, 245, 0.05);
  border-color: var(--glass-border);
  color: var(--text-secondary);
  border-radius: 10px;
  padding: 10px 24px;
}

.btn-cancel:hover {
  background: rgba(255, 248, 245, 0.08);
  border-color: var(--glass-border-hover);
  color: var(--text-primary);
}

.btn-primary {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  border-radius: 10px;
  padding: 10px 24px;
  font-weight: 500;
}

.btn-primary:hover {
  background: var(--accent-secondary);
  border-color: var(--accent-secondary);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--accent-glow-soft);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

// Result Card
.result-card {
  padding: 8px;
}

.result-title {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.result-section {
  margin-bottom: 16px;
}

.result-label {
  font-weight: 600;
  color: var(--accent-tertiary);
  margin-bottom: 6px;
  font-size: 13px;
}

.result-content {
  color: var(--text-secondary);
  line-height: 1.6;
}

.result-content.pre-wrap {
  white-space: pre-wrap;
}

// Transitions
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// Responsive
@media (max-width: 1200px) {
  .page-scroll {
    padding: 20px 24px;
  }

  .toolbar {
    flex-wrap: wrap;
    gap: 12px;
  }

  .filter-group {
    order: 3;
    width: 100%;
    margin-top: 4px;
  }

  .search-input,
  .status-select {
    flex: 1;
    width: auto !important;
  }
}

@media (max-width: 768px) {
  .page-scroll {
    padding: 16px;
    gap: 16px;
  }

  .toolbar {
    padding: 10px 16px;
  }

  .nav-radio-group :deep(.el-radio-button__inner) {
    padding: 6px 12px;
    font-size: 13px;
  }

  .content-card {
    border-radius: 16px;
  }

  .pagination-card {
    padding: 10px 16px;
  }

  .pagination-card :deep(.el-pagination__sizes) {
    display: none;
  }

  .source-cards {
    grid-template-columns: 1fr;
  }

  .source-card {
    flex-direction: row;
    justify-content: flex-start;
    padding: 16px;
  }

  .card-icon-wrapper {
    margin-bottom: 0;
    margin-right: 12px;
  }
}

@media (max-width: 480px) {
  .page-scroll {
    padding: 12px;
  }

  .toolbar {
    padding: 8px 12px;
  }

  .filter-group {
    flex-direction: column;
    gap: 8px;
  }

  .search-input,
  .status-select {
    width: 100% !important;
  }

  .dialog-footer {
    flex-direction: column;
  }

  .dialog-footer .el-button {
    width: 100%;
  }
}
</style>
