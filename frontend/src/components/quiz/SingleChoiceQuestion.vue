<script setup>
import { ref, computed, watch } from 'vue'
import { InfoFilled, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'

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
  // 是否有解析内容
  hasExplanation: {
    type: Boolean,
    default: true
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

// 检查当前用户答案是否正确
const isCurrentAnswerCorrect = computed(() => {
  if (!props.isSubmitted || selectedIndex.value === -1) return false
  return isCorrectAnswer(selectedIndex.value)
})

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
  } else {
    // userAnswer 为空或 null 时，重置选中状态
    selectedIndex.value = -1
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
  <div class="single-choice-question" :class="{
    'compact-mode': compact,
    'has-knowledge-point': !!knowledgePoint,
    'is-submitted': isSubmitted
  }">
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
      <div v-for="(option, idx) in options" :key="idx" class="option-item" :class="{
        'is-selected': isSelected(idx),
        'is-correct': shouldShowCorrect(idx),
        'is-wrong': isWrongAnswer(idx),
        'is-hovered': hoveredIndex === idx,
        'is-disabled': isSubmitted && !isSelected(idx)
      }" @mouseenter="hoveredIndex = idx" @mouseleave="hoveredIndex = -1" @click="handleOptionSelect(idx, option)">
        <div class="option-marker">
          {{ optionLetters(idx) }}
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

.single-choice-question {
  --card-radius: 24px;
  --transition-spring: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  --transition-smooth: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #34c759; // Apple Green
  --danger-color: #ff3b30;  // Apple Red

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
  position: relative;
  opacity: 0.95;

  &.compact-mode {
    padding: 20px;
    margin-bottom: 16px;
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--el-border-color-lighter);
    backdrop-filter: none;

    .question-number { display: none; }
    .question-text { font-size: 15px; margin-bottom: 16px; }
    .options-list { gap: 10px; }
    .option-item { padding: 12px 16px; border-radius: 12px; }
    .option-marker { width: 28px; height: 28px; font-size: 13px; border-radius: 8px; }
    .option-content { font-size: 14px; }
  }

  &.is-submitted .option-item {
    cursor: default;
    &:active { transform: none; }
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

  // Question Text
  .question-text {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    margin-bottom: 32px;
    letter-spacing: -0.01em;
  }

  // Options List
  .options-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }

  // Option Item
  .option-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 20px; // Reduced padding for minimalist look
    border: none; // Remove border
    border-radius: 16px;
    cursor: pointer;
    transition: background-color 0.2s ease, transform 0.2s ease, color 0.2s ease; // Smoother transition
    position: relative;
    background: var(--el-fill-color-light); // Default light background
    min-height: 64px; // Reduced minimum height
    box-sizing: border-box;
    
    &:hover:not(.is-disabled) {
      background: var(--el-fill-color); // Slightly darker on hover
      transform: scale(1.01); // Subtle scale
      z-index: 1;

      .option-marker {
        background: var(--el-color-primary-light-9);
        color: var(--primary-color);
      }
    }

    &:active:not(.is-disabled) {
      transform: scale(0.99);
    }
  }

  // Option Marker
  .option-marker {
    width: 36px; // Slightly smaller
    height: 36px;
    border-radius: 10px; // Softer radius
    background: var(--el-fill-color-light);
    color: var(--el-text-color-regular);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 15px;
    font-weight: 600;
    flex-shrink: 0;
    transition: var(--transition-smooth);
  }

  // Option Content
  .option-content {
    flex: 1;
    font-size: 16px;
    color: var(--el-text-color-primary);
    line-height: 1.5;
    font-weight: 400;
    transition: color 0.2s ease;
  }

  // Selected State
  .option-item.is-selected {
    background: var(--el-color-primary-light-9); // Light primary background
    box-shadow: none; // Remove shadow to keep it flat/minimal
    z-index: 2;

    .option-marker {
      background: var(--primary-color);
      color: white;
      box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb), 0.3);
    }

    .option-content {
      font-weight: 600;
      color: var(--primary-color);
    }
  }

  // Correct State
  .option-item.is-correct {
    background: var(--el-color-success-light-9);
    box-shadow: none;
    z-index: 2;

    .option-marker {
      background: var(--success-color);
      color: white;
      box-shadow: 0 2px 8px rgba(52, 199, 89, 0.3);
    }

    .option-content {
      color: #1a7f37;
      font-weight: 600;
    }
  }

  // Wrong State
  .option-item.is-wrong {
    background: var(--el-color-danger-light-9);
    animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
    
    .option-marker {
      background: var(--danger-color);
      color: white;
      opacity: 0.9;
    }

    .option-content {
      color: var(--danger-color);
      text-decoration: line-through;
      opacity: 0.8;
    }
  }

  // Disabled State
  .option-item.is-disabled {
    opacity: 0.6;
    cursor: default;
    filter: grayscale(0.5);

    &:hover {
      transform: none;
      box-shadow: none;
      background: var(--el-fill-color-light);
    }
  }

  // Explanation Box
  .explanation-box {
    margin-top: 32px;
    padding: 24px;
    border-radius: 20px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    backdrop-filter: blur(10px);
    animation: slideUpFade 0.5s cubic-bezier(0.2, 0.8, 0.2, 1);
    opacity: 0.95;

    .explanation-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      font-size: 16px;
    }

    .explanation-content {
      font-size: 15px;
      line-height: 1.7;
      color: var(--el-text-color-regular);

      .explanation-text {
        margin-bottom: 16px;
        color: var(--el-text-color-primary);
        padding: 8px 16px;
        background: rgba(0, 0, 0, 0.04);
        border-radius: 10px;
        display: inline-block;
        font-weight: 500;
      }

      .explanation-detail {
        padding-left: 20px;
        border-left: 3px solid var(--primary-color);
        color: var(--el-text-color-regular);
        
        p { margin: 0; }
      }
    }
  }

  // Animations
  @keyframes slideUpFade {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
  }

  @keyframes shake {
    10%, 90% { transform: translate3d(-1px, 0, 0); }
    20%, 80% { transform: translate3d(2px, 0, 0); }
    30%, 50%, 70% { transform: translate3d(-4px, 0, 0); }
    40%, 60% { transform: translate3d(4px, 0, 0); }
  }

  // Dark Mode Adaptation
}

