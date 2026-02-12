<script setup>
import { ref, onMounted, onUnmounted, useTemplateRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Reading, Delete, FolderOpened, Download } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import AnimatedList from '../../components/AnimatedList.vue'
import QuizLoadingModal from '../../components/quiz/QuizLoadingModal.vue'

const route = useRoute()
const router = useRouter()
const info = ref({})
const loading = ref(false)
const showQuizLoading = ref(false)
const estimatedSeconds = ref(40)
const quizCompletedId = ref(null) // 存储已完成生成的 quizId
const apiCompleted = ref(false) // API 是否已完成
const exporting = ref(false)

onMounted(() => {
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await api.getCollectionDetail(route.params.id)
    info.value = res
  } finally {
    loading.value = false
  }
}

async function startLearning() {
  if (!info.value.analysisResults || info.value.analysisResults.length === 0) {
    ElMessage.warning('合集为空，无法开始学习')
    return
  }

  try {
    // 1. 检测版本
    const checkResult = await api.checkQuizVersion(info.value.id)

    // 2. 没有历史记录，直接创建
    if (!checkResult.hasExistingQuiz) {
      await createQuiz()
      return
    }

    // 3. 版本一致，直接使用
    if (checkResult.versionMatch) {
      router.push(`/collections/${info.value.id}/quiz?quizId=${checkResult.quizId}`)
      return
    }

    // 4. 版本不一致，弹窗询问
    showVersionMismatchDialog(checkResult)

  } catch (error) {
    ElMessage.error('版本检测失败: ' + error.message)
  }
}

/**
 * 显示版本不匹配对话框
 */
function showVersionMismatchDialog(checkResult) {
  ElMessageBox.confirm(
    '检测到合集内容已更新，题库可能不再完整。是否基于最新内容重新生成题目？',
    '题库版本提示',
    {
      distinguishCancelAndClose: true,
      confirmButtonText: '重新生成',
      cancelButtonText: '继续当前题型',
      type: 'warning',
    }
  ).then(() => {
    regenerateQuiz()
  }).catch((action) => {
    if (action === 'cancel') {
      router.push(`/collections/${info.value.id}/quiz?quizId=${checkResult.quizId}`)
    }
  })
}

/**
 * 计算预估生成时间
 */
function calculateEstimatedTime() {
  const resultCount = info.value.analysisResults?.length || 0

  // 小合集（≤5个分析结果）：30-40秒
  if (resultCount <= 5) {
    estimatedSeconds.value = 30 + Math.floor(Math.random() * 11) // 30-40秒随机
  } else {
    // 大合集：基础30秒 + 每个分析结果1秒
    const estimated = 30 + resultCount*2
    // 限制在 30-90 秒之间
    estimatedSeconds.value = Math.min(estimated, 90)
  }
}

/**
 * 创建新题库
 */
async function createQuiz() {
  calculateEstimatedTime()
  showQuizLoading.value = true
  apiCompleted.value = false

  try {
    const quiz = await api.generateQuiz({ collectionId: info.value.id })
    // API 完成，记录 quizId，等待进度条到 100%
    quizCompletedId.value = quiz.id
    apiCompleted.value = true
  } catch (error) {
    showQuizLoading.value = false
    ElMessage.error('生成题库失败: ' + error.message)
  }
}

/**
 * 重新生成题库
 */
async function regenerateQuiz() {
  calculateEstimatedTime()
  showQuizLoading.value = true
  apiCompleted.value = false

  try {
    const newQuiz = await api.regenerateQuiz(info.value.id)
    // API 完成，记录 quizId，等待进度条到 100%
    quizCompletedId.value = newQuiz.id
    apiCompleted.value = true
  } catch (error) {
    showQuizLoading.value = false
    ElMessage.error('重新生成题库失败: ' + error.message)
  }
}

/**
 * 题库生成完成后的跳转
 */
function onQuizCompleted() {
  showQuizLoading.value = false
  if (quizCompletedId.value) {
    router.push(`/collections/${info.value.id}/quiz?quizId=${quizCompletedId.value}`)
    quizCompletedId.value = null
  }
}

