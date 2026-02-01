<script setup>
import { ref, computed, watch } from 'vue'

/**
 * 单选题组件
 * 支持标准单选题和代码识别题
 */
const props = defineProps({
  // 题目内容
  question: {
    type: String,
    required: true
  },
  // 选项列表
  options: {
    type: [Array, Object],
    required: true,
    validator: (value) => value && value.length >= 2 && value.length <= 6
  },
  // 用户已选择的答案
  userAnswer: {
    type: String,
    default: null
  },
  // 是否已提交（提交后禁用选项）
  isSubmitted: {
    type: Boolean,
    default: false
  },
  // 正确答案（用于高亮）
  correctAnswer: {
    type: String,
    required: true
  },
  // 题目索引（用于显示序号）
  index: {
    type: Number,
    default: 1
  },
  // 知识点（可选）
  knowledgePoint: {
    type: String,
    default: null
  },
  // 是否显示解析
  showExplanation: {
    type: Boolean,
    default: false
  },
  // 是否紧凑模式（用于列表显示）
  compact: {
    type: Boolean,
    default: false
  },
  // 题目类型（用于兼容）
  type: {
    type: String,
    default: 'single_choice'
  }
})

const emit = defineEmits([
  'answer-selected',
  'answer-changed'
])

// 选中的选项索引
const selectedIndex = ref(-1)
// 鼠标悬停的选项索引
const hoveredIndex = ref(-1)

// 当前选项字母（A, B, C, D, E, F）
const optionLetters = computed(() => {
  const letters = ['A', 'B', 'C', 'D', 'E', 'F']
  return (idx) => letters[idx] || ''
})

// 检查选项是否正确
const isCorrectAnswer = (index) => {
  return props.options[index] && props.options[index].startsWith(props.correctAnswer)
}

// 检查选项是否被用户选中
const isSelected = (index) => {
  return selectedIndex.value === index
}

// 检查选项是否已提交且答案错误
const isWrongAnswer = (index) => {
  return props.isSubmitted && 
         selectedIndex.value === index && 
         !isCorrectAnswer(index)
}

// 检查是否应该显示正确答案
const shouldShowCorrect = (index) => {
  return props.isSubmitted && isCorrectAnswer(index)
}

	// 处理选项选择
	const handleOptionSelect = (index, option) => {
	  if (props.isSubmitted) return

	  const selectedLetter = optionLetters.value(index)
	  emit('answer-selected', {
	    letter: selectedLetter,
	    fullOption: option,
	    index: index
	  })
	  emit('answer-changed', selectedLetter)
	}

// 监听 props.userAnswer 变化，同步选中状态
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer && newAnswer.length > 0) {
    const targetLetter = newAnswer.charAt(0).toUpperCase()
    const targetIndex = props.options.findIndex(opt => opt.startsWith(targetLetter))
    if (targetIndex >= 0) {
      selectedIndex.value = targetIndex
    }
  }
}, { immediate: true })

// 重置选中状态
const resetSelection = () => {
  selectedIndex.value = -1
}

// 暴露给父组件的重置方法
defineExpose({
  resetSelection
})
</script>

<template>
  <div 
    class="single-choice-question"
    :class="{
      'compact-mode': compact,
      'has-knowledge-point': !!knowledgePoint,
      'is-submitted': isSubmitted
    }"
  >
    <!-- 知识点标签 -->
    <div v-if="knowledgePoint && !compact" class="knowledge-badge">
      {{ knowledgePoint }}
    </div>

    <!-- 题号 -->
    <div v-if="!compact" class="question-number">
      Question {{ index }}
    </div>

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
          'is-wrong': isWrongAnswer(idx),
          'is-hovered': hoveredIndex === idx,
          'is-disabled': isSubmitted && !isSelected(idx)
        }"
        @mouseenter="hoveredIndex = idx"
        @mouseleave="hoveredIndex = -1"
        @click="handleOptionSelect(idx, option)"
      >
        <div class="option-marker">
          {{ optionLetters.value(idx) }}
        </div>
        <div class="option-content">
          {{ option.substring(2) || option }}
        </div>
        
        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted" class="answer-icon">
            <el-icon v-if="isCorrectAnswer(idx)" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else-if="isWrongAnswer(idx)" color="#f56c6c">
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

