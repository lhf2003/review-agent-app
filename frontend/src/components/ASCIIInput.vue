<script setup>
/**
 * ASCII风格输入框组件
 * 模拟终端输入: > __________|
 */
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  disabled: {
    type: Boolean,
    default: false
  },
  showPassword: {
    type: Boolean,
    default: false
  },
  prefix: {
    type: String,
    default: ''
  },
  prefixIcon: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur', 'enter'])

const focused = ref(false)
const inputRef = ref(null)

const isPassword = computed(() => props.type === 'password' && !props.showPassword)
const displayValue = computed(() => {
  if (isPassword.value) {
    return '•'.repeat(props.modelValue.length)
  }
  return props.modelValue
})

const sizeClass = computed(() => ({
  'ascii-input--small': props.size === 'small',
  'ascii-input--medium': props.size === 'medium',
  'ascii-input--large': props.size === 'large'
}))

function handleInput(e) {
  emit('update:modelValue', e.target.value)
}

function handleFocus(e) {
  focused.value = true
  emit('focus', e)
}

function handleBlur(e) {
  focused.value = false
  emit('blur', e)
}

function handleKeydown(e) {
  if (e.key === 'Enter') {
    emit('enter', e)
  }
}

function focus() {
  inputRef.value?.focus()
}

defineExpose({
  focus
})
</script>

<template>
  <div class="ascii-input-wrapper" :class="sizeClass">
    <label v-if="label" class="ascii-input__label">
      {{ label }}
    </label>
    <div
      class="ascii-input"
      :class="{ 'ascii-input--focused': focused, 'ascii-input--disabled': disabled }"
      @click="focus"
    >
      <span class="ascii-input__prompt">&gt;</span>
      <span v-if="prefix" class="ascii-input__prefix">{{ prefix }}</span>
      <span v-else-if="prefixIcon" class="ascii-input__prefix-icon">
        <component :is="prefixIcon" />
      </span>
      <input
        ref="inputRef"
        :type="type === 'password' && showPassword ? 'text' : type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        class="ascii-input__field"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />
      <span v-if="focused" class="ascii-input__cursor"></span>
      <span class="ascii-input__underline">
        <span
          v-for="i in 30"
          :key="i"
          class="ascii-input__underline-char"
          :class="{ 'ascii-input__underline--filled': i <= displayValue.length + 1 }"
        >_</span>
      </span>
    </div>
  </div>
</template>

<style scoped>
.ascii-input-wrapper {
  font-family: var(--ascii-font-mono);
}

.ascii-input__label {
  display: block;
  color: var(--ascii-text-secondary);
  margin-bottom: var(--ascii-space-xs);
  font-size: 0.875rem;
}

.ascii-input {
  display: flex;
  align-items: center;
  gap: var(--ascii-space-xs);
  color: var(--ascii-text-primary);
  position: relative;
  cursor: text;
  padding: var(--ascii-space-xs) 0;
}

.ascii-input--focused {
  text-shadow: var(--ascii-glow-text);
}

.ascii-input--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 提示符 */
.ascii-input__prompt {
  color: var(--ascii-ansi-green);
  font-weight: 600;
  flex-shrink: 0;
}

/* 前缀 */
.ascii-input__prefix,
.ascii-input__prefix-icon {
  color: var(--ascii-text-muted);
  flex-shrink: 0;
}

/* 输入框 */
.ascii-input__field {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  background: transparent;
  border: none;
  outline: none;
  color: var(--ascii-text-primary);
  font-family: var(--ascii-font-mono);
  font-size: inherit;
  padding-left: 1.5em;
  cursor: inherit;
}

/* 光标 */
.ascii-input__cursor {
  display: inline-block;
  width: 0.6em;
  height: 1.2em;
  background-color: var(--ascii-cursor);
  animation: cursor-blink var(--ascii-cursor-blink) step-end infinite;
}

@keyframes cursor-blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 下划线 */
.ascii-input__underline {
  display: flex;
  color: var(--ascii-border-color);
  letter-spacing: -0.05em;
  flex: 1;
  overflow: hidden;
}

.ascii-input__underline-char {
  transition: color 0.2s ease;
}

.ascii-input__underline--filled {
  color: var(--ascii-text-muted);
}

.ascii-input--focused .ascii-input__underline--filled {
  color: var(--ascii-ansi-green);
}

/* 尺寸变体 */
.ascii-input--small {
  font-size: 0.75rem;
}

.ascii-input--medium {
  font-size: 0.875rem;
}

.ascii-input--large {
  font-size: 1rem;
}
</style>
