<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const form = ref({ username: '', password: '', confirm: '' })
const loading = ref(false)
const auth = useAuthStore()

async function onSubmit() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.value.password !== form.value.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }
  try {
    loading.value = true
    await auth.register(form.value.username, form.value.password)
    ElMessage.success('注册成功，请登录')
    auth.routerPushLogin()
  } catch (e) {
    ElMessage.error(`注册失败: ${e.message}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display:flex;align-items:center;justify-content:center;height:100%;">
    <el-card style="width:380px;">
      <template #header>
        <span>注册</span>
      </template>
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button :loading="loading" type="primary" @click="onSubmit">注册</el-button>
          <el-button type="text" @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
 </template>

<style scoped>
</style>

