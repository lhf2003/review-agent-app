<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const records = ref([])

async function load() {
  try {
    loading.value = true
    const resp = await api.getSyncHistory()
    const list = resp?.data || resp
    records.value = Array.isArray(list) ? list : []
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}
onMounted(() => { if (auth.userId) load() })
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;gap:12px;">
        <span>同步记录</span>
        <el-tag v-if="auth.userId" type="success">ID: {{ auth.userId }}</el-tag>
        <el-button :loading="loading" type="primary" @click="load">加载</el-button>
      </div>
    </template>

    <el-table :data="records" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="userId" label="用户ID" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="message" label="消息" />
      <el-table-column prop="createdTime" label="创建时间" width="200" />
    </el-table>
  </el-card>
</template>

<style scoped>
</style>
