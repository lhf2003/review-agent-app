<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth'
import { api } from './api/http'
import md5 from 'blueimp-md5'

const auth = useAuthStore()
const isDark = ref(true)
const avatarDialog = ref(false)
const avatarSizeLarge = ref(false)
const profileForm = ref({ avatar: '', newPassword: '', confirm: '' })

function toggleTheme(val) {
  isDark.value = val
  if (val) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'light') {
    isDark.value = false
    document.documentElement.classList.remove('dark')
  } else {
    isDark.value = true
    document.documentElement.classList.add('dark')
    if (!savedTheme) localStorage.setItem('theme', 'dark')
  }
})

function openAvatar() { avatarDialog.value = true }
function toggleAvatarSize() { avatarSizeLarge.value = !avatarSizeLarge.value }

async function saveAvatar() {
  if (!auth.userId || !profileForm.value.avatar) { ElMessage.warning('缺少用户ID或头像URL'); return }
  try {
    await api.updateUserInfo({ id: Number(auth.userId), avatar: profileForm.value.avatar })
    ElMessage.success('头像已更新')
  } catch (e) { ElMessage.error(`更新失败: ${e.message}`) }
}

function onLogout() {
  avatarDialog.value = false
  auth.logout()
}

watch(() => auth.isAuthenticated, (val) => {
  if (!val) {
    avatarDialog.value = false
  }
})
</script>

<template>
  <div v-if="['/login','/register'].includes($route.path)" style="height:100vh;">
    <router-view />
  </div>
  <el-container v-else style="height:100vh;">
    <el-aside width="65px" class="app-aside">
      <div class="aside-avatar">
        <el-tooltip content="个人中心" placement="right">
          <el-avatar class="user-avatar" :size="50" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" @click="openAvatar"/>
        </el-tooltip>
      </div>
      <el-menu router :default-active="$route.path" class="aside-menu">
        <el-tooltip content="数据" placement="right">
          <template #default>
            <el-menu-item index="/data">
              <el-icon><List /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <el-tooltip content="分析结果" placement="right">
          <template #default>
            <el-menu-item index="/analysis">
              <el-icon><Folder /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <el-tooltip content="标签" placement="right">
          <template #default>
            <el-menu-item index="/tags">
              <el-icon><PriceTag /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip> 
        <el-tooltip content="同步记录" placement="right">
          <template #default>
            <el-menu-item index="/sync">
              <el-icon><Clock /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <el-tooltip content="词云" placement="right">
          <template #default>
            <el-menu-item index="/word-cloud">
              <el-icon><TrendCharts /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <div class="menu-spacer"></div>
        <el-tooltip content="配置" placement="right">
          <template #default>
            <el-menu-item index="/config">
              <el-icon><Setting /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="title">Review Agent</div>
        <div class="theme-switch-wrapper" @click="toggleTheme(!isDark)">
          <div class="theme-switch" :class="{ 'is-dark': isDark }">
            <div class="switch-handle">
              <el-icon class="switch-icon">
                <component :is="isDark ? Moon : Sunny" />
              </el-icon>
            </div>
          </div>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="avatarDialog" width="380px" align-center>
    <div style="display:flex;flex-direction:column;align-items:center;gap:16px;">
      <el-avatar class="dialog-avatar" :size="128" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" @click="toggleAvatarSize"/>
      <div style="width:100%;">
        <el-form label-width="100px">
          <el-form-item label="用户名">
            <el-input :model-value="auth.username" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" @click="onLogout">退出登录</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.app-aside { border-right: 1px solid var(--el-border-color); display:flex; flex-direction:column; background: var(--el-bg-color); }
.app-header { display:flex; align-items:center; justify-content:space-between; }
.title { font-weight:bold; }
.app-main { padding:16px; }
.aside-avatar { display:flex; align-items:center; justify-content:center; padding:16px 8px; border-bottom: 1px solid var(--el-border-color); }
.aside-menu { flex:1; display:flex; flex-direction:column; }
.menu-spacer { flex:1; }
.user-avatar { cursor: pointer; transition: transform 200ms ease; }
.user-avatar:hover { transform: scale(1.05); }
.dialog-avatar { cursor: zoom-in; transition: transform 200ms ease; }
.dialog-avatar:hover { transform: scale(1.03); }
.aside-menu :deep(.el-menu-item) { justify-content: center; }

/* Theme Switch Styles */
.theme-switch-wrapper {
  cursor: pointer;
  padding: 4px;
}

.theme-switch {
  width: 70px;
  height: 34px;
  background: linear-gradient(to right, #87CEEB, #4da9d5);
  border-radius: 17px;
  position: relative;
  transition: all 0.4s ease;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
}

.theme-switch.is-dark {
  background: linear-gradient(to right, #2C3E50, #4b6cb7);
}

.switch-handle {
  width: 28px;
  height: 28px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  left: 3px;
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.theme-switch.is-dark .switch-handle {
  transform: translateX(36px);
  background: #1a1a1a;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}

.switch-icon {
  font-size: 18px;
  color: #f1c40f;
  animation: rotate-icon 0.5s ease-out;
}

@keyframes rotate-icon {
  from { transform: rotate(-180deg) scale(0.5); opacity: 0; }
  to { transform: rotate(0) scale(1); opacity: 1; }
}
</style>