html.dark .single-choice-question {
  background: var(--el-bg-color);
  border-color: var(--el-border-color);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);

  &.compact-mode {
    background: transparent;
    border-color: var(--el-border-color);
  }

  .question-text {
    color: #FFFFFF;
    text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  }

  .question-number {
    color: rgba(255, 255, 255, 0.6);
  }

  .option-item {
    background: rgba(255, 255, 255, 0.05);
    border: none;

    &:hover:not(.is-disabled) {
      background: rgba(255, 255, 255, 0.1);
      box-shadow: none;
      transform: scale(1.01);
    }
  }

  .option-marker {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.9);
  }

  .option-content {
    color: rgba(255, 255, 255, 0.9);
  }

  .option-item.is-selected {
    background: rgba(var(--el-color-primary-rgb), 0.25);
    border: none;

    .option-content { color: white; }

    .option-marker {
      background: var(--primary-color);
      color: white;
    }
  }

  .option-item.is-correct {
    background: rgba(52, 199, 89, 0.2);
    border: none;

    .option-content { color: #4cd964; }

    .option-marker {
      background: var(--success-color);
      color: white;
    }
  }

  .option-item.is-wrong {
    background: rgba(255, 69, 58, 0.2);
    border: none;

    .option-content { color: #ff453a; }

    .option-marker {
      background: var(--danger-color);
      color: white;
    }
  }

  .explanation-box {
    background: var(--el-bg-color);
    border-color: var(--el-border-color);

    .explanation-text {
      background: var(--el-fill-color-darker);
      color: white;
    }

    .explanation-content {
      color: rgba(255, 255, 255, 0.8);
    }

    .explanation-detail {
      color: rgba(255, 255, 255, 0.7);
    }
  }
}
</style>
