<script setup>
import { ref, onMounted, computed } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, WarningFilled, CircleCheck } from '@element-plus/icons-vue'
import { api } from '../../api/http'

const loading = ref(false)
const notificationPermission = ref('default')

const form = ref({
  browserNotificationEnabled: false,
  reminderFrequencyDays: 1,
  reminderHour: 9,
  mistakeReviewEnabled: true,
  quizCompletionEnabled: true
})

const originalForm = ref(null)

const frequencyOptions = [
  { label: '每天', value: 1 },
  { label: '每3天', value: 3 },
  { label: '每周', value: 7 }
]

const hourOptions = Array.from({ length: 24 }, (_, i) => ({
  label: `${i.toString().padStart(2, '0')}:00`,
  value: i
}))

const canRequestNotification = computed(() => {
  return 'Notification' in window && notificationPermission.value !== 'denied'
})

const notificationStatusText = computed(() => {
  switch (notificationPermission.value) {
    case 'granted':
      return { text: '通知权限已开启', type: 'success' }
    case 'denied':
      return { text: '通知权限被拒绝，请在浏览器设置中开启', type: 'error' }
    default:
      return { text: '点击请求通知权限', type: 'warning' }
  }
})

onMounted(async () => {
  await loadSettings()
  checkNotificationPermission()
})

function checkNotificationPermission() {
  if ('Notification' in window) {
    notificationPermission.value = Notification.permission
  }
}

async function requestNotificationPermission() {
  if (!('Notification' in window)) {
    ElMessage.warning('您的浏览器不支持通知功能')
    return
  }

  try {
    const permission = await Notification.requestPermission()
    notificationPermission.value = permission

    if (permission === 'granted') {
      ElMessage.success('通知权限已开启')
      // 发送测试通知
      new Notification('Review Agent', {
        body: '通知设置成功！您将收到复习提醒。',
        icon: '/favicon.ico'
      })
    } else if (permission === 'denied') {
      ElMessage.warning('通知权限被拒绝，请在浏览器设置中开启')
    }
  } catch (e) {
    
    ElMessage.error('请求通知权限失败')
  }
}

