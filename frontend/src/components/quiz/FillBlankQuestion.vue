<script setup>
import { ref, computed, watch } from 'vue'
import { SuccessFilled, CircleCloseFilled, InfoFilled, WarningFilled } from '@element-plus/icons-vue'

/**
 * 填空题组件
 * 支持多空填空和实时验证
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
    default: ''
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
  // 是否区分大小写
  caseSensitive: {
    type: Boolean,
    default: true
  },
  // 是否紧凑模式
  compact: {
    type: Boolean,
    default: false
  },
  // 最大长度限制
  maxLength: {
    type: Number,
    default: 100
  },
  // 占位符数量（由后端提供）
  blankCount: {
    type: Number,
    required: true
  },
  // 知识点
  knowledgePoint: {
    type: String,
    default: null
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
  }
})

const emit = defineEmits([
  'answer-selected'
])

// 填空索引（从0开始）
const blankIndices = computed(() => {
    const count = props.blankCount || 0
    return Array.from({length: count}, (_, i) => i)
})

// 用户答案数组（按逗号或分号分隔）
const userAnswers = ref([])

// 监听 props.userAnswer 变化
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer) {
    // 尝试逗号分隔
    userAnswers.value = newAnswer.split(/,|;/)
  } else {
    // userAnswer 为空或 null 时，清空答案数组
    userAnswers.value = []
  }
}, {immediate: true})

// 输入框引用数组
const inputRefs = ref([])

// 正确答案数组
const correctAnswersArray = computed(() => {
  if (!props.correctAnswer) return []
  return props.correctAnswer.split(/,|;/)
})

// 检查单个填空是否正确
const isBlankCorrect = (index) => {
  if (!correctAnswersArray.value[index] || !userAnswers.value[index]) return false
  
  const userVal = props.caseSensitive
      ? userAnswers.value[index].trim()
      : userAnswers.value[index].trim().toLowerCase()
      
  const correctVal = props.caseSensitive
      ? correctAnswersArray.value[index].trim()
      : correctAnswersArray.value[index].trim().toLowerCase()
      
  return userVal === correctVal
}

// 检查是否所有填空都正确
const isAllCorrect = computed(() => {
  if (props.blankCount === 0) return false

  return blankIndices.value.every(index => isBlankCorrect(index))
})

// 检查是否有任何填空被填写
const hasAnyFilled = computed(() => {
  return userAnswers.value.some(answer => answer && answer.trim() !== '')
})

// 计算正确填空的数量
const correctCount = computed(() => {
  if (props.blankCount === 0) return 0
  return blankIndices.value.filter(index => isBlankCorrect(index)).length
})

// 输入处理
const handleInput = (index, event) => {
  const value = event.target.value
  const newAnswers = [...userAnswers.value]
  newAnswers[index] = value

  // 如果已经达到最大长度，截断
  if (value.length > props.maxLength) {
    newAnswers[index] = value.substring(0, props.maxLength)
  }

  userAnswers.value = newAnswers
  emitAnswerChanged()
}

// 触发答案变化事件
const emitAnswerChanged = () => {
  emit('answer-selected', userAnswers.value.join(','))
}

// 暴露聚焦方法
defineExpose({
  focusInput: (index) => {
    const input = inputRefs.value[index]
    if (input) {
      input.focus()
      input.select()
    }
  },
  reset: () => {
    userAnswers.value = new Array(calculatedBlankCount.value).fill('')
    if (inputRefs.value.length > 0) {
      inputRefs.value[0]?.focus()
    }
  }
})
</script>

<template>
  <div
      class="fill-blank-question"
      :class="{
      'compact-mode': compact,
      'is-submitted': isSubmitted,
      'is-all-correct': isSubmitted && isAllCorrect,
      'has-any-filled': hasAnyFilled
    }"
  >
    <!-- 知识点标签 -->
    <div v-if="knowledgePoint && !compact" class="knowledge-badge">
      {{ knowledgePoint }}
    </div>

    <!-- 题目文本 -->
    <div class="question-text">{{ question }}</div>

    <!-- 填空区域 -->
    <div class="blanks-container">
      <div
          v-for="(placeholder, index) in blankIndices"
          :key="index"
          class="blank-item"
          :class="{
          'is-correct': isSubmitted && isBlankCorrect(index),
          'is-wrong': isSubmitted && !isBlankCorrect(index) && userAnswers[index],
          'is-empty': !userAnswers[index],
          'is-disabled': isSubmitted
        }"
      >
        <!-- 编号 -->
        <div class="blank-number">{{ index + 1 }}</div>

        <!-- 输入框 -->
        <input
            :ref="(el) => { if (el) inputRefs[index] = el }"
            type="text"
            class="blank-input"
            :placeholder="`第${index+1}空`"
            :value="userAnswers[index]"
            @input="handleInput(index, $event)"
            :disabled="isSubmitted"
            :maxlength="maxLength"
        />

        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted" class="blank-status-icon">
            <el-icon v-if="isBlankCorrect(index)" color="#67c23a">
              <SuccessFilled/>
            </el-icon>
            <el-icon v-else-if="userAnswers[index]" color="#f56c6c">
              <CircleCloseFilled/>
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
        :class="{ 'is-correct': isAllCorrect, 'is-wrong': !isAllCorrect }"
      >
        <div class="explanation-header">
          <div class="status-icon" :class="isAllCorrect ? 'is-correct' : hasAnyFilled ? 'is-partial' : 'is-wrong'">
            <el-icon v-if="isAllCorrect"><SuccessFilled/></el-icon>
            <el-icon v-else-if="hasAnyFilled"><WarningFilled/></el-icon>
            <el-icon v-else><CircleCloseFilled/></el-icon>
          </div>
          <div class="explanation-title">
            <template v-if="isAllCorrect">全部正确</template>
            <template v-else-if="hasAnyFilled">部分正确</template>
            <template v-else>未作答</template>
          </div>
        </div>
        <div class="explanation-content">
          <!-- 正确率和正确答案 -->
          <div class="correct-answer-section">
            <span class="section-label">正确答案</span>
            <span class="section-value">{{ correctAnswer }}</span>
            <span v-if="!isAllCorrect" class="accuracy-badge">
              {{ Math.round(correctCount / props.blankCount * 100) }}%
            </span>
          </div>
          <!-- 解析内容 -->
          <div v-if="explanation" class="explanation-detail">
            <div class="detail-label">解析</div>
            <div class="detail-content">{{ explanation }}</div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
@use '../../styles/nebula-theme.scss' as *;
@import '../../styles/quiz-common';

.fill-blank-question {
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
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--glass-border);
    backdrop-filter: none;
  }

  &.is-submitted {
    .blank-input {
      color: var(--text-primary);
    }
  }

  .question-text {
    @extend .question-text;
    line-height: 1.6;

    :deep(.blank-placeholder) {
      display: inline-block;
      min-width: 60px;
      border-bottom: 2px solid var(--text-muted);
      margin: 0 4px;
      vertical-align: bottom;
    }
  }

  .blanks-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .blank-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    border-radius: var(--radius-md);
    background: rgba(255, 248, 245, 0.05);
    border: 1px solid var(--glass-border);
    transition: var(--transition-base);

    &:focus-within {
      background: var(--glass-surface-hover);
      border-color: var(--accent-primary);
      box-shadow: 0 4px 12px rgba(204, 102, 51, 0.15);
      transform: translateY(-1px);
    }
  }

  .blank-number {
    width: 32px;
    height: 32px;
    border-radius: var(--radius-sm);
    background: rgba(255, 248, 245, 0.08);
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 700;
    font-family: var(--font-mono);
    flex-shrink: 0;
  }

  .blank-input {
    flex: 1;
    padding: 8px 0;
    font-size: 18px;
    font-family: var(--font-mono);
    background: transparent;
    border: none;
    color: var(--text-primary);
    outline: none;
    transition: var(--transition-base);
    border-bottom: 2px solid transparent;

    &::placeholder {
      color: var(--text-muted);
      opacity: 0.5;
    }

    &:focus {
      border-bottom-color: var(--accent-primary);
    }

    &:disabled {
      cursor: default;
      opacity: 0.8;
      border-bottom-color: transparent;
    }
  }

  .blank-status-icon {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
  }

  .blank-item.is-correct {
    background: rgba(34, 197, 94, 0.1);
    border-color: var(--mastery-high);

    .blank-input {
      color: var(--mastery-high);
    }

    .blank-number {
      background: rgba(34, 197, 94, 0.2);
      color: var(--mastery-high);
    }
  }

  .blank-item.is-wrong {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.12) 0%, rgba(239, 68, 68, 0.06) 100%);
    border: 2px solid var(--mastery-low);
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.08);
    animation: shake 0.5s ease-in-out;
    z-index: 2;

    .blank-input {
      color: var(--mastery-low);
      font-weight: 600;
    }

    .blank-number {
      background: rgba(239, 68, 68, 0.25);
      color: var(--mastery-low);
      font-weight: 700;
    }

    .blank-status-icon {
      animation: iconPop 0.3s ease both;
    }
  }

  @keyframes iconPop {
    0% { transform: scale(0); opacity: 0; }
    50% { transform: scale(1.2); }
    100% { transform: scale(1); opacity: 1; }
  }

  .blank-item.is-empty {
    opacity: 0.7;
  }

  .blank-item.is-disabled {
    opacity: 0.8;
    cursor: default;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }

  @keyframes shake {
    10%, 90% {
      transform: translate3d(-1px, 0, 0);
    }
    20%, 80% {
      transform: translate3d(2px, 0, 0);
    }
    30%, 50%, 70% {
      transform: translate3d(-4px, 0, 0);
    }
    40%, 60% {
      transform: translate3d(4px, 0, 0);
    }
  }
}
</style>
