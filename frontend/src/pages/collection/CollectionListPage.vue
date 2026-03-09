<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Folder, Delete, Edit, MagicStick, More } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../../components/CustomScroll.vue'
import AchievementNotification from '../../components/AchievementNotification.vue'
import QuickCreateDialog from '../../components/collection/QuickCreateDialog.vue'

const router = useRouter()
const collections = ref([])
const loading = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const quickCreateDialogVisible = ref(false)
const createForm = ref({ name: '', description: '' })
const editForm = ref({ id: null, name: '', description: '' })

// 成就弹窗
const achievementDialogVisible = ref(false)
const newlyUnlockedAchievements = ref([])

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const res = await api.getCollectionList()
    collections.value = res.list || []
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/collections/${id}`)
}

async function handleCreate() {
  if (!createForm.value.name) return
  try {
    const result = await api.createCollection(createForm.value)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    createForm.value = { name: '', description: '' }
    fetchList()

    // 检查是否有新解锁的成就
    if (result && result.newlyUnlockedAchievements && result.newlyUnlockedAchievements.length > 0) {
      newlyUnlockedAchievements.value = result.newlyUnlockedAchievements
      setTimeout(() => {
        achievementDialogVisible.value = true
      }, 500)
    }
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

function handleAchievementClose() {
  achievementDialogVisible.value = false
  newlyUnlockedAchievements.value = []
}

function openEditDialog(e, item) {
  e.stopPropagation()
  editForm.value = { ...item }
  editDialogVisible.value = true
}

async function handleEdit() {
  if (!editForm.value.name) return
  try {
    await api.updateCollection(editForm.value.id, {
        name: editForm.value.name,
        description: editForm.value.description
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

function confirmDelete(e, id) {
  e.stopPropagation()
  ElMessageBox.confirm('确定要删除这个合集吗？',
    '提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await api.deleteCollection(id)
      ElMessage.success('已删除')
      fetchList()
    })
}

// 处理智能创建成功
function handleQuickCreateSuccess(result) {
  fetchList()
  if (result && result.newlyUnlockedAchievements && result.newlyUnlockedAchievements.length > 0) {
    newlyUnlockedAchievements.value = result.newlyUnlockedAchievements
    setTimeout(() => {
      achievementDialogVisible.value = true
    }, 500)
  }
}

// 获取掌握度等级样式
function getMasteryClass(mastery) {
  if (mastery >= 80) return 'high'
  if (mastery >= 50) return 'medium'
  return 'low'
}

// 模拟获取合集的掌握度（实际应从API获取）
function getCollectionMastery(item) {
  // 这里使用模拟数据，实际应根据item数据计算
  return Math.floor(Math.random() * 100)
}

// 获取合集标签（模拟）
function getCollectionTags(item) {
  const tags = ['React', 'Vue', 'TypeScript', 'Node.js', 'Docker', 'K8s', 'GraphQL', 'Microservices']
  return tags.slice(0, Math.floor(Math.random() * 4) + 2)
}

// 获取文件夹图标
function getFolderIcon(name) {
  const icons = {
    'React': '⚛️',
    'Vue': '💚',
    'Docker': '🐳',
    'K8s': '☸️',
    'AI': '🤖',
    'Security': '🛡️',
    'Writing': '📝',
    'Architecture': '🏗️',
    'DevOps': '🚢',
    'default': '📁'
  }
  
  for (const key in icons) {
    if (name.toLowerCase().includes(key.toLowerCase())) {
      return icons[key]
    }
  }
  return icons.default
}
</script>

<template>
  <div class="page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="title-group">
        <h1>知识合集</h1>
        <p>整理和掌握你在专业领域的知识</p>
      </div>
      <div class="actions-group">
        <el-button class="btn-text" @click="quickCreateDialogVisible = true">
          <el-icon><MagicStick /></el-icon>
          智能创建
        </el-button>
      </div>
    </div>

    <!-- Grid Container -->
    <div v-loading="loading" class="grid-container">
      <CustomScroll>
        <div class="cards-wrapper">
          <div class="grid">
            <!-- Create New Card -->
            <div class="new-collection-card" @click="createDialogVisible = true">
              <div class="plus-icon">+</div>
              <div class="create-text">创建合集</div>
            </div>

            <!-- Collection Cards -->
            <div 
              v-for="item in collections" 
              :key="item.id" 
              class="collection-card glass"
              @click="goDetail(item.id)"
            >
              <div class="card-top">
                <div class="folder-icon">{{ getFolderIcon(item.name) }}</div>
                <div class="options-dots" @click="(e) => { e.stopPropagation(); }">
                  <el-dropdown trigger="click">
                    <span class="dots">•••</span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="(e) => openEditDialog(e, item)">
                          <el-icon><Edit /></el-icon> 编辑
                        </el-dropdown-item>
                        <el-dropdown-item @click="(e) => confirmDelete(e, item.id)" divided>
                          <el-icon><Delete /></el-icon> 删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
              
              <div class="card-info">
                <div class="collection-name">{{ item.name }}</div>
                <div class="collection-meta">
                  <span>{{ item.count || 0 }} 个文件</span>
                  <span>更新于 {{ item.updatedTime ? new Date(item.updatedTime).toLocaleDateString() : '刚刚' }}</span>
                </div>
              </div>

              <!-- Topic Tags -->
              <div class="topic-tags">
                <span 
                  v-for="tag in getCollectionTags(item).slice(0, 4)" 
                  :key="tag" 
                  class="tag-small"
                >
                  {{ tag }}
                </span>
              </div>

              <!-- Mastery Progress -->
              <div class="mastery-progress-container">
                <div class="progress-label">
                  <span>掌握程度</span>
                  <span :class="getMasteryClass(getCollectionMastery(item))">
                    {{ getCollectionMastery(item) }}%
                  </span>
                </div>
                <div class="progress-bar-bg">
                  <div 
                    class="progress-bar-fill" 
                    :class="getMasteryClass(getCollectionMastery(item))"
                    :style="{ width: getCollectionMastery(item) + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </CustomScroll>

      <el-empty v-if="!loading && collections.length === 0" description="暂无合集" />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="createDialogVisible" title="新建合集" width="400px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="例如：React Hooks" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑合集" width="400px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" placeholder="例如：React Hooks" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Achievement Notification -->
    <AchievementNotification
      v-model:visible="achievementDialogVisible"
      :achievements="newlyUnlockedAchievements"
      @close="handleAchievementClose"
    />

    <!-- Quick Create Dialog -->
    <QuickCreateDialog
      v-model:visible="quickCreateDialogVisible"
      @created="handleQuickCreateSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
// Import nebula theme variables
@use '../../styles/nebula-theme.scss' as *;
// Import nebula theme variables
@use '../../styles/nebula-theme.scss' as *;

.page-container {
  padding: var(--space-s) var(--space-l) var(--space-l) var(--space-l);
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
}

.grid-container {
  flex: 1;
  overflow: hidden;
  padding: 4px;
}

.cards-wrapper {
  padding-right: 12px;
  overflow-x: hidden;
}

// Page Header
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-m);
  flex-wrap: wrap;
  gap: var(--space-s);
  padding-top: 0;
}

.title-group h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.title-group p {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.actions-group {
  display: flex;
  gap: 12px;
}

// Grid Layout
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-m);
}

// Glass Effect Base
.glass {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
}

// New Collection Card
.new-collection-card {
  border-radius: var(--radius-card);
  border: 2px dashed rgba(255, 248, 245, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-secondary);
  transition: all 0.2s;
  cursor: pointer;
  min-height: 100px;
  padding: var(--space-s) var(--space-m);
}

.new-collection-card:hover {
  border-color: rgba(204, 102, 51, 0.5);
  background: rgba(204, 102, 51, 0.05);
  color: var(--text-primary);
}

.plus-icon {
  font-size: 32px;
  font-weight: 300;
}

.create-text {
  font-size: 14px;
}

// Collection Card
.collection-card {
  border-radius: var(--radius-card);
  padding: var(--space-m);
  transition: all 0.2s;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 100px;
}

.collection-card:hover {
  transform: translateY(-4px);
  background: var(--glass-surface-hover);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.folder-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 248, 245, 0.05);
  border: 1px solid rgba(255, 248, 245, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.options-dots {
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
}

.dots {
  font-size: 16px;
  letter-spacing: 2px;
}

.collection-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.collection-meta {
  font-size: 13px;
  color: var(--text-tertiary);
  display: flex;
  gap: 12px;
}

// Topic Tags
.topic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-small {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(255, 248, 245, 0.05);
  color: var(--text-secondary);
  border: 1px solid rgba(255, 248, 245, 0.05);
  transition: all 0.2s;
}

.tag-small:hover {
  background: rgba(204, 102, 51, 0.1);
  border-color: rgba(204, 102, 51, 0.2);
  color: var(--accent-tertiary);
}

// Mastery Progress
.mastery-progress-container {
  margin-top: auto;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.progress-label .low {
  color: var(--mastery-low);
}

.progress-label .medium {
  color: var(--mastery-med);
}

.progress-label .high {
  color: var(--mastery-high);
}

.progress-bar-bg {
  height: 6px;
  background: rgba(255, 248, 245, 0.05);
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: var(--accent-primary);
  border-radius: 3px;
  box-shadow: 0 0 10px var(--accent-glow-soft);
  transition: width 0.5s ease;
}

.progress-bar-fill.low {
  background: var(--mastery-low);
  box-shadow: 0 0 10px var(--mastery-low-glow);
}

.progress-bar-fill.medium {
  background: var(--mastery-med);
  box-shadow: 0 0 10px var(--mastery-med-glow);
}

.progress-bar-fill.high {
  background: var(--mastery-high);
  box-shadow: 0 0 10px var(--mastery-high-glow);
}

// Button Styles
.btn-text {
  color: var(--text-primary);
  background: transparent;
  border: 1px solid rgba(255, 248, 245, 0.2);
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-text:hover {
  background: rgba(255, 248, 245, 0.05);
  border-color: rgba(255, 248, 245, 0.4);
}

.btn-primary {
  background: var(--accent-primary);
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-primary:hover {
  background: var(--accent-secondary);
  transform: scale(1.02);
  box-shadow: 0 4px 12px var(--accent-glow-soft);
}

// Responsive
@media (max-width: 768px) {
  .page-container {
    padding: var(--space-m);
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions-group {
    width: 100%;
    flex-direction: column;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .collection-card {
    min-height: 140px;
  }
}

@media (max-width: 480px) {
  .title-group h1 {
    font-size: 24px;
  }
}
</style>
