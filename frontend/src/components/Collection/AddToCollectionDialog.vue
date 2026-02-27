<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/http'

const props = defineProps({
  sessionId: {
    type: [String, Number],
    required: true
  }
})

const visible = ref(false)
const loading = ref(false)
const mode = ref('select') // 'select' | 'create'
const collections = ref([])
const selectedCollectionId = ref(null)

const newCollectionForm = ref({
  name: '',
  description: ''
})

function open() {
  if (!props.sessionId) {
    ElMessage.error('无法添加：会话ID缺失')
    return
  }
  visible.value = true
  fetchCollections()
}

async function fetchCollections() {
  loading.value = true
  try {
    const res = await api.getCollectionList()
    collections.value = res.list || []
  } catch {
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  if (mode.value === 'select' && !selectedCollectionId.value) {
    ElMessage.warning('请选择一个合集')
    return
  }
  if (mode.value === 'create' && !newCollectionForm.value.name) {
    ElMessage.warning('请输入合集名称')
    return
  }

  let targetId = selectedCollectionId.value

  try {
    loading.value = true
    
    // 如果是新建模式，先创建合集
    if (mode.value === 'create') {
      const res = await api.createCollection(newCollectionForm.value)
      if (res && res.id) {
        targetId = res.id
      } else {
        throw new Error('创建合集失败：未返回有效ID')
      }
    }

    // 加入合集
    await api.addSessionToCollection({
      collectionId: targetId,
      sessionId: props.sessionId
    })

    ElMessage.success('已成功加入合集')
    visible.value = false
    // 重置表单
    newCollectionForm.value = { name: '', description: '' }
    mode.value = 'select'
    selectedCollectionId.value = null
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    title="添加到合集"
    width="400px"
    destroy-on-close
  >
    <div class="dialog-body">
      <el-radio-group v-model="mode" class="mode-switch">
        <el-radio-button label="select">选择已有</el-radio-button>
        <el-radio-button label="create">新建合集</el-radio-button>
      </el-radio-group>

      <div v-if="mode === 'select'" class="form-item">
        <el-select 
          v-model="selectedCollectionId" 
          placeholder="请选择目标合集" 
          style="width: 100%"
          :loading="loading"
        >
          <el-option
            v-for="item in collections"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <div class="empty-tip" v-if="collections.length === 0 && !loading">
          暂无合集，请先新建
        </div>
      </div>

      <div v-else class="form-item">
        <el-input v-model="newCollectionForm.name" placeholder="合集名称" class="mb-2" />
        <el-input 
          v-model="newCollectionForm.description" 
          type="textarea" 
          placeholder="描述（可选）" 
          :rows="2" 
        />
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm" :loading="loading">
          确定
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.mode-switch {
  margin-bottom: 20px;
  width: 100%;
  display: flex;
  justify-content: center;
}
.form-item {
  padding: 0 10px;
}
.mb-2 {
  margin-bottom: 12px;
}
.empty-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
  margin-top: 8px;
}
</style>