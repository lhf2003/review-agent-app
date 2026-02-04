<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../CustomScroll.vue'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

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

// 预定义一组好看的颜色用于标签左侧
const tagColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#9C27B0', '#009688', '#3F51B5', '#FF9800', '#795548',
  '#607D8B', '#E91E63'
]

function getTagColor(index) {
  return tagColors[index % tagColors.length]
}

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
  const resp = await api.getTagRelations(mid)
  relations.value = resp?.data || resp || []
}
async function loadAll() {
  try {
    loading.value = true
    await loadMain()

    // 如果主标签为空，则不继续加载子标签和关系
    if (!mainTags.value || mainTags.value.length === 0) {
      return
    }

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
function onDragEnterAssociated(e) { e.preventDefault(); isOverAssociated.value = true; try { e.dataTransfer.dropEffect = 'move' } catch {} }
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
function onDragEnterAvailable(e) { e.preventDefault(); isOverAvailable.value = true; try { e.dataTransfer.dropEffect = 'move' } catch {} }
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
  try {
    await ElMessageBox.confirm(`确认删除主标签「${mt.name}」？`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await api.deleteMainTag(mt.id)
    ElMessage.success('删除成功')
    await loadMain(); await loadRelation()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
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
function openRenameSub(st) {
  subRenameForm.value = { id: st.id, name: st.name }
  subRenameDialog.value = true
}
async function doRenameSub() {
  if (!subRenameForm.value.name.trim()) { ElMessage.warning('请输入新名称'); return }
  await api.updateSubTag({ id: subRenameForm.value.id, name: subRenameForm.value.name.trim(), userId: auth.userId })
  ElMessage.success('已重命名')
  subRenameDialog.value = false
  await loadSub(); await loadRelation()
}
async function deleteSubInDialog() {
  const st = { id: subRenameForm.value.id, name: subRenameForm.value.name }
  try {
    await doDeleteSub(st)
    subRenameDialog.value = false
  } catch (e) {}
}
async function doDeleteSub(st) {
  try {
    await ElMessageBox.confirm(`确认删除子标签「${st.name}」？`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await api.deleteSubTag(st.id)
    ElMessage.success('删除成功')
    await loadSub(); await loadRelation()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
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
  <div class="tag-management-pane">
    <el-row :gutter="20" class="full-height-row">

      <el-col :span="6" class="full-height-col">
        <el-card shadow="hover" class="region-card flex-fill-card">
          <template #header>
            <div class="region-header">
              <span class="region-title">🏷️ 主标签</span>
              <el-button type="plain" link size="large" @click="openCreateMain">
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </template>

          <div class="main-list-wrapper" v-loading="loading">
            <CustomScroll>
              <div v-if="mainTags.length" class="main-list">
                <div v-for="(mt, index) in mainTags"
                     :key="mt.id"
                     :class="['main-item', { active: selectedMainId === mt.id }]"
                     :style="{ '--tag-color': getTagColor(index) }"
                     @click="onSelectMain(mt)">
                  <div class="main-name">{{ mt.name }}</div>
                  <div class="main-meta">
                    <el-button text size="small" link @click.stop="openRenameMain(mt)">编辑</el-button>
                    <el-button text size="small" type="danger" link @click.stop="doDeleteMain(mt)">删除</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无主标签" :image-size="80" />
            </CustomScroll>
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
            <CustomScroll>
              <div style="display:flex;flex-direction:column;min-height:100%">
                <div class="region-body">
                  <div style=" margin-bottom: 12px;">已关联 ({{ associatedSubTags.length }})</div>
                  <div v-loading="loading" :class="['sub-list-associated','droppable', { 'droppable--over': isOverAssociated, 'drag-target': draggingFromAvailable, 'empty-container': !associatedSubTags.length }]" @dragover="onDragOverAssociated" @dragenter="onDragEnterAssociated" @dragleave="onDragLeaveAssociated" @drop="onDropToAssociated">
                    <div v-for="st in associatedSubTags" :key="st.id" class="sub-item compact-card" :draggable="true" @dragstart="onDragStartFromAssociated(st, $event)" @dragend="onDragEndFromAssociated">
                      <div class="sub-name">{{ st.name }}</div>
                    </div>
                    <div v-if="draggingFromAvailable" class="drag-hint">关联</div>
                    <el-empty v-if="!associatedSubTags.length" description="尚未关联任何子标签" :image-size="60" class="full-size-empty" />
                  </div>
                </div>

                <el-divider />

                <div class="region-footer">
                  <div style="font-weight:200;margin-bottom:12px;">可用子标签 ({{ availableSubTags.length }})
                    <el-input v-model="searchSub" placeholder="搜索可用子标签..." prefix-icon="Search" clearable style="height:20px; width:45%; margin: 0 auto;" />
                  </div>

                  <div :class="['sub-list','droppable', { 'droppable--over': isOverAvailable, 'drag-target': draggingFromAssociated, 'empty-container': !availableSubTags.length }]" @dragover="onDragOverAvailable" @dragenter="onDragEnterAvailable" @dragleave="onDragLeaveAvailable" @drop="onDropToAvailable">
                    <div v-for="st in availableSubTags" :key="st.id" class="sub-item compact-card" :draggable="true" @dragstart="onDragStartFromAvailable(st, $event)" @dragend="onDragEndFromAvailable">
                      <div class="sub-name">{{ st.name }}</div>
                    </div>
                    <div v-if="draggingFromAssociated" class="drag-hint--cancel">取消关联</div>
                    <el-empty v-if="!availableSubTags.length" description="暂无可用子标签" :image-size="60" class="full-size-empty" />
                  </div>
                </div>
              </div>
            </CustomScroll>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8" class="full-height-col">
        <el-card shadow="hover" class="region-card flex-fill-card">
          <template #header>
            <div class="region-header">
              <span class="region-title">📚 通用子标签库</span>
              <el-button type="plain" link size="large" @click="openCreateSub">
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="region-content-scroll">
            <CustomScroll>
              <div v-loading="loading" class="sub-list library-list-wrapper">
                <div class="two-per-row">
                  <el-card
                    v-for="st in subTags"
                    :key="st.id"
                    shadow="hover"
                    class="sub-item"
                  >
                    <div class="sub-item-content">
                      <div class="sub-name is-editable" @click="openRenameSub(st)">{{ st.name }}</div>
                    </div>
                  </el-card>
                </div>
                <el-empty v-if="!subTags.length" description="暂无子标签" :image-size="80" />
              </div>
            </CustomScroll>
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
          <div style="flex:1"></div>
          <el-button type="danger" link size="large" @click="createDialog = false">取消</el-button>
          <el-button type="primary" link size="large" @click="createMain">创建</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="renameDialog" title="重命名主标签" width="380px" align-center>
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="renameForm.name" />
        </el-form-item>
        <el-form-item>
          <div style="flex:1"></div>
          <el-button type="danger" link size="large" @click="renameDialog = false">取消</el-button>
          <el-button type="primary" link size="large" @click="doRenameMain">保存</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="subCreateDialog" title="创建子标签" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="subCreateForm.name" />
        </el-form-item>
        <el-form-item>
          <div style="flex:1"></div>
          <el-button type="danger" link size="large" @click="subCreateDialog = false">取消</el-button>
          <el-button type="primary" link size="large" @click="createSub">创建</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog v-model="subRenameDialog" title="编辑子标签" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <div style="display:flex;align-items:center;gap:8px;width:100%;">
            <el-input v-model="subRenameForm.name" style="max-width:240px" />
            <el-button type="primary" link size="large" @click="doRenameSub">保存</el-button>
            <div style="flex:1"></div>
            <el-button type="danger" link size="large" @click="deleteSubInDialog">删除</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<style scoped>
.tag-management-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
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
  font-weight:600;
  font-size: 18px;
}

.flex-fill-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.flex-fill-card :deep(.el-card__body) {
  flex-grow: 1;
  overflow: hidden;
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.region-content-scroll {
  flex-grow: 1;
  height: 0;
  overflow-y: hidden;
}

.region-body {
  display: flex;
  flex-direction: column;
  min-height: 200px;
  flex-grow: 1;
}

.region-footer {
  min-height: 200px;
  flex-grow: 1;
}

.main-list-wrapper {
  height: 100%;
  overflow-y: hidden;
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
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-light);
  border-left: 7px solid var(--tag-color);
  border-radius: var(--el-border-radius-base);
  transition: all 0.75s cubic-bezier(0.25, 0.8, 0.25, 1);
  background-color: var(--el-bg-color-overlay);
  color: var(--el-text-color-primary);
}

.main-item:hover {
  background-color: var(--el-fill-color-light);
  transform: translateX(6px);
}

.main-item.active {
  border-top-color: var(--el-border-color-light);
  border-right-color: var(--el-border-color-light);
  border-bottom-color: var(--el-border-color-light);
  border-left-color: var(--tag-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateX(10px);
  z-index: 1;
  background-color: var(--el-bg-color-overlay);
  background-image: linear-gradient(to right, color-mix(in srgb, var(--tag-color), transparent 80%) 0%, transparent 65%);
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

.sub-list-associated {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}

.sub-list-associated.empty-container,
.sub-list.empty-container {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  min-height: 200px;
  height: 100%;
}

.full-size-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sub-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}

.library-list-wrapper {
  display: flex;
  flex-direction: column;
}

.two-per-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(80px, 1fr));
  gap: 8px;
}

.compact-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-bg-color-overlay);
  padding: 4px 8px;
  cursor: grab;
  transition: all 0.2s;
  box-shadow: var(--el-box-shadow-light);
  height: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.compact-card:hover {
  border-color: var(--el-color-primary-light-5);
  background-color: var(--el-color-primary-light-9);
  transform: translateY(-1px);
}

.compact-card:active {
  cursor: grabbing;
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
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
}

.sub-name.is-editable {
  cursor: pointer;
  text-align: left;
}

.sub-name.is-editable:hover {
  color: var(--el-color-primary);
  text-decoration: underline;
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
.droppable { position: relative; }
.droppable.drag-target .sub-item { filter: blur(6px); opacity: 0.4; pointer-events: none; }
.drag-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 100;
  letter-spacing: 3px;
  color: var(--el-color-primary);
  text-shadow: 0 3px 7px rgba(0,0,0,0.18);
  background: rgba(255,255,255,0.68);
  backdrop-filter: blur(3px);
  border: 2px dashed var(--el-color-primary);
}
.drag-hint--cancel {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 100;
  letter-spacing: 3px;
  color: var(--el-color-danger);
  text-shadow: 0 3px 7px rgba(0,0,0,0.18);
  background: rgba(255,255,255,0.68);
  backdrop-filter: blur(3px);
  border: 2px dashed var(--el-color-danger);
}
</style>