function confirmRemove(item) {
  ElMessageBox.confirm(
    '确定要从合集中移除这个问题吗？',
    '移除确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    removeSession(item.id)
  }).catch(() => {})
}

async function removeSession(sessionId) {
  try {
    await api.removeSessionFromCollection(info.value.id, sessionId)
    ElMessage.success('已移除')
    // 本地移除
    if (info.value.analysisResults) {
      info.value.analysisResults = info.value.analysisResults.filter(s => s.id !== sessionId)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 模拟跳转到原始分析页
function viewOriginal(item) {
  if (item.fileId) {
     // 跳转到 trace 页，并带上 activeId 以高亮对应的 session
     router.push({
       path: `/trace/${item.fileId}`,
       query: { activeId: item.id }
     })
  } else {
     ElMessage.warning('无法跳转：文件ID缺失')
  }
}

async function exportCollection() {
  if (!info.value.id) {
    ElMessage.warning('无法导出：合集ID缺失')
    return
  }

  try {
    exporting.value = true
    const blob = await api.exportCollection(info.value.id)
    console.log('导出的 Blob:', blob)
    // 从 Content-Disposition 提取文件名，或使用默认名称
    const safeName = (info.value.name || 'collection').replace(/[\\/:*?"<>|]/g, '_')
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const filename = `collection_${safeName}_${dateStr}.md`
    api.downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(`导出失败: ${e.message}`)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="collection-detail-page" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="nav-header">
      <el-page-header :icon="ArrowLeft" @back="router.back()">
        <template #content>
          <span class="header-title">合集详情</span>
        </template>
        <template #extra>
          <el-button :icon="Download" :loading="exporting" @click="exportCollection" class="action-btn" round>
            导出 Markdown
          </el-button>
          <el-button type="primary" :icon="Reading" @click="startLearning" class="action-btn" round>
            AI 学习辅导
          </el-button>
        </template>
      </el-page-header>
    </div>

    <div class="content-wrapper">
      <!-- 头部概览卡片 -->
      <div class="hero-card compact">
        <div class="hero-content">
          <div class="hero-icon">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="hero-info">
             <div class="hero-header-row">
                <h1 class="title">{{ info.name }}</h1>
                <div class="meta-tags">
                  <el-tag effect="plain" round size="small" class="meta-tag">
                    {{ info.analysisResults?.length || 0 }} 个问题
                  </el-tag>
                  <span class="update-time" v-if="info.updatedAt">
                    更新于 {{ info.updatedAt }}
                  </span>
                </div>
             </div>
            <p class="description">{{ info.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>

      <!-- 列表区域 -->
      <div class="list-section relative">
        <div class="section-header">
          <h3>收录问题列表</h3>
          <span class="subtitle">点击卡片查看详情</span>
        </div>
        
        <div v-if="info.analysisResults?.length" class="list-container">
          <AnimatedList 
            :items="info.analysisResults"
            @itemSelected="(item) => viewOriginal(item)"
          >
            <template #default="{ item, index, isSelected }">
              <div 
                class="problem-card"
                :class="{ 'is-active': isSelected }"
              >
                <div class="card-index">{{ String(index + 1).padStart(2, '0') }}</div>
                <div class="card-body">
                  <h4 class="problem-statement">{{ item.problemStatement }}</h4>
                </div>
                <div class="card-actions" @click.stop>
                  <el-button 
                    circle 
                    text 
                    type="danger" 
                    :icon="Delete" 
                    class="delete-btn"
                    @click="confirmRemove(item)"
                  />
                </div>
              </div>
            </template>
          </AnimatedList>
        </div>
        
        <div class="empty-state" v-else>
          <el-empty description="合集暂为空，去分析页添加一些问题吧" />
        </div>
      </div>
    </div>

    <!-- 题库生成加载弹窗 -->
    <QuizLoadingModal
      v-model="showQuizLoading"
      :collection-name="info.name"
      :estimated-seconds="estimatedSeconds"
      :api-completed="apiCompleted"
      @completed="onQuizCompleted"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

// CSS Variables for local theme
.collection-detail-page {
  --page-padding: 32px;
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
  overflow: hidden;
}

.nav-header {
  padding: 12px var(--page-padding);
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(20px) saturate(180%);
  transition: all 0.3s ease;
  
  .header-title {
    font-weight: 600;
    font-size: 18px;
    color: var(--el-text-color-primary);
    letter-spacing: -0.5px;
  }

  /* Customize el-page-header to remove default divider if needed */
  :deep(.el-page-header__left) {
    margin-right: 16px;
  }

  .action-btn {
    font-weight: 500;
    padding: 8px 20px;
    height: 36px;
    transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
    }
    
    &:active {
      transform: translateY(0);
    }
  }
}

html.dark .nav-header {
  background: rgba(28, 28, 30, 0.75);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.content-wrapper {
  padding: var(--page-padding);
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

// Hero Card Styles
.hero-card {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 24px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light);

  &.compact {
    .hero-content {
      align-items: center;
    }
    .hero-icon {
      width: 48px;
      height: 48px;
      font-size: 24px;
      border-radius: 12px;
    }
    .title {
      font-size: 20px;
      margin: 0;
    }
    .description {
      margin: 8px 0 0 0;
      font-size: 14px;
      line-height: 1.5;
    }
  }

  .hero-content {
    position: relative;
    z-index: 2;
    display: flex;
    gap: 16px;
  }

  .hero-icon {
    width: 64px;
    height: 64px;
    background: var(--el-color-primary-light-9);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    color: var(--el-color-primary);
    flex-shrink: 0;
  }

  .hero-info {
    flex: 1;

    .hero-header-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .title {
      font-size: 24px;
      margin: 0 0 8px 0;
      color: var(--el-text-color-primary);
      font-weight: 700;
    }

    .description {
      font-size: 14px;
      color: var(--el-text-color-regular);
      margin: 0 0 16px 0;
      line-height: 1.6;
      max-width: 800px;
    }

    .meta-tags {
      display: flex;
      align-items: center;
      gap: 12px;

      .update-time {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}

// List Section Styles
.list-section {
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    
    .section-header {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 24px;
    flex-shrink: 0;

    h3 {
      font-size: 20px;
      margin: 0;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .subtitle {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  .list-container {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    overflow-x: hidden;
  }
}

.problem-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: var(--transition-base);
  position: relative;

  &.is-active, &:hover {
    background-color: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);

    .card-actions .delete-btn {
      opacity: 1;
    }
  }

  .card-index {
    font-size: 14px;
    font-family: monospace;
    font-weight: 700;
    color: var(--el-text-color-placeholder);
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    transition: var(--transition-base);
    flex-shrink: 0;
  }

  .card-body {
    flex: 1;
    min-width: 0; // Fix truncate inside flex
    padding-top: 0;

    .problem-statement {
      font-size: 15px;
      font-weight: 500;
      color: var(--el-text-color-primary);
      margin: 0;
      line-height: 1.5;
    }
  }

  .card-actions {
    align-self: center;
    
    .delete-btn {
      opacity: 0;
      transition: opacity 0.2s;
      color: var(--el-color-danger); /* Ensure icon color is visible */
      
      &:hover {
        background-color: var(--el-color-danger-light-9);
      }

      /* Force icon size and visibility */
      :deep(.el-icon) {
        font-size: 16px;
        vertical-align: middle;
      }
    }
  }
}

// Transitions
.list-enter-active,
.list-leave-active {
  transition: all 0.4s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

// Responsive
@media (max-width: 768px) {
  .collection-detail-page {
    --page-padding: 16px;
  }
  
  .hero-card {
    padding: 24px;
    flex-direction: column;
    
    .hero-content {
      flex-direction: column;
      align-items: flex-start;
    }
    
    .hero-icon {
      width: 60px;
      height: 60px;
      font-size: 30px;
    }
    
    .title {
      font-size: 24px !important;
    }
  }

  .problem-card {
    padding: 16px;
    gap: 12px;
    
    .card-index {
      width: 32px;
      height: 32px;
      font-size: 14px;
    }
    
    .delete-btn {
      opacity: 1; // Always show on mobile
    }
  }
}
</style>
