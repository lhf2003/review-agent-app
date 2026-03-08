<script setup>
/**
 * ASCII风格登录页 - PoC概念验证
 * 将现代AppleStyle转换为Digital Retro终端风格
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import ASCIIArtText from '../components/ASCIIArtText.vue'
import ASCIIButton from '../components/ASCIIButton.vue'
import ASCIIInput from '../components/ASCIIInput.vue'
import ASCIICard from '../components/ASCIICard.vue'

const form = ref({ username: '', password: '' })
const loading = ref(false)
const auth = useAuthStore()

// 终端打字机效果的状态
const bootLines = ref([])
const showLogin = ref(false)
const currentBootLine = ref(0)

const bootSequence = [
  { text: '> SYSTEM BOOT SEQUENCE INITIATED...', delay: 100 },
  { text: '> LOADING KERNEL MODULES...', delay: 300 },
  { text: '> MOUNTING FILESYSTEMS...', delay: 600 },
  { text: '> INITIALIZING SECURITY PROTOCOLS...', delay: 900 },
  { text: '> CONNECTING TO REVIEW AGENT SERVER...', delay: 1200 },
  { text: '> CONNECTION ESTABLISHED', delay: 1500 },
  { text: '> READY FOR AUTHENTICATION', delay: 1800 }
]

let bootTimer = null

function startBootSequence() {
  bootLines.value = []
  currentBootLine.value = 0

  bootSequence.forEach((line, index) => {
    setTimeout(() => {
      bootLines.value.push(line.text)
      currentBootLine.value = index + 1

      if (index === bootSequence.length - 1) {
        setTimeout(() => {
          showLogin.value = true
        }, 500)
      }
    }, line.delay)
  })
}

async function onSubmit() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('> ERROR: CREDENTIALS REQUIRED')
    return
  }
  try {
    loading.value = true
    await auth.login(form.value.username, form.value.password)
    auth.hydrate()
    ElMessage.success('> AUTHENTICATION SUCCESSFUL')
    auth.routerPushAfterLogin()
  } catch (e) {
    ElMessage.error(`> AUTHENTICATION FAILED: ${e.message}`)
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  ElMessage.info('> REDIRECTING TO REGISTRATION...')
  setTimeout(() => {
    // 路由跳转
  }, 500)
}

onMounted(() => {
  startBootSequence()
  document.body.style.overflow = 'hidden'
})

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <div class="ascii-login-page">
    <!-- CRT扫描线效果 -->
    <div class="ascii-crt-overlay"></div>
    <div class="ascii-scan-line"></div>

    <!-- 背景装饰 -->
    <div class="ascii-bg-decoration">
      <div class="ascii-grid"></div>
      <div class="ascii-binary-rain">
        <span v-for="i in 20" :key="i" class="binary-column" :style="{ animationDelay: `${i * 0.1}s` }">
          {{ Array(30).fill(0).map(() => Math.random() > 0.5 ? '1' : '0').join('') }}
        </span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="ascii-login-container">
      <!-- 启动序列 -->
      <div v-if="!showLogin" class="ascii-boot-sequence">
        <div class="ascii-terminal-window">
          <div
            v-for="(line, index) in bootLines"
            :key="index"
            class="boot-line"
            :class="{ 'boot-line--current': index === currentBootLine - 1 }"
          >
            {{ line }}
          </div>
          <div v-if="bootLines.length > 0" class="boot-cursor">
            <span class="ascii-cursor"></span>
          </div>
        </div>
      </div>

      <!-- 登录表单 -->
      <transition name="ascii-fade">
        <div v-if="showLogin" class="ascii-login-form">
          <ASCIICard title="AUTHENTICATION REQUIRED" width="480px">
            <!-- Logo区域 -->
            <div class="ascii-logo-section">
              <ASCIIArtText text="RAVEN" font="block" :animate="true" />
              <p class="ascii-subtitle">:: REVIEW AGENT v2.0 ::</p>
            </div>

            <!-- 分隔线 -->
            <div class="ascii-divider">
              <span class="divider-char">-</span>
              <span class="divider-char">-</span>
              <span class="divider-char">-</span>
              <span class="divider-char">[</span>
              <span class="divider-text">LOGIN</span>
              <span class="divider-char">]</span>
              <span class="divider-char">-</span>
              <span class="divider-char">-</span>
              <span class="divider-char">-</span>
            </div>

            <!-- 输入表单 -->
            <div class="ascii-form">
              <ASCIIInput
                v-model="form.username"
                label="USERNAME"
                placeholder="ENTER_USERNAME"
                size="large"
                @enter="onSubmit"
              />

              <ASCIIInput
                v-model="form.password"
                label="PASSWORD"
                type="password"
                placeholder="ENTER_PASSWORD"
                size="large"
                @enter="onSubmit"
              />

              <!-- 操作区 -->
              <div class="ascii-actions">
                <ASCIIButton
                  type="primary"
                  size="large"
                  :loading="loading"
                  block
                  @click="onSubmit"
                >
                  AUTHENTICATE
                </ASCIIButton>

                <div class="ascii-secondary-action">
                  <span class="action-text">NO_ACCOUNT?</span>
                  <ASCIIButton type="default" size="small" @click="goToRegister">
                    REGISTER
                  </ASCIIButton>
                </div>
              </div>
            </div>

            <!-- 底部信息 -->
            <div class="ascii-footer">
              <div class="ascii-status-bar">
                <span class="status-indicator">●</span>
                <span class="status-text">SYSTEM ONLINE</span>
                <span class="status-separator">|</span>
                <span class="status-text">SECURE CONNECTION</span>
              </div>
            </div>
          </ASCIICard>
        </div>
      </transition>
    </div>

    <!-- 版本信息 -->
    <div class="ascii-version">
      v2.0.1 | BUILD 20250307
    </div>
  </div>
</template>

<style>
/* 导入ASCII主题 */
@import '../styles/ascii-theme.scss';

