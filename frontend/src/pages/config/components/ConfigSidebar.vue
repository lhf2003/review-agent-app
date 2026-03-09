<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Monitor, Bell, Connection, Cpu, InfoFilled, BellFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const navItems = [
  {
    group: '通用设置',
    items: [
      { path: '/config/basic', label: '基本信息', icon: User },
      { path: '/config/scan', label: '扫描配置', icon: Monitor },
      { path: '/config/push', label: '推送配置', icon: Bell },
      { path: '/config/notification', label: '通知设置', icon: BellFilled }
    ]
  },
  {
    group: '模型设置',
    items: [
      { path: '/config/model-provider', label: '模型提供商', icon: Connection },
      { path: '/config/default-model', label: '默认模型', icon: Cpu }
    ]
  },
  {
    group: '其他',
    items: [
      { path: '/config/about', label: '关于我们', icon: InfoFilled, level1: true }
    ]
  }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const navigate = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="nav-menu">
    <template v-for="group in navItems" :key="group.group">
      <div class="nav-group-title">{{ group.group }}</div>
      <div
        v-for="item in group.items"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path), 'level-1-item': item.level1 }"
        @click="navigate(item.path)"
      >
        <el-icon class="nav-icon">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: 1px solid transparent;
}

.nav-item:hover {
  background-color: var(--glass-surface-hover);
  color: var(--text-primary);
  border-color: var(--glass-border-hover);
}

.nav-item.active {
  background-color: var(--accent-primary);
  color: #fff;
  border-color: var(--accent-primary);
  box-shadow: 0 4px 16px var(--accent-glow-soft);
}

.nav-group-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  padding: 8px 12px;
  margin-top: 8px;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.nav-item.level-1-item {
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
}
</style>
