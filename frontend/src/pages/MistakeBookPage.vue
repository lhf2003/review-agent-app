<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, RefreshRight, Check, FolderChecked, Delete, SuccessFilled, WarningFilled, Collection, Edit, Loading, PriceTag, Clock, MoreFilled, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../components/CustomScroll.vue'
import MistakeDrawer from '../components/quiz/MistakeDrawer.vue'
import ReviewCard from '../components/quiz/ReviewCard.vue'
import { api } from '../api/http'

/**
 * 错题本页面
 * 查看和复习历史错题
 */
const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')

// 数据
const mistakes = ref([])
const selectedMistakes = ref(new Set())
const stats = ref({
  total: 0,
  unmastered: 0,
  mastered: 0
})

// 筛选方式：'all' | 'unmastered' | 'mastered'
const filterMode = ref('all')

// 搜索状态
const isSearching = ref(false)

// 复习推荐数据
const reviewRecommendations = ref([])
const showRecommendations = ref(true)

// 监听错题数据变化，生成复习推荐
watch(mistakes, (newMistakes) => {
  generateReviewRecommendations(newMistakes)
}, { immediate: true })

// 生成复习推荐（基于遗忘曲线）
function generateReviewRecommendations(mistakeData) {
  const now = new Date()
  const recommendations = []

  mistakeData.forEach(mistake => {
    // 只推荐未掌握的错题
    if (mistake.mastered) return

    const lastMistakeDate = new Date(mistake.lastMistakeTime)
    const daysSinceMistake = Math.floor((now - lastMistakeDate) / (1000 * 60 * 60 * 24))

    // 艾宾浩斯遗忘曲线
    // 1天、3天、7天、15天、30天
    const reviewIntervals = [1, 3, 7, 15, 30]
    let nextReviewDays = 1
    let priority = 5

    for (let i = 0; i < reviewIntervals.length; i++) {
      if (daysSinceMistake < reviewIntervals[i]) {
        nextReviewDays = reviewIntervals[i] - daysSinceMistake
        priority = 5 - i
        break
      }
    }

    // 计算下次复习日期
    const nextReviewDate = new Date(now)
    nextReviewDate.setDate(nextReviewDate.getDate() + nextReviewDays)

    recommendations.push({
      mistakeId: mistake.id,
      questionId: mistake.questionId,
      questionText: mistake.question,
      knowledgePoint: mistake.knowledgePoint,
      mistakeCount: mistake.mistakeCount,
      lastMistakeTime: mistake.lastMistakeTime,
      nextReviewDate: nextReviewDate.toISOString(),
      priority,
      daysUntilReview: nextReviewDays
    })
  })

  // 按优先级排序
  recommendations.sort((a, b) => {
    if (a.daysUntilReview < 0 && b.daysUntilReview >= 0) return -1
    if (a.daysUntilReview >= 0 && b.daysUntilReview < 0) return 1
    if (a.priority !== b.priority) return b.priority - a.priority
    return a.daysUntilReview - b.daysUntilReview
  })

  // 只显示前5个推荐
  reviewRecommendations.value = recommendations.slice(0, 5)
}

// 错题详情抽屉
const drawerVisible = ref(false)
const currentMistakeId = ref(null)
const currentQuestionId = ref(null)

onMounted(() => {
  fetchMistakes()
})

// 返回习题历史
function goBack() {
  router.push('/quiz-history')
}

// 获取错题列表
async function fetchMistakes() {
  loading.value = true
  try {
    const data = await api.getMistakeList(filterMode.value)
    // 转换数据格式以适配前端组件
    mistakes.value = data.map(m => ({
      id: m.id,
      questionId: m.questionId,
      question: m.questionText,
      questionType: m.questionType,
      options: m.optionsJson ? JSON.parse(m.optionsJson) : [],
      answer: m.correctAnswer,
      explanation: m.explanation,
      knowledgePoint: m.knowledgePoint,
      mistakeCount: m.mistakeCount,
      lastMistakeTime: m.lastMistakeTime,
      mastered: m.mastered,
      createdTime: m.createdTime
    }))
    updateStats(mistakes.value)
  } catch (error) {
    console.error('获取错题列表失败:', error)
    ElMessage.error('加载错题失败')
  } finally {
    loading.value = false
  }
}

// 更新统计信息
function updateStats(mistakeData) {
  stats.value.total = mistakeData.length
  stats.value.unmastered = mistakeData.filter(m => !m.mastered).length
  stats.value.mastered = mistakeData.filter(m => m.mastered).length
}

