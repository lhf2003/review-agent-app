<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { api } from '../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
 import { User, Monitor, Bell, Cpu, Lock, Check, Close, Connection, Plus, Delete, Edit, InfoFilled, ArrowRight } from '@element-plus/icons-vue'
import AboutUs from './AboutUs.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const originalForm = ref(null)

const form = ref({
  scanDirectory: '',
  autoScanEnabled: true,
  scanIntervalHours: 1,
  llmProvider: 'deepseek',
  openaiApiKey: '',
  geminiApiKey: '',
  bailianApiKey: '',
  glmApiKey: '',
  ollamaBaseUrl: 'http://localhost:11434',
  dailyEnabled: false,
  dailyTime: null,
  weeklyEnabled: false,
  weeklyDay: 1,
  weeklyTime: null
})

const userInfoForm = ref({
  username: '',
  email: '',
  phone: ''
})

const editing = ref({
  username: false,
  email: false,
  phone: false
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

const passwordDialog = ref(false)

const activeSection = ref('basic')
let scrollContainer = null

const handleNavClick = (section) => {
  if (route.path !== '/config') {
    router.push('/config').then(() => {
      setTimeout(() => scrollTo(section), 100)
    })
  } else {
    scrollTo(section)
  }
}

// 监听滚动事件，更新 activeSection
const onScroll = () => {
  const sections = ['basic', 'scan', 'push']
  // 触发阈值：视口高度的 40%
  const threshold = window.innerHeight * 0.4

  let current = sections[0]

  for (const section of sections) {
    const el = document.getElementById(section)
    if (el) {
      const rect = el.getBoundingClientRect()
      if (rect.top < threshold) {
        current = section
      }
    }
  }
  activeSection.value = current
}

onMounted(() => {
  if (auth.userId) {
    loadConfig()
  }

  // 查找滚动容器 (Element Plus 的 el-main 通常是 .app-main')
  scrollContainer = document.querySelector('.app-main') || window
  scrollContainer.addEventListener('scroll', onScroll, true)

  // 初始执行一次
  onScroll()
})

// 监听路由变化，更新激活的子部分
watch(() => route.path, (newPath) => {
  if (newPath === '/config') {
    // 主配置页面，根据滚动更新激活项
    nextTick(() => onScroll())
  }
}, { immediate: true })

import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', onScroll, true)
  }
})

const scrollTo = (id) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