/* 应用主题类 */
.ascii-login-page {
  composes: ascii-theme;
}
</style>

<style scoped>
.ascii-login-page {
  min-height: 100vh;
  background: var(--ascii-bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  font-family: var(--ascii-font-mono);
}

/* 背景装饰 */
.ascii-bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.ascii-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 255, 65, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 65, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* 二进制雨效果 */
.ascii-binary-rain {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: space-around;
  font-size: 12px;
  color: var(--ascii-text-muted);
  opacity: 0.1;
  overflow: hidden;
}

.binary-column {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  animation: rain 10s linear infinite;
}

@keyframes rain {
  0% {
    transform: translateY(-100%);
    opacity: 0;
  }
  10% {
    opacity: 0.3;
  }
  90% {
    opacity: 0.3;
  }
  100% {
    transform: translateY(100vh);
    opacity: 0;
  }
}

/* 主容器 */
.ascii-login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 500px;
  padding: 20px;
}

/* 启动序列 */
.ascii-boot-sequence {
  min-height: 200px;
}

.ascii-terminal-window {
  background: var(--ascii-bg-secondary);
  border: 1px solid var(--ascii-border-color);
  padding: var(--ascii-space-lg);
  font-size: 0.875rem;
}

.boot-line {
  color: var(--ascii-ansi-green);
  line-height: 1.8;
  opacity: 0.8;
}

.boot-line--current {
  opacity: 1;
  text-shadow: var(--ascii-glow-text);
}

.boot-cursor {
  display: inline-block;
  margin-top: var(--ascii-space-sm);
}

.ascii-cursor {
  display: inline-block;
  width: 0.6em;
  height: 1.2em;
  background-color: var(--ascii-ansi-green);
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 登录表单 */
.ascii-login-form {
  animation: form-appear 0.5s ease-out;
}

@keyframes form-appear {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Logo区域 */
.ascii-logo-section {
  text-align: center;
  margin-bottom: var(--ascii-space-md);
}

.ascii-logo-section :deep(.ascii-art-text) {
  justify-content: center;
}

.ascii-subtitle {
  color: var(--ascii-text-muted);
  font-size: 0.75rem;
  margin-top: var(--ascii-space-sm);
  letter-spacing: 0.2em;
}

/* 分隔线 */
.ascii-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--ascii-border-color);
  margin: var(--ascii-space-md) 0;
  font-size: 0.75rem;
}

.divider-text {
  color: var(--ascii-text-secondary);
  padding: 0 var(--ascii-space-sm);
  letter-spacing: 0.1em;
}

/* 表单 */
.ascii-form {
  display: flex;
  flex-direction: column;
  gap: var(--ascii-space-md);
}

/* 操作区 */
.ascii-actions {
  margin-top: var(--ascii-space-md);
  display: flex;
  flex-direction: column;
  gap: var(--ascii-space-md);
}

.ascii-secondary-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ascii-space-sm);
}

.action-text {
  color: var(--ascii-text-muted);
  font-size: 0.75rem;
}

/* 状态栏 */
.ascii-footer {
  margin-top: var(--ascii-space-lg);
  padding-top: var(--ascii-space-md);
  border-top: 1px solid var(--ascii-border-color);
}

.ascii-status-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ascii-space-sm);
  font-size: 0.625rem;
  color: var(--ascii-text-muted);
}

.status-indicator {
  color: var(--ascii-ansi-green);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-separator {
  color: var(--ascii-border-color);
}

/* 版本信息 */
.ascii-version {
  position: fixed;
  bottom: var(--ascii-space-md);
  right: var(--ascii-space-md);
  font-size: 0.625rem;
  color: var(--ascii-text-muted);
  z-index: 100;
}

/* 过渡动画 */
.ascii-fade-enter-active,
.ascii-fade-leave-active {
  transition: all 0.5s ease;
}

.ascii-fade-enter-from,
.ascii-fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
