<script setup>
/**
 * ASCII风格按钮组件
 * 模拟终端按钮风格: [ 按钮文本 ]
 */
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'success', 'warning', 'danger', 'default'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  block: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const sizeClasses = computed(() => ({
  'ascii-btn--small': props.size === 'small',
  'ascii-btn--medium': props.size === 'medium',
  'ascii-btn--large': props.size === 'large'
}))

const typeClasses = computed(() => ({
  'ascii-btn--primary': props.type === 'primary',
  'ascii-btn--success': props.type === 'success',
  'ascii-btn--warning': props.type === 'warning',
  'ascii-btn--danger': props.type === 'danger',
  'ascii-btn--default': props.type === 'default'
}))

function handleClick(e) {
  if (!props.loading && !props.disabled) {
    emit('click', e)
  }
}
</script>

<template>
  <button
    class="ascii-btn"
    :class="[sizeClasses, typeClasses, { 'ascii-btn--block': block }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span class="ascii-btn__bracket ascii-btn__bracket--left">[</span>
    <span class="ascii-btn__content">
      <span v-if="loading" class="ascii-btn__spinner">◐</span>
      <slot />
    </span>
    <span class="ascii-btn__bracket ascii-btn__bracket--right">]</span>
  </button>
</template>

<style scoped>
.ascii-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5em;
  font-family: var(--ascii-font-mono);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--ascii-text-primary);
  transition: all 0.2s ease;
  position: relative;
  white-space: nowrap;
}

.ascii-btn:hover:not(:disabled) {
  text-shadow: var(--ascii-glow-text);
}

.ascii-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 尺寸变体 */
.ascii-btn--small {
  font-size: 0.75rem;
  padding: 0.25rem 0;
}

.ascii-btn--medium {
  font-size: 0.875rem;
  padding: 0.5rem 0;
}

.ascii-btn--large {
  font-size: 1rem;
  padding: 0.75rem 0;
}

.ascii-btn--block {
  width: 100%;
}

/* 方括号样式 */
.ascii-btn__bracket {
  color: var(--ascii-text-muted);
  transition: color 0.2s ease;
}

.ascii-btn:hover:not(:disabled) .ascii-btn__bracket {
  color: var(--ascii-text-primary);
}

.ascii-btn__bracket--left {
  margin-right: 0.5em;
}

.ascii-btn__bracket--right {
  margin-left: 0.5em;
}

/* 内容区域 */
.ascii-btn__content {
  display: flex;
  align-items: center;
  gap: 0.5em;
}

/* 加载动画 */
.ascii-btn__spinner {
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 类型变体 */
.ascii-btn--primary {
  color: var(--ascii-ansi-green);
}

.ascii-btn--primary:hover:not(:disabled) {
  color: var(--ascii-text-bright);
  text-shadow: var(--ascii-glow-green);
}

.ascii-btn--success {
  color: var(--ascii-ansi-green);
}

.ascii-btn--warning {
  color: var(--ascii-ansi-yellow);
}

.ascii-btn--danger {
  color: var(--ascii-ansi-red);
}

.ascii-btn--default {
  color: var(--ascii-text-secondary);
}
</style>