async function loadConfig() {
  try {
    loading.value = true
    const resp = await api.getConfig()
    const cfg = resp?.data || resp
    form.value.scanDirectory = cfg?.scanDirectory || ''
    form.value.autoScanEnabled = !!cfg?.autoScanEnabled
    const seconds = cfg?.scanIntervalSeconds ?? 3600
    form.value.scanIntervalHours = Math.max(1, Math.min(12, Math.round(seconds / 3600)))
    form.value.llmProvider = cfg?.llmProvider || 'deepseek'

    form.value.dailyEnabled = !!cfg?.dailyEnabled

    // Parse Daily Cron
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

    // Parse Weekly Cron
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
        // Handle numeric or string days (MON-SUN)
        const dayMap = { 'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6, '7': 0 }
        if (!isNaN(dayStr)) {
          let d = parseInt(dayStr)
          if (d === 7) d = 0 // Spring can treat 7 as Sunday
          form.value.weeklyDay = d
        } else if (dayMap[dayStr.toUpperCase()] !== undefined) {
          form.value.weeklyDay = dayMap[dayStr.toUpperCase()]
        }
      }
    } catch (e) {
      form.value.weeklyTime = new Date().setHours(18, 0, 0, 0)
      form.value.weeklyDay = 1
    }

    // Load User Info
    const userResp = await api.getUserInfo(auth.userId)
    const user = userResp?.data || userResp
    if (user) {
      userInfoForm.value.username = user.username || ''
      userInfoForm.value.email = user.email || ''
      userInfoForm.value.phone = user.phone || ''
    }

    // Save original form for change detection
    originalForm.value = JSON.parse(JSON.stringify(form.value))
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

async function saveUserInfo() {
  try {
    loading.value = true
    // Save User Info
    await api.updateUserInfo({
      id: Number(auth.userId),
      username: userInfoForm.value.username,
      email: userInfoForm.value.email,
      phone: userInfoForm.value.phone
    })

    // Update auth store
    if (auth.username !== userInfoForm.value.username) {
      auth.username = userInfoForm.value.username
      localStorage.setItem('auth', JSON.stringify({ isAuthenticated: auth.isAuthenticated, username: auth.username, userId: auth.userId }))
    }

    // Reset editing state
    editing.value.username = false
    editing.value.email = false
    editing.value.phone = false

    ElMessage.success('基本信息已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
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
      llmProvider: form.value.llmProvider,
      dailyEnabled: form.value.dailyEnabled,
      dailyAnalysisTime: form.value.dailyTime ? formatTime(form.value.dailyTime) : null,
      weeklyEnabled: form.value.weeklyEnabled,
      weeklyAnalysisDay: form.value.weeklyDay,
      weeklyAnalysisTime: form.value.weeklyTime ? formatTime(form.value.weeklyTime) : null
    }

    // Save Config
    await api.updateConfig(body)

    // Update original form
    originalForm.value = JSON.parse(JSON.stringify(form.value))

    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function formatTime(date) {
  if (!date) return null
  if (typeof date === 'string') return date
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  const s = date.getSeconds().toString().padStart(2, '0')
  return `${h}:${m}:${s}`
}

function openPasswordDialog() {
  passwordForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  passwordDialog.value = true
}

async function updatePassword() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    ElMessage.warning('请输入原密码和新密码')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirm) {
    ElMessage.warning('两次新密码输入不一致')
    return
  }
  try {
    loading.value = true
    await api.updateUserPassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
    ElMessage.success('密码修改成功')
    passwordDialog.value = false
  } catch (e) {
    ElMessage.error(`修改失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

// Navigation Guard
onBeforeRouteLeave((to, from, next) => {
  if (!originalForm.value) {
    next()
    return
  }

  const normalize = (f) => {
    const c = JSON.parse(JSON.stringify(f))
    return JSON.stringify(c)
  }

  if (normalize(form.value) !== JSON.stringify(originalForm.value)) {
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
        // Save and leave
        await saveConfig()
        next()
      })
      .catch((action) => {
        if (action === 'cancel') {
          // Discard and leave
          next()
        } else {
          // Stay (close dialog)
          next(false)
        }
      })
  } else {
    next()
  }
})
</script>

<template>
  <div class="config-page">
    <!-- 左侧导航 -->
    <div class="sidebar-wrapper">
      <div class="nav-menu">
        <!-- 一级目录：基础配置 -->
        <div class="nav-group-title">通用设置</div>
        <div class="nav-item" :class="{ active: route.path === '/config' && activeSection === 'basic' }" @click="handleNavClick('basic')">
          <el-icon class="nav-icon">
            <User />
          </el-icon>
          <span>基本信息</span>
        </div>
        <div class="nav-item" :class="{ active: route.path === '/config' && activeSection === 'scan' }" @click="handleNavClick('scan')">
          <el-icon class="nav-icon">
            <Monitor />
          </el-icon>
          <span>扫描配置</span>
        </div>
        <div class="nav-item" :class="{ active: route.path === '/config' && activeSection === 'push' }" @click="handleNavClick('push')">
          <el-icon class="nav-icon">
            <Bell />
          </el-icon>
          <span>推送配置</span>
        </div>
        <!-- 一级目录：模型配置 -->
        <div class="nav-group-title">模型设置</div>
        <div class="nav-item" :class="{ active: route.path.includes('/model-provider') }" @click="router.push('/config/model-provider')">
          <el-icon class="nav-icon">
            <Connection />
          </el-icon>
          <span>模型提供商</span>
        </div>
        <div class="nav-item" :class="{ active: route.path.includes('/default-model') }" @click="router.push('/config/default-model')">
          <el-icon class="nav-icon">
            <Cpu />
          </el-icon>
          <span>默认模型</span>
        </div>
        <!-- 一级菜单：关于我们 -->
        <div class="nav-item level-1-item" :class="{ active: route.path.includes('/about') }" @click="router.push('/config/about')">
          <el-icon class="nav-icon">
            <InfoFilled />
          </el-icon>
          <span>关于我们</span>
        </div>
      </div>
    </div>

    <!-- 右侧表单 -->
    <div class="content-wrapper">
      <AboutUs v-if="route.path === '/config/about'" />
      <template v-else-if="route.path === '/config'">
      <el-form label-position="top" class="apple-form">

        <!-- 基本信息 -->
        <div id="basic" class="settings-group">
          <div class="group-header">
            <h3>基本信息</h3>
            <p>管理您的个人账户信息</p>
          </div>
          <div class="group-content">
            <el-form-item label="用户名">
              <div class="input-row">
                <el-input v-model="userInfoForm.username" :disabled="!editing.username" class="apple-input" />
                <el-button type="primary" link @click="editing.username = true" v-if="!editing.username">修改</el-button>
                <el-button type="success" link @click="saveUserInfo" v-else>保存</el-button>
              </div>
            </el-form-item>
            <el-divider />
            <el-form-item label="邮箱">
              <div class="input-row">
                <el-input v-model="userInfoForm.email" :disabled="!editing.email" class="apple-input" />
                <el-button type="primary" link @click="editing.email = true" v-if="!editing.email">修改</el-button>
                <el-button type="success" link @click="saveUserInfo" v-else>保存</el-button>
              </div>
            </el-form-item>
            <el-divider />
            <el-form-item label="手机号">
              <div class="input-row">
                <el-input v-model="userInfoForm.phone" :disabled="!editing.phone" class="apple-input" />
                <el-button type="primary" link @click="editing.phone = true" v-if="!editing.phone">修改</el-button>
                <el-button type="success" link @click="saveUserInfo" v-else>保存</el-button>
              </div>
            </el-form-item>
            <el-divider />
            <el-form-item label="密码">
              <el-button class="action-btn" @click="openPasswordDialog">
                <el-icon>
                  <Lock />
                </el-icon> 修改密码
              </el-button>
            </el-form-item>
          </div>
        </div>

        <!-- 扫描配置 -->
        <div id="scan" class="settings-group">
          <div class="group-header">
            <h3>扫描配置</h3>
            <p>设置自动文件扫描规则</p>
          </div>
          <div class="group-content">
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
        </div>



        <!-- 推送配置 -->
        <div id="push" class="settings-group">
          <div class="group-header">
            <h3>推送配置</h3>
            <p>定制您的学习报告推送</p>
          </div>
          <div class="group-content">
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
        </div>




        <div class="floating-save-bar">
          <el-button :loading="loading" type="primary" size="large" @click="saveConfig"
            class="save-btn">保存更改</el-button>
        </div>
      </el-form>
      </template>
      <router-view v-else />
    </div>
  </div>

  <el-dialog v-model="passwordDialog" title="修改密码" width="400px" class="apple-dialog" align-center>
    <el-form label-position="top">
      <el-form-item label="原密码">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password class="apple-input" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.newPassword" type="password" show-password class="apple-input" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="passwordForm.confirm" type="password" show-password class="apple-input" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="updatePassword">确定</el-button>
      </div>
    </template>
  </el-dialog>

</template>

<style scoped>
.config-page {
  display: flex;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  align-items: flex-start;
}

/* Sidebar */
.sidebar-wrapper {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 10px;
  cursor: pointer;
  color: var(--el-text-color-regular);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.nav-item:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

.nav-item.active {
  background-color: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 4px 12px var(--el-color-primary-light-5);
}

.nav-group-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  padding: 8px 12px;
  margin-top: 8px;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mt-4 {
  margin-top: 16px !important;
}

.mt-2 {
  margin-top: 8px !important;
}

.nav-item.level-1-item {
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
}

/* Content Area */
.content-wrapper {
  flex: 1;
  min-width: 0;
  padding-bottom: 80px;
  /* Space for floating bar */
}

.settings-group {
  margin-bottom: 40px;
  scroll-margin-top: 40px;
}

.group-header {
  margin-bottom: 16px;
  padding-left: 4px;
}

.group-header h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--el-text-color-primary);
}

.group-header p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.group-content {
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 1px solid var(--el-border-color-light);
  padding: 20px 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

/* Form Styles */
.apple-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--el-text-color-primary);
  padding-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: center;
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

.action-btn {
  background-color: var(--el-fill-color-light);
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  height: auto;
}

.action-btn:hover {
  background-color: var(--el-fill-color);
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

:deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top-color: var(--el-border-color-lighter);
}
</style>
