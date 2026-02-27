<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Folder, Delete, Edit, MagicStick } from '@element-plus/icons-vue'
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
    createForm.value = { name: '', description: '' } // Reset form
    fetchList()

    // 检查是否有新解锁的成就
    if (result && result.newlyUnlockedAchievements && result.newlyUnlockedAchievements.length > 0) {
      newlyUnlockedAchievements.value = result.newlyUnlockedAchievements
      // 延迟显示成就弹窗，让用户先看到创建成功的提示
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
  e.stopPropagation() // 防止触发卡片点击
  ElMessageBox.confirm('确定要删除这个合集吗？',
    '提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await api.deleteCollection(id)
      ElMessage.success('已删除')
      fetchList() // 刷新
    })
}

// 处理智能创建成功
function handleQuickCreateSuccess(result) {
  fetchList() // 刷新列表
  // 检查是否有新解锁的成就
  if (result && result.newlyUnlockedAchievements && result.newlyUnlockedAchievements.length > 0) {
    newlyUnlockedAchievements.value = result.newlyUnlockedAchievements
    setTimeout(() => {
      achievementDialogVisible.value = true
    }, 500)
  }
}
</script>

<template>
  <div class="page-container">
    <div class="header">
      <div class="title-area">
        <h1>问题合集</h1>
        <p class="subtitle">归纳整理相似问题，构建你的知识库</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" plain :icon="MagicStick" @click="quickCreateDialogVisible = true">
          智能创建
        </el-button>
        <el-button type="primary" :icon="Plus" @click="createDialogVisible = true">
          新建合集
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="grid-container">
      <CustomScroll>
        <div class="cards-wrapper">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in collections" :key="item.id">
              <div class="collection-card" @click="goDetail(item.id)">
                <div class="card-icon">
                  <el-icon>
                    <Folder />
                  </el-icon>
                </div>
                <div class="card-info">
                  <div class="card-title">{{ item.name }}</div>
                  <div class="card-desc">{{ item.description || '暂无描述' }}</div>
                  <div class="card-meta">
                    <span>{{ item.count || 0 }} 个问题</span>
                    <span class="date">{{ item.updatedTime ? new Date(item.updatedTime).toLocaleDateString() : '刚刚'
                      }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <div class="action-btn edit-btn" @click="(e) => openEditDialog(e, item)">
                    <el-icon><Edit /></el-icon>
                  </div>
                  <div class="action-btn delete-btn" @click="(e) => confirmDelete(e, item.id)">
                    <el-icon><Delete /></el-icon>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </CustomScroll>

      <el-empty v-if="!loading && collections.length === 0" description="暂无合集" />
    </div>

    <el-dialog v-model="createDialogVisible" title="新建合集" width="400px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="例如：Java 并发问题" />
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

    <el-dialog v-model="editDialogVisible" title="编辑合集" width="400px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" placeholder="例如：Java 并发问题" />
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

    <!-- 成就解锁弹窗 -->
    <AchievementNotification
      v-model:visible="achievementDialogVisible"
      :achievements="newlyUnlockedAchievements"
      @close="handleAchievementClose"
    />

    <!-- 智能创建弹窗 -->
    <QuickCreateDialog
      v-model:visible="quickCreateDialogVisible"
      @created="handleQuickCreateSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.page-container {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 24px;
  background-color: var(--el-bg-color-page);
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px;
    border-radius: 16px;
  }
}

/* Dark Mode - 三级背景色层级 */
html.dark .page-container {
  background-color: #161616;
}

.grid-container {
  flex: 1;
  overflow: hidden;
  padding: 4px; /* 防止 box-shadow 被截断 */
}

.cards-wrapper {
  padding-right: 12px; /* 预留滚动条空间，防止el-row负margin导致的横向滚动 */
  overflow-x: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  flex-wrap: wrap; /* Wrap on mobile */
  gap: 16px; /* Gap when wrapped */

  h1 {
    font-size: 24px;
    margin: 0 0 8px 0;
    font-weight: 600;
  }

  .subtitle {
    color: var(--el-text-color-secondary);
    margin: 0;
    font-size: 14px;
  }
}

.header-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .page-container {
    border-radius: 20px;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions .el-button {
    width: 100%;
  }

  .collection-card {
    border-radius: 16px;
  }
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 20px;
  }

  .title-area {
    width: 100%;
  }

  .card-actions .action-btn {
    opacity: 1;
  }
}

.collection-card {
  background: #ffffff;
  border: none;
  border-radius: 18px;
  padding: 20px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
}

.card-icon {
  width: 48px;
  height: 48px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  font-size: 24px;
}

.card-info {
  flex: 1;
  overflow: hidden;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px;
  border-radius: 8px;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: var(--el-fill-color-dark);
  }
}

.edit-btn {
  color: var(--el-color-primary);
}

.delete-btn {
  color: var(--el-color-danger);
}

.collection-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);

  .action-btn {
    opacity: 1;
  }
}

/* Dark Mode Support - 三级背景色层级 */
html.dark .collection-card {
  background: #161616;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

html.dark .collection-card:hover {
  background: #25252a;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
}
</style>
