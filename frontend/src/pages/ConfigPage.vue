<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { api } from '../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { User, Monitor, Bell, Cpu, Lock, Check, Close, Connection } from '@element-plus/icons-vue'

const auth = useAuthStore()
const loading = ref(false)
const testingModel = ref('')
const form = ref({
  scanDirectory: '',
  autoScanEnabled: true,
  scanIntervalHours: 1,
  llmProvider: 'openai',
  openaiApiKey: '',
  geminiApiKey: '',
  ollamaBaseUrl: 'http://localhost:11434',
  dailyEnabled: false,
  dailyTime: null,
  weeklyEnabled: false,
  weeklyDay: 1,
  weeklyTime: null
})

const models = [
  { id: 'openai', name: 'OpenAI', icon: '/icons/openai.svg', placeholder: '输入APIKEY' },
  { id: 'gemini', name: 'Gemini', icon: '/icons/gemini-color.svg', placeholder: '输入APIKEY' },
  { id: 'bailian', name: '百炼', icon: '/icons/bailian-color.svg', placeholder: '输入APIKEY' },
  { id: 'GLM', name: 'GLM', icon: '/icons/chatglm-color.svg', placeholder: '输入APIKEY' },
  { id: 'ollama', name: 'Ollama', icon: '/icons/ollama.svg', placeholder: 'http://localhost:11434' },
]

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

const passwordDialog = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

const activeSection = ref('basic')

// 监听滚动事件，更新 activeSection
const onScroll = () => {
  const sections = ['basic', 'scan', 'push', 'model']
  for (const section of sections) {
    const el = document.getElementById(section)
    if (el) {
      const rect = el.getBoundingClientRect()
      // 这里的 100 是一个阈值，表示元素顶部距离视口顶部的距离
      // 当元素顶部接近视口顶部时（在视口顶部下方 100px 以内），或者元素已经在视口中时，认为该 section 是活跃的
      // 注意：由于 sticky header 或者 padding 的存在，可能需要调整这个阈值
      // 简单的逻辑是：找到第一个 top > 0 的元素的前一个元素，或者第一个 top 在某个范围内（比如 0 到 视口高度的一半）的元素
      // 这里采用一种简单的策略：检查每个 section 的位置，取离视口顶部最近且未完全滚出视口的那个
      if (rect.top >= 0 && rect.top < 300) {
        activeSection.value = section
        break
      } else if (rect.top < 0 && rect.bottom > 100) {
        // 如果当前 section 顶部已经滚上去，但底部还在视口内（留有 100px 余量），它仍然是活跃的
        activeSection.value = section
        // 继续检查下一个，因为可能下一个的顶部也已经进来了，但通常我们希望高亮最上面的那个
        // 但在自上而下的遍历中，如果当前这个满足条件，它就是当前视口中最主要的 section
        break
      }
    }
  }
}

onMounted(() => {
  if (auth.userId) loadConfig()
  window.addEventListener('scroll', onScroll, true)
})

// 注意：如果滚动是在 window 上，直接监听 window 即可。
// 如果是在某个 div 上（比如 .content-wrapper），则需要监听那个 div。
// 根据模板结构，滚动应该是发生在 body/html 上（因为 .content-wrapper 没有设置 overflow: auto 且 height 不是固定的），
// 或者是由父级容器控制。
// 让我们检查一下 App.vue 的布局。通常是 window 滚动。
// 但为了保险，我们在 onMounted 里添加监听，并在 onUnmounted 里移除。

import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll, true)
})

const scrollTo = (id) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    // 使用 scrollIntoView 时，可能会因为 sticky header 遮挡。
    // 可以手动计算位置并 window.scrollTo
    const offset = 20 // 顶部留白
    const elementPosition = el.getBoundingClientRect().top + window.pageYOffset
    const offsetPosition = elementPosition - offset

    window.scrollTo({
      top: offsetPosition,
      behavior: "smooth"
    })
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
    form.value.llmProvider = cfg?.llmProvider || 'openai'
    // Load OpenAI Key if available (decrypted or masked)
    form.value.openaiApiKey = cfg?.openaiApiKeyEncrypted || ''

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

    ElMessage.success('配置已加载')
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

