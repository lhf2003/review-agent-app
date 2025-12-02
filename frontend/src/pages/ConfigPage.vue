<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const form = ref({
  scanDirectory: '',
  autoScanEnabled: true,
  scanIntervalSeconds: 30,
  llmProvider: 'openai',
})

async function loadConfig() {
  try {
    loading.value = true
    const resp = await api.getConfig(auth.userId)
    const cfg = resp?.data || resp
    form.value.scanDirectory = cfg?.scanDirectory || ''
    form.value.autoScanEnabled = !!cfg?.autoScanEnabled
    const seconds = cfg?.scanIntervalSeconds ?? 3600
    form.value.scanIntervalHours = Math.max(1, Math.min(12, Math.round(seconds / 3600)))
    form.value.llmProvider = cfg?.llmProvider || 'openai'
    ElMessage.success('配置已加载')
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  try {
    loading.value = true
    const body = {
      userId: Number(auth.userId),
      scanDirectory: form.value.scanDirectory,
      autoScanEnabled: form.value.autoScanEnabled,
      scanIntervalSeconds: form.value.scanIntervalSeconds * 3600,
      llmProvider: form.value.llmProvider,
    }
    await api.updateConfig(body)
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

onMounted(() => { if (auth.userId) loadConfig() })
</script>

<template>
  <el-card>
    <el-form label-width="120px">
      <el-form-item label="扫描目录">
        <el-input v-model="form.scanDirectory" placeholder="如 C:\\Users\\Me\\ai_logs" />
      </el-form-item>
      <el-form-item label="自动扫描">
        <el-switch v-model="form.autoScanEnabled" />
      </el-form-item>
      <el-form-item label="扫描间隔(小时)">
        <el-slider v-model="form.scanIntervalSeconds" :min="1" :max="12" :step="1" show-stops />
      </el-form-item>
      <el-form-item label="LLM 提供商">
        <el-select v-model="form.llmProvider" style="width:200px">
          <el-option label="OpenAI" value="openai" />
          <el-option label="Local" value="local" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :loading="loading" type="primary" @click="saveConfig">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
</style>
