<script setup>
import { computed } from 'vue'
import { Document, FolderAdd } from '@element-plus/icons-vue'

/**
 * 节点详情面板组件
 * 显示流程图节点的详细信息
 */
const props = defineProps({
  node: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['viewSource', 'addToCollection'])

// 节点类型文本映射
const nodeTypeText = computed(() => {
  const map = {
    START: '开始',
    PROCESS: '过程',
    DECISION: '决策',
    END: '结束'
  }
  return map[props.node?.nodeType] || '未知'
})

// 节点类型标签样式映射
const nodeTypeTagType = computed(() => {
  const map = {
    START: 'success',
    PROCESS: 'primary',
    DECISION: 'warning',
    END: 'danger'
  }
  return map[props.node?.nodeType] || 'info'
})

// 查看原文
const viewSource = () => {
  emit('viewSource', props.node)
}

// 加入合集
const addToCollection = () => {
  emit('addToCollection', props.node)
}
</script>

<template>
  <div v-if="node" class="node-detail-panel">
    <div class="panel-header">
      <h4 class="node-title">{{ node.label }}</h4>
      <ElTag size="small" :type="nodeTypeTagType">{{ nodeTypeText }}</ElTag>
    </div>

    <div class="panel-content">
      <div class="description-section">
        <div class="section-label">描述</div>
        <p class="description">{{ node.description || '暂无描述' }}</p>
      </div>

      <div v-if="node.codeSnippet" class="code-section">
        <div class="section-label">代码片段</div>
        <pre class="code-snippet"><code>{{ node.codeSnippet }}</code></pre>
      </div>
    </div>

    <div class="panel-footer">
      <ElButton type="primary" size="small" @click="viewSource">
        <ElIcon><Document /></ElIcon>
        <span>查看原文</span>
      </ElButton>
      <ElButton size="small" @click="addToCollection">
        <ElIcon><FolderAdd /></ElIcon>
        <span>加入合集</span>
      </ElButton>
    </div>
  </div>

  <div v-else class="node-detail-panel empty">
    <div class="empty-content">
      <ElIcon :size="48" class="empty-icon"><Document /></ElIcon>
      <p>点击流程图节点查看详情</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.node-detail-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-bg-color-overlay);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  overflow: hidden;

  &.empty {
    align-items: center;
    justify-content: center;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

.node-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  word-break: break-word;
  flex: 1;
  margin-right: 12px;
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.description-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.description {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.code-section {
  margin-top: 16px;
}

.code-snippet {
  margin: 0;
  padding: 12px;
  background: var(--el-fill-color);
  border-radius: 8px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.panel-footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);

  .el-button {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }
}

// 空状态
.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-placeholder);
  text-align: center;

  .empty-icon {
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    margin: 0;
    font-size: 14px;
  }
}

// 深色模式适配
html.dark {
  .node-detail-panel {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }

  .panel-header {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }

  .code-snippet {
    background: rgba(255, 255, 255, 0.05);
  }

  .panel-footer {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
}
</style>
