<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import markdownit from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

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

const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })

// Drawer for fileContent preview (markdown)
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerContent = ref('')
const logs = ref([])
const showLogs = ref(false)
let eventSource = null

const md = new markdownit({
  breaks: true,
  highlight: function (str, lang) {
    const trimmed = str.trimEnd()
    let highlighted = ''
    const lg = (lang || '').trim().toLowerCase()
    if (lg && hljs.getLanguage(lg)) {
      try {
        highlighted = hljs.highlight(trimmed, { language: lg, ignoreIllegals: true }).value
      } catch (__) {}
    } else {
      highlighted = md.utils.escapeHtml(trimmed)
    }
    
    return `<div class="code-block-wrapper"><div class="code-block-header"><span class="code-lang">${lg || 'text'}</span><button class="copy-btn">复制</button></div><div class="code-block-body"><pre><code class="hljs ${lg}">${highlighted}</code></pre></div></div>`
  }
})

function handleMdClick(e) {
  if (e.target.classList.contains('copy-btn')) {
    const btn = e.target
    const wrapper = btn.closest('.code-block-wrapper')
    if (wrapper) {
      const codeEl = wrapper.querySelector('pre code')
      if (codeEl) {
        const text = codeEl.textContent
        navigator.clipboard.writeText(text).then(() => {
          const originalText = btn.textContent
          btn.textContent = '已复制'
          btn.classList.add('copied')
          setTimeout(() => {
            btn.textContent = originalText
            btn.classList.remove('copied')
          }, 2000)
        }).catch(err => {
          ElMessage.error('复制失败')
          console.error(err)
        })
      }
    }
  }
}

function toMdInput(v) {
  if (typeof v === 'string') return v
  if (v == null) return ''
  try { return '```json\n' + JSON.stringify(v, null, 2) + '\n```' } catch { return String(v) }
}
const mdRenderedDrawer = ref('')
function openContent(row) {
  drawerTitle.value = row.fileName ? `内容 - ${row.fileName}` : `内容 #${row.id}`
  drawerContent.value = row.fileContent ?? ''
  mdRenderedDrawer.value = DOMPurify.sanitize(md.render(toMdInput(drawerContent.value)))
  drawerVisible.value = true
}