async function loadSettings() {
  try {
    loading.value = true
    const resp = await request('/notification/settings')

    if (resp) {
      form.value = {
        browserNotificationEnabled: resp.browserNotificationEnabled ?? false,
        reminderFrequencyDays: resp.reminderFrequencyDays ?? 1,
        reminderHour: resp.reminderHour ?? 9,
        mistakeReviewEnabled: resp.mistakeReviewEnabled ?? true,
        quizCompletionEnabled: resp.quizCompletionEnabled ?? true
      }
    }

    originalForm.value = JSON.parse(JSON.stringify(form.value))
  } catch (e) {
    ElMessage.error(`加载设置失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  try {
    loading.value = true

    const body = {
      browserNotificationEnabled: form.value.browserNotificationEnabled,
      reminderFrequencyDays: form.value.reminderFrequencyDays,
      reminderHour: form.value.reminderHour,
      mistakeReviewEnabled: form.value.mistakeReviewEnabled,
      quizCompletionEnabled: form.value.quizCompletionEnabled
    }

    await request('/notification/settings', { method: 'PUT', body })

    originalForm.value = JSON.parse(JSON.stringify(form.value))
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

// 复用 http.js 中的 request 函数
async function request(path, { method = 'GET', body } = {}) {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  }

  const res = await fetch('/api' + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })

  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `HTTP ${res.status}`)
  }

  return res.json()
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
        await saveSettings()
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
      <h2>通知设置</h2>
      <p>配置复习提醒和通知偏好</p>
    </div>

    <el-form label-position="top" class="apple-form">
      <!-- 浏览器通知权限 -->
      <div class="form-card">
        <div class="card-title">
          <el-icon><Bell /></el-icon>
          <span>浏览器通知</span>
        </div>

        <div class="permission-status" :class="notificationStatusText.type">
          <el-icon v-if="notificationPermission === 'granted'"><CircleCheck /></el-icon>
          <el-icon v-else-if="notificationPermission === 'denied'"><WarningFilled /></el-icon>
          <span>{{ notificationStatusText.text }}</span>
        </div>

        <el-button
          v-if="canRequestNotification && notificationPermission !== 'granted'"
          type="primary"
          @click="requestNotificationPermission"
          class="permission-btn"
        >
          请求通知权限
        </el-button>

        <el-divider />

        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>启用浏览器通知</span>
              <span class="sub-label">开启后将收到浏览器推送的复习提醒</span>
            </div>
          </template>
          <el-switch
            v-model="form.browserNotificationEnabled"
            :disabled="notificationPermission !== 'granted'"
          />
        </el-form-item>

        <template v-if="form.browserNotificationEnabled">
          <el-divider />

          <el-form-item label="提醒频率">
            <el-select v-model="form.reminderFrequencyDays" class="apple-select full-width">
              <el-option
                v-for="opt in frequencyOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="提醒时间">
            <el-select v-model="form.reminderHour" class="apple-select full-width">
              <el-option
                v-for="opt in hourOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </template>
      </div>

      <!-- 复习提醒类型 -->
      <div class="form-card">
        <div class="card-title">
          <span>提醒类型</span>
        </div>

        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>错题复习提醒</span>
              <span class="sub-label">根据遗忘曲线提醒复习错题本中的内容</span>
            </div>
          </template>
          <el-switch v-model="form.mistakeReviewEnabled" />
        </el-form-item>

        <el-divider />

        <el-form-item class="flex-item">
          <template #label>
            <div class="label-text">
              <span>习题完成提醒</span>
              <span class="sub-label">提醒完成未完成的测验或重新练习分数较低的测验</span>
            </div>
          </template>
          <el-switch v-model="form.quizCompletionEnabled" />
        </el-form-item>
      </div>

      <!-- 遗忘曲线说明 -->
      <div class="form-card info-card">
        <div class="card-title">
          <span>关于艾宾浩斯遗忘曲线</span>
        </div>
        <div class="info-content">
          <p>系统基于艾宾浩斯遗忘曲线自动计算最佳复习时间：</p>
          <ul>
            <li>第1次复习：学习后1天</li>
            <li>第2次复习：学习后3天</li>
            <li>第3次复习：学习后7天</li>
            <li>第4次复习：学习后15天</li>
            <li>第5次复习：学习后30天</li>
          </ul>
          <p class="tip">按照此间隔复习，可以有效巩固记忆，提高学习效率。</p>
        </div>
      </div>
    </el-form>

    <div class="floating-save-bar">
      <el-button :loading="loading" type="primary" size="large" @click="saveSettings" class="save-btn">
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
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.permission-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  margin-bottom: 16px;
}

.permission-status.success {
  background-color: rgba(34, 197, 94, 0.15);
  color: var(--mastery-high);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.permission-status.error {
  background-color: rgba(239, 68, 68, 0.15);
  color: var(--mastery-low);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.permission-status.warning {
  background-color: rgba(245, 158, 11, 0.15);
  color: var(--mastery-med);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.permission-btn {
  margin-bottom: 16px;
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
  color: var(--text-tertiary);
  margin-top: 2px;
}

.apple-select {
  width: 140px;
}

.apple-select.full-width {
  width: 100%;
}

.apple-select :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--glass-border) inset !important;
  background-color: rgba(255, 248, 245, 0.03);
  border-radius: var(--radius-md);
}

:deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top-color: var(--glass-border);
}

.info-card {
  background: rgba(204, 102, 51, 0.08);
  border-color: rgba(204, 102, 51, 0.2);
}

.info-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.info-content ul {
  margin: 12px 0;
  padding-left: 24px;
}

.info-content li {
  margin-bottom: 4px;
}

.info-content .tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--accent-tertiary);
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
  .form-card {
    padding: 16px;
  }
}
</style>