// 切换筛选模式
async function switchFilter(mode) {
  filterMode.value = mode
  await fetchMistakes()
}

// 获取筛选后的错题列表
const filteredMistakes = computed(() => {
  let result = mistakes.value

  // 应用搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase().trim()
    result = result.filter(m =>
      m.question.toLowerCase().includes(keyword) ||
      m.knowledgePoint?.toLowerCase().includes(keyword)
    )
  }

  // 应用模式筛选
  result = result.filter(m => {
    if (filterMode.value === 'all') return true
    if (filterMode.value === 'unmastered') return !m.mastered
    if (filterMode.value === 'mastered') return m.mastered
    return false
  })

  return result
})

// 搜索错题
function handleSearch() {
  if (!searchKeyword.value.trim()) {
    return
  }
  isSearching.value = true
  // 搜索会自动触发 computed 重新计算
}

// 清空搜索
function clearSearch() {
  searchKeyword.value = ''
  isSearching.value = false
}

// 选择/取消选择错题
const toggleMistake = (mistake) => {
  if (selectedMistakes.value.has(mistake.id)) {
    selectedMistakes.value.delete(mistake.id)
  } else {
    selectedMistakes.value.add(mistake.id)
  }
}

// 是否全部选中
const isAllSelected = computed(() =>
  mistakes.value.length > 0 && selectedMistakes.value.size === mistakes.value.length
)

// 全选/反选
const toggleAll = () => {
  if (isAllSelected.value) {
    selectedMistakes.value.clear()
  } else {
    mistakes.value.forEach(m => selectedMistakes.value.add(m.id))
  }
}

// 标记为已掌握
const markAsMastered = async (mistake) => {
  try {
    await api.markMistakesMastered([mistake.questionId])
    mistake.mastered = true
    ElMessage.success('已标记为掌握')

    // 从列表中移除已掌握的错题
    if (filterMode.value !== 'all') {
      mistakes.value = mistakes.value.filter(m => m.id !== mistake.id)
    }
    // 更新统计
    updateStats(mistakes.value)
  } catch (error) {
    console.error('标记失败:', error)
    ElMessage.error('标记失败')
  }
}

// 刷新错题本
const refreshMistakeBook = () => {
  fetchMistakes()
}

// 查看错题详情
const viewDetail = (mistake) => {
  currentMistakeId.value = mistake.id
  currentQuestionId.value = mistake.questionId
  drawerVisible.value = true
}

// 处理抽屉中的标记掌握回调
const handleMarkedMastered = (questionId) => {
  // 更新本地状态
  const mistake = mistakes.value.find(m => m.questionId === questionId)
  if (mistake) {
    mistake.mastered = true
  }
  // 更新统计
  updateStats(mistakes.value)
}

// 处理抽屉中的删除回调
const handleDeleted = (questionId) => {
  // 从列表中移除
  mistakes.value = mistakes.value.filter(m => m.questionId !== questionId)
  // 更新统计
  updateStats(mistakes.value)
  ElMessage.success('已从错题本移除')
}

// 批量操作
const batchAction = async (action, actionName) => {
  const count = selectedMistakes.value.size
  if (count === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要${actionName}选中的 ${count} 个错题吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 获取选中错题的 questionId 列表
    const selectedMistakeList = mistakes.value.filter(m => selectedMistakes.value.has(m.id))
    const questionIds = selectedMistakeList.map(m => m.questionId)

    if (action === 'delete') {
      await api.deleteMistakes(questionIds)
    } else if (action === 'mastered') {
      await api.markMistakesMastered(questionIds)
    }

    selectedMistakes.value.clear()
    ElMessage.success(`${actionName}成功`)
    fetchMistakes()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error(`${actionName}失败`)
    }
  }
}

// 开始复习（从推荐卡片）
const handleStartReview = (recommendation) => {
  // 打开错题详情抽屉
  currentMistakeId.value = recommendation.mistakeId
  currentQuestionId.value = recommendation.questionId
  drawerVisible.value = true
}

// 忽略推荐
const handleDismissRecommendation = (recommendation) => {
  reviewRecommendations.value = reviewRecommendations.value.filter(
    r => r.mistakeId !== recommendation.mistakeId
  )
  ElMessage.success('已忽略该推荐')
}

// 切换推荐区块显示
const toggleRecommendations = () => {
  showRecommendations.value = !showRecommendations.value
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  return date.toLocaleDateString('zh-CN')
}

