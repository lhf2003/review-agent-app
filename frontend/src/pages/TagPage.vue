<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const auth = useAuthStore()
const loading = ref(false)

// 数据源
const mainTags = ref([])
const subTags = ref([])
const relations = ref([])

// 选择状态
const selectedMainId = ref(null)

// 创建/重命名主标签
const createDialog = ref(false)
const createForm = ref({ name: '' })
const renameDialog = ref(false)
const renameForm = ref({ id: null, name: '' })

// 子标签创建/重命名
const subCreateDialog = ref(false)
const subCreateForm = ref({ name: '' })
const subRenameDialog = ref(false)
const subRenameForm = ref({ id: null, name: '' })

// 过滤
const searchSub = ref('')

// 加载方法
async function loadMain() {
  const resp = await api.getMainTagList(auth.userId)
  mainTags.value = resp?.data || resp || []
  if (!selectedMainId.value && mainTags.value.length) selectedMainId.value = mainTags.value[0].id
}
async function loadSub() {
  const resp = await api.getSubTagList(auth.userId)
  subTags.value = resp?.data || resp || []
}
async function loadRelation() {
  const mid = selectedMainId.value ?? undefined
  const resp = await api.getTagRelations(auth.userId, mid)
  relations.value = resp?.data || resp || []
}
async function loadAll() {
  try {
    loading.value = true
    await loadMain()
    await Promise.all([loadSub(), loadRelation()])
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

// 关联数据派生
const associatedSubTags = computed(() => {
  const rel = relations.value || []
  return Array.isArray(rel) ? rel : []
})

const availableSubTags = computed(() => {
  const usedIds = new Set(associatedSubTags.value.map(t => t.id))
  return subTags.value.filter(t => !usedIds.has(t.id) && (!searchSub.value || t.name?.toLowerCase().includes(searchSub.value.toLowerCase())))
})

const draggingFromAvailable = ref(false)
const draggingFromAssociated = ref(false)
const isOverAssociated = ref(false)
const isOverAvailable = ref(false)
function onDragStartFromAvailable(st, e) {
  draggingFromAvailable.value = true
  try { e.dataTransfer.setData('application/json', JSON.stringify({ id: st.id })) } catch {}
}
function onDragStartFromAssociated(st, e) {
  draggingFromAssociated.value = true
  try { e.dataTransfer.setData('application/json', JSON.stringify({ id: st.id })) } catch {}
}
function onDragEndFromAvailable() { draggingFromAvailable.value = false }
function onDragEndFromAssociated() { draggingFromAssociated.value = false }
function onDragOverAssociated(e) { e.preventDefault(); isOverAssociated.value = true }
function onDragLeaveAssociated() { isOverAssociated.value = false }
function onDropToAssociated(e) {
  e.preventDefault(); isOverAssociated.value = false
  draggingFromAvailable.value = false
  try {
    const d = JSON.parse(e.dataTransfer.getData('application/json') || '{}')
    const tag = availableSubTags.value.find(x => x.id === d.id)
    if (tag) attachSub(tag)
  } catch {}
}
function onDragOverAvailable(e) { e.preventDefault(); isOverAvailable.value = true }
function onDragLeaveAvailable() { isOverAvailable.value = false }
function onDropToAvailable(e) {
  e.preventDefault(); isOverAvailable.value = false
  draggingFromAssociated.value = false
  try {
    const d = JSON.parse(e.dataTransfer.getData('application/json') || '{}')
    const tag = associatedSubTags.value.find(x => x.id === d.id)
    if (tag) detachSub(tag)
  } catch {}
}

// 主标签操作
function openCreateMain() { createDialog.value = true }
async function createMain() {
  if (!createForm.value.name.trim()) { ElMessage.warning('请输入主标签名称'); return }
  await api.addMainTag({ name: createForm.value.name.trim(), userId: auth.userId })
  ElMessage.success('创建成功')
  createDialog.value = false
  createForm.value = { name: '' }
  await loadMain(); await loadRelation()
}
function openRenameMain(mt) { renameForm.value = { id: mt.id, name: mt.name }; renameDialog.value = true }
async function doRenameMain() {
  if (!renameForm.value.name.trim()) { ElMessage.warning('请输入新名称'); return }
  await api.updateMainTag({ id: renameForm.value.id, name: renameForm.value.name.trim(), userId: auth.userId })
  ElMessage.success('已重命名')
  renameDialog.value = false
  await loadMain()
}
async function doDeleteMain(mt) {
  await ElMessageBox.confirm(`确认删除主标签「${mt.name}」？`, '提示', { type: 'warning' })
  await api.deleteMainTag(auth.userId, mt.id)
  ElMessage.success('删除成功')
  await loadMain(); await loadRelation()
}

// 子标签操作
function openCreateSub() { subCreateDialog.value = true }
async function createSub() {
  if (!subCreateForm.value.name.trim()) { ElMessage.warning('请输入子标签名称'); return }
  await api.addSubTag({ name: subCreateForm.value.name.trim(), userId: auth.userId })
  ElMessage.success('创建成功')
  subCreateDialog.value = false
  subCreateForm.value = { name: '' }
  await loadSub()
}
// 【V2 优化】 点击子标签名称弹出重命名
function openRenameSub(st) {
  subRenameForm.value = { id: st.id, name: st.name };
  subRenameDialog.value = true
}
async function doRenameSub() {
  if (!subRenameForm.value.name.trim()) { ElMessage.warning('请输入新名称'); return }
  await api.updateSubTag({ id: subRenameForm.value.id, name: subRenameForm.value.name.trim(), userId: auth.userId })
  ElMessage.success('已重命名')
  subRenameDialog.value = false
  await loadSub(); await loadRelation()
}
async function doDeleteSub(st) {
  await ElMessageBox.confirm(`确认删除子标签「${st.name}」？`, '提示', { type: 'warning' })
  await api.deleteSubTag(auth.userId, st.id)
  ElMessage.success('删除成功')
  await loadSub(); await loadRelation()
}

// 关联/解除关联
async function attachSub(st) {
  if (!selectedMainId.value) { ElMessage.warning('请先选择主标签'); return }
  await api.addTagRelation({ mainTagId: selectedMainId.value, subTagId: st.id, userId: auth.userId })
  ElMessage.success('关联成功')
  await loadRelation()
}
async function detachSub(st) {
  if (!selectedMainId.value) { ElMessage.warning('请先选择主标签'); return }
  await api.deleteTagRelation({ mainTagId: selectedMainId.value, subTagId: st.id, userId: auth.userId })
  ElMessage.success('解除关联成功')
  await loadRelation()
}

function onSelectMain(mt) {
  selectedMainId.value = mt.id
  loadRelation()
}

onMounted(loadAll)
</script>

<template>
  <div class="tag-page-container">
    <el-row :gutter="20" class="full-height-row">

      <el-col :span="6" class="full-height-col">
        <el-card shadow="hover" class="region-card flex-fill-card">
          <template #header>
            <div class="region-header">
              <span class="region-title">🏷️ 主标签</span>
              <el-button type="primary" size="small" @click="openCreateMain">新增</el-button>
            </div>
          </template>

          <div v-loading="loading" class="main-list-wrapper">
            <div v-if="mainTags.length" class="main-list">
              <div v-for="mt in mainTags" :key="mt.id" :class="['main-item', { active: selectedMainId === mt.id }]"
                @click="onSelectMain(mt)">
                <div class="main-name">{{ mt.name }}</div>
                <div class="main-meta">
                  <el-button text size="small" @click.stop="openRenameMain(mt)">编辑</el-button>
                  <el-button text size="small" type="danger" @click.stop="doDeleteMain(mt)">删除</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无主标签" :image-size="80" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="10" class="full-height-col">
        <el-card shadow="hover" class="region-card flex-fill-card">
          <template #header>
            <div class="region-header">
              <span class="region-title">🔗 当前主标签的子标签</span>
            </div>
          </template>

          <div class="region-content-scroll">
            <div class="region-body">
              <div style="font-weight:600; margin-bottom: 12px;">已关联 ({{ associatedSubTags.length }})</div>
              <div v-loading="loading" :class="['sub-list-associated','droppable', { 'droppable--over': isOverAssociated, 'drag-target': draggingFromAvailable }]" @dragover="onDragOverAssociated" @dragleave="onDragLeaveAssociated" @drop="onDropToAssociated">
                <el-card v-for="st in associatedSubTags" :key="st.id" shadow="never" class="sub-item associated-item" :draggable="true" @dragstart="onDragStartFromAssociated(st, $event)" @dragend="onDragEndFromAssociated">
                  <div class="sub-name">{{ st.name }}</div>
                </el-card>
                <div v-if="draggingFromAvailable" class="drag-hint">拖拽</div>
                <el-empty v-if="!associatedSubTags.length" description="尚未关联任何子标签" :image-size="60" />
              </div>
            </div>

            <el-divider />

            <div class="region-footer">
              <div style="font-weight:600;margin-bottom:12px;">可用子标签 ({{ availableSubTags.length }})</div>
              <el-input v-model="searchSub" placeholder="搜索可用子标签..." prefix-icon="Search" clearable
                style="margin-bottom:12px;" />
              <div :class="['sub-list','droppable', { 'droppable--over': isOverAvailable, 'drag-target': draggingFromAssociated }]" @dragover="onDragOverAvailable" @dragleave="onDragLeaveAvailable" @drop="onDropToAvailable">
                <el-card v-for="st in availableSubTags" :key="st.id" shadow="hover" class="sub-item available-item" :draggable="true" @dragstart="onDragStartFromAvailable(st, $event)" @dragend="onDragEndFromAvailable">
                  <div class="sub-name">{{ st.name }}</div>
                </el-card>
                <div v-if="draggingFromAssociated" class="drag-hint">拖拽</div>
                <el-empty v-if="!availableSubTags.length" description="暂无可用子标签" :image-size="60" />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8" class="full-height-col">
        <el-card shadow="hover" class="region-card flex-fill-card">
          <template #header>
            <div class="region-header">
              <span class="region-title">📚 通用子标签库</span>
              <el-button type="primary" size="small" @click="openCreateSub">新增</el-button>
            </div>
          </template>
          <div class="region-content-scroll">
            <div v-loading="loading" class="sub-list library-list-wrapper">
              <!-- 一行两卡片：外层用 grid 控制 -->
              <div class="two-per-row">
                <el-card
                  v-for="st in subTags"
                  :key="st.id"
                  shadow="hover"
                  class="sub-item"
                >
                  <div class="sub-item-content">
                    <div class="sub-name is-editable" @click="openRenameSub(st)">{{ st.name }}</div>
                    <el-button text size="small" type="danger" @click="doDeleteSub(st)">删除</el-button>
                  </div>
                </el-card>
              </div>
              <el-empty v-if="!subTags.length" description="暂无子标签" :image-size="80" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="createDialog" title="创建主标签" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item>
          <el-button @click="createDialog = false">取消</el-button>
          <el-button type="primary" @click="createMain">创建</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="renameDialog" title="重命名主标签" width="380px" align-center>
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="renameForm.name" />
        </el-form-item>
        <el-form-item>
          <el-button @click="renameDialog = false">取消</el-button>
          <el-button type="primary" @click="doRenameMain">保存</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="subCreateDialog" title="创建子标签" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="subCreateForm.name" />
        </el-form-item>
        <el-form-item>
          <el-button @click="subCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="createSub">创建</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="subRenameDialog" title="重命名子标签" width="380px" align-center>
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="subRenameForm.name" />
        </el-form-item>
        <el-form-item>
          <el-button @click="subRenameDialog = false">取消</el-button>
          <el-button type="primary" @click="doRenameSub">保存</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 1. 标签栏长度填充满 (需要父容器支持) */
/* 假设 tag-page-container 的父级或它本身的高度是确定的（例如 viewport 高度减去头部） */
.tag-page-container {
  padding: 10px;
  height: calc(100vh - 70px);
  /* 示例：假设页面高度 - 顶部导航栏高度 */
}

.full-height-row {
  height: 100%;
}

.full-height-col {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.region-card {
  min-height: 0;
}

.region-header { 
  display:flex; 
  align-items:center; 
  justify-content:space-between;
}

.region-title { 
  font-weight:700; 
  font-size: 18px;
}

.flex-fill-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 覆盖 ElCard 默认的 body 样式，使其填充剩余空间 */
.flex-fill-card :deep(.el-card__body) {
  flex-grow: 1;
  overflow: hidden;
  /* 确保内容不会溢出卡片 */
  padding: 15px;
}

/* 允许内容区域滚动 */
.region-content-scroll {
  height: 100%;
  overflow-y: auto;
  padding-right: 5px;
  /* 留出滚动条空间 */
}

/* 主标签列表 A 区 */
.main-list-wrapper {
  height: 100%;
  /* 继承 el-card__body 的高度 */
  overflow-y: auto;
}

.main-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.main-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 10px 15px;
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--el-border-radius-base);
  transition: all 0.2s ease-in-out;
}

.main-item:hover {
  background-color: var(--el-fill-color-light);
}

.main-item.active {
  border-color: var(--el-color-primary);
  border-left: 4px solid var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
  padding-left: 12px;
}

.main-name {
  font-weight: 600;
  flex-grow: 1;
}

.main-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 子标签列表 B, C 区 */
.sub-list-associated {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
}

.sub-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 8px;
}

