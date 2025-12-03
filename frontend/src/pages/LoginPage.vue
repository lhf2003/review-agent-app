<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowRight } from '@element-plus/icons-vue'

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
     <div class="login-page">
           
        <div class="animated-bg" />
         
          <div class="login-container">
               <transition name="fade-up">
                        <el-card class="login-card dark-theme-card" shadow="always">
                         <template #header>
                              <div class="login-header">
                                   <div class="logo">RA</div>
                                   <div class="title-wrap">
                                        <div class="title">Review Agent</div>
                                        <div class="subtitle">欢迎回来，即刻开启您的体验</div>
                                       </div>
                                  </div>
                             </template>
                         
                         <el-form label-position="top" class="login-form" @keyup.enter.native="onSubmit">
                              <el-form-item label="用户名">
                                   <el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名"
                                size="large" />
                                  </el-form-item>
                              <el-form-item label="密码">
                                   <el-input v-model="form.password" type="password" show-password
                                :prefix-icon="Lock" placeholder="请输入密码" size="large" />
                                  </el-form-item>
                             
                              <el-form-item class="button-group">
                                   <el-button         :loading="loading"         type="primary" 
                                       size="large"         class="submit-btn"        
                                @click="onSubmit"       >
                                        立即登录
                                        <el-icon class="el-icon--right">
                                    <ArrowRight />
                                </el-icon>
                                       </el-button>
                                  
                                   <el-button type="text" @click="$router.push('/register')"
                                class="register-btn">
                                        注册新账号
                                       </el-button>
                                  </el-form-item>
                             </el-form>
                        </el-card>
                   </transition>
              </div>
        
    </div>
</template>

<style scoped>
/* 定义焦点颜色 */
:root {
    --accent-color: #00FFFF;
    /* 亮青色 */
    --card-bg: rgba(25, 30, 45, 0.9);
    /* 深色半透明背景 */
}

/* ====================================
 * 1. 页面和背景样式 (更深邃的渐变)
 * ==================================== */
.login-page {
    position: relative;
    height: 100vh;
    min-height: 600px;
    overflow: hidden;
}

.animated-bg {
    position: absolute;
    inset: 0;
    /* 使用深蓝色到紫色的渐变，作为暗黑主题的背景 */
    background: linear-gradient(135deg, #ffffff, #5fbeeb, #0086f3);
    background-size: 300% 300%;
    /* 调整动画速度，使其更平滑和缓和 */
    animation: gradientFlow 10s ease infinite;
    filter: brightness(1.2);
    /* 略微提亮，增加动态感 */
}

@keyframes gradientFlow {
    0% {
        background-position: 0% 50%;
    }

    50% {
        background-position: 100% 50%;
    }

    100% {
        background-position: 0% 50%;
    }
}

.login-container {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 380px;
    /* 略微收窄，更精致 */
    max-width: 90vw;
}


.login-card {
    background-color: var(--card-bg);
    backdrop-filter: blur(12px);
    /* 保持轻微模糊 */
    border: 1px solid rgba(255, 255, 255, 0.1);
    /* 极细的白色边框 */
    border-radius: 16px;
    box-shadow: 0 5px 20px rgba(92, 82, 82, 0.2);
    transition: transform 0.6s ease, box-shadow 0.3s ease;
}

.login-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 15px 50px rgba(92, 82, 82, 0.2);
}

/* 覆盖 Element Plus 卡片头部和身体的默认边框颜色 */
:deep(.el-card__header) {
    border-color: rgba(255, 255, 255, 0.08);
    padding-bottom: 20px;
}

:deep(.el-card__body) {
    padding-top: 5px;
}


.login-header {
    display: flex;
    align-items: center;
    gap: 15px;
}

.logo {
    width: 55px;
    height: 55px;
    border-radius: 50%;
    /* 改为圆形 Logo */
    background: var(--accent-color);
    color: #0A1931;
    /* 深色文字 */
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 900;
    font-size: 26px;
    box-shadow: 0 0 20px rgba(0, 255, 255, 0.5);
    /* 霓虹灯效果阴影 */
}

.title-wrap {
    display: flex;
    flex-direction: column;
}

.title {
    font-weight: 700;
    font-size: 24px;
    color: #ffffff;
    letter-spacing: 0.5px;
}

.subtitle {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.6);
    margin-top: 4px;
}

.login-form {
    padding-top: 15px;
}

/* 标签文字颜色 */
.login-form :deep(.el-form-item__label) {
    color: #000000;
    font-weight: 500;
}

.login-form :deep(.el-input__wrapper) {
    background-color: rgba(255, 255, 255, 0.05) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    border-radius: 8px;
}

/* 输入框内容颜色 */
.login-form :deep(.el-input__inner) {
    color: #ffffff;
}

.login-form :deep(.el-input__inner::placeholder) {
    color: rgba(255, 255, 255, 0.4);
}

.login-form :deep(.el-input__prefix) {
    color: var(--accent-color);
    /* 图标颜色设为焦点色 */
}

/* 输入框聚焦高亮 */
.login-form :deep(.el-input__wrapper.is-focus) {
    /* 使用焦点色作为边框高亮 */
    box-shadow: 0 0 0 2px var(--accent-color) inset !important;
}


/* 按钮组 */
.button-group {
    margin-top: 15px;
}

:deep(.button-group .el-form-item__content) {
    display: flex;
    flex-direction: column;
    width: 100%;
}

.submit-btn {
    width: 100%;
    height: 50px;
    font-size: 18px;
    border-radius: 8px;
    /* 使用焦点色背景 */
    background-color: var(--accent-color);
    border-color: var(--accent-color);
    color: #0A1931;
    /* 确保文字是深色高对比度 */
    font-weight: 700;
    box-shadow: 0 4px 15px rgba(0, 255, 255, 0.4);
    transition: all 0.3s ease;
}

.submit-btn:hover {
    filter: brightness(1.1);
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 255, 255, 0.6);
}

.register-btn {
    margin-top: 20px;
    color: rgba(255, 255, 255, 0.4);
    /* 弱化注册按钮 */
    font-size: 13px;
    align-self: center;
    /* 居中 */
}

.register-btn:hover {
    color: var(--accent-color);
}


/* ====================================
 * 5. 动画
 * ==================================== */
.fade-up-enter-from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
}

.fade-up-enter-active {
    transition: all .6s cubic-bezier(0.23, 1, 0.32, 1);
}

/* 慢速、平滑的入场 */
.fade-up-enter-to {
    opacity: 7;
    transform: translateY(0) scale(1);
}
</style>