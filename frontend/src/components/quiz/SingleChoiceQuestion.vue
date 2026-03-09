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

// 选项字母数组
const letters = ['A', 'B', 'C', 'D', 'E', 'F']

// 监听 props.userAnswer 变化，同步选中状态
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer && newAnswer.length > 0) {
    // 提取答案中的字母部分（支持 "B" 或 "B. xxx" 格式）
    const answerStr = newAnswer.toString().trim().toUpperCase()
    const letterMatch = answerStr.match(/^([A-F])/)
    if (letterMatch) {
      const targetLetter = letterMatch[1]
      const targetIndex = letters.findIndex(l => l === targetLetter)
      if (targetIndex >= 0 && targetIndex < props.options.length) {
        selectedIndex.value = targetIndex
        return
      }
    }
    // 如果没有匹配到字母，尝试直接匹配选项内容
    const targetIndex = props.options.findIndex(opt =>
      opt.toLowerCase().includes(answerStr.toLowerCase())
    )
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
    <!-- 题目头部：题号 + 知识点标签 -->
    <div v-if="!compact" class="question-header">
      <div class="question-number">Question {{ index }}</div>
      <div v-if="knowledgePoint" class="knowledge-badge">{{ knowledgePoint }}</div>
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
      <div
        v-if="isSubmitted"
        class="explanation-box"
        :class="{ 'is-correct': isCurrentAnswerCorrect, 'is-wrong': !isCurrentAnswerCorrect }"
      >
        <div class="explanation-header">
          <div class="status-icon" :class="isCurrentAnswerCorrect ? 'is-correct' : 'is-wrong'">
            <el-icon v-if="isCurrentAnswerCorrect"><SuccessFilled /></el-icon>
            <el-icon v-else><CircleCloseFilled /></el-icon>
          </div>
          <div class="explanation-title">
            {{ isCurrentAnswerCorrect ? '答案正确' : '答案错误' }}
          </div>
        </div>
        <div class="explanation-content">
          <!-- 正确答案 -->
          <div class="correct-answer-section">
            <span class="section-label">正确答案</span>
            <span class="section-value">{{ correctAnswer }}</span>
          </div>
          <!-- 解析内容 -->
          <div v-if="props.hasExplanation && props.explanation" class="explanation-detail">
            <div class="detail-label">解析</div>
            <div class="detail-content">{{ props.explanation }}</div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
@use '../../styles/nebula-theme.scss' as *;
@import '../../styles/quiz-common';

.single-choice-question {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: var(--radius-card);
  padding: 32px;
  margin-bottom: 24px;
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-base);
  position: relative;

  &.compact-mode {
    padding: 20px;
    margin-bottom: 16px;
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--glass-border);
    backdrop-filter: none;

    .question-number { display: none; }
    .question-text { font-size: 15px; margin-bottom: 16px; }
    .options-list { gap: 10px; }
    .option-item { padding: 12px 16px; border-radius: var(--radius-md); }
    .option-marker { width: 28px; height: 28px; font-size: 13px; border-radius: 8px; }
    .option-content { font-size: 14px; }
  }

  &.is-submitted .option-item {
    cursor: default;
    &:active { transform: none; }
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
    padding: 14px 20px;
    border: 1px solid var(--glass-border);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all 0.2s ease;
    position: relative;
    background: rgba(255, 248, 245, 0.05);
    min-height: 64px;
    box-sizing: border-box;

    &:hover:not(.is-disabled) {
      background: var(--glass-surface-hover);
      transform: scale(1.01);
      z-index: 1;
      border-color: var(--glass-border-hover);

      .option-marker {
        background: rgba(204, 102, 51, 0.15);
        color: var(--accent-tertiary);
      }
    }

    &:active:not(.is-disabled) {
      transform: scale(0.99);
    }
  }

  // Option Marker
  .option-marker {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: rgba(255, 248, 245, 0.08);
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 15px;
    font-weight: 600;
    flex-shrink: 0;
    transition: var(--transition-base);
  }

  // Option Content
  .option-content {
    flex: 1;
    font-size: 16px;
    color: var(--text-primary);
    line-height: 1.5;
    font-weight: 400;
    transition: color 0.2s ease;
  }

  // Selected State
  .option-item.is-selected {
    background: rgba(204, 102, 51, 0.1);
    border-color: var(--accent-primary);
    box-shadow: 0 2px 8px rgba(204, 102, 51, 0.15);
    z-index: 2;

    .option-marker {
      background: var(--accent-primary);
      color: white;
      box-shadow: 0 2px 8px rgba(204, 102, 51, 0.3);
    }

    .option-content {
      font-weight: 600;
      color: var(--accent-tertiary);
    }
  }

  // Correct State
  .option-item.is-correct {
    background: rgba(34, 197, 94, 0.1);
    border-color: var(--mastery-high);
    box-shadow: 0 2px 8px rgba(34, 197, 94, 0.15);
    z-index: 2;

    .option-marker {
      background: var(--mastery-high);
      color: white;
      box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
    }

    .option-content {
      color: var(--mastery-high);
      font-weight: 600;
    }
  }

  // Wrong State
  .option-item.is-wrong {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.08) 100%);
    border: 2px solid var(--mastery-low);
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.1);
    animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
    z-index: 3;

    .option-marker {
      background: var(--mastery-low);
      color: white;
      box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
    }

    .option-content {
      color: var(--mastery-low);
      font-weight: 600;
    }

    .answer-icon {
      animation: iconPop 0.3s ease both;
    }
  }

  @keyframes iconPop {
    0% { transform: scale(0); opacity: 0; }
    50% { transform: scale(1.2); }
    100% { transform: scale(1); opacity: 1; }
  }

  // Disabled State
  .option-item.is-disabled {
    opacity: 0.6;
    cursor: default;
    filter: grayscale(0.5);

    &:hover {
      transform: none;
      box-shadow: none;
      background: rgba(255, 248, 245, 0.05);
    }
  }
}
</style>
