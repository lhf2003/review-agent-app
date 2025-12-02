<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'

const route = useRoute()
const id = Number(route.params.id)
const loading = ref(false)
const list = ref([])

async function load() {
  try {
    loading.value = true
    const resp = await api.getAnalysisByFile(id)
    list.value = resp.list || []
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <span>文件详情 - {{ id }}</span>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </template>
    <el-skeleton v-if="loading" :rows="4" animated />
    <template v-else>
      <el-empty v-if="!list.length" description="暂无分析结果" />
      <div v-else style="display:flex;flex-direction:column;gap:12px;">
        <el-card v-for="item in list" :key="item.id" shadow="never">
          <div style="font-weight:600;">{{ item.title }}</div>
          <div>Problem: {{ item.problemStatement }}</div>
          <div>Root Cause: {{ item.rootCause }}</div>
          <div style="margin-top:8px;display:flex;gap:6px;flex-wrap:wrap;">
            <el-tag v-for="t in (item.tags || [])" :key="t" size="small">{{ t }}</el-tag>
          </div>
        </el-card>
      </div>
    </template>
  </el-card>
</template>

<style scoped>
</style>

