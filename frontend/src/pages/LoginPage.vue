<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowRight } from '@element-plus/icons-vue'

const form = ref({ username: '', password: '' })
const loading = ref(false)
const auth = useAuthStore()

// 鼠标视差效果
const mouseX = ref(0)
const mouseY = ref(0)

const handleMouseMove = (e) => {
    // 计算相对于屏幕中心的偏移量
    mouseX.value = e.clientX - window.innerWidth / 2
    mouseY.value = e.clientY - window.innerHeight / 2
}

// 不同层级的视差系数
const orb1Style = computed(() => ({
    transform: `translate(${mouseX.value * 0.03}px, ${mouseY.value * 0.03}px)`
}))

const orb2Style = computed(() => ({
    transform: `translate(${mouseX.value * -0.05}px, ${mouseY.value * -0.05}px)`
}))

const orb3Style = computed(() => ({
    transform: `translate(${mouseX.value * 0.02}px, ${mouseY.value * 0.02}px)`
}))

onMounted(() => {
    window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
    window.removeEventListener('mousemove', handleMouseMove)
})

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
        <!-- 动态背景 -->
        <div class="background-container">
            <div class="orb-wrapper" :style="orb1Style">
                <div class="glow-orb orb-1"></div>
            </div>
            <div class="orb-wrapper" :style="orb2Style">
                <div class="glow-orb orb-2"></div>
            </div>
            <div class="orb-wrapper" :style="orb3Style">
                <div class="glow-orb orb-3"></div>
            </div>
            <div class="grid-overlay"></div>
        </div>

        <div class="login-container">
            <transition name="fade-up" appear>
                <div class="card-wrapper">
                    <el-card class="login-card" shadow="never">
                        <template #header>
                            <div class="login-header">
                                <div class="logo-container">
                                    <div class="logo">RA</div>
                                    <div class="logo-ring"></div>
                                </div>
                                <div class="title-wrap">
                                    <h1 class="title">Review Agent</h1>
                                    <p class="subtitle">智能代码审查助手</p>
                                </div>
                            </div>
                        </template>

                        <el-form label-position="top" class="login-form" @keyup.enter="onSubmit">
                            <el-form-item label="用户名">
                                <el-input 
                                    v-model="form.username" 
                                    :prefix-icon="User" 
                                    placeholder="请输入用户名"
                                    size="large" 
                                />
                            </el-form-item>
                            <el-form-item label="密码">
                                <el-input 
                                    v-model="form.password" 
                                    type="password" 
                                    show-password
                                    :prefix-icon="Lock" 
                                    placeholder="请输入密码" 
                                    size="large" 
                                />
                            </el-form-item>

                            <div class="action-area">
                                <el-button 
                                    :loading="loading" 
                                    type="primary" 
                                    size="large" 
                                    class="submit-btn"
                                    @click="onSubmit"
                                >
                                    <span>立即登录</span>
                                    <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                                </el-button>

                                <div class="secondary-actions">
                                    <a class="register-link" @click="$router.push('/register')">
                                        还没有账号？点击注册
                                    </a>
                                </div>
                            </div>
                        </el-form>
                    </el-card>
                </div>
            </transition>
        </div>
    </div>
</template>

<style>
@keyframes float {
    0%, 100% { 
        transform: translate(0, 0) scale(1); 
        opacity: 0.5;
    }
    25% { 
        transform: translate(100px, 100px) scale(1.2); 
        opacity: 0.8;
    }
    50% { 
        transform: translate(-50px, 200px) scale(0.8); 
        opacity: 0.4;
    }
    75% { 
        transform: translate(-100px, 50px) scale(1.1); 
        opacity: 0.7;
    }
}
</style>

<style scoped>
:root {
    --primary-color: #3B82F6;
    --accent-color: #06b6d4;
    --bg-dark: #0f172a;
    --card-bg: rgba(30, 41, 59, 0.7);
    --text-main: #f8fafc;
    --text-muted: #94a3b8;
}

/* ====================================
 * 1. 布局与背景
 * ==================================== */
.login-page {
    position: relative;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
    background-color: #0f172a;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.background-container {
    position: absolute;
    inset: 0;
    z-index: 0;
    overflow: hidden;
}

.orb-wrapper {
    position: absolute;
    inset: 0;
    pointer-events: none;
    will-change: transform;
}

