<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './stores/auth'
import { api } from './api/http'
import md5 from 'blueimp-md5'

const auth = useAuthStore()
const avatarDialog = ref(false)
const avatarSizeLarge = ref(false)
const profileForm = ref({ avatar: '', newPassword: '', confirm: '' })

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
        <div>
          <el-button type="primary" @click="ElMessage.success('欢迎使用')">帮助</el-button>
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
</style>
