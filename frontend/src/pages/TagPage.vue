<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const auth = useAuthStore()
const search = ref('')
const typeTabs = ref([ { type: 0, name: '全部', count: 0 }, { type: 1, name: '技术栈', count: 0 }, { type: 2, name: '问题类型', count: 0 }, { type: 3, name: '难度', count: 0 } ])
const activeType = ref(0)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const tags = ref([])

const createDialog = ref(false)
const createForm = ref({ name: '', type: 1 })

const renameDialog = ref(false)
const renameForm = ref({ id: null, name: '' })

const mergeDialog = ref(false)
const selectedIds = ref([])
const mergeTargetId = ref(null)

async function loadStats() {
  try {
    const resp = await api.getTagTypeStats()
    const stats = resp.stats || []
    if (stats.length) typeTabs.value = stats
  } catch (e) {
    // 使用默认埋点
  }
}

async function loadList() {
  try {
    loading.value = true
    const resp = await api.getTagPage({ page: page.value - 1, size: pageSize.value, userId: auth.userId, tagName: search.value })
    tags.value = resp.data.content || []
    total.value = resp.data.totalElements || tags.value.length
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function openCreate() { createDialog.value = true }
async function createTag() {
  if (!createForm.value.name.trim()) { ElMessage.warning('请输入标签名称'); return }
  try {
    await api.addTag({ name: createForm.value.name.trim(), type: createForm.value.type, userId: auth.userId })
    ElMessage.success('创建成功')
    createDialog.value = false
    createForm.value = { name: '', type: 1 }
    await loadList(); await loadStats()
  } catch (e) {
    ElMessage.error(`创建失败: ${e.message}`)
  }
}

function openRename(tag) {
  renameForm.value = { id: tag.id, name: tag.name }
  renameDialog.value = true
}
async function doRename() {
  if (!renameForm.value.name.trim()) { ElMessage.warning('请输入新名称'); return }
  try {
    await api.renameTag(auth.userId, renameForm.value.id, renameForm.value.name.trim())
    ElMessage.success('已重命名')
    renameDialog.value = false
    await loadList()
  } catch (e) {
    ElMessage.error(`重命名失败: ${e.message}`)
  }
}

function toggleSelect(tag) {
  const id = tag.id
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

async function doDelete(tag) {
  try {
    await ElMessageBox.confirm(`确认删除标签「${tag.name}」？`, '提示', { type: 'warning' })
    await api.deleteTag(tag.id, auth.userId)
    ElMessage.success('删除成功')
    await loadList(); await loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`删除失败: ${e.message}`)
  }
}

async function doMerge() {
  if (!mergeTargetId.value || selectedIds.value.length === 0) { ElMessage.warning('请选择要合并的标签与目标'); return }
  try {
    await api.mergeTags(selectedIds.value, mergeTargetId.value, auth.userId)
    ElMessage.success('合并成功')
    mergeDialog.value = false
    selectedIds.value = []
    mergeTargetId.value = null
    await loadList(); await loadStats()
  } catch (e) {
    ElMessage.error(`合并失败: ${e.message}`)
  }
}

function onTabChange() { page.value = 1; loadList() }
function onSearch() { page.value = 1; loadList() }

onMounted(async () => { await loadStats(); await loadList() })
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:12px;">
    <div style="display:flex;align-items:center;gap:12px;">
      <el-input v-model="search" placeholder="搜索标签名称..." prefix-icon="Search" clearable @change="onSearch" style="max-width:360px" />
      <el-button type="primary" @click="openCreate">新建</el-button>
      <el-button :disabled="!selectedIds.length" @click="mergeDialog = true">合并所选</el-button>
    </div>

    <el-tabs v-model="activeType" @tab-change="onTabChange">
      <el-tab-pane v-for="t in typeTabs" :key="t.type" :label="`${t.name} (${t.count})`" :name="t.type" />
    </el-tabs>

    <div v-loading="loading">
      <div v-if="tags.length" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px;">
        <el-card v-for="tag in tags" :key="tag.id" class="tag-card" shadow="hover">
          <div class="tag-name">{{ tag.name }}</div>
          <div class="tag-actions">
            <el-tooltip content="选择合并" placement="top"><el-checkbox :model-value="selectedIds.includes(tag.id)" @change="toggleSelect(tag)" /></el-tooltip>
            <el-tooltip content="重命名" placement="top"><el-button size="small" text @click="openRename(tag)">编辑</el-button></el-tooltip>
            <el-tooltip content="删除" placement="top"><el-button size="small" text type="danger" @click="doDelete(tag)">删除</el-button></el-tooltip>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无标签" />
    </div>

    <div style="display:flex;justify-content:center;">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10,20,50]"
        layout="prev, sizes, pager, next"
        :total="total"
        @current-change="loadList"
        @size-change="() => { page.value = 1; loadList() }"
      />
    </div>

    <!-- 新建标签 -->
    <el-dialog v-model="createDialog" title="创建新标签" width="420px" align-center>
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="createForm.type">
            <el-radio :label="1">技术栈</el-radio>
            <el-radio :label="2">问题类型</el-radio>
            <el-radio :label="3">难度</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button @click="createDialog = false">取消</el-button>
          <el-button type="primary" @click="createTag">创建</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 重命名 -->
    <el-dialog v-model="renameDialog" title="重命名标签" width="380px" align-center>
      <el-form label-width="80px">
        <el-form-item label="新名称">
          <el-input v-model="renameForm.name" />
        </el-form-item>
        <el-form-item>
          <el-button @click="renameDialog = false">取消</el-button>
          <el-button type="primary" @click="doRename">保存</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 合并 -->
    <el-dialog v-model="mergeDialog" title="合并标签" width="420px" align-center>
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div>已选择：{{ selectedIds.length }} 个标签</div>
        <el-select v-model="mergeTargetId" placeholder="选择目标标签">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
        <div>
          <el-button @click="mergeDialog = false">取消</el-button>
          <el-button type="primary" @click="doMerge">合并</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.tag-card { display:flex; align-items:center; justify-content:space-between; }
.tag-name { font-weight:600; }
.tag-actions { display:flex; align-items:center; gap:8px; }
</style>
