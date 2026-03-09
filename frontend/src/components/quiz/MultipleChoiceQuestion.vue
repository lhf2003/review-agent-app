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
  const userAnswers = props.userAnswer.split(',').map(a => a.trim().toUpperCase())
  const currentLetter = optionLetters.value(index)
  return userAnswers.includes(currentLetter)
}

// 检查选项是否被选中但错误（仅在提交后显示）
const isSelectedWrong = (index) => {
  return props.isSubmitted && isSelected(index) && !isCorrectOption(index)
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
                <span v-if="isSubmitted && isCorrectOption(idx)">✓</span>
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

.multiple-choice-question {
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

  &.compact-mode {
    padding: 20px;
    margin-bottom: 16px;
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--glass-border);
    backdrop-filter: none;

    .options-list { gap: 10px; grid-template-columns: 1fr; }
    .option-item { padding: 12px 16px; }
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
    border: 1px solid var(--glass-border);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all 0.2s ease;
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
        border-color: var(--accent-primary);
      }
    }

    &.is-selected {
      background: rgba(204, 102, 51, 0.1);
      border-color: var(--accent-primary);
      box-shadow: 0 2px 8px rgba(204, 102, 51, 0.15);
      z-index: 2;

      .option-marker {
        background: var(--accent-primary);
        border-color: var(--accent-primary);
        color: white;
        box-shadow: 0 2px 8px rgba(204, 102, 51, 0.3);
      }

      .option-content {
        color: var(--accent-tertiary);
        font-weight: 600;
      }
    }

    &.is-correct {
      background: rgba(34, 197, 94, 0.1);
      border-color: var(--mastery-high);
      box-shadow: 0 2px 8px rgba(34, 197, 94, 0.15);
      z-index: 2;

      .option-marker {
        background: var(--mastery-high);
        border-color: var(--mastery-high);
        box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
      }

      .option-content {
        color: var(--mastery-high);
        font-weight: 600;
      }
    }

    &.is-wrong {
      background: linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.08) 100%);
      border: 2px solid var(--mastery-low);
      box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.1);
      animation: shake 0.5s ease-in-out;
      z-index: 3;

      .option-marker {
        background: var(--mastery-low);
        border-color: var(--mastery-low);
        box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
      }

      .option-content {
        color: var(--mastery-low);
        font-weight: 600;
      }

      .answer-status-icon {
        animation: iconPop 0.3s ease both;
      }
    }

    @keyframes iconPop {
      0% { transform: scale(0); opacity: 0; }
      50% { transform: scale(1.2); }
      100% { transform: scale(1); opacity: 1; }
    }

    &.is-disabled {
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
  
  .option-label {
    display: flex;
    align-items: center;
    gap: 16px;
    width: 100%;
    cursor: pointer;
  }

  .option-checkbox {
    display: none;
  }

  .option-marker {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    border: 2px solid var(--glass-border);
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: var(--transition-base);
    flex-shrink: 0;

    .marker-inner {
      font-size: 16px;
      font-weight: 800;
      color: white;
      line-height: 1;
    }
  }

  .option-content {
    flex: 1;
    font-size: 16px;
    color: var(--text-primary);
    line-height: 1.5;
    word-break: break-word;
    transition: color 0.2s ease;
  }

  .answer-status-icon {
    position: absolute;
    right: 12px;
    top: 12px;
    font-size: 20px;
    filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
  }
}
</style>
