<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Upload, Edit } from '@element-plus/icons-vue'
import { useChatStore } from './stores/chat'
import ChatBox from './components/ChatBox.vue'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const isAuthPage = computed(() => {
  return ['/login', '/register'].includes(route.path)
})

// 判断是否为一级路由（显示完整导航）
const isTopLevelRoute = computed(() => {
  const topLevelPaths = ['/dashboard', '/knowledge-graph', '/collections', '/tags', '/report', '/config']
  return topLevelPaths.includes(route.path) || route.path.startsWith('/config/')
})

// 判断是否为配置页面（显示配置侧边栏布局）
const isConfigPage = computed(() => route.path.startsWith('/config'))

// 导航配置
const navItems = [
  { name: 'Dashboard', label: '主页', path: '/dashboard' },
  { name: 'Knowledge Graph', label: '知识图谱', path: '/knowledge-graph' },
  { name: 'Collections', label: '合集', path: '/collections' },
  { name: 'Tags', label: '标签', path: '/tags' },
  { name: 'Reports', label: '报告', path: '/report' },
  { name: 'Config', label: '设置', path: '/config' }
]

// 当前活跃导航项
const activeNav = ref('Dashboard')

// 根据当前路由设置活跃导航
watch(() => route.path, (newPath) => {
  const currentNav = navItems.find(item => item.path === newPath)
  if (currentNav) {
    activeNav.value = currentNav.name
  }
}, { immediate: true })

function navigateTo(path, navName) {
  if (navName && navName !== activeNav.value) {
    activeNav.value = navName
  }
  router.push(path)
}

function handleImportLog() {
  router.push('/data')
}

function handleStartQuiz() {
  router.push('/quiz-history')
}

function handleOpenChat() {
  chatStore.open()
}
</script>

<template>
  <div v-if="isAuthPage" style="height:100vh; width: 100%;">
    <router-view />
  </div>
  <div v-else class="app-container">
    <!-- Global Navigation Header - 仅在一级路由显示 -->
    <header v-if="isTopLevelRoute" class="global-header">
      <div class="header-brand" @click="handleOpenChat">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="3" width="7" height="7" rx="1"/>
            <rect x="3" y="14" width="7" height="7" rx="1"/>
            <rect x="14" y="14" width="7" height="7" rx="1"/>
          </svg>
        </div>
      </div>

      <nav class="header-nav">
        <button
          v-for="item in navItems"
          :key="item.name"
          class="nav-item"
          :class="{ active: activeNav === item.name }"
          @click="navigateTo(item.path, item.name)"
        >
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="header-actions">
        <button class="action-btn secondary" @click="handleImportLog">
          <el-icon><Upload /></el-icon>
          <span>Import Log</span>
        </button>
        <button class="action-btn primary" @click="handleStartQuiz">
          <el-icon><Edit /></el-icon>
          <span>Start Quiz</span>
        </button>
      </div>
    </header>

    <!-- Main Content -->
    <div class="main-content" :class="{ 'config-layout': isConfigPage }">
      <router-view />
    </div>

    <!-- ChatBox -->
    <ChatBox />
  </div>
</template>

<style scoped>
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.main-content.config-layout {
  overflow: visible;
}

/* Global Header Styles */
.global-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 24px;
  background: linear-gradient(180deg, rgba(26, 15, 8, 0.3) 0%, rgba(26, 15, 8, 0.15) 100%);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(255, 248, 245, 0.05);
  flex-shrink: 0;
  z-index: 50;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 32px;
  cursor: pointer;
}

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 248, 245, 0.05);
  border: 1px solid rgba(255, 248, 245, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
}

/* ============ 导航样式 - 底部横线指示器 ============ */
.header-nav {
  position: relative;
  display: flex;
  gap: 4px;
  padding: 4px;
}

.nav-item {
  position: relative;
  padding: 8px 18px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.55);
  background: transparent;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: color 200ms ease, text-shadow 200ms ease;
}

/* 底部横线 - 默认隐藏 */
.nav-item::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  width: calc(100% - 20px);
  height: 2px;
  background: linear-gradient(90deg,
    rgba(255, 154, 0, 0) 0%,
    rgba(255, 154, 0, 0.8) 50%,
    rgba(255, 154, 0, 0) 100%
  );
  border-radius: 1px;
  transform: translateX(-50%) scaleX(0);
  transform-origin: center;
  transition: transform 250ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 悬浮时横线从中间向两边展开 */
.nav-item:hover::after {
  transform: translateX(-50%) scaleX(1);
}

/* 悬浮效果 */
.nav-item:hover {
  color: rgba(255, 200, 120, 0.95);
  text-shadow: 0 0 20px rgba(255, 154, 0, 0.5);
}

/* 激活状态 - 文字高亮 */
.nav-item.active {
  color: #fff8f5;
  text-shadow: 0 0 10px rgba(255, 180, 80, 0.4);
}

/* 激活状态 - 横线保持显示 */
.nav-item.active::after {
  transform: translateX(-50%) scaleX(1);
  background: linear-gradient(90deg,
    rgba(255, 200, 120, 0) 0%,
    rgba(255, 200, 120, 1) 50%,
    rgba(255, 200, 120, 0) 100%
  );
}

/* 点击时的轻微缩放 */
.nav-item:active {
  transform: scale(0.98);
}

/* Reduced Motion Support */
@media (prefers-reduced-motion: reduce) {
  .nav-item::after {
    transition: none;
  }
  .nav-item {
    transition: none;
  }
}

/* ============ 操作按钮样式 ============ */
.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

.action-btn.secondary {
  background: rgba(255, 248, 245, 0.05);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 248, 245, 0.1);
}

.action-btn.secondary:hover {
  background: rgba(255, 248, 245, 0.1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-btn.primary {
  background: linear-gradient(135deg, #fff8f5 0%, #ffe8d6 100%);
  color: #1a0f08;
  box-shadow: 0 2px 8px rgba(255, 154, 0, 0.2);
}

.action-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 154, 0, 0.35);
}

.action-btn:active {
  transform: translateY(0) scale(0.98);
}

/* 按钮光扫效果 */
.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg,
    transparent,
    rgba(255, 255, 255, 0.2),
    transparent
  );
  transition: left 0.5s ease;
}

.action-btn:hover::before {
  left: 100%;
}

/* Responsive */
@media (max-width: 1024px) {
  .global-header {
    padding: 8px 16px;
  }

  .nav-item {
    padding: 6px 14px;
    font-size: 12px;
  }
}

@media (max-width: 900px) {
  .header-nav {
    display: none;
  }
}

@media (max-width: 768px) {
  .global-header {
    padding: 8px 12px;
  }

  .brand-text {
    display: none;
  }

  .action-btn span {
    display: none;
  }

  .action-btn {
    padding: 8px;
  }
}
</style>
