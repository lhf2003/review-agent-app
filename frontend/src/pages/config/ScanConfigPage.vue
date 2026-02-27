<script setup>
import { ref, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

const loading = ref(false)
const form = ref({
  scanDirectory: '',
  autoScanEnabled: true,
  scanIntervalHours: 1
})

const originalForm = ref(null)

onMounted(async () => {
  await loadConfig()
})

async function loadConfig() {
  try {
    loading.value = true
    const resp = await api.getConfig()
    const cfg = resp?.data || resp

    form.value.scanDirectory = cfg?.scanDirectory || ''
    form.value.autoScanEnabled = !!cfg?.autoScanEnabled
    const seconds = cfg?.scanIntervalSeconds ?? 3600
    form.value.scanIntervalHours = Math.max(1, Math.min(12, Math.round(seconds / 3600)))

    originalForm.value = JSON.parse(JSON.stringify(form.value))
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
      scanIntervalSeconds: form.value.scanIntervalHours * 3600
    }

    await api.updateConfig(body)

    originalForm.value = JSON.parse(JSON.stringify(form.value))

    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

onBeforeRouteLeave((to, from, next) => {
  if (!originalForm.value) {
    next()
    return
  }

  const normalize = (f) => JSON.stringify(JSON.parse(JSON.stringify(f)))
  if (normalize(form.value) !== normalize(originalForm.value)) {
    ElMessageBox.confirm(
      '您有未保存的更改，确定要离开吗？',
      '未保存更改',
      {
        confirmButtonText: '保存并离开',
        cancelButtonText: '放弃修改',
        distinguishCancelAndClose: true,
        type: 'warning',
      }
    )
      .then(async () => {
        await saveConfig()
        next()
      })
      .catch((action) => {
        if (action === 'cancel') next()
        else next(false)
      })
  } else {
    next()
  }
})
</script>

<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>扫描配置</h2>
      <p>设置自动文件扫描规则</p>
    </div>

    <el-form label-position="top" class="apple-form">
      <div class="form-card">
        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>自动扫描</span>
              <span class="sub-label">定期扫描指定目录下的新文件</span>
            </div>
          </template>
          <el-switch v-model="form.autoScanEnabled" />
        </el-form-item>

        <template v-if="form.autoScanEnabled">
          <el-divider />
          <el-form-item label="扫描目录">
            <el-input v-model="form.scanDirectory" placeholder="如 C:\\Users\\Me\\ai_logs" class="apple-input" />
          </el-form-item>
          <el-divider />
          <el-form-item label="扫描间隔">
            <div class="slider-container">
              <span class="slider-val">{{ form.scanIntervalHours }} 小时</span>
              <el-slider v-model="form.scanIntervalHours" :min="1" :max="12" :step="1" show-stops />
            </div>
          </el-form-item>
        </template>
      </div>
    </el-form>

    <div class="floating-save-bar">
      <el-button :loading="loading" type="primary" size="large" @click="saveConfig" class="save-btn">
        保存更改
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.form-card {
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 1px solid var(--el-border-color-light);
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--el-text-color-primary);
  padding-bottom: 8px;
}

.flex-item :deep(.el-form-item__content) {
  justify-content: space-between;
}

.label-text {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.sub-label {
  font-size: 13px;
  font-weight: normal;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.slider-container {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.slider-val {
  min-width: 80px;
  font-feature-settings: "tnum";
}

.apple-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 4px 12px;
  transition: all 0.2s;
}

.apple-input :deep(.el-input__wrapper.is-focus) {
  background-color: var(--el-bg-color);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
}

:deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top-color: var(--el-border-color-lighter);
}

.floating-save-bar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: fit-content;
  z-index: 100;
}

.save-btn {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-radius: 24px;
  padding: 12px 32px;
  font-weight: 600;
  transition: all 0.3s;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.16);
}

@media (max-width: 768px) {
  .config-container {
    padding: 16px;
  }
}
</style>
