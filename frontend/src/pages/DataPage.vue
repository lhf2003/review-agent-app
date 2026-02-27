<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox, ElTour } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRouter } from 'vue-router'
import { Document, Select, UploadFilled, Close } from '@element-plus/icons-vue'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import AnalysisLoadingModal from '../components/AnalysisLoadingModal.vue'
import CustomScroll from '../components/CustomScroll.vue'
import GeminiIcon from '../../public/icons/gemini-color.svg'
import OpenAIIcon from '../../public/icons/openai.svg'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchName = ref('')
const statusFilter = ref(null)

const importDialog = ref(false)
const importFile = ref(null)
const uploadRef = ref(null)
const importSource = ref(0)

// Filter source
const sourceFilter = ref(0) // Default to Local (0)

const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })

// Drawer for fileContent preview (markdown)
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerContent = ref('')

// 分析状态
const showLogs = ref(false)
const currentAnalysisFile = ref(null)
const analysisStage = ref(1)      // 当前阶段：1, 2, 3
const analysisError = ref(false)  // 是否失败
let logStream = null

// 新手引导 Tour
const showTour = ref(false)
const tourSteps = ref([
  {
    target: '#import-btn',
    title: '第一步：导入数据',
    description: '点击"导入数据"按钮，支持上传本地文件、Gemini 导出或 ChatGPT 导出格式的 AI 对话记录。',
    placement: 'bottom'
  },
  {
    target: '#data-table',
    title: '第二步：查看分析结果',
    description: '导入的数据会自动进行分析，分析完成后可以在这里查看结构化的问题描述和解决方案。',
    placement: 'top'
  },
  {
    target: '#filter-group',
    title: '第三步：筛选和搜索',
    description: '使用搜索框和状态筛选器快速找到你需要的分析结果。完成后，可以点击侧边栏的"标签"来整理数据。',
    placement: 'bottom'
  }
])

function openContent(row) {
  if (row.processedStatus === 2) {
    router.push({ path: `/trace/${row.id}` })
    return
  }
  drawerTitle.value = row.fileName ? `内容 - ${row.fileName}` : `内容 #${row.id}`
  drawerContent.value = row.fileContent ?? ''
  drawerVisible.value = true
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
    // Switch to the tab we just imported to
    sourceFilter.value = importSource.value
    await load()
  } catch (e) { ElMessage.error(`操作失败: ${e.message}`) }
}

