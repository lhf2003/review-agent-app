xiu'gaiuga
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { useRoute } from 'vue-router'
import markdownit from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

const auth = useAuthStore()
const chatStore = useChatStore()
const route = useRoute()
const search = ref('')
const sort = ref('mastery')
const tagMode = ref('main')
const tags = ref([])
const selectedTags = ref([])
const loading = ref(false)
const cards = ref([])
const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })

const analysisMap = ref({})
const fileNames = computed(() => Object.keys(analysisMap.value))
const selectedFileName = ref('ALL')

// --- Tag Color Management ---
// Palette of pastel colors for main tags to ensure distinct and readable visuals with dark text
const TAG_COLORS = [
  '#409EFF', // Primary Blue
  '#67C23A', // Success Green
  '#E6A23C', // Warning Orange
  '#F56C6C', // Danger Red
  '#9B59B6', // Purple
  '#1ABC9C', // Teal
  '#FF9F43', // Orange Peel
  '#34495E', // Dark Blue Grey
  '#2ECC71', // Emerald
  '#3498DB', // Peter River
  '#E74C3C', // Alizarin
  '#8E44AD', // Wisteria
  '#2C3E50', // Midnight Blue
  '#D35400', // Pumpkin
  '#16A085', // Green Sea
  '#7F8C8D', // Concrete
  '#27AE60', // Nephritis
  '#2980B9', // Belize Hole
  '#F39C12', // Orange
  '#C0392B'  // Pomegranate
]

const tagColorMap = ref({})

/**
 * Assigns a unique color to each main tag.
 * Stores the mapping in tagColorMap.
 * Uses a deterministic approach so colors remain consistent on reload.
 */
function assignTagColors(allTags) {
  const mainTags = allTags.filter(t => t.type === 'main')
    .sort((a, b) => a.name.localeCompare(b.name))

  const map = {}
  mainTags.forEach((t, index) => {
    map[t.name] = TAG_COLORS[index % TAG_COLORS.length]
  })
  tagColorMap.value = map
}

