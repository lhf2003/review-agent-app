<script setup>
import { ref, computed, watch } from 'vue'
import { SuccessFilled, CircleCloseFilled, InfoFilled } from '@element-plus/icons-vue'

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
  'answer-changed'
])

// 填空索引（从0开始）
const blankIndices = computed(() =>
    Array.from({length: props.blankCount}, (_, i) => i)
)

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
  emit('answer-changed', userAnswers.value.join(','))
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
      <div v-if="isSubmitted" class="explanation-box">
        <div class="explanation-content">
          <el-icon>
            <InfoFilled/>
          </el-icon>
          <div class="explanation-text">
            <template v-if="isAllCorrect">
              <div class="explanation-title">全部正确！</div>
            </template>
            <template v-else-if="hasAnyFilled">
              <div class="explanation-title">部分正确</div>
              <div class="explanation-detail">
                正确率：{{ Math.round(correctCount / props.blankCount * 100) }}%
                {{ correctCount }}/{{ props.blankCount }}
              </div>
              <div class="explanation-detail" style="margin-top: 8px;">
                <strong>正确答案：</strong>{{ correctAnswer }}
              </div>
            </template>
            <template v-else>
              <div class="explanation-title">未作答</div>
              <div class="explanation-detail">
                <strong>正确答案：</strong>{{ correctAnswer }}
              </div>
            </template>
            
            <!-- 统一显示解析内容 -->
            <div v-if="explanation" class="explanation-detail" style="margin-top: 12px; border-top: 1px solid var(--el-border-color-light); padding-top: 12px;">
              <p><strong>解析：</strong>{{ explanation }}</p>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/quiz-common';
@import '../../styles/variables';

.fill-blank-question {
  // 使用共享的 CSS 变量
  --card-radius: var(--quiz-card-radius);
  --transition-spring: var(--quiz-transition-spring);
  --transition-smooth: var(--quiz-transition-smooth);
  --primary-color: var(--quiz-primary-color);
  --success-color: var(--quiz-success-color);
  --danger-color: var(--quiz-danger-color);
  --warning-color: var(--quiz-warning-color);

  background: var(--el-bg-color);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--card-radius);
  padding: 32px;
  margin-bottom: 24px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02),
  0 10px 15px -3px rgba(0, 0, 0, 0.04),
  0 0 0 1px rgba(0, 0, 0, 0.02);
  transition: var(--transition-smooth);
  position: relative;
  opacity: 0.95;

  &.compact-mode {
    padding: 20px;
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--el-border-color-lighter);
    backdrop-filter: none;
  }

  &.is-submitted {
    .blank-input {
      color: var(--el-text-color-primary);
    }
  }

  .question-text {
    @extend .question-text;
    line-height: 1.6;

    :deep(.blank-placeholder) {
      display: inline-block;
      min-width: 60px;
      border-bottom: 2px solid var(--el-text-color-placeholder);
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
    border-radius: 16px;
    background: var(--el-fill-color-light);
    border: 1px solid rgba(0, 0, 0, 0.06);
    transition: var(--transition-smooth);

    &:focus-within {
      background: var(--el-bg-color);
      border-color: var(--primary-color);
      box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.15);
      transform: translateY(-1px);
    }
  }

  .blank-number {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    background: rgba(0, 0, 0, 0.05);
    color: var(--el-text-color-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 700;
    font-family: monospace;
    flex-shrink: 0;
  }

  .blank-input {
    flex: 1;
    padding: 8px 0;
    font-size: 18px;
    font-family: monospace;
    background: transparent;
    border: none;
    color: var(--el-text-color-primary);
    outline: none;
    transition: var(--transition-smooth);
    border-bottom: 2px solid transparent;

    &::placeholder {
      color: var(--el-text-color-placeholder);
      opacity: 0.5;
    }

    &:focus {
      border-bottom-color: var(--primary-color);
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
    background: rgba(52, 199, 89, 0.1);
    border-color: var(--success-color);

    .blank-input {
      color: #1a7f37;
    }

    .blank-number {
      background: rgba(52, 199, 89, 0.2);
      color: #1a7f37;
    }
  }

  .blank-item.is-wrong {
    background: rgba(255, 59, 48, 0.08);
    border-color: var(--danger-color);
    animation: shake 0.5s ease-in-out;

    .blank-input {
      color: var(--danger-color);
      text-decoration: line-through;
    }

    .blank-number {
      background: rgba(255, 59, 48, 0.2);
      color: var(--danger-color);
    }
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

  // Dark Mode Adaptation
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
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);

      :deep(.blank-placeholder) {
        border-bottom-color: rgba(255, 255, 255, 0.3);
      }
    }

    .blank-item {
      background: rgba(44, 44, 46, 0.4);
      border-color: rgba(255, 255, 255, 0.08);

      &:focus-within {
        background: rgba(58, 58, 60, 0.8);
        border-color: var(--primary-color);
        box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.6);
      }
    }

    .blank-number {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.8);
    }

    .blank-input {
      color: white;

      &::placeholder {
        color: rgba(255, 255, 255, 0.3);
      }
    }

    .blank-item.is-correct {
      background: rgba(52, 199, 89, 0.2);
      border-color: var(--success-color);

      .blank-input {
        color: #4cd964;
      }

      .blank-number {
        background: rgba(52, 199, 89, 0.3);
        color: white;
      }
    }

    .blank-item.is-wrong {
      background: rgba(255, 69, 58, 0.2);
      border-color: var(--danger-color);

      .blank-input {
        color: #ff453a;
      }

      .blank-number {
        background: rgba(255, 69, 58, 0.3);
        color: white;
      }
    }
  }
}
</style>
