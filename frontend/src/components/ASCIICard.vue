<script setup>
/**
 * ASCII风格卡片组件
 * 模拟终端窗口: +-------------+
 *              |  标题       |
 *              +-------------+
 *              | 内容...     |
 *              +-------------+
 */
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: 'auto'
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  noBorder: {
    type: Boolean,
    default: false
  }
})

const cardStyle = computed(() => ({
  width: props.width !== 'auto' ? props.width : undefined
}))
</script>

<template>
  <div class="ascii-card" :class="{ 'ascii-card--no-border': noBorder }" :style="cardStyle">
    <!-- 顶部边框 -->
    <div v-if="!noBorder" class="ascii-card__border ascii-card__border--top">
      <span class="ascii-card__corner">┌</span>
      <span class="ascii-card__line">─</span>
      <span class="ascii-card__corner">┐</span>
    </div>

    <!-- 标题区域 -->
    <div v-if="showHeader && title" class="ascii-card__header">
      <div v-if="!noBorder" class="ascii-card__side">│</div>
      <div class="ascii-card__title">
        <span v-if="!title" class="ascii-card__title-text">
          <slot name="header">{{ title }}</slot>
        </span>
        <span v-else class="ascii-card__title-text">{{ title }}</span>
      </div>
      <div v-if="!noBorder" class="ascii-card__side">│</div>
    </div>

    <!-- 标题与内容分隔线 -->
    <div v-if="!noBorder && showHeader && title" class="ascii-card__border">
      <span class="ascii-card__corner">├</span>
      <span class="ascii-card__line">─</span>
      <span class="ascii-card__corner">┤</span>
    </div>

    <!-- 内容区域 -->
    <div class="ascii-card__body">
      <div v-if="!noBorder" class="ascii-card__side">│</div>
      <div class="ascii-card__content">
        <slot />
      </div>
      <div v-if="!noBorder" class="ascii-card__side">│</div>
    </div>

    <!-- 底部边框 -->
    <div v-if="!noBorder" class="ascii-card__border ascii-card__border--bottom">
      <span class="ascii-card__corner">└</span>
      <span class="ascii-card__line">─</span>
      <span class="ascii-card__corner">┘</span>
    </div>
  </div>
</template>

<style scoped>
.ascii-card {
  font-family: var(--ascii-font-mono);
  color: var(--ascii-text-primary);
  display: flex;
  flex-direction: column;
}

.ascii-card--no-border {
  background: var(--ascii-bg-card);
  border: 1px solid var(--ascii-border-color);
  padding: var(--ascii-space-md);
}

/* 边框行 */
.ascii-card__border {
  display: flex;
  align-items: center;
  white-space: nowrap;
  overflow: hidden;
  line-height: 1;
}

.ascii-card__corner {
  color: var(--ascii-text-muted);
  flex-shrink: 0;
}

.ascii-card__line {
  color: var(--ascii-border-color);
  flex: 1;
  overflow: hidden;
  letter-spacing: -0.1em;
}

/* 头部 */
.ascii-card__header {
  display: flex;
  align-items: center;
  gap: var(--ascii-space-sm);
}

.ascii-card__title {
  flex: 1;
  padding: var(--ascii-space-sm) 0;
}

.ascii-card__title-text {
  color: var(--ascii-text-bright);
  font-weight: 600;
}

/* 侧边 */
.ascii-card__side {
  color: var(--ascii-text-muted);
  flex-shrink: 0;
  padding: 0 var(--ascii-space-xs);
}

/* 内容体 */
.ascii-card__body {
  display: flex;
  align-items: stretch;
  gap: var(--ascii-space-sm);
}

.ascii-card__content {
  flex: 1;
  padding: var(--ascii-space-sm) 0;
  min-height: 0;
}

/* 底部边框特殊处理 */
.ascii-card__border--bottom {
  margin-top: auto;
}
</style>
