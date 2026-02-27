xiu'gaiuga
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import { useRoute, useRouter } from 'vue-router'
import { View } from '@element-plus/icons-vue'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import CustomScroll from '../components/CustomScroll.vue'

const auth = useAuthStore()
const chatStore = useChatStore()
const route = useRoute()
const router = useRouter()
const search = ref('')
const sort = ref('mastery')
const tagMode = ref('main')
const tags = ref([])
const selectedTags = ref([])
const loading = ref(false)
const cards = ref([])
const resultDialog = ref(false)
const result = ref({ title: '', problemStatement: '', solution: '' })
const highlightedCardId = ref(null)

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

    // 如果有 mainTag 参数，自动选中该标签
    const mainTagFromQuery = route.query.mainTag
    if (mainTagFromQuery && Array.isArray(selectedTags.value)) {
      const tagExists = tags.value.some(t => t.name === mainTagFromQuery)
      if (tagExists) {
        selectedTags.value = [mainTagFromQuery]
        tagMode.value = 'main'
      }
    }

    // 如果有 highlightId 参数，高亮对应的卡片
    const highlightIdFromQuery = route.query.highlightId
    if (highlightIdFromQuery) {
      highlightedCardId.value = highlightIdFromQuery
      // 等待 DOM 更新后滚动到高亮卡片
      setTimeout(() => {
        scrollToHighlightedCard()
      }, 100)
    }
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
      mainTagName: it.mainTagName,
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
        fileId: card.fileId,
        analysisId: card.id,
      }
      resultDialog.value = true
    })
    .catch(e => ElMessage.error(`查询失败: ${e.message}`))
}

function goToTrace() {
  if (!result.value?.fileId) {
    ElMessage.error('缺少文件ID，无法跳转')
    return
  }
  router.push({
    path: `/trace/${result.value.fileId}`,
    query: { activeId: result.value.analysisId }
  })
}

