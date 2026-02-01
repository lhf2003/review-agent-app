<script setup>
import { ref, computed, watch } from 'vue'

/**
 * 多选题组件
 * 支持复选框，支持多答案提交
 */
const props = defineProps({
  // 题目内容
  question: {
    type: String,
    required: true
  },
  // 选项列表
  options: {
    type: Array,
    required: true,
    validator: (value) => value && value.length >= 2 && value.length <= 6
  },
  // 用户已选择的答案（逗号分隔的字母，如 "A,B,D"）
  userAnswer: {
    type: String,
    default: null
  },
  // 是否已提交
  isSubmitted: {
    type: Boolean,
    default: false
  },
  // 正确答案（逗号分隔）
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

// 选中的选项索引集合
const selectedIndices = ref(new Set())

// 悬停的选项索引
const hoveredIndex = ref(-1)

// 当前选项字母（A, B, C, D, E, F）
const optionLetters = computed(() => {
  const letters = ['A', 'B', 'C', 'D', 'E', 'F']
  return (idx) => letters[idx] || ''
})

// 检查选项是否正确
const isCorrectOption = (index) => {
  const correctAnswers = props.correctAnswer.split(',')
  const currentLetter = optionLetters.value(index)
  return correctAnswers.includes(currentLetter)
}

// 检查选项是否被用户选中
const isSelected = (index) => {
  if (!props.userAnswer) return false
  const userAnswers = props.userAnswer.split(',')
  const currentLetter = optionLetters.value(index)
  return userAnswers.includes(currentLetter)
}

// 检查选项是否被选中但错误
const isSelectedWrong = (index) => {
  return isSelected(index) && !isCorrectOption(index)
}

// 检查选项是否部分正确（部分选中但未全对）
const isPartiallyCorrect = (index) => {
  return isSelected(index) && isCorrectOption(index)
}

// 检查是否应该显示正确答案
const shouldShowCorrect = (index) => {
  return props.isSubmitted && isCorrectOption(index)
}

// 处理选项点击
const handleOptionClick = (index, option) => {
  if (props.isSubmitted) return

  const currentLetter = optionLetters.value(index)
  
  if (selectedIndices.value.has(index)) {
    // 取消选中
    selectedIndices.value.delete(index)
  } else {
    // 添加选中
    selectedIndices.value.add(index)
  }
  
  // 触发事件
  emitSelectedAnswers()
}

// 监听 props.userAnswer 变化，同步选中状态
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer && newAnswer.length > 0) {
    const userAnswers = newAnswer.split(',')
    selectedIndices.value.clear()
    
    userAnswers.forEach(letter => {
      const index = optionLetters.value.findIndex(l => l === letter.toUpperCase())
      if (index >= 0) {
        selectedIndices.value.add(index)
      }
    })
  }
}, { immediate: true })

// 触发选中答案事件
const emitSelectedAnswers = () => {
  const selectedLetters = Array.from(selectedIndices.value)
    .map(index => optionLetters.value(index))
    .sort()
    .join(',')
  
  emit('answer-selected', selectedLetters)
  emit('answer-changed', selectedLetters)
}

// 暴露给父组件的重置方法
defineExpose({
  resetSelection: () => {
    selectedIndices.value.clear()
    hoveredIndex.value = -1
  }
})
</script>

<template>
  <div 
    class="multiple-choice-question"
    :class="{
      'compact-mode': compact,
      'has-options': options && options.length > 0
    }"
  >
    <!-- 题目文本 -->
    <div class="question-text">
      {{ question }}
    </div>

    <!-- 选项列表 -->
    <div class="options-list">
      <div
        v-for="(option, idx) in options"
        :key="idx"
        class="option-item"
        :class="{
          'is-selected': isSelected(idx),
          'is-correct': shouldShowCorrect(idx),
          'is-wrong': isSelectedWrong(idx),
          'is-partially-correct': isPartiallyCorrect(idx),
          'is-hovered': hoveredIndex === idx,
          'is-disabled': isSubmitted
        }"
        @mouseenter="hoveredIndex = idx"
        @mouseleave="hoveredIndex = -1"
        @click="handleOptionClick(idx, option)"
      >
        <label class="option-label">
          <input 
            type="checkbox" 
            class="option-checkbox"
            :checked="isSelected(idx)"
            :disabled="isSubmitted"
            @click.prevent.stop="handleOptionClick(idx, option)"
          >

          <div class="option-marker">
            <span class="marker-inner">
              <template v-if="isSelected(idx)">
                <span v-if="isCorrectOption(idx)">✓</span>
                <span v-else>•</span>
              </template>
            </span>
          </div>
          
          <div class="option-content">
            {{ option.substring(2) || option }}
          </div>
        </label>
        
        <!-- 答题后显示的图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted" class="answer-status-icon">
            <el-icon v-if="isCorrectOption(idx)" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else-if="isSelectedWrong(idx)" color="#f56c6c">
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