// 获取题型标签
const getQuestionTypeLabel = (type) => {
  const labels = {
    'single_choice': '单选',
    'multiple_choice': '多选',
    'true_false': '判断',
    'fill_blank': '填空',
    'code_snippet': '代码'
  }
  return labels[type] || '未知'
}

// 获取题型标签类型
const getQuestionTypeTagType = (type) => {
  const types = {
    'single_choice': 'primary',
    'multiple_choice': 'success',
    'true_false': 'info',
    'fill_blank': 'warning',
    'code_snippet': 'danger'
  }
  return types[type] || ''
}
</script>

<template>
  <div class="mistake-book-page">
    <!-- 页面标题栏 -->
    <div class="page-header-bar">
      <div class="header-left">
        <el-button @click="goBack" link class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回习题
        </el-button>
        <div class="title-section">
          <h1 class="page-title">错题本</h1>
          <p class="page-subtitle">复习和巩固你的薄弱知识点</p>
        </div>
      </div>
      <div class="header-right">
        <el-button
          :icon="RefreshRight"
          @click="refreshMistakeBook"
          :loading="loading"
        >
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-container">
      <div class="stat-card total">
        <div class="stat-icon">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总错题</div>
        </div>
      </div>

      <div class="stat-card unmastered">
        <div class="stat-icon">
          <el-icon><Edit /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.unmastered }}</div>
          <div class="stat-label">待掌握</div>
        </div>
      </div>

      <div class="stat-card mastered">
        <div class="stat-icon">
          <el-icon><SuccessFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.mastered }}</div>
          <div class="stat-label">已掌握</div>
        </div>
      </div>
    </div>

    <!-- 筛选和搜索栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <el-radio-group v-model="filterMode" @change="switchFilter(filterMode)" size="default">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unmastered">未掌握</el-radio-button>
          <el-radio-button value="mastered">已掌握</el-radio-button>
        </el-radio-group>
      </div>

      <div class="search-group">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题目或知识点..."
          :prefix-icon="Search"
          clearable
          @clear="clearSearch"
          @input="handleSearch"
          class="search-input"
        />
      </div>
    </div>

    <!-- 复习推荐区块 -->
    <div v-if="showRecommendations && reviewRecommendations.length > 0" class="recommendations-section">
      <div class="section-header">
        <div class="header-left">
          <el-icon class="section-icon"><TrendCharts /></el-icon>
          <h3 class="section-title">复习推荐</h3>
          <el-tag type="primary" size="small">{{ reviewRecommendations.length }} 题</el-tag>
        </div>
        <el-button text @click="toggleRecommendations">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="recommendations-list">
        <ReviewCard
          v-for="rec in reviewRecommendations"
          :key="rec.mistakeId"
          :recommendation="rec"
          @start-review="handleStartReview"
          @dismiss="handleDismissRecommendation"
        />
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions-bar" v-if="selectedMistakes.size > 0">
      <div class="selection-info">
        已选择 <strong>{{ selectedMistakes.size }}</strong> 个错题
      </div>
      <div class="batch-buttons">
        <el-button
          type="primary"
          :icon="FolderChecked"
          @click="batchAction('mastered', '标记为已掌握')"
          :disabled="loading"
          size="small"
        >
          标记为已掌握
        </el-button>
        <el-button
          type="danger"
          :icon="Delete"
          @click="batchAction('delete', '删除选中')"
          :disabled="loading"
          size="small"
        >
          删除
        </el-button>
      </div>
    </div>

    <!-- 错题列表 -->
    <div class="mistakes-list-wrapper">
      <CustomScroll>
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-else-if="!loading && filteredMistakes.length === 0"
          :description="searchKeyword.trim() ? '未找到相关错题' : '暂无错题记录'"
        >
          <template #image>
            <el-icon :size="80" color="#909399"><WarningFilled /></el-icon>
          </template>
        </el-empty>

        <!-- 错题列表 -->
        <div v-else class="mistakes-list">
          <!-- 全选框 -->
          <div class="select-all-bar" v-if="mistakes.length > 0">
            <el-checkbox
              :model-value="isAllSelected"
              @change="toggleAll"
            >
              全选
            </el-checkbox>
          </div>

          <!-- 错题卡片 -->
          <div
            v-for="mistake in filteredMistakes"
            :key="mistake.id"
            class="mistake-card"
            :class="{
              'is-selected': selectedMistakes.has(mistake.id),
              'is-mastered': mistake.mastered
            }"
          >
            <div class="card-header">
              <div class="header-left">
                <el-checkbox
                  :model-value="selectedMistakes.has(mistake.id)"
                  @change="toggleMistake(mistake)"
                />
                <el-tag
                  :type="getQuestionTypeTagType(mistake.questionType)"
                  size="small"
                >
                  {{ getQuestionTypeLabel(mistake.questionType) }}
                </el-tag>
                <span class="mistake-count-badge">错误 {{ mistake.mistakeCount }} 次</span>
              </div>
              <div class="header-right">
                <el-tag v-if="mistake.mastered" type="success" size="small" effect="plain">
                  <el-icon><SuccessFilled /></el-icon>
                  已掌握
                </el-tag>
                <el-dropdown trigger="click" @command="(cmd) => cmd === 'mastered' ? markAsMastered(mistake) : null">
                  <el-button circle text size="small">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="mastered" :disabled="mistake.mastered">
                        <el-icon><SuccessFilled /></el-icon>
                        标记为已掌握
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <div class="card-content" @click="viewDetail(mistake)">
              <div class="question-text">
                {{ mistake.question }}
              </div>

              <div class="meta-info">
                <div class="knowledge-point" v-if="mistake.knowledgePoint">
                  <el-icon><PriceTag /></el-icon>
                  <span>{{ mistake.knowledgePoint }}</span>
                </div>
                <div class="time-info">
                  <el-icon><Clock /></el-icon>
                  <span>{{ formatTime(mistake.lastMistakeTime) }}</span>
                </div>
              </div>

              <div class="answer-preview" v-if="mistake.explanation">
                <div class="preview-label">解析</div>
                <div class="preview-text">{{ mistake.explanation }}</div>
              </div>
            </div>
          </div>
        </div>
      </CustomScroll>
    </div>

    <!-- 错题详情抽屉 -->
    <MistakeDrawer
      v-model:visible="drawerVisible"
      :mistake-id="currentMistakeId"
      :question-id="currentQuestionId"
      @marked-mastered="handleMarkedMastered"
      @deleted="handleDeleted"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/variables';

