<script setup>
import { computed } from 'vue'

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
      <span>会话列表 ({{ sessions.length }})</span>
    </div>
    <div class="list-content">
      <div 
        v-for="(session, index) in sessions" 
        :key="index"
        class="session-item"
        :class="{ active: index === activeSessionIndex }"
        @click="handleSelect(index)"
      >
        <div class="session-indicator" :class="session.problem ? 'has-problem' : 'info'"></div>
        <div class="session-info">
          <div class="session-title">Session {{ index + 1 }}</div>
          <div class="session-desc">{{ session.problemStatement ? session.problemStatement.slice(0, 30) + '...' : 'No description' }}</div>
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
  border-right: 1px solid var(--el-border-color);
  background-color: var(--el-bg-color);
}

.list-header {
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color);
  background-color: var(--el-fill-color-light);
}

.list-content {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background-color 0.2s;
}

.session-item:hover {
  background-color: var(--el-fill-color-light);
}

.session-item.active {
  background-color: var(--el-color-primary-light-9);
  border-left: 3px solid var(--el-color-primary);
}

.session-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.session-indicator.has-problem {
  background-color: var(--el-color-danger);
}

.session-indicator.info {
  background-color: var(--el-color-warning); /* Default to warning/info since it's analysis */
}

.session-info {
  overflow: hidden;
}

.session-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.session-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