.library-list-wrapper {
  display: flex;
  /* 确保 empty 居中 */
  flex-direction: column;
}

/* C 区：通用子标签库一行两卡片 */
.two-per-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

/* 2. 子标签卡片优化 */
.sub-item {
  padding: 2px 5px;
}

.sub-item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.sub-name {
  font-weight: 500;
  flex-grow: 1;
}

/* C 区：通用子标签库 - 点击文字可编辑 */
.sub-name.is-editable {
  cursor: pointer;
}

.sub-name.is-editable:hover {
  color: var(--el-color-primary);
  text-decoration: underline;
}

/* B 区：已关联/可用子标签 (保持不变，因为 B 区的功能是关联/解除关联) */
.associated-item,
.available-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 保证卡片内容（el-card__body）为同行布局 */
.associated-item :deep(.el-card__body),
.available-item :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
}
  .droppable {
    border: 2px dashed transparent;
    border-radius: var(--el-border-radius-base);
    transition: border-color .2s ease, background-color .2s ease;
  }
  .droppable--over {
    border-color: var(--el-color-primary);
    background-color: var(--el-color-primary-light-9);
  }
</style>

.droppable { position: relative; }
.droppable.drag-target .sub-item { filter: blur(4px); opacity: 0.5; pointer-events: none; }
.drag-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
  font-weight: 800;
  letter-spacing: 2px;
  color: var(--el-color-primary);
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
  background: rgba(255,255,255,0.65);
  backdrop-filter: blur(2px);
  border: 2px dashed var(--el-color-primary);
}