.mistake-book-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 20px;
}

// 页面标题栏
.page-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .back-button {
    align-self: flex-start;
    font-size: 14px;
    padding: 4px 8px;

    &:hover {
      background: var(--el-fill-color);
    }
  }

  .title-section {
    .page-title {
      font-size: 28px;
      font-weight: 700;
      color: var(--el-text-color-primary);
      margin: 0 0 8px 0;
    }

    .page-subtitle {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      margin: 0;
    }
  }
}

.header-right {
  display: flex;
  gap: 12px;
}

// 统计卡片
.stats-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    border-color: var(--el-color-primary-light-5);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;
}

.stat-card.total .stat-icon {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.stat-card.unmastered .stat-icon {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.stat-card.mastered .stat-icon {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

// 筛选和搜索栏
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
}

.filter-group {
  flex-shrink: 0;
}

.search-group {
  flex: 1;
  max-width: 400px;

  .search-input {
    width: 100%;
  }
}

// 复习推荐区块
.recommendations-section {
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  padding: 20px;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.section-header .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// 批量操作栏
.batch-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: var(--el-color-primary-light-9);
  border-radius: 12px;
  border: 1px solid var(--el-color-primary-light-7);
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.selection-info {
  font-size: 14px;
  color: var(--el-text-color-primary);

  strong {
    color: var(--el-color-primary);
    font-size: 18px;
    margin: 0 4px;
  }
}

.batch-buttons {
  display: flex;
  gap: 8px;
}

// 错题列表容器
.mistakes-list-wrapper {
  flex: 1;
  min-height: 0;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;
}

.mistakes-list {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.select-all-bar {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 8px;
}

// 错题卡片
.mistake-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }

  &.is-selected {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  &.is-mastered {
    opacity: 0.7;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mistake-count-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-danger);
  padding: 4px 8px;
  background: var(--el-color-danger-light-9);
  border-radius: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-info {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.knowledge-point,
.time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);

  .el-icon {
    font-size: 16px;
  }
}

.knowledge-point {
  color: var(--el-color-primary);
}

.answer-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.preview-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.preview-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: var(--el-text-color-secondary);

  .el-icon {
    color: var(--el-color-primary);
  }
}

// 响应式设计
@media (max-width: 1024px) {
  .stats-container {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 24px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .page-header-bar {
    flex-direction: column;
    gap: 16px;
  }

  .stats-container {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;

    .search-group {
      max-width: none;
    }
  }

  .batch-actions-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;

    .batch-buttons {
      justify-content: center;
    }
  }

  .mistakes-list {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .meta-info {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