async function testModel(modelId) {
  testingModel.value = modelId
  try {
    // 简单的连接测试：发送一个 Hello 消息
    // 注意：这里测试的是当前保存的配置，如果用户修改了 Key 但没保存，可能测试不通过
    // 这是一个模拟检测，实际应该由后端提供专门的 test 接口
    const handlers = {
      onEvent: (text) => { }, // 忽略输出
      onDone: () => {
        ElMessage.success(`${modelId} 连接成功`)
        testingModel.value = ''
      },
      onError: (e) => {
        ElMessage.error(`${modelId} 连接失败: ${e.message}`)
        testingModel.value = ''
      }
    }
    // 我们暂时无法指定测试哪个模型，只能测试当前生效的 LLM
    // 如果要支持多模型测试，后端需要支持在请求中指定 provider
    if (form.value.llmProvider !== modelId) {
      ElMessage.warning(`请先将 LLM 提供商切换为 ${modelId} 并保存，再进行测试`)
      testingModel.value = ''
      return
    }

    api.chatStream('Hello', handlers)
  } catch (e) {
    testingModel.value = ''
    ElMessage.error('测试请求失败')
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
      openaiApiKeyEncrypted: form.value.openaiApiKey, // 仅保存 OpenAI Key
      dailyEnabled: form.value.dailyEnabled,
      dailyAnalysisTime: form.value.dailyTime ? formatTime(form.value.dailyTime) : null,
      weeklyEnabled: form.value.weeklyEnabled,
      weeklyAnalysisDay: form.value.weeklyDay,
      weeklyAnalysisTime: form.value.weeklyTime ? formatTime(form.value.weeklyTime) : null
    }

    await api.updateConfig(body)
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


</script>

<template>
  <div class="config-page">
    <!-- 左侧导航 -->
    <div class="sidebar-wrapper">
      <div class="nav-menu">
        <div class="nav-item" :class="{ active: activeSection === 'basic' }" @click="scrollTo('basic')">
          <el-icon class="nav-icon">
            <User />
          </el-icon>
          <span>基本信息</span>
        </div>
        <div class="nav-item" :class="{ active: activeSection === 'scan' }" @click="scrollTo('scan')">
          <el-icon class="nav-icon">
            <Monitor />
          </el-icon>
          <span>扫描配置</span>
        </div>
        <div class="nav-item" :class="{ active: activeSection === 'push' }" @click="scrollTo('push')">
          <el-icon class="nav-icon">
            <Bell />
          </el-icon>
          <span>推送配置</span>
        </div>
        <div class="nav-item" :class="{ active: activeSection === 'model' }" @click="scrollTo('model')">
          <el-icon class="nav-icon">
            <Cpu />
          </el-icon>
          <span>模型配置</span>
        </div>
      </div>
    </div>

    <!-- 右侧表单 -->
    <div class="content-wrapper">
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

        <!-- 模型配置 -->
        <div id="model" class="settings-group">
          <div class="group-header">
            <div style="display:flex;align-items:center;gap:8px;">
              <h3>模型配置</h3>
              <el-tag type="warning" size="small" effect="plain" round class="beta-tag">Beta</el-tag>
            </div>
            <p>选择并配置用于分析的 AI 模型</p>
          </div>
          <div class="group-content">

            <div v-for="model in models" :key="model.id">
              <div class="model-row" :class="{ active: form.llmProvider === model.id }">
                <div class="model-info" @click="form.llmProvider = model.id">
                  <div class="model-icon-wrapper">
                    <img v-if="model.id !== 'ollama'" :src="model.icon" class="model-icon" />
                    <el-icon v-else class="model-icon-fallback">
                      <Cpu />
                    </el-icon>
                  </div>
                  <div class="model-name">
                    <span>{{ model.name }}</span>
                    <el-icon v-if="form.llmProvider === model.id" class="check-icon">
                      <Check />
                    </el-icon>
                  </div>
                </div>

                <div class="model-config">
                  <el-input v-if="model.id === 'openai'" v-model="form.openaiApiKey" type="password" show-password
                    :placeholder="model.placeholder" class="apple-input key-input" />
                  <el-input v-else-if="model.id === 'gemini'" v-model="form.geminiApiKey" type="password" show-password
                    :placeholder="model.placeholder" class="apple-input key-input" />
                  <el-input v-else v-model="form.ollamaBaseUrl" :placeholder="model.placeholder"
                    class="apple-input key-input" />

                  <el-button circle plain class="test-btn" :loading="testingModel === model.id"
                    @click="testModel(model.id)">
                    <el-icon>
                      <Connection />
                    </el-icon>
                  </el-button>
                </div>
              </div>
              <el-divider v-if="model.id !== 'ollama'" />
            </div>

          </div>
        </div>

        <div class="floating-save-bar">
          <el-button :loading="loading" type="primary" size="large" @click="saveConfig"
            class="save-btn">保存更改</el-button>
        </div>
      </el-form>
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
  right: 40px;
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

/* Model Config Specific */
.beta-tag {
  background-color: #FFF8E1;
  border-color: #FFD54F;
  color: #F57F17;
  font-weight: 600;
}

.model-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  gap: 20px;
}

.model-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex: 0 0 160px;
}

.model-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.model-icon {
  width: 24px;
  height: 24px;
}

.model-icon-fallback {
  font-size: 20px;
  color: var(--el-text-color-secondary);
}

.model-name {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.check-icon {
  color: var(--el-color-primary);
  font-size: 16px;
}

.model-config {
  flex: 1;
  display: flex;
  gap: 12px;
  align-items: center;
}

.key-input {
  flex: 1;
}

.test-btn {
  flex-shrink: 0;
}
</style>
