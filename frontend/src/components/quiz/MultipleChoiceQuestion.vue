<script setup>
import { ref, computed, watch } from 'vue'
import { CircleCloseFilled, InfoFilled } from '@element-plus/icons-vue'

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
  // 解析内容
  explanation: {
    type: String,
    default: ''
  },
  // 是否显示解析
  showExplanation: {
    type: Boolean,
    default: false
  },
  // 是否有解析内容
  hasExplanation: {
    type: Boolean,
    default: true
  },
  // 是否紧凑模式
  compact: {
    type: Boolean,
    default: false
  },
  // 题号
  index: {
    type: Number,
    default: 1
  },
  // 知识点
  knowledgePoint: {
    type: String,
    default: null
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

// 选项字母数组
const letters = ['A', 'B', 'C', 'D', 'E', 'F']

// 当前选项字母（A, B, C, D, E, F）
const optionLetters = computed(() => {
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
      const index = letters.findIndex(l => l === letter.toUpperCase())
      if (index >= 0) {
        selectedIndices.value.add(index)
      }
    })
  } else {
    // userAnswer 为空或 null 时，清空选中状态
    selectedIndices.value.clear()
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
          'is-wrong': isSelectedWrong(idx),
          'is-partially-correct': isPartiallyCorrect(idx),
          'is-hovered': hoveredIndex === idx,
          'is-disabled': isSubmitted
        }"
        @mouseenter="hoveredIndex = idx"
        @mouseleave="hoveredIndex = -1"
        @click="handleOptionClick(idx, option)"
      >
        <div class="option-label">
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
        </div>
        
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

    <!-- 答题后显示的解析 -->
    <transition name="fade">
      <div v-if="isSubmitted" class="explanation-box">
        <div class="explanation-header">
          <el-icon>
            <InfoFilled />
          </el-icon>
          <div class="explanation-title">
            {{ isCurrentAnswerCorrect ? '答案正确！' : '答案错误' }}
          </div>
        </div>
        <div class="explanation-content">
          <!-- 答错时显示正确答案 -->
          <div v-if="!isCurrentAnswerCorrect" class="explanation-text">
            <strong>正确答案：</strong>{{ correctAnswer }}
          </div>
          <!-- 有解析时显示解析内容 -->
          <div v-if="props.hasExplanation && props.explanation" class="explanation-detail">
            <p><strong>解析：</strong>{{ props.explanation }}</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.multiple-choice-question {
  --card-radius: 24px;
  --transition-spring: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  --transition-smooth: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #34c759;
  --danger-color: #ff3b30;

  background: var(--el-bg-color);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--card-radius);
  padding: 32px;
  margin-bottom: 24px;
  border: 1px solid var(--el-border-color-light);
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.02),
    0 10px 15px -3px rgba(0, 0, 0, 0.04),
    0 0 0 1px rgba(0, 0, 0, 0.02);
  transition: var(--transition-smooth);
  opacity: 0.95;

  &.compact-mode {
    padding: 20px;
    margin-bottom: 16px;
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--el-border-color-lighter);
    backdrop-filter: none;
    
    .question-text { font-size: 15px; margin-bottom: 16px; }
    .options-list { gap: 10px; grid-template-columns: 1fr; }
    .option-item { padding: 12px 16px; }
  }
  
  .question-text {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    margin-bottom: 32px;
    letter-spacing: -0.01em;
  }
  
  .options-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }
  
  .option-item {
    position: relative;
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 14px 20px;
    border: none; // Remove border
    border-radius: 16px;
    cursor: pointer;
    transition: background-color 0.2s ease, transform 0.2s ease;
    background: var(--el-fill-color-light);
    min-height: 64px;
    box-sizing: border-box;
    
    &:hover:not(.is-disabled) {
      background: var(--el-fill-color);
      transform: scale(1.01);
      z-index: 1;

      .option-marker {
        background: var(--el-color-primary-light-9);
        border-color: var(--primary-color);
      }
    }
    
    &.is-selected {
      background: var(--el-color-primary-light-9);
      box-shadow: none;
      z-index: 2;

      .option-marker {
        background: var(--primary-color);
        border-color: var(--primary-color);
        color: white;
        box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb), 0.3);
      }
      
      .option-content {
        color: var(--primary-color);
        font-weight: 600;
      }
    }
    
    &.is-correct {
      background: var(--el-color-success-light-9);
      box-shadow: none;
      z-index: 2;

      .option-marker {
        background: var(--success-color);
        border-color: var(--success-color);
        box-shadow: 0 2px 8px rgba(52, 199, 89, 0.3);
      }
      
      .option-content {
        color: #1a7f37;
        font-weight: 600;
      }
    }
    
    &.is-wrong {
      background: var(--el-color-danger-light-9);
      animation: shake 0.5s ease-in-out;

      .option-marker {
        background: var(--danger-color);
        border-color: var(--danger-color);
      }
      
      .option-content {
        color: var(--danger-color);
        text-decoration: line-through;
        opacity: 0.8;
      }
    }
    
    &.is-disabled {
      opacity: 0.6;
      cursor: default;
      filter: grayscale(0.5);

      &:hover {
        transform: none;
        box-shadow: none;
        background: var(--el-fill-color-light);
      }
    }
  }
  
  .option-label {
    display: flex;
    align-items: center;
    gap: 16px;
    width: 100%;
    cursor: pointer;
  }
  
  .option-checkbox {
    display: none; // Hide native checkbox
  }
  
  .option-marker {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    border: 2px solid rgba(0, 0, 0, 0.15);
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: var(--transition-smooth);
    flex-shrink: 0;
    
    .marker-inner {
      font-size: 16px;
      font-weight: 800;
      color: white;
      line-height: 1;
    }
  }
  
    // Knowledge Badge
  .knowledge-badge {
    display: inline-flex;
    align-items: center;
    font-size: 12px;
    font-weight: 600;
    color: var(--primary-color);
    background: rgba(var(--el-color-primary-rgb), 0.1);
    padding: 6px 12px;
    border-radius: 20px;
    margin-bottom: 20px;
    letter-spacing: 0.3px;
    backdrop-filter: blur(4px);
  }

  // Question Number
  .question-number {
    font-size: 13px;
    font-weight: 700;
    color: var(--el-text-color-secondary);
    margin-bottom: 12px;
    font-family: var(--el-font-family);
    letter-spacing: 1.5px;
    text-transform: uppercase;
    opacity: 0.8;
  }
  
  .option-content {
    flex: 1;
    font-size: 16px;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    word-break: break-word;
    transition: color 0.2s ease;
  }
  
  .answer-status-icon {
    position: absolute;
    right: 12px;
    top: 12px;
    font-size: 20px;
    filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
  }
  
  @keyframes shake {
    10%, 90% { transform: translate3d(-1px, 0, 0); }
    20%, 80% { transform: translate3d(2px, 0, 0); }
    30%, 50%, 70% { transform: translate3d(-4px, 0, 0); }
    40%, 60% { transform: translate3d(4px, 0, 0); }
  }
  
  // Dark Mode
  :global(.dark) & {
    background: rgba(28, 28, 30, 0.65);
    border-color: rgba(255, 255, 255, 0.12);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
    
    &.compact-mode {
      background: transparent;
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    .question-text {
      color: #FFFFFF;
      text-shadow: 0 1px 2px rgba(0,0,0,0.5);
    }
    
    .option-item {
      background: rgba(255, 255, 255, 0.05);
      border: none;

      &:hover:not(.is-disabled) {
        background: rgba(255, 255, 255, 0.1);
        box-shadow: none;
        transform: scale(1.01);
        
        .option-marker {
          border-color: var(--primary-color);
        }
      }
    }
    
    .option-marker {
      border-color: rgba(255, 255, 255, 0.3);
    }
    
    .option-content {
      color: rgba(255, 255, 255, 0.9);
    }
    
    .option-item.is-selected {
      background: rgba(var(--el-color-primary-rgb), 0.25);
      border: none;
      
      .option-content { color: white; }
    }
    
    .option-item.is-correct {
      background: rgba(52, 199, 89, 0.2);
      border: none;
      
      .option-content { color: #4cd964; }
    }
    
    .option-item.is-wrong {
      background: rgba(255, 69, 58, 0.2);
      border: none;
      
      .option-content { color: #ff453a; }
    }
  }

  // Explanation Box
  .explanation-box {
    margin-top: 32px;
    padding: 24px;
    border-radius: 20px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);

    .explanation-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);

      .el-icon {
        font-size: 20px;
      }
    }

    .explanation-content {
      .explanation-text {
        margin-bottom: 12px;
        padding: 12px 16px;
        border-radius: 12px;
        background: var(--danger-color, #ff3b30);
        color: white;
        font-size: 14px;
      }

      .explanation-detail {
        padding: 16px;
        border-radius: 12px;
        background: var(--el-fill-color-light);
        color: var(--el-text-color-regular);
        line-height: 1.8;
        font-size: 14px;

        p {
          margin: 0;
        }

        strong {
          color: var(--el-text-color-primary);
        }
      }
    }
  }
}
</style>
