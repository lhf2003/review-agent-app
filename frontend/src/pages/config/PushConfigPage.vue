<script setup>
import { ref, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

const loading = ref(false)
const form = ref({
  dailyEnabled: false,
  dailyTime: null,
  weeklyEnabled: false,
  weeklyDay: 1,
  weeklyTime: null
})

const originalForm = ref(null)

const formatTime = (date) => {
  if (!date) return null
  if (typeof date === 'string') return date
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  const s = date.getSeconds().toString().padStart(2, '0')
  return `${h}:${m}:${s}`
}

onMounted(async () => {
  await loadConfig()
})

async function loadConfig() {
  try {
    loading.value = true
    const resp = await api.getConfig()
    const cfg = resp?.data || resp

    form.value.dailyEnabled = !!cfg?.dailyEnabled

    const dailyCron = cfg?.dailyCron || '0 0 18 * * ?'
    try {
      const parts = dailyCron.split(' ')
      if (parts.length >= 3) {
        const date = new Date()
        date.setHours(parseInt(parts[2]) || 18)
        date.setMinutes(parseInt(parts[1]) || 0)
        date.setSeconds(0)
        form.value.dailyTime = date
      }
    } catch (e) {
      form.value.dailyTime = new Date().setHours(18, 0, 0, 0)
    }

    form.value.weeklyEnabled = !!cfg?.weeklyEnabled

    const weeklyCron = cfg?.weeklyCron || '0 0 18 ? * 5'
    try {
      const parts = weeklyCron.split(' ')
      if (parts.length >= 6) {
        const date = new Date()
        date.setHours(parseInt(parts[2]) || 18)
        date.setMinutes(parseInt(parts[1]) || 0)
        date.setSeconds(0)
        form.value.weeklyTime = date

        const dayStr = parts[5]
        const dayMap = { 'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6, '7': 0 }
        if (!isNaN(dayStr)) {
          let d = parseInt(dayStr)
          if (d === 7) d = 0
          form.value.weeklyDay = d
        } else if (dayMap[dayStr.toUpperCase()] !== undefined) {
          form.value.weeklyDay = dayMap[dayStr.toUpperCase()]
        }
      }
    } catch (e) {
      form.value.weeklyTime = new Date().setHours(18, 0, 0, 0)
      form.value.weeklyDay = 1
    }

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
      dailyEnabled: form.value.dailyEnabled,
      dailyAnalysisTime: form.value.dailyTime ? formatTime(form.value.dailyTime) : null,
      weeklyEnabled: form.value.weeklyEnabled,
      weeklyAnalysisDay: form.value.weeklyDay,
      weeklyAnalysisTime: form.value.weeklyTime ? formatTime(form.value.weeklyTime) : null
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
      <h2>推送配置</h2>
      <p>定制您的学习报告推送</p>
    </div>

    <el-form label-position="top" class="apple-form">
      <div class="form-card">
        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>每日日报</span>
              <span class="sub-label">每天定时发送学习总结</span>
            </div>
          </template>
          <el-switch v-model="form.dailyEnabled" />
        </el-form-item>
        <template v-if="form.dailyEnabled">
          <el-divider />
          <el-form-item label="推送时间">
            <el-time-picker v-model="form.dailyTime" format="HH:mm" placeholder="选择时间" class="apple-picker" />
          </el-form-item>
        </template>

        <el-divider />

        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>每周周报</span>
              <span class="sub-label">每周发送详细学习分析报告</span>
            </div>
          </template>
          <el-switch v-model="form.weeklyEnabled" />
        </el-form-item>
        <template v-if="form.weeklyEnabled">
          <el-divider />
          <el-form-item label="推送时间">
            <div class="row-inputs">
              <el-select v-model="form.weeklyDay" placeholder="选择星期" class="apple-select">
                <el-option label="周一" :value="1" />
                <el-option label="周二" :value="2" />
                <el-option label="周三" :value="3" />
                <el-option label="周四" :value="4" />
                <el-option label="周五" :value="5" />
                <el-option label="周六" :value="6" />
                <el-option label="周日" :value="0" />
              </el-select>
              <el-time-picker v-model="form.weeklyTime" format="HH:mm" placeholder="选择时间" class="apple-picker" />
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

.row-inputs {
  display: flex;
  gap: 12px;
}

.apple-select {
  width: 140px;
}

.apple-select.full-width {
  width: 100%;
}

.apple-select :deep(.el-input__wrapper),
.apple-picker :deep(.el-input__wrapper) {
  box-shadow: none !important;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
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
