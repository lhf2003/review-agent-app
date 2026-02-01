<script setup>
import { ref, computed, watch } from 'vue'

/**
 * 判断题组件
 * 简洁的是/否选择
 */
const props = defineProps({
  // 题目内容
  question: {
    type: String,
    required: true
  },
  // 用户答案
  userAnswer: {
    type: String,
    default: null
  },
  // 是否已提交
  isSubmitted: {
    type: Boolean,
    default: false
  },
  // 正确答案
  correctAnswer: {
    type: String,
    required: true
  },
  // 是否紧凑模式
  compact: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'answer-selected',
  'answer-changed'
])

// 当前选中值
const selectedValue = ref(null)

// 悬停状态
const isHoveredTrue = ref(false)
const isHoveredFalse = ref(false)

// 检查当前选中是否正确
const isCorrect = computed(() => {
  if (!selectedValue.value) return false
  return selectedValue.value.toLowerCase() === props.correctAnswer.toLowerCase()
})

// 检查已提交后的显示答案
const displayAnswer = computed(() => {
  if (!props.userAnswer) return null
  return props.userAnswer.toLowerCase()
})

// 检查是否应该显示正确答案
const shouldShowCorrect = computed(() => {
  return props.isSubmitted && isCorrect.value
})

// 处理选择
const handleSelect = (value) => {
  if (props.isSubmitted) return
  
  selectedValue.value = value
  
  emit('answer-selected', value.toLowerCase())
  emit('answer-changed', value.toLowerCase())
}

// 监听 props.userAnswer 变化
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer) {
    selectedValue.value = newAnswer.toLowerCase()
  }
}, { immediate: true })

// 暴露重置方法
defineExpose({
  resetSelection: () => {
    selectedValue.value = null
    isHoveredTrue.value = false
    isHoveredFalse.value = false
  }
})
</script>

<template>
  <div 
    class="true-false-question"
    :class="{
      'compact-mode': compact,
      'is-submitted': isSubmitted
    }"
  >
    <!-- 题目文本 -->
    <div class="question-text">
      {{ question }}
    </div>

    <!-- 选项卡片 -->
    <div class="options-container">
      <!-- 选项 -->
      <div 
        class="option-item option-true"
        :class="{
          'is-selected': displayAnswer === 'true' && selectedValue === 'true',
          'is-show-correct': shouldShowCorrect && props.correctAnswer.toLowerCase() === 'true',
          'is-hovered': isHoveredTrue,
          'is-disabled': isSubmitted
        }"
        @mouseenter="isHoveredTrue = true"
        @mouseleave="isHoveredTrue = false"
        @click="handleSelect('true')"
      >
        <div class="option-content">
          <span class="option-label">是</span>
          <span class="option-value">YES</span>
        </div>
        
        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted" class="answer-icon">
            <el-icon v-if="displayAnswer === 'true' && selectedValue === 'true'" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else-if="displayAnswer === 'true' && selectedValue === 'false'" color="#f56c6c">
              <CircleCloseFilled />
            </el-icon>
          </div>
        </transition>
      </div>

      <!-- 否选项 -->
      <div 
        class="option-item option-false"
        :class="{
          'is-selected': displayAnswer === 'false' && selectedValue === 'false',
          'is-show-correct': shouldShowCorrect && props.correctAnswer.toLowerCase() === 'false',
          'is-hovered': isHoveredFalse,
          'is-disabled': isSubmitted
        }"
        @mouseenter="isHoveredFalse = true"
        @mouseleave="isHoveredFalse = false"
        @click="handleSelect('false')"
      >
        <div class="option-content">
          <span class="option-label">否</span>
          <span class="option-value">NO</span>
        </div>
        
        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted" class="answer-icon">
            <el-icon v-if="displayAnswer === 'false' && selectedValue === 'false'" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else-if="displayAnswer === 'false' && selectedValue === 'false'" color="#f56c6c">
              <CircleCloseFilled />
            </el-icon>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.true-false-question {
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #67c23a;
  --danger-color: #f56c6c;
  --true-bg: #ecfdf5;
  --false-bg: #fef2f2;
  --true-hover: #d1fae5;
  --false-hover: #fee2e2;

  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: var(--transition-base);
  position: relative;

  &.compact-mode {
    padding: 12px;
    margin-bottom: 8px;
  }

  &.is-submitted .option-item {
    cursor: not-allowed;
  }
  
  .question-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.6;
    margin-bottom: 20px;
  }
  
  .options-container {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }
  
  .option-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px;
    border-radius: 16px;
    border: 2px solid var(--el-border-color);
    background: var(--el-fill-color-blank);
    cursor: pointer;
    transition: var(--transition-base);
    min-height: 120px;
    
    &.option-true {
      border-color: var(--el-border-color-light);
      
      &:hover:not(.is-disabled) {
        background: var(--true-hover);
        border-color: var(--primary-color);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(103, 194, 58, 0.08);
      }
    }
    
    &.option-false {
      border-color: var(--el-border-color-light);
      
      &:hover:not(.is-disabled) {
        background: var(--false-hover);
        border-color: var(--primary-color);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(103, 194, 58, 0.08);
      }
    }
    
    &.is-selected {
      border-width: 3px;
      box-shadow: 0 0 16px rgba(0, 0, 0, 0.1);
    }
    
    &.option-true.is-selected {
      border-color: var(--success-color);
      background: var(--true-bg);
    }
    
    &.option-false.is-selected {
      border-color: var(--success-color);
      background: var(--false-bg);
    }
    
    &.is-show-correct {
      border-color: var(--success-color) !important;
      background: var(--true-bg) !important;
      animation: correct-pulse 1.5s ease-out;
    }
    
    &.is-disabled {
      opacity: 0.6;
      cursor: not-allowed;
      
      &:hover {
        transform: none;
        box-shadow: none;
        border-color: var(--el-border-color);
        background: var(--el-fill-color-blank);
      }
    }
  }
  
  .option-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  
  .option-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-regular);
    text-transform: uppercase;
  }
  
  .option-value {
    font-size: 32px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }
  
  .answer-icon {
    position: absolute;
    right: 20px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 28px;
  }
  
  @keyframes correct-pulse {
    0%, 100% {
      box-shadow: 0 0 0 rgba(103, 194, 58, 0);
    }
    50% {
      box-shadow: 0 0 20px rgba(103, 194, 58, 0.3);
    }
  }
  
  .icon-fade-enter-active,
  .icon-fade-leave-active {
    transition: all 0.3s ease;
  }
  
  .icon-fade-enter-from {
    opacity: 0;
    transform: translateY(-50%) scale(0.5);
  }
  
  .icon-fade-leave-to {
    opacity: 0;
    transform: translateY(-50%) scale(1.2);
  }
  
  .icon-fade-enter-to {
    opacity: 1;
    transform: translateY(-50%) scale(1);
  }
  
  .icon-fade-leave-from {
    opacity: 1;
    transform: translateY(-50%) scale(1);
  }
}

// 深色模式
.dark .true-false-question {
    background: rgba(28, 28, 30, 0.75);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  }
  
  .dark .question-text {
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .option-label {
    color: rgba(255, 255, 255, 0.7);
  }
  
  .dark .option-value {
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .option-item {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    
    &.option-true {
      &:hover:not(.is-disabled) {
        background: rgba(255, 255, 255, 0.1);
      }
    }
    
    &.option-false {
      &:hover:not(.is-disabled) {
        background: rgba(255, 255, 255, 0.1);
      }
    }
  }
</style>
