<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import markdownit from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchName = ref('')
const statusFilter = ref(null)

const importDialog = ref(false)
const importFile = ref(null)

const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })

// Drawer for fileContent preview (markdown)
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerContent = ref('')
const logs = ref([])
const showLogs = ref(false)
let eventSource = null

const md = markdownit({
  breaks: true,
  highlight: function (str, lang) {
    const lg = (lang || '').trim().toLowerCase()
    if (lg && hljs.getLanguage(lg)) {
      try {
        return '<pre><code class="hljs">' +
               hljs.highlight(str, { language: lg, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch (__) {}
    }
    return '<pre><code class="hljs">' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

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
       const baseUrl = 'http://localhost:8081' // Hardcoded base URL matching http.js
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
        api.startAnalysis({ userId: auth.userId, fileId: Number(row.id) })
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
  <div style="display:flex;flex-direction:column;gap:12px;">
    <div style="display:flex;align-items:center;gap:12px;">
      <el-input v-model="searchName" placeholder="搜索文件名..." prefix-icon="Search" clearable @change="() => { page = 1; load() }" style="max-width:280px" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:160px" @change="() => { page = 1; load() } ">
        <el-option :value="null" label="全部" />
        <el-option :value="0" label="未分析" />
        <el-option :value="2" label="已分析" />
        <el-option :value="3" label="有更新" />
        <el-option :value="4" label="失败" />
      </el-select>
      <el-button type="primary" @click="openImport" icon="Upload">
        导入
      </el-button>
      <el-button @click="load" icon="Refresh">
        刷新
      </el-button>
    </div>

  <el-table :data="tableData" v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="fileName" label="文件名" width="200" />
      <el-table-column prop="sessionCount" label="会话数" width="140" />
      <el-table-column prop="processedStatus" label="状态" width="160">
        <template #default="{ row }">
          <el-tag v-if="row.processedStatus===0">未分析</el-tag>
          <el-tag type="primary" v-else-if="row.processedStatus===1">正在分析</el-tag>
          <el-tag type="success" v-else-if="row.processedStatus===2">已分析</el-tag>
          <el-tag type="warning" v-else-if="row.processedStatus===3">有更新</el-tag>
          <el-tag type="danger" v-else-if="row.processedStatus===4">失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="同步时间" width="250" />
            <el-table-column label="文件内容" width="160">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain style="opacity:0.7" @click="openContent(row)">查看内容</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button size="small" :type="row.processedStatus === 2 ? 'success' : (row.processedStatus === 1 ? 'warning' : 'primary')" @click="onAction(row)">{{ row.processedStatus === 2 ? '查看' : (row.processedStatus === 1 ? '分析中' : '分析') }}</el-button>
          <el-button size="small" type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
  </el-table>

  <div style="display:flex;justify-content:center;">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10,20,50]"
        layout="prev, sizes, pager, next"
        :total="total"
        @current-change="load"
        @size-change="() => { page = 1; load() }"
      />
  </div>

  <!-- 导入文件 -->
    <el-dialog v-model="showLogs" title="分析日志" width="600px" align-center @close="() => { /* if(eventSource) eventSource.close() */ }">
      <div id="log-container" style="background:#f5f7fa;color:#303133;padding:12px;border-radius:4px;height:300px;overflow-y:auto;font-family:monospace;border:1px solid #dcdfe6;">
        <div v-for="(log, idx) in logs" :key="idx" style="margin-bottom:4px;border-bottom:1px dashed #ebeef5;padding-bottom:2px;">
          <span style="color:#909399;margin-right:8px;">[{{ new Date().toLocaleTimeString() }}]</span>
          <span>{{ log }}</span>
        </div>
        <div v-if="logs.length === 0" style="color:#909399;text-align:center;margin-top:20px;">暂无日志...</div>
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
          <span style="margin-left:8px;color:var(--el-text-color-secondary);font-size:12px;">
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
    <div class="md-content" v-html="mdRenderedDrawer" style="height:100%;overflow:auto;"></div>
  </el-drawer>
  </div>
</template>

<style scoped>
.md-content :deep(pre) {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 10px;
  overflow: auto;
}
.md-content :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}
.md-content :deep(.hljs) {
  display: block;
  overflow-x: auto;
  padding: 0.5em;
}
</style>