.single-choice-question {
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #67c23a;
  --danger-color: #f56c6c;

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
    
    .question-number {
      display: none;
    }
    
    .question-text {
      font-size: 14px;
    }
    
    .options-list {
      gap: 8px;
    }
    
    .option-item {
      padding: 10px 12px;
    }
    
    .option-marker {
      width: 24px;
      height: 24px;
      font-size: 13px;
    }
    
    .option-content {
      font-size: 13px;
    }
  }
  
  &.is-submitted {
    .option-item {
      cursor: not-allowed;
    }
  }
  
  // 知识点标签
  .knowledge-badge {
    display: inline-block;
    font-size: 12px;
    font-weight: 600;
    color: var(--primary-color);
    background: var(--el-color-primary-light-9);
    padding: 4px 12px;
    border-radius: 12px;
    margin-bottom: 12px;
  }
  
  // 题号
  .question-number {
    font-size: 13px;
    font-weight: 700;
    color: var(--el-text-color-secondary);
    margin-bottom: 8px;
    font-family: monospace;
    letter-spacing: 1px;
  }
  
  // 题目文本
  .question-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.6;
    margin-bottom: 20px;
  }
  
  // 选项列表
  .options-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  // 选项项
  .option-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    border: 1px solid var(--el-border-color);
    border-radius: 12px;
    cursor: pointer;
    transition: var(--transition-base);
    position: relative;
    
    &:hover:not(.is-disabled) {
      border-color: var(--primary-color);
      background-color: var(--el-color-primary-light-9);
      transform: translateX(4px);
    }
    
    &.is-hovered:not(.is-disabled):not(.is-selected):not(.is-correct) {
      border-color: var(--primary-color);
      background-color: var(--el-color-primary-light-9);
    }
  }
  
  // 选项标记
  .option-marker {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--el-fill-color-dark);
    color: var(--el-text-color-regular);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    flex-shrink: 0;
    transition: var(--transition-base);
  }
  
  // 选项内容
  .option-content {
    flex: 1;
    font-size: 15px;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    word-break: break-word;
  }
  
  // 选中状态
  .option-item.is-selected {
    border-color: var(--primary-color);
    background-color: var(--el-color-primary-light-9);
    box-shadow: 0 0 8px rgba(103, 194, 58, 0.15);
  }
  
  .option-item.is-selected .option-marker {
    background: var(--primary-color);
    color: white;
    transform: scale(1.1);
  }
  
  .option-item.is-selected .option-content {
    font-weight: 600;
    color: var(--primary-color);
  }
  
  // 正确答案状态
  .option-item.is-correct {
    border-color: var(--success-color);
    background-color: var(--el-color-success-light-9);
    animation: success-pulse 0.6s ease-in-out;
  }
  
  .option-item.is-correct .option-marker {
    background: var(--success-color);
    color: white;
  }
  
  .option-item.is-correct .option-content {
    color: var(--success-color);
    font-weight: 600;
  }
  
  // 错误答案状态
  .option-item.is-wrong {
    border-color: var(--danger-color);
    background-color: var(--el-color-danger-light-9);
    animation: shake 0.5s ease-in-out;
  }
  
  .option-item.is-wrong .option-marker {
    background: var(--danger-color);
    color: white;
    opacity: 0.8;
  }
  
  .option-item.is-wrong .option-content {
    color: var(--danger-color);
    text-decoration: line-through;
    opacity: 0.8;
  }
  
  // 禁用状态
  .option-item.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
    
    &:hover {
      transform: none;
      background-color: transparent;
      border-color: var(--el-border-color);
    }
  }
  
  // 答案图标
  .answer-icon {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
  }
  
  // 动画
  @keyframes success-pulse {
    0%, 100% {
      box-shadow: 0 0 8px rgba(103, 194, 58, 0.15);
    }
    50% {
      box-shadow: 0 0 16px rgba(103, 194, 58, 0.3);
    }
  }
  
  @keyframes shake {
    0%, 100% {
      transform: translateX(0);
    }
    20%, 60% {
      transform: translateX(-4px);
    }
    40%, 80% {
      transform: translateX(4px);
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
    transform: translateY(-50%) scale(0.5);
  }
}

// 深色模式
.dark .single-choice-question {
    background: rgba(28, 28, 30, 0.75);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  }
  
  .dark .option-marker {
    background: rgba(255, 255, 255, 0.15);
    color: rgba(255, 255, 255, 0.9);
  }
  
  .dark .option-item.is-selected {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.3);
  }
  
  .dark .option-item.is-selected .option-marker {
    background: rgba(255, 255, 255, 0.9);
    color: rgba(28, 28, 30, 1);
  }
  
  .dark .option-item.is-correct {
    background: rgba(82, 196, 26, 0.2);
    border-color: rgba(82, 196, 26, 0.4);
  }
  
  .dark .option-item.is-correct .option-marker {
    background: rgba(82, 196, 26, 0.9);
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .option-item.is-wrong {
    background: rgba(239, 68, 68, 0.2);
    border-color: rgba(239, 68, 68, 0.4);
  }
  
  .dark .option-item.is-wrong .option-marker {
    background: rgba(239, 68, 68, 0.9);
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .option-content {
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .question-text {
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .knowledge-badge {
    background: rgba(255, 255, 255, 0.15);
    color: rgba(255, 255, 255, 1);
  }
</style>