function onAction(row) {
  if (row.processedStatus !== 2) {
    // 记录当前分析的文件
    currentAnalysisFile.value = row
    showLogs.value = true

    // 重置状态
    analysisStage.value = 1
    analysisError.value = false

    if (logStream && typeof logStream.cancel === 'function') {
      logStream.cancel()
    }

    // SSE 接收整型阶段和错误事件
    logStream = api.analysisLogStream({
      onStage: (data) => {
        // data 是整型字符串：'1', '2', '3'
        const stage = parseInt(data)
        if ([1, 2, 3].includes(stage)) {
          analysisStage.value = stage
        }
      },
      onErrorEvent: (errorMessage) => {
        // 后端发送的错误事件
        analysisError.value = true
      },
      onError: () => {
        // 网络错误或连接错误
        analysisError.value = true
      },
      onDone: () => {
        // SSE 流结束
      }
    })

    // Only trigger startAnalysis if it is NOT already analyzing (status != 1)
    if (row.processedStatus !== 1) {
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
  } else {
    router.push({ path: '/analysis', query: { dataId: row.id } })
  }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除数据 ${row.fileName || `#${row.id}`}？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    await api.dataDelete(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

// Tour 超时保护
let tourTimeout = null

onMounted(() => {
  load()
  // 延迟显示新手引导，确保页面完全加载
  const tourShown = localStorage.getItem('tour-shown')
  if (!tourShown) {
    setTimeout(() => {
      // 检查所有目标元素是否存在
      const targets = ['#import-btn', '#data-table', '#filter-group']
      const allTargetsExist = targets.every(selector => document.querySelector(selector) !== null)

      if (allTargetsExist) {
        showTour.value = true
        // 30秒超时保护
        tourTimeout = setTimeout(() => {
          if (showTour.value) closeTour()
        }, 30000)
      } else {
        // 元素未准备好，跳过本次引导
      }
    }, 1500)
  }
})

// Tour 完成/跳过处理
function onTourFinish() {
  localStorage.setItem('tour-shown', 'true')
  showTour.value = false
}

function onTourSkip() {
  localStorage.setItem('tour-shown', 'true')
  showTour.value = false
}

// 手动关闭 Tour（用于紧急关闭）
function closeTour() {
  localStorage.setItem('tour-shown', 'true')
  showTour.value = false
  if (tourTimeout) {
    clearTimeout(tourTimeout)
    tourTimeout = null
  }
}

// 处理分析弹窗关闭
function handleCloseModal() {
  if (logStream && typeof logStream.cancel === 'function') {
    logStream.cancel()
  }
  showLogs.value = false
  currentAnalysisFile.value = null

  // 立即刷新表格
  load()
}

// 处理分析弹窗重试
function handleRetry(fileId) {
  api.startAnalysis(fileId)
    .then(() => {
      ElMessage.success('已重新触发分析')
      // 重置状态
      analysisStage.value = 1
      analysisError.value = false
      // 刷新表格
      load()
    })
    .catch(e => {
      ElMessage.error(`触发失败: ${e.message}`)
      analysisError.value = true
    })
}
</script>

<template>
  <div class="page-container">
     <!-- 顶部工具栏 -->
     <div class="toolbar">
       <!-- 紧急关闭引导按钮（仅在引导显示时出现） -->
       <el-button v-if="showTour" type="danger" size="small" @click="closeTour" style="margin-right: 12px;">
         关闭引导
       </el-button>

       <el-radio-group v-model="sourceFilter" @change="() => { page = 1; load() }">
         <el-radio-button :label="0">本地文件</el-radio-button>
         <el-radio-button :label="1">Gemini</el-radio-button>
         <el-radio-button :label="2">ChatGPT</el-radio-button>
       </el-radio-group>

      <div class="spacer"></div>

      <div class="filter-group" id="filter-group">
        <el-input v-model="searchName" placeholder="输入文件名..." prefix-icon="Search" clearable @change="() => { page = 1; load() }" style="width: 160px; max-width: 100%;" />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 110px; max-width: 100%;" @change="() => { page = 1; load() } ">
        <el-option :value="null" label="全部" />
        <el-option :value="0" label="未分析" />
        <el-option :value="2" label="已分析" />
        <el-option :value="3" label="有更新" />
        <el-option :value="4" label="失败" />
      </el-select>
      </div>

      <el-button type="plain" id="import-btn" @click="openImport" icon="Upload">
        导入
      </el-button>
      <el-button type="plain" link size="large" @click="load">
        <el-icon><Refresh /></el-icon>
      </el-button>
    </div>

    <!-- 表格区域 -->
    <div class="table-wrapper">
      <div class="table-scroll-wrapper">
        <CustomScroll>
          <el-table id="data-table" :data="tableData" v-loading="loading" style="width: 100%; max-width: 100%;" row-key="id" size="small" class="glass-table">
        <template #empty>
            <el-empty description="暂无数据" :image-size="100" />
        </template>
        <el-table-column :resizable="false" prop="id" label="ID" width="80" align="center" />
        <el-table-column :resizable="false" prop="fileName" label="文件名" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column :resizable="false" prop="sessionCount" label="会话数" width="100" align="center" />
        <el-table-column :resizable="false" prop="processedStatus" label="状态" width="150" align="center">
          <template #default="{ row }">
            <el-tag class="status-tag" v-if="row.processedStatus===0" type="info" effect="light">未分析</el-tag>
            <el-tag class="status-tag" type="primary" v-else-if="row.processedStatus===1" effect="light">正在分析</el-tag>
            <el-tag class="status-tag" type="success" v-else-if="row.processedStatus===2" effect="light">已分析</el-tag>
            <el-tag class="status-tag" type="warning" v-else-if="row.processedStatus===3" effect="light">有更新</el-tag>
            <el-tag class="status-tag" type="danger" v-else-if="row.processedStatus===4" effect="light">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" prop="createdTime" label="同步时间" width="250" align="center" show-overflow-tooltip />
        <el-table-column :resizable="false" label="文件内容" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openContent(row)">查看内容</el-button>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" label="操作" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <el-button :type="row.processedStatus === 2 ? 'success' : (row.processedStatus === 1 ? 'warning' : 'primary')" @click="onAction(row)">{{ row.processedStatus === 2 ? '结果' : (row.processedStatus === 1 ? '分析中' : '分析') }}</el-button>
            <el-button type="danger" plain @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
        </CustomScroll>
      </div>
    </div>

    <!-- 分页导航 -->
    <div class="pagination-bar">
      <el-config-provider :locale="zhCn">
        <el-pagination
          small
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10,20,50,100]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="load"
          @size-change="() => { page = 1; load() }"
        />
      </el-config-provider>
    </div>

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

    <el-dialog v-model="importDialog" title="导入/新建数据" width="640px" align-center class="import-dialog">
      <div class="import-container">

        <!-- 数据来源 -->
        <div class="section-block">
            <div class="section-label">数据来源</div>
            <div class="source-cards">
                <div class="source-card" :class="{ active: importSource === 0 }" @click="importSource = 0">
                    <div class="card-icon-wrapper local"><el-icon><Document /></el-icon></div>
                    <div class="card-info">
                        <div class="card-title">本地文件</div>
                        <div class="card-desc">上传本地文档</div>
                    </div>
                    <div class="check-mark" v-if="importSource === 0"><el-icon><Select /></el-icon></div>
                </div>
                <div class="source-card" :class="{ active: importSource === 1 }" @click="importSource = 1">
                    <div class="card-icon-wrapper gemini">
                        <img :src="GeminiIcon" class="svg-icon" alt="Gemini" />
                    </div>
                    <div class="card-info">
                        <div class="card-title">Gemini</div>
                        <div class="card-desc">对话记录导入</div>
                    </div>
                    <div class="check-mark" v-if="importSource === 1"><el-icon><Select /></el-icon></div>
                </div>
                <div class="source-card" :class="{ active: importSource === 2 }" @click="importSource = 2">
                    <div class="card-icon-wrapper chatgpt">
                        <img :src="OpenAIIcon" class="svg-icon" alt="ChatGPT" />
                    </div>
                    <div class="card-info">
                        <div class="card-title">ChatGPT</div>
                        <div class="card-desc">对话记录导入</div>
                    </div>
                    <div class="check-mark" v-if="importSource === 2"><el-icon><Select /></el-icon></div>
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

             <transition name="el-fade-in">
                 <div v-if="importFile" class="file-preview">
                    <el-icon class="file-icon"><Document /></el-icon>
                    <span class="file-name">{{ importFile.name }}</span>
                    <el-icon class="remove-file" @click.stop="importFile = null"><Close /></el-icon>
                 </div>
             </transition>
        </div>

        <!-- 按钮组 -->
        <div class="dialog-footer">
          <el-button size="large" @click="importDialog=false">取消</el-button>
          <el-button size="large" type="primary" @click="doImport" :disabled="!importFile">
            开始导入
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 分析结果 -->
    <el-dialog v-model="resultDialog" title="分析结果" width="620px" align-center>
      <el-card shadow="never">
        <div style="font-weight:600;margin-bottom:8px;">{{ result.title }}</div>
        <div><b>Problem:</b> {{ result.problemStatement }}</div>
        <div style="margin-top:8px;"><b>Solution:</b></div>
        <div style="white-space:pre-wrap;">{{ result.solution }}</div>
      </el-card>
    </el-dialog>

    <!-- 文件内容抽屉（Markdown） -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" direction="rtl" size="50%">
      <CustomScroll>
        <MarkdownRenderer :content="drawerContent" />
      </CustomScroll>
    </el-drawer>

    <!-- 新手引导 Tour -->
    <el-tour
      v-model="showTour"
      :steps="tourSteps"
      :close-on-press-escape="true"
      :close-on-page-visibility-change="true"
      :z-index="9999"
      @finish="onTourFinish"
      @skip="onTourSkip"
      @close="onTourSkip"
    />
  </div>
</template>

<style scoped>
/* Page Layout Styles */
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  padding: 0 6px;
  max-width: 100%;
  overflow: hidden;
  box-sizing: border-box;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  background: transparent;
  padding: 4px 0;
  flex-wrap: wrap;
  max-width: 100%;
  overflow: hidden;
}

@media (max-width: 768px) {
  .toolbar {
    gap: 8px;
  }

  .toolbar .el-input {
    width: 100% !important;
    max-width: 100%;
    flex: 1;
    min-width: 120px;
  }

  .toolbar .el-select {
    width: auto !important;
    min-width: 100px;
  }

  .toolbar .el-radio-group {
    flex-wrap: wrap;
  }

  .toolbar .el-radio-button {
    font-size: 13px;
  }

  .spacer {
    display: none;
  }
}

.spacer {
  flex: 1;
}

.table-wrapper {
  flex: 1;
  min-height: 0;
  border-radius: 16px;
  overflow: hidden;
  max-width: 100%;
  display: flex;
  flex-direction: column;

  /* Glass Effect */
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);

  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.table-scroll-wrapper {
  flex-grow: 1;
  height: 0;
  overflow-y: hidden;
}

/* Dark Mode Table Wrapper */
html.dark .table-wrapper {
  background: rgba(28, 28, 30, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* Glass Table Overrides */
.glass-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.3);
  --el-table-tr-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(0, 0, 0, 0.02);
  background-color: transparent !important;
}

html.dark .glass-table {
  --el-table-header-bg-color: rgba(0, 0, 0, 0.2);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.05);
  --el-table-bg-color: transparent;
}

