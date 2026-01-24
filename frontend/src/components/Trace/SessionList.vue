<script setup>
import { computed } from 'vue'
import { ChatLineRound, Document, Warning, CircleCheckFilled } from '@element-plus/icons-vue'

const props = defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  activeSessionIndex: {
    type: Number,
    default: -1
  }
})

const emit = defineEmits(['select'])

function handleSelect(index) {
  emit('select', index)
}
</script>

<template>
  <div class="session-list">
    <div class="list-header">
      <span class="header-title">审查会话</span>
      <span class="header-count">{{ sessions.length }}</span>
    </div>
    <div class="list-content custom-scrollbar">
      <div 
        v-for="(session, index) in sessions" 
        :key="index"
        class="session-item"
        :class="{ active: index === activeSessionIndex }"
        @click="handleSelect(index)"
      >
        <div class="item-icon">
          <el-icon v-if="session.problem" class="icon-problem"><Warning /></el-icon>
          <el-icon v-else class="icon-normal"><ChatLineRound /></el-icon>
        </div>
        <div class="session-info">
          <div class="session-title">
            <span class="title-text">会话 #{{ index + 1 }}</span>
          </div>
          <div class="session-desc">{{ session.problemStatement || '暂无描述' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.session-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: transparent;
}

.list-header {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.header-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.header-count {
  font-size: 12px;
  background-color: var(--el-fill-color);
  padding: 2px 8px;
  border-radius: 10px;
  color: var(--el-text-color-secondary);
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: 1px solid transparent;
}

.session-item:hover {
  background-color: var(--el-fill-color-light);
}

.session-item.active {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}

:global(html.dark) .session-item.active {
  background-color: var(--el-color-primary-light-9); /* Element Plus dark theme handles this opacity usually, but explicit override might be needed if using light-9 directly */
  background-color: rgba(var(--el-color-primary-rgb), 0.15);
  border-color: rgba(var(--el-color-primary-rgb), 0.3);
}

.item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-fill-color);
  margin-right: 12px;
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
}

.session-item.active .item-icon {
  background-color: var(--el-color-primary);
  color: #ffffff;
}

.icon-problem {
  color: var(--el-color-danger);
}

.session-item.active .icon-problem {
  color: #ffffff;
}

.session-item.active .item-icon:has(.icon-problem) {
  background-color: var(--el-color-danger);
}

.session-info {
  flex: 1;
  overflow: hidden;
  padding-top: 2px;
}

.session-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.title-text {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.time-tag {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.session-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

/* Custom Scrollbar for Webkit */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: var(--el-text-color-secondary);
}
</style>
