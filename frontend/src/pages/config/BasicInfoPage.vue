<script setup>
import { ref, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import PasswordDialog from './components/PasswordDialog.vue'

const auth = useAuthStore()

const loading = ref(false)
const editing = ref({
  username: false,
  email: false,
  phone: false
})

const userInfoForm = ref({
  username: '',
  email: '',
  phone: ''
})

const passwordDialog = ref(false)
const originalInfo = ref(null)

onMounted(async () => {
  await loadUserInfo()
})

async function loadUserInfo() {
  try {
    loading.value = true
    const userResp = await api.getUserInfo(auth.userId)
    const user = userResp?.data || userResp
    if (user) {
      userInfoForm.value.username = user.username || ''
      userInfoForm.value.email = user.email || ''
      userInfoForm.value.phone = user.phone || ''
      originalInfo.value = JSON.parse(JSON.stringify(userInfoForm.value))
    }
  } catch (e) {
    ElMessage.error(`加载失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

async function saveUserInfo() {
  try {
    loading.value = true
    await api.updateUserInfo({
      id: Number(auth.userId),
      username: userInfoForm.value.username,
      email: userInfoForm.value.email,
      phone: userInfoForm.value.phone
    })

    if (auth.username !== userInfoForm.value.username) {
      auth.username = userInfoForm.value.username
      localStorage.setItem('auth', JSON.stringify({
        isAuthenticated: auth.isAuthenticated,
        username: auth.username,
        userId: auth.userId
      }))
    }

    editing.value.username = false
    editing.value.email = false
    editing.value.phone = false
    originalInfo.value = JSON.parse(JSON.stringify(userInfoForm.value))

    ElMessage.success('基本信息已保存')
  } catch (e) {
    ElMessage.error(`保存失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function openPasswordDialog() {
  passwordDialog.value = true
}

async function handlePasswordConfirm({ oldPassword, newPassword, confirm }) {
  if (!oldPassword || !newPassword) {
    ElMessage.warning('请输入原密码和新密码')
    return
  }
  if (newPassword !== confirm) {
    ElMessage.warning('两次新密码输入不一致')
    return
  }
  try {
    loading.value = true
    await api.updateUserPassword(oldPassword, newPassword)
    ElMessage.success('密码修改成功')
    passwordDialog.value = false
  } catch (e) {
    ElMessage.error(`修改失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function handleEditField(field) {
  editing.value[field] = true
}

onBeforeRouteLeave((to, from, next) => {
  if (!originalInfo.value) {
    next()
    return
  }

  const normalize = (f) => JSON.stringify(JSON.parse(JSON.stringify(f)))
  if (normalize(userInfoForm.value) !== normalize(originalInfo.value)) {
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
        await saveUserInfo()
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
      <h2>基本信息</h2>
      <p>管理您的个人账户信息</p>
    </div>

    <el-form label-position="top" class="apple-form">
      <div class="form-card">
        <el-form-item label="用户名">
          <div class="input-row">
            <el-input v-model="userInfoForm.username" :disabled="!editing.username" class="apple-input" />
            <el-button type="primary" link @click="handleEditField('username')" v-if="!editing.username">修改</el-button>
            <el-button type="success" link @click="saveUserInfo" v-else>保存</el-button>
          </div>
        </el-form-item>
        <el-divider />
        <el-form-item label="邮箱">
          <div class="input-row">
            <el-input v-model="userInfoForm.email" :disabled="!editing.email" class="apple-input" />
            <el-button type="primary" link @click="handleEditField('email')" v-if="!editing.email">修改</el-button>
            <el-button type="success" link @click="saveUserInfo" v-else>保存</el-button>
          </div>
        </el-form-item>
        <el-divider />
        <el-form-item label="手机号">
          <div class="input-row">
            <el-input v-model="userInfoForm.phone" :disabled="!editing.phone" class="apple-input" />
            <el-button type="primary" link @click="handleEditField('phone')" v-if="!editing.phone">修改</el-button>
            <el-button type="success" link @click="saveUserInfo" v-else>保存</el-button>
          </div>
        </el-form-item>
        <el-divider />
        <el-form-item label="密码">
          <el-button class="action-btn" @click="openPasswordDialog">
            修改密码
          </el-button>
        </el-form-item>
      </div>
    </el-form>

    <PasswordDialog
      v-model="passwordDialog"
      :loading="loading"
      @confirm="handlePasswordConfirm"
    />
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

:deep(.el-divider--horizontal) {
  margin: 16px 0;
  border-top-color: var(--el-border-color-lighter);
}
</style>