:deep(.el-table), :deep(.el-table__expanded-cell) {
  background-color: transparent !important;
}

:deep(.el-table tr) {
  background-color: transparent !important;
}

:deep(.el-table th.el-table__cell) {
  background-color: var(--el-table-header-bg-color) !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05) !important;
}

html.dark :deep(.el-table th.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.02) !important;
}

html.dark :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.02) !important;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

.pagination-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 12px 16px;
  background: #ffffff;
  border-top: none;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  margin-bottom: 4px;
  max-width: 100%;
  overflow: hidden;
  transition: background-color 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), border-color 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* Dark mode adjustment */
html.dark .pagination-bar {
  background: #1c1c1e;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 响应式表格优化 */
@media (max-width: 768px) {
  .table-wrapper {
    border-radius: 12px;
  }

  :deep(.el-table) {
    font-size: 13px;
  }

  :deep(.el-table__cell) {
    padding: 8px 4px;
  }

  :deep(.el-table__header-wrapper),
  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  .pagination-bar {
    padding: 10px 12px;
    font-size: 13px;
  }

  .pagination-bar :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }

  .pagination-bar :deep(.el-pagination__sizes) {
    display: none;
  }
}

/* Import Dialog Styles */
.import-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 8px 4px;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-left: 2px;
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
  padding: 16px 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
  text-align: center;
  gap: 8px;
  overflow: hidden;
}

.source-card:hover {
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.source-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 1px var(--el-color-primary);
}

.card-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  transition: all 0.2s;
}

.source-card.active .card-icon-wrapper.local {
  background: var(--el-color-primary);
  color: white;
}

.source-card.active .card-icon-wrapper.gemini,
.source-card.active .card-icon-wrapper.chatgpt {
  background: var(--el-bg-color);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-5);
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
  color: var(--el-text-color-primary);
}

.card-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  transform: scale(0.9);
}

.check-mark {
  position: absolute;
  top: 6px;
  right: 6px;
  color: var(--el-color-primary);
  font-size: 16px;
}

/* Upload Area Customization */
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 160px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-style: dashed;
  transition: all 0.2s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.el-upload__text {
  margin-top: 8px;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  margin-top: 12px;
}

.file-icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

.file-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-file {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: color 0.2s;
}

.remove-file:hover {
  color: var(--el-color-danger);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