// --- markdown-it 配置 (参考官方文档) ---
const md = markdownit({
  html: false,
  breaks: true,
  highlight: function (str, lang) {
    const trimmed = str.trimEnd()
    let highlighted = ''
    const lg = (lang || '').trim().toLowerCase()
    if (lg && hljs.getLanguage(lg)) {
      try {
        highlighted = hljs.highlight(trimmed, { language: lg, ignoreIllegals: true }).value
      } catch (__) { }
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
    list = [...list].sort((a, b) => b.mastery - a.mastery)
  } else {
    list = [...list].sort((a, b) => new Date(b.date) - new Date(a.date))
  }
  return list
})

const sidebarTags = computed(() => {
  return tags.value.filter(t => t.type === tagMode.value)
})

async function loadData() {
  try {
    loading.value = true
    const res = await api.getFileNameList()
    analysisMap.value = res.data || res || {}

    // 如果未选择文件，尝试根据路由参数或默认第一个
    if (selectedFileName.value === 'ALL') {
      const dataId = route.query.dataId
      if (dataId) {
        for (const [name, list] of Object.entries(analysisMap.value)) {
          if (list && list.length > 0 && String(list[0].fileId) === String(dataId)) {
            selectedFileName.value = name
            break
          }
        }
      }
      // 如果没有指定 dataId，保持 'ALL' 不变
    }

    updateCards()

    const tagStats = await api.getTagStats()
    const rawTags = Array.isArray(tagStats) ? tagStats : tagStats?.data ?? []

    tags.value = Array.isArray(rawTags)
      ? rawTags.map(t => {
        const ty = t.type
        return {
          id: t.tagId,
          name: t.tagName,
          count: t.count ?? 0,
          type: ty
        }
      }) : []

    assignTagColors(tags.value)
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function updateCards() {
  let list = []
  if (selectedFileName.value === 'ALL') {
    // 合并所有文件列表
    Object.values(analysisMap.value).forEach(subList => {
      if (Array.isArray(subList)) {
        list = list.concat(subList)
      }
    })
  } else {
    list = analysisMap.value[selectedFileName.value] || []
  }

  cards.value = list.map(it => {
    const displayTags = []
    const allTagNames = []

    if (it.mainTagName) {
      displayTags.push({ name: it.mainTagName, type: 'main' })
      allTagNames.push(it.mainTagName)
    }

    if (it.subTagNameList && Array.isArray(it.subTagNameList)) {
      it.subTagNameList.forEach(t => {
        displayTags.push({ name: t, type: 'sub' })
        allTagNames.push(t)
      })
    }

    return {
      id: it.id,
      fileId: it.fileId,
      title: it.problemStatement ? it.problemStatement : '分析项',
      problem: it.problemStatement || '',
      rootCause: it.solution || '',
      mastery: it.mastery ?? 50,
      tags: allTagNames.join(','),
      displayTags: displayTags,
      recommendTags: it.recommendTagList || [],
      date: it.createdTime || '',
    }
  })
}

watch(selectedFileName, () => {
  updateCards()
})

function openDetail(card) {
  if (!card?.fileId) {
    ElMessage.error('缺少文件ID，无法查询结果')
    return
  }
  api.getAnalysisResultByIds(card.fileId, card.id)
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

const tagActionDialog = ref({
  visible: false,
  tagName: '',
  analysisId: null,
  loading: false
})

function handleRecommendTagClick(tagName, card) {
  tagActionDialog.value = {
    visible: true,
    tagName: tagName,
    analysisId: card.id,
    loading: false
  }
}

async function confirmAddTag(type) {
  const { tagName, analysisId } = tagActionDialog.value
  if (!tagName) return

  try {
    tagActionDialog.value.loading = true
    // 1: main tag, 2: sub tag
    const tagType = type === 'main' ? 1 : 2
    await api.addRecommendTag({ name: tagName, analysisId, tagType })

    ElMessage.success(`添加${type === 'main' ? '主' : '子'}标签成功`)
    tagActionDialog.value.visible = false
    // 刷新数据以更新标签状态
    loadData()
  } catch (e) {
    ElMessage.error(`添加失败: ${e.message}`)
  } finally {
    tagActionDialog.value.loading = false
  }
}

function onChat(card) {
  chatStore.startAnalysisChat(card)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:12px;height:100%;">
    <!-- Top Row: Search (Left) + Horizontal Tags (Right) -->
    <div style="display: flex; gap: 24px;">
      <div style="width: 200px; flex-shrink: 0; padding-top: 4px;">
        <el-input v-model="search" placeholder="搜索问题、技术点..." prefix-icon="Search" clearable />
      </div>
      <div style="flex: 1; min-width: 0;">
        <!-- 横向滚动标签栏 -->
        <div class="horizontal-tags-container" v-if="fileNames.length">
          <div class="tag-item" :class="{ active: selectedFileName === 'ALL' }" @click="selectedFileName = 'ALL'">
            全部
          </div>
          <div v-for="name in fileNames" :key="name" class="tag-item" :class="{ active: selectedFileName === name }"
            @click="selectedFileName = name">
            {{ name }}
          </div>
        </div>
      </div>
    </div>

    <div style="display:flex;gap:24px;flex:1;min-height:0;">
      <!-- Left Sidebar: Filter Card -->
      <div style="width:200px; flex-shrink: 0;">
        <el-card>
          <div style="font-weight:600;display:flex;justify-content:space-between;align-items:center;">
            <span>标签筛选</span>
            <el-radio-group v-model="tagMode" size="small">
              <el-radio-button label="main">主</el-radio-button>
              <el-radio-button label="sub">子</el-radio-button>
            </el-radio-group>
          </div>
          <div style="margin-top:12px;">
            <el-checkbox-group v-model="selectedTags">
              <div v-for="t in sidebarTags" :key="t.id"
                style="display:flex;align-items:center;justify-content:space-between;padding:6px 0;">
                <el-checkbox size="large" :label="t.name">
                  <span style="font-size: 15px;">{{ t.name }}</span>
                </el-checkbox>
                <el-tag size="small" :color="t.type === 'main' ? tagColorMap[t.name] : ''"
                  :effect="t.type === 'main' ? 'dark' : 'plain'"
                  :style="{ ...(t.type === 'main' ? { border: 'none' } : {}), width: '32px', height: '18px !important', fontSize: '12px !important', justifyContent: 'center', padding: '0' }">
                  {{ t.count }}
                </el-tag>
              </div>
            </el-checkbox-group>
          </div>
          <div style="margin-top:16px;">
            <div style="font-size:13px;color:var(--el-text-color-secondary);">排序</div>
            <el-radio-group v-model="sort" size="small" style="margin-top:8px;display:flex;">
              <el-radio-button label="mastery">最近掌握</el-radio-button>
              <el-radio-button label="date">日期</el-radio-button>
            </el-radio-group>
          </div>
        </el-card>
      </div>
      <div style="flex:1;min-width:0;">
        <template v-if="filteredCards.length">
          <div
            style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));column-gap:70px;row-gap:18px;">
            <el-card v-for="c in filteredCards" :key="c.id" @click="openDetail(c)" shadow="hover"
              :class="{ 'recommend-card': c.recommendTags && c.recommendTags.length }"
              style="position:relative;overflow:visible;">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                <div
                  style="font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1;margin-right:8px;">
                  {{ c.title.slice(0, 40) }}{{ c.title.length > 40 ? '...' : '' }}
                </div>
                <el-button type="primary" link @click.stop="onChat(c)" style="padding: 0 5px;">
                  对话
                </el-button>
              </div>
              <div style="margin-top:8px;display:flex;gap:6px;flex-wrap:wrap;">
                <el-tag v-for="(t, idx) in c.displayTags" :key="idx" size="small"
                  :color="t.type === 'main' ? tagColorMap[t.name] : ''" :type="t.type === 'main' ? '' : 'info'"
                  :effect="t.type === 'main' ? 'dark' : 'plain'" :style="t.type === 'main' ? { border: 'none' } : {}">
                  {{ t.name }}
                </el-tag>
              </div>
              <div style="display:flex;align-items:center;gap:8px;margin-top:8px;">
                <span>Mastery:</span>
                <el-progress :percentage="c.mastery" :stroke-width="8" style="flex:1;" />
              </div>
              <div v-if="c.recommendTags && c.recommendTags.length" class="recommend-section">
                <div class="recommend-title">推荐标签</div>
                <div style="display:flex; flex-direction:column; gap:4px;">
                  <el-tag v-for="rt in c.recommendTags" :key="rt" size="small" type="warning" effect="plain"
                    class="recommend-tag-item" @click.stop="handleRecommendTagClick(rt, c)">
                    + {{ rt }}
                  </el-tag>
                </div>
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
        <div class="md-content" v-html="mdRenderedProblem" @click="handleMdClick"></div>

        <!-- 过渡区域 -->
        <el-divider content-position="left">
          <el-icon>
            <ArrowDown />
          </el-icon>
          <span style="margin-left:4px;">分析结果</span>
        </el-divider>

        <div class="md-content" v-html="mdRenderedSolution" @click="handleMdClick"></div>
      </div>
    </el-card>
  </el-dialog>

  <el-dialog v-model="tagActionDialog.visible" title="添加推荐标签" width="400px" align-center append-to-body>
    <div style="text-align:center; padding: 20px 0;">
      <p style="margin-bottom: 20px; font-size: 16px;">
        是否将 <el-tag type="warning" size="large">{{ tagActionDialog.tagName }}</el-tag> 添加为新标签？
      </p>
      <div style="display:flex; justify-content:center; gap:16px;">
        <el-button type="primary" :loading="tagActionDialog.loading" @click="confirmAddTag('main')">
          设为主标签
        </el-button>
        <el-button type="success" :loading="tagActionDialog.loading" @click="confirmAddTag('sub')">
          设为子标签
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
@import '../styles/_mixins.scss';
@import '../styles/_variables.scss';

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
  background-color: transparent;
  /* Remove header background to blend in */
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
  background-color: transparent;
  /* Transparent so it uses wrapper/body bg */
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

/* 推荐卡片样式 */
.recommend-card {
  border: 1px solid #e6a23c !important;
  /* Warning color border */
  z-index: 1;
}

.recommend-card:hover {
  z-index: 100 !important;
}

.recommend-section {
  position: absolute;
  left: 100%;
  top: 0;
  width: auto;
  min-width: 60px;
  max-width: 120px;
  background-color: transparent;
  padding: 0 0 0 4px;
  /* 减小间距 */

  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease-in-out;
  z-index: 10;
  pointer-events: none;
}

.recommend-card:hover .recommend-section {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

.recommend-title {
  font-size: 11px;
  /* 字体更小 */
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  text-align: left;
}

.recommend-tag-item {
  cursor: pointer;
  transition: all 0.2s;
  font-size: 10px !important;
  /* 强制更小的字体 */
  height: 20px !important;
  /* 强制更小的高度 */
  padding: 0 4px !important;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex !important;
  justify-content: center;
  align-items: center;
}

.recommend-tag-item:hover {
  transform: translateX(4px);
  /* 悬浮时向右微动，而不是放大，显得更精致 */
}

/* 横向标签栏样式 */
.horizontal-tags-container {
  display: flex;
  gap: 0;
  /* No gap */
  overflow-x: auto;
  padding: 4px 2px 12px 2px;
  /* Add padding for shadow/glow space */
  margin-bottom: 4px;
  scrollbar-width: thin;
  /* Firefox */
  scrollbar-color: #dcdfe6 transparent;
}

.horizontal-tags-container::-webkit-scrollbar {
  height: 4px;
  /* 滚动条高度 */
}

.horizontal-tags-container::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 4px;
}

.horizontal-tags-container::-webkit-scrollbar-track {
  background: transparent;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 20px;
  border-radius: 0;
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  border: 1px solid var(--el-border-color);
  border-right: none;

  // Use glow mixin
  @include glow-hover($glow-color-light);

  &:last-child {
    border-right: 1px solid var(--el-border-color);
  }
}

.tag-item:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-color-primary);
  z-index: 10;
  /* Bring to front */
  box-shadow: 0 0 12px 3px rgba(64, 158, 255, 0.5);
}

.tag-item.active {
  background-color: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
  z-index: 5;
  box-shadow: 0 0 $glow-blur-radius $glow-spread-radius $glow-color-light;
}

.tag-count {
  transition: all 0.3s;
  cursor: default;
}
.tag-count:hover {
  box-shadow: 0 0 8px var(--tag-glow-color);
  transform: scale(1.1);
  z-index: 1;
}
</style>
