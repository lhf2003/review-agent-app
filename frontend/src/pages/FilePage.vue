<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import markdownit from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const auth = useAuthStore()
const search = ref('')
const sort = ref('mastery')
const tags = ref([])
const selectedTags = ref([])
const loading = ref(false)
const cards = ref([])
const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })

// --- markdown-it 配置 (参考官方文档) ---
const md = markdownit({
  html: false,
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

// 渲染辅助：统一将非字符串转为 JSON 围栏代码块
function toMarkdownInput(v) {
  if (typeof v === 'string') return v
  if (v == null) return ''
  try {
    return '\n\n```json\n' + JSON.stringify(v, null, 2) + '\n```\n\n'
  } catch {
    return String(v)
  }
}

// 使用 markdown-it + DOMPurify 的完整渲染逻辑
const mdRenderedProblem = computed(() => {
  const html = md.render(toMarkdownInput(result.value.problemStatement))
  return DOMPurify.sanitize(html)
})

const mdRenderedSolution = computed(() => {
  const html = md.render(toMarkdownInput(result.value.solution))
  return DOMPurify.sanitize(html)
})

const filteredCards = computed(() => {
  let list = cards.value
  if (search.value.trim()) {
    const q = search.value.trim().toLowerCase()
    list = list.filter(c => (c.title + ' ' + (c.problem || '') + ' ' + String(c.tags || '')).toLowerCase().includes(q))
  }
  if (selectedTags.value.length) {
    list = list.filter(c => {
      const arr = String(c.tags || '').split(',').map(s => s.trim()).filter(Boolean)
      return selectedTags.value.every(t => arr.includes(t))
    })
  }
  if (sort.value === 'mastery') {
    list = [...list].sort((a,b) => b.mastery - a.mastery)
  } else {
    list = [...list].sort((a,b) => new Date(b.date) - new Date(a.date))
  }
  return list
})

async function loadData() {
  try {
    loading.value = true
    const { list } = await api.getAnalysisList({ problemStatement: search.value, userId: auth.userId, page: 0, size: 10 })
    cards.value = list.map(it => ({
      id: it.id,
      fileId: it.fileId,
      title: it.problemStatement ? it.problemStatement : '分析项',
      problem: it.problemStatement || '',
      rootCause: it.solution || '',
      mastery: it.mastery ?? 50,
      tags: it.tagName || '',
      date: it.createdTime || '',
    }))
    const tagStats = await api.getTagStats({ userId: auth.userId })
    tags.value = (tagStats.data || tagStats.tags || []).map(t => ({ id: t.id, name: t.tagName, count: t.count }))
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function openDetail(card) {
  if (!card?.fileId) {
    ElMessage.error('缺少文件ID，无法查询结果')
    return
  }
  api.getAnalysisResultByIds(auth.userId, card.fileId, card.id)
    .then(resp => {
      const data = resp?.data || resp

      result.value = {
        title: typeof data?.problemStatement === 'string' && data.problemStatement
          ? String(data.problemStatement)
          : `结果 #${card.id}`,
        problemStatement: data?.problemStatement ?? '',
        solution: data?.solution ?? '',
      }
      resultDialog.value = true
    })
    .catch(e => ElMessage.error(`查询失败: ${e.message}`))
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:12px;height:100%;">
    <el-input v-model="search" placeholder="搜索问题、技术点..." prefix-icon="Search" clearable @change="loadData" />
    <div style="display:flex;gap:16px;flex:1;min-height:0;">
      <div style="width:260px;">
        <el-card>
          <div style="font-weight:600;">标签筛选</div>
          <div style="margin-top:12px;">
            <el-checkbox-group v-model="selectedTags">
              <div v-for="t in tags" :key="t.id" style="display:flex;align-items:center;justify-content:space-between;padding:6px 0;">
                <el-checkbox :label="t.name">{{ t.name }}</el-checkbox>
                <el-tag size="small">{{ t.count }}</el-tag>
              </div>
            </el-checkbox-group>
          </div>
          <div style="margin-top:16px;">
            <div style="font-size:13px;color:var(--el-text-color-secondary);">排序</div>
            <el-radio-group v-model="sort" style="margin-top:8px;display:flex;gap:8px;">
              <el-radio-button label="mastery">最近掌握</el-radio-button>
              <el-radio-button label="date">日期</el-radio-button>
            </el-radio-group>
          </div>
        </el-card>
      </div>
      <div style="flex:1;min-width:0;">
        <template v-if="filteredCards.length">
          <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:12px;">
            <el-card v-for="c in filteredCards" :key="c.id" @click="openDetail(c)" shadow="hover">
              <el-tooltip :content="c.title" placement="top">
                <div style="font-weight:600;margin-bottom:6px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                  {{ c.title.slice(0, 20) }}{{ c.title.length > 20 ? '...' : '' }}
                </div>
              </el-tooltip>
              <div style="margin-top:8px;display:flex;gap:6px;flex-wrap:wrap;">
                <el-tag v-for="t in c.tags.split(',')" :key="t" size="small">{{ t }}</el-tag>
              </div>
              <div style="display:flex;align-items:center;gap:8px;margin-top:8px;">
                <span>Mastery:</span>
                <el-progress :percentage="c.mastery" :stroke-width="8" style="flex:1;" />
              </div>
            </el-card>
          </div>
        </template>
        <el-empty v-else description="尚未发现任何分析结果" />
      </div> 
    </div>
  </div>
  <el-dialog v-model="resultDialog" title="分析结果" width="720px" align-center>
    <el-card shadow="never">
      <div style="height:520px; overflow:auto; padding-right:8px;">
        <div><b>您的原始请求:</b></div>
        <div class="md-content" v-html="mdRenderedProblem"></div>

        <!-- 过渡区域 -->
        <el-divider content-position="left">
          <el-icon><ArrowDown /></el-icon>
          <span style="margin-left:4px;">分析结果</span>
        </el-divider>

        <div class="md-content" v-html="mdRenderedSolution"></div>
      </div>
    </el-card>
  </el-dialog>
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