async function load() {
  try {
    loading.value = true
    const resp = await api.dataPage({ userId: auth.userId, page: page.value - 1, size: pageSize.value, fileName: searchName.value || null, processedStatus: statusFilter.value })
    const pageData = resp?.data || resp
    const content = pageData?.content || []
    tableData.value = content
    total.value = pageData?.totalElements ?? content.length
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function openImport() { importDialog.value = true }
function onFileChange(e) { importFile.value = e.target.files?.[0] || null }
async function doImport() {
  if (!importFile.value) { ElMessage.warning('请选择文件'); return }
  try {
    await api.dataImport(auth.userId, importFile.value)
    ElMessage.success('导入成功')
    importDialog.value = false
    importFile.value = null
    await load()
  } catch (e) { ElMessage.error(`导入失败: ${e.message}`) }
}

function onAction(row) {
  if (row.processedStatus !== 2) {
    // Open logs dialog
    showLogs.value = true

    // If it's a new analysis (status != 1), clear logs
    if (row.processedStatus !== 1) {
      logs.value = []
    }
    
    if (eventSource && eventSource.readyState !== EventSource.CLOSED) {

    } else {
       const baseUrl = '/api'
       eventSource = new EventSource(`${baseUrl}/analysis/log/stream?userId=${auth.userId}`)
       
       eventSource.addEventListener('log', (event) => {
         logs.value.push(event.data)
         // Auto scroll to bottom
         setTimeout(() => {
            const logContainer = document.getElementById('log-container')
            if (logContainer) logContainer.scrollTop = logContainer.scrollHeight
         }, 0)
       })
       
       eventSource.onerror = () => {
         eventSource.close()
       }
    }

    // Only trigger startAnalysis if it is NOT already analyzing (status != 1)
    if (row.processedStatus !== 1) {
        api.startAnalysis(row.id )
          .then(() => { 
            ElMessage.success('已触发分析')
            logs.value.push('分析任务已提交...')
            // 立即将状态置为“正在分析”，UI 即时反馈
            row.processedStatus = 1
            load() 
          })
          .catch(e => {
            ElMessage.error(`触发失败: ${e.message}`)
            logs.value.push(`错误: ${e.message}`)
            if(eventSource) eventSource.close()
          })
    } else {
        logs.value.push('已重新连接到日志流...')
    }
  } else {
    router.push({ path: '/analysis', query: { dataId: row.id } })
  }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除数据 #${row.id}？`, '提示', { type: 'warning' })
    const resp = await api.dataDelete(row.id)
    if (resp?.ok === false) {
      ElMessage.warning('后端缺少 /data/delete 接口，已占位埋点')
      return
    }
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-input v-model="searchName" placeholder="搜索文件名..." prefix-icon="Search" clearable @change="() => { page = 1; load() }" style="max-width:280px" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:160px" @change="() => { page = 1; load() } ">
        <el-option :value="null" label="全部" />
        <el-option :value="0" label="未分析" />
        <el-option :value="2" label="已分析" />
        <el-option :value="3" label="有更新" />
        <el-option :value="4" label="失败" />
      </el-select>
      <div class="spacer"></div>
      <el-button type="primary" @click="openImport" icon="Upload">
        导入
      </el-button>
      <el-button @click="load" icon="Refresh">
        刷新
      </el-button>
    </div>

    <!-- 表格区域 -->
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" style="width:100%; height:100%;" row-key="id" border stripe>
        <el-table-column :resizable="false" prop="id" label="ID" width="90" align="center" />
        <el-table-column :resizable="false" prop="fileName" label="文件名" width="350"  align="center" />
        <el-table-column :resizable="false" prop="sessionCount" label="会话数" width="100" align="center" />
        <el-table-column :resizable="false" prop="processedStatus" label="状态" width="180" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.processedStatus===0" type="info" effect="light">未分析</el-tag>
            <el-tag type="primary" v-else-if="row.processedStatus===1" effect="light">正在分析</el-tag>
            <el-tag type="success" v-else-if="row.processedStatus===2" effect="light">已分析</el-tag>
            <el-tag type="warning" v-else-if="row.processedStatus===3" effect="light">有更新</el-tag>
            <el-tag type="danger" v-else-if="row.processedStatus===4" effect="light">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" prop="createdTime" label="同步时间" width="280" align="center" />
        <el-table-column :resizable="false" label="文件内容" width="218" align="center">
          <template #default="{ row }">
            <el-button size="default" type="primary" link @click="openContent(row)">查看内容</el-button>
          </template>
        </el-table-column>
        <el-table-column :resizable="false" label="操作" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="default" :type="row.processedStatus === 2 ? 'success' : (row.processedStatus === 1 ? 'warning' : 'primary')" @click="onAction(row)">{{ row.processedStatus === 2 ? '查看' : (row.processedStatus === 1 ? '分析中' : '分析') }}</el-button>
            <el-button size="default" type="danger" plain @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页导航 -->
    <div class="pagination-bar">
      <el-pagination
        small
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10,20,50,100]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        background
        @current-change="load"
        @size-change="() => { page = 1; load() }"
      />
    </div>

    <!-- 导入文件 -->
    <el-dialog v-model="showLogs" title="分析日志" width="600px" align-center @close="() => { /* if(eventSource) eventSource.close() */ }">
      <div id="log-container" style="background:var(--el-fill-color-light);color:var(--el-text-color-primary);padding:12px;border-radius:4px;height:300px;overflow-y:auto;font-family:monospace;border:1px solid var(--el-border-color);">
        <div v-for="(log, idx) in logs" :key="idx" style="margin-bottom:4px;border-bottom:1px dashed var(--el-border-color-lighter);padding-bottom:2px;">
          <span style="color:var(--el-text-color-secondary);margin-right:8px;">[{{ new Date().toLocaleTimeString() }}]</span>
          <span>{{ log }}</span>
        </div>
        <div v-if="logs.length === 0" style="color:var(--el-text-color-secondary);text-align:center;margin-top:20px;">暂无日志...</div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showLogs = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialog" title="导入文件" width="420px" align-center>
      <div style="display:flex;flex-direction:column;gap:12px;">
        <!-- 文件选择 -->
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="(file) => importFile = file.raw"
          accept=".md,.txt"
        >
          <template #trigger>
            <el-button type="primary" plain icon="Upload">选择文件</el-button>
          </template>
          <span style="margin-left:8px;color:var(--el-text-color-secondary);font-size:var(--el-font-size-small);">
            支持 txt、md
          </span>
        </el-upload>

        <!-- 已选文件提示 -->
        <el-alert
          v-if="importFile"
          :title="`已选择：${importFile.name}`"
          type="info"
          :closable="false"
          show-icon
        />

        <!-- 按钮组 -->
        <div style="display:flex;justify-content:flex-end;gap:8px;">
          <el-button @click="importDialog=false">取消</el-button>
          <el-button type="primary" :disabled="!importFile" @click="doImport">导入</el-button>
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
      <div class="md-content" v-html="mdRenderedDrawer" @click="handleMdClick" style="height:100%;overflow:auto;"></div>
    </el-drawer>
  </div>
</template>

<style scoped>
.md-content :deep(.code-block-wrapper) {
  margin: 1em 0;
  border-radius: 8px;
  background-color: #282c34;
  overflow: hidden;
  border: 1px solid #3e4451;
}

.md-content :deep(.code-block-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: transparent; /* Remove header background to blend in */
  border-bottom: 1px solid #3e4451;
  color: #abb2bf;
  font-size: 12px;
  user-select: none;
}

.md-content :deep(.code-block-body) {
  display: flex;
  background-color: #282c34;
  padding: 12px 0;
}

.md-content :deep(.code-lang) {
  font-weight: 600;
  text-transform: uppercase;
}

.md-content :deep(.copy-btn) {
  cursor: pointer;
  background: transparent;
  border: 1px solid #5c6370;
  color: #abb2bf;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  transition: all 0.2s;
}

.md-content :deep(.copy-btn:hover) {
  background-color: #3e4451;
  border-color: #abb2bf;
  color: #fff;
}

.md-content :deep(.copy-btn.copied) {
  border-color: #98c379;
  color: #98c379;
}

.md-content :deep(pre) {
  margin: 0;
  padding: 0 12px;
  overflow: auto;
  background-color: transparent; /* Transparent so it uses wrapper/body bg */
  border: none;
  border-radius: 0;
  flex: 1;
  line-height: 1.5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 14px;
}

.md-content :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  color: #abb2bf;
  font-size: 14px;
  line-height: 1.5;
}

.md-content :deep(.hljs) {
  display: block;
  overflow-x: auto;
  padding: 0;
  background: transparent;
}

/* Page Layout Styles */
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 5px;
  /* Ensure it takes full height of parent */
  min-height: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 1px;
  flex-shrink: 0;
  background: var(--el-bg-color);
  padding: 4px 0;
}

.spacer {
  flex: 1;
}

.table-wrapper {
  flex: 1;
  min-height: 0; /* Crucial for scrolling */
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}
.table-wrapper :deep(.el-table__body-wrapper) {
  overflow: hidden !important;
}
.table-wrapper :deep(.el-scrollbar__wrap) {
  overflow: hidden !important;
}

.pagination-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 2px 10px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.05);
}

/* Dark mode adjustment */
html.dark .pagination-bar {
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.2);
}
</style>