/* 动态光球效果 */
.glow-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.6;
    animation: float 15s infinite ease-in-out;
    will-change: transform, opacity;
}

.orb-1 {
    top: -10%;
    left: -10%;
    width: 50vw;
    height: 50vw;
    background: radial-gradient(circle, #4f46e5 0%, transparent 70%);
    animation-delay: 0s;
}

.orb-2 {
    bottom: -10%;
    right: -10%;
    width: 40vw;
    height: 40vw;
    background: radial-gradient(circle, #06b6d4 0%, transparent 70%);
    animation-delay: -5s;
}

.orb-3 {
    top: 40%;
    left: 40%;
    width: 30vw;
    height: 30vw;
    background: radial-gradient(circle, #8b5cf6 0%, transparent 70%);
    opacity: 0.4;
    animation-delay: -10s;
}

.grid-overlay {
    position: absolute;
    inset: 0;
    background-image: 
        linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
    background-size: 50px 50px;
    mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
    pointer-events: none;
}

/* ====================================
 * 2. 登录卡片
 * ==================================== */
.login-container {
    position: relative;
    z-index: 10;
    width: 100%;
    max-width: 420px;
    padding: 20px;
}

.login-card {
    background: rgba(30, 41, 59, 0.65);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 24px;
    box-shadow: 
        0 20px 40px rgba(0, 0, 0, 0.4),
        0 0 0 1px rgba(255, 255, 255, 0.05) inset;
    overflow: visible; /* 允许Logo发光溢出 */
}

/* 覆盖 Element Card 样式 */
:deep(.el-card__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    padding: 30px 30px 20px;
}

:deep(.el-card__body) {
    padding: 30px;
}

/* ====================================
 * 3. 头部设计
 * ==================================== */
.login-header {
    display: flex;
    align-items: center;
    gap: 20px;
}

.logo-container {
    position: relative;
    width: 64px;
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.logo {
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #06b6d4, #3b82f6);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 800;
    font-size: 28px;
    color: white;
    box-shadow: 0 8px 20px rgba(6, 182, 212, 0.4);
    z-index: 2;
    position: relative;
}

.logo-ring {
    position: absolute;
    inset: -4px;
    border-radius: 20px;
    background: linear-gradient(135deg, rgba(6, 182, 212, 0.5), transparent);
    z-index: 1;
    filter: blur(2px);
}

.title-wrap {
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.title {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: #f8fafc;
    letter-spacing: 0.5px;
    background: linear-gradient(to right, #fff, #cbd5e1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.subtitle {
    margin: 4px 0 0;
    font-size: 13px;
    color: #94a3b8;
    font-weight: 400;
}

/* ====================================
 * 4. 表单样式
 * ==================================== */
.login-form :deep(.el-form-item__label) {
    color: #cbd5e1;
    font-weight: 500;
    padding-bottom: 8px;
    font-size: 14px;
}

.login-form :deep(.el-input__wrapper) {
    background-color: rgba(15, 23, 42, 0.6) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    border-radius: 12px;
    padding: 4px 12px;
    height: 48px;
    transition: all 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.2) inset !important;
    background-color: rgba(15, 23, 42, 0.8) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 2px #3b82f6 inset !important;
    background-color: rgba(15, 23, 42, 0.9) !important;
}

.login-form :deep(.el-input__inner) {
    color: #f8fafc;
    font-size: 15px;
}

.login-form :deep(.el-input__prefix) {
    color: #94a3b8;
    font-size: 18px;
}

/* ====================================
 * 5. 按钮与交互
 * ==================================== */
.action-area {
    margin-top: 32px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.submit-btn {
    width: 100%;
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 12px;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.submit-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(37, 99, 235, 0.5);
    background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}

.submit-btn:active {
    transform: translateY(0);
}

.secondary-actions {
    display: flex;
    justify-content: center;
}

.register-link {
    font-size: 14px;
    color: #94a3b8;
    cursor: pointer;
    transition: color 0.2s;
    text-decoration: none;
}

.register-link:hover {
    color: #3b82f6;
    text-decoration: underline;
    text-underline-offset: 4px;
}

/* ====================================
 * 6. 动画
 * ==================================== */
.fade-up-enter-from {
    opacity: 0;
    transform: translateY(40px) scale(0.98);
}

.fade-up-enter-active {
    transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-up-enter-to {
    opacity: 1;
    transform: translateY(0) scale(1);
}
</style>
