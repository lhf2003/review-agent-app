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
  scanIntervalHours: 1,
  autoAnalysisEnabled: false,
  analysisIntervalMinutes: 10
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
    form.value.autoAnalysisEnabled = !!cfg?.autoAnalysisEnabled
    form.value.analysisIntervalMinutes = cfg?.analysisIntervalMinutes ?? 10

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
      scanIntervalSeconds: form.value.scanIntervalHours * 3600,
      autoAnalysisEnabled: form.value.autoAnalysisEnabled,
      analysisIntervalMinutes: form.value.analysisIntervalMinutes
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

      <div class="form-card" style="margin-top: 24px;">
        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>自动分析</span>
              <span class="sub-label">定时分析未处理的数据文件</span>
            </div>
          </template>
          <el-switch v-model="form.autoAnalysisEnabled" />
        </el-form-item>

        <template v-if="form.autoAnalysisEnabled">
          <el-divider />
          <el-form-item label="分析间隔">
            <div class="slider-container">
              <span class="slider-val">{{ form.analysisIntervalMinutes }} 分钟</span>
              <el-slider v-model="form.analysisIntervalMinutes" :min="5" :max="60" :step="5" show-stops />
            </div>
            <div class="help-text">
              建议：高频使用设为5-10分钟，日常使用设为15-30分钟
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
  padding-bottom: 80px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.form-card {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: var(--radius-card);
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  padding: 24px;
  transition: var(--transition-base);
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
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

.help-text {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
  line-height: 1.5;
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
  color: var(--text-secondary);
}

.apple-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--glass-border) inset;
  background-color: rgba(255, 248, 245, 0.03);
  border-radius: var(--radius-md);
  padding: 4px 12px;
  transition: all 0.2s;
}

.apple-input :deep(.el-input__wrapper.is-focus) {
  background-color: rgba(255, 248, 245, 0.05);
  box-shadow: 0 0 0 2px var(--accent-glow-soft) inset;
}

:deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top-color: var(--glass-border);
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
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  border-radius: var(--radius-pill);
  padding: 12px 32px;
  font-weight: 600;
  transition: all var(--transition-base);
}

.save-btn:hover {
  background: var(--accent-secondary);
  border-color: var(--accent-secondary);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px var(--accent-glow-soft);
}

@media (max-width: 768px) {
  .config-container {
    padding: 16px;
  }
}
</style>