.multiple-choice-question {
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #67c23a;
  --danger-color: #f56c6c;
  --success-light: #f0f9ff;
  --danger-light: #fee;

  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: var(--transition-base);

  &.compact-mode {
    padding: 12px;
    margin-bottom: 8px;
  }
  
  .question-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.6;
    margin-bottom: 20px;
  }
  
  .options-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 12px;
  }
  
  .option-item {
    position: relative;
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
    border: 2px solid var(--el-border-color);
    border-radius: 12px;
    cursor: pointer;
    transition: var(--transition-base);
    background: var(--el-fill-color-blank);
    
    &:hover:not(.is-disabled) {
      border-color: var(--primary-color);
      background: var(--el-color-primary-light-9);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(103, 194, 58, 0.12);
    }
    
    &.is-hovered:not(.is-disabled):not(.is-selected) {
      border-color: var(--primary-color);
      background: var(--el-color-primary-light-9);
    }
    
    &.is-selected {
      border-color: var(--primary-color);
      background: var(--el-color-primary-light-9);
      box-shadow: 0 0 0 1px rgba(103, 194, 58, 0.1);
    }
    
    &.is-correct {
      border-color: var(--success-color);
      background: var(--success-light);
      box-shadow: 0 0 0 1px rgba(103, 194, 58, 0.1);
    }
    
    &.is-wrong {
      border-color: var(--danger-color);
      background: var(--danger-light);
      animation: shake 0.5s ease-in-out;
    }
    
    &.is-disabled {
      opacity: 0.5;
      cursor: not-allowed;
      
      &:hover {
        transform: none;
        box-shadow: none;
      }
    }
  }
  
  .option-label {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    cursor: pointer;
  }
  
  .option-checkbox {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
  
  .option-marker {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    border: 2px solid var(--el-border-color);
    background: var(--el-fill-color-dark);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    transition: var(--transition-base);
  }
  
  .option-item.is-selected .option-marker {
    background: var(--primary-color);
    border-color: var(--primary-color);
  }
  
  .option-item.is-correct .option-marker {
    background: var(--success-color);
    border-color: var(--success-color);
  }
  
  .option-item.is-wrong .option-marker {
    background: var(--danger-color);
    border-color: var(--danger-color);
    animation: pulse 0.5s ease-in-out;
  }
  
  .marker-inner {
    color: white;
    font-size: 14px;
  }
  
  .option-content {
    flex: 1;
    font-size: 15px;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    word-break: break-word;
  }
  
  .option-item.is-selected .option-content,
  .option-item.is-correct .option-content {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
  
  .option-item.is-wrong .option-content {
    color: var(--danger-color);
    text-decoration: line-through;
  }
  
  .answer-status-icon {
    position: absolute;
    right: 16px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 18px;
  }
  
  .icon-fade-enter-active,
  .icon-fade-leave-active {
    transition: all 0.3s ease;
  }
  
  .icon-fade-enter-from,
  .icon-fade-leave-to {
    opacity: 0;
    transform: translateY(-50%) scale(0.8);
  }
  
  @keyframes shake {
    0%, 100% {
      transform: translateX(0);
    }
    10%, 30%, 50%, 70%, 90% {
      transform: translateX(-4px);
    }
    20%, 40%, 60%, 80% {
      transform: translateX(4px);
    }
  }
  
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
    }
  }
}

.dark .multiple-choice-question {
    background: rgba(28, 28, 30, 0.75);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  }
  
  .dark .option-marker {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .dark .option-item.is-selected {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.4);
  }
  
  .dark .option-item.is-correct {
    background: rgba(82, 196, 26, 0.2);
    border-color: rgba(82, 196, 26, 0.4);
  }
  
  .dark .option-item.is-wrong {
    background: rgba(239, 68, 68, 0.2);
    border-color: rgba(239, 68, 68, 0.4);
  }
</style>
