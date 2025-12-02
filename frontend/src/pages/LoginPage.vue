<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const form = ref({ username: '', password: '' })
const loading = ref(false)
const auth = useAuthStore()

async function onSubmit() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    loading.value = true
    await auth.login(form.value.username, form.value.password)
    auth.hydrate()
    ElMessage.success('登录成功')
    auth.routerPushAfterLogin()
  } catch (e) {
    ElMessage.error(`登录失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display:flex;align-items:center;justify-content:center;height:100%;">
    <el-card style="width:380px;">
      <template #header>
        <span>登录</span>
      </template>
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" type="primary" @click="onSubmit">登录</el-button>
          <el-button type="text" @click="$router.push('/register')">注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
 </template>

<style scoped>
</style>
