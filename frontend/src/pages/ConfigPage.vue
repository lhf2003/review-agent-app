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
  scanIntervalHours: 1,
  llmProvider: 'openai',
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

const passwordDialog = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

const activeSection = ref('basic')

const scrollTo = (id) => {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
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

onMounted(() => { if (auth.userId) loadConfig() })
</script>

<template>
  <div style="display:flex; gap: 20px; align-items: flex-start;">
    <!-- 左侧导航 -->
    <el-card style="width: 200px; flex-shrink: 0;" shadow="never">
      <div class="nav-item" :class="{ active: activeSection === 'basic' }" @click="scrollTo('basic')">基本信息</div>
      <div class="nav-item" :class="{ active: activeSection === 'scan' }" @click="scrollTo('scan')">扫描配置</div>
      <div class="nav-item" :class="{ active: activeSection === 'push' }" @click="scrollTo('push')">推送配置</div>
      <div class="nav-item" :class="{ active: activeSection === 'model' }" @click="scrollTo('model')">模型配置</div>
    </el-card>

    <!-- 右侧表单 -->
    <el-card style="flex:1;">
      <el-form label-width="120px">
        
        <!-- 基本信息 -->
        <div id="basic" class="section-title">基本信息</div>
        <el-form-item label="用户名">
          <div style="display:flex;gap:10px;align-items:center;">
            <el-input v-model="userInfoForm.username" :disabled="!editing.username" style="width:240px" />
            <el-button type="primary" link @click="editing.username = true">修改</el-button>
          </div>
        </el-form-item>
        <el-form-item label="邮箱">
          <div style="display:flex;gap:10px;align-items:center;">
            <el-input v-model="userInfoForm.email" :disabled="!editing.email" style="width:240px" />
            <el-button type="primary" link @click="editing.email = true">修改</el-button>
          </div>
        </el-form-item>
        <el-form-item label="手机号">
          <div style="display:flex;gap:10px;align-items:center;">
            <el-input v-model="userInfoForm.phone" :disabled="!editing.phone" style="width:240px" />
            <el-button type="primary" link @click="editing.phone = true">修改</el-button>
          </div>
        </el-form-item>
        <el-form-item label="密码">
          <el-button type="primary" link @click="openPasswordDialog">修改密码</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="saveUserInfo">保存基本信息</el-button>
        </el-form-item>
        
        <el-divider />

        <!-- 扫描配置 -->
        <div id="scan" class="section-title">扫描配置</div>
        <el-form-item label="自动扫描">
          <el-switch v-model="form.autoScanEnabled" />
        </el-form-item>
        <div v-if="form.autoScanEnabled">
          <el-form-item label="扫描目录">
            <el-input v-model="form.scanDirectory" placeholder="如 C:\\Users\\Me\\ai_logs" />
          </el-form-item>
          <el-form-item label="扫描间隔(小时)">
            <el-slider v-model="form.scanIntervalHours" :min="1" :max="12" :step="1" show-stops />
          </el-form-item>
        </div>

        <el-divider />

        <!-- 推送配置 -->
        <div id="push" class="section-title">推送配置</div>
        <el-form-item label="每日日报">
          <el-switch v-model="form.dailyEnabled" />
        </el-form-item>
        <el-form-item label="日报时间" v-if="form.dailyEnabled">
           <el-time-picker v-model="form.dailyTime" format="HH:mm" placeholder="选择时间" />
        </el-form-item>
        
        <el-form-item label="每周周报">
          <el-switch v-model="form.weeklyEnabled" />
        </el-form-item>
        <el-form-item label="周报时间" v-if="form.weeklyEnabled">
           <div style="display:flex;gap:10px;">
             <el-select v-model="form.weeklyDay" placeholder="选择星期" style="width:120px">
               <el-option label="周一" :value="1" />
               <el-option label="周二" :value="2" />
               <el-option label="周三" :value="3" />
               <el-option label="周四" :value="4" />
               <el-option label="周五" :value="5" />
               <el-option label="周六" :value="6" />
               <el-option label="周日" :value="0" />
             </el-select>
             <el-time-picker v-model="form.weeklyTime" format="HH:mm" placeholder="选择时间" />
           </div>
        </el-form-item>

        <el-divider />

        <!-- 模型配置 -->
        <div id="model" class="section-title">模型配置</div>
        <el-form-item label="LLM 提供商">
          <el-select v-model="form.llmProvider" style="width:200px">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Local" value="local" />
          </el-select>
        </el-form-item>

        <el-divider />

        <el-form-item>
          <el-button :loading="loading" type="primary" @click="saveConfig">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>

  <el-dialog v-model="passwordDialog" title="修改密码" width="400px">
    <el-form label-width="100px">
      <el-form-item label="原密码">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="passwordForm.confirm" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialog = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="updatePassword">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.nav-item {
  padding: 10px 16px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.3s;
  color: var(--el-text-color-regular);
}
.nav-item:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.nav-item.active {
  border-left-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 500;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
  line-height: 1;
}
</style>