function scrollToHighlightedCard() {
  const cardElement = document.getElementById('card-' + highlightedCardId.value)
  if (cardElement) {
    cardElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    // 添加临时高亮效果
    cardElement.classList.add('highlighted')
    setTimeout(() => {
      cardElement.classList.remove('highlighted')
      highlightedCardId.value = null
    }, 2000)
  }
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
  if (typeof chatStore.startAnalysisChat === 'function') {
    chatStore.startAnalysisChat(card)
  } else {
    ElMessage.error('聊天功能暂不可用，请刷新页面重试')
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:12px;height:calc(100vh - 100px);">
    <!-- Top Row: Search (Left) + Horizontal Tags (Right) -->
    <div style="display: flex; gap: 24px;">
      <div style="width: 200px; flex-shrink: 0; padding-top: 4px;">
        <el-input v-model="search" placeholder="搜索问题、技术点..." prefix-icon="Search" clearable />
      </div>
      <div style="flex: 1; min-width: 0; position: relative;">
        <!-- 横向滚动标签栏 -->
        <div class="tags-scroll-wrapper">
          <el-button class="scroll-btn left" @click="scrollTags('left')" circle size="small" v-if="showLeftBtn">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          
          <div class="horizontal-tags-container" ref="tagsContainer" @scroll="checkScroll" v-if="fileNames.length">
            <div class="tag-item" :class="{ active: selectedFileName === 'ALL' }" @click="selectedFileName = 'ALL'">
              全部
            </div>
            <div v-for="name in fileNames" :key="name" class="tag-item" :class="{ active: selectedFileName === name }"
              @click="selectedFileName = name">
              {{ name }}
            </div>
          </div>
          
          <el-button class="scroll-btn right" @click="scrollTags('right')" circle size="small" v-if="showRightBtn">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div class="analysis-layout">
      <!-- Left Sidebar: Filter Card -->
      <div class="filter-sidebar">
        <el-card style="height: 100%; display: flex; flex-direction: column;">
          <div style="font-weight:600;display:flex;justify-content:space-between;align-items:center; flex-shrink: 0;">
            <span>标签筛选</span>
            <el-radio-group v-model="tagMode" size="small">
              <el-radio-button label="main">主</el-radio-button>
              <el-radio-button label="sub">子</el-radio-button>
            </el-radio-group>
          </div>
          <div style="margin-top:6px; flex: 1; min-height: 0;">
            <CustomScroll>
              <el-checkbox-group v-model="selectedTags">
                <div v-for="t in sidebarTags" :key="t.id"
                  style="display:flex;align-items:center;justify-content:space-between;padding:2px 0;">
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
            </CustomScroll>
          </div>
          <div style="margin-top:16px; flex-shrink: 0;">
            <div style="font-size:13px;color:var(--el-text-color-secondary);">排序</div>
            <div style="margin-top:8px;">
              <el-radio-group v-model="sort" size="small">
                <el-radio-button label="mastery">最近掌握</el-radio-button>
                <el-radio-button label="date">日期</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </el-card>
      </div>
      <div class="content-area-wrapper">
        <CustomScroll class="content-scroll-area">
          <template v-if="filteredCards.length">
            <div class="cards-grid">
              <el-card
                v-for="c in filteredCards"
                :key="c.id"
                :id="'card-' + c.id"
                @click="openDetail(c)"
                shadow="hover"
                :class="{ 'recommend-card': c.recommendTags && c.recommendTags.length, 'highlighted': c.id === highlightedCardId }"
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
                    :effect="t.type === 'main' ? 'dark' : 'plain'"
                    class="mini-tag"
                    :style="t.type === 'main' ? { border: 'none' } : {}">
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
        </CustomScroll>
        <!-- Blur Overlays -->
        <div class="scroll-blur top"></div>
        <div class="scroll-blur bottom"></div>
      </div>
    </div>
  </div>
  <el-dialog v-model="resultDialog" width="720px" align-center>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>分析结果</span>
        <el-button type="primary" link @click="goToTrace" size="small">
          <el-icon><View /></el-icon>
          <span style="margin-left:4px;">查看原始会话</span>
        </el-button>
      </div>
    </template>
    <el-card shadow="never">
      <div style="height:520px; overflow:auto; padding-right:8px;">
        <div><b>您的原始请求:</b></div>
        <MarkdownRenderer :content="result.problemStatement" />

        <!-- 过渡区域 -->
        <el-divider content-position="left">
          <el-icon>
            <ArrowDown />
          </el-icon>
          <span style="margin-left:4px;">分析结果</span>
        </el-divider>

        <MarkdownRenderer :content="result.solution" />
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
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
}

.mini-tag {
  height: 20px !important;
  padding: 0 6px !important;
  font-size: 11px !important;
  line-height: 18px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.content-area-wrapper {
  flex: 1;
  min-width: 0;
  position: relative;
  overflow: hidden;
}

.content-scroll-area {
  height: 100%;
  padding: 2px;
}

.scroll-blur {
  position: absolute;
  left: 0;
  right: 0;
  height: 64px;
  pointer-events: none;
  z-index: 10;
  backdrop-filter: blur(12px) saturate(180%);
  -webkit-backdrop-filter: blur(12px) saturate(180%);
}

.scroll-blur.top {
  top: 0;
  /* Removed white gradient background to avoid fog effect on gray page */
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.6) 20%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.6) 10%, transparent 100%);
}

.scroll-blur.bottom {
  bottom: 0;
  /* Removed white gradient background */
  mask-image: linear-gradient(to top, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.6) 40%, transparent 100%);
  -webkit-mask-image: linear-gradient(to top, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.6) 20%, transparent 100%);
}
.tag-item:last-child {
  border-right: 1px solid var(--el-border-color);
}


.tag-item:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-color-primary);
  z-index: 20;
  /* Bring to front */
  /* Removed blue border and shadow effects */
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

/* 覆盖 ElCard 默认的 body 样式，使其填充剩余空间 */
.el-card :deep(.el-card__body) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.analysis-layout {
  display: flex;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

.filter-sidebar {
  width: 200px;
  flex-shrink: 0;
  overflow: hidden;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill,minmax(300px,1fr));
  column-gap: 70px;
  row-gap: 18px;
}

@media (max-width: 768px) {
  .analysis-layout {
    flex-direction: column;
    gap: 12px;
  }

  .filter-sidebar {
    width: 100%;
    height: auto;
    max-height: 200px; /* Limit height on mobile */
    flex-shrink: 0;
  }

  .cards-grid {
    grid-template-columns:1fr; /* Single column on mobile */
    column-gap: 0;
  }
}

/* 高亮卡片样式 */
.el-card.highlighted {
  animation: highlight-pulse 2s ease-in-out;
  border: 2px solid var(--el-color-primary);
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.2);
}

@keyframes highlight-pulse {
  0% {
    box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 20px 8px rgba(64, 158, 255, 0.4);
  }
  100% {
    box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.2);
  }
}
</style>
