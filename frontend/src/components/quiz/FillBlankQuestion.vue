<script setup>
import { ref, computed, watch } from 'vue'

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
  // 占位符数量（根据问题文本中的 _____ 自动计算）
  blankCount: {
    type: Number,
    default: null
  }
})

const emit = defineEmits([
  'answer-changed'
])

// 计算填空数量（如果未提供）
const calculatedBlankCount = computed(() => {
  if (props.blankCount !== null) {
    const matches = props.question.match(/_/g)
    return matches ? matches.length : 1
  }
  return props.blankCount || 1
})

// 填空索引（从1开始）
const blankIndices = computed(() => 
  Array.from({ length: calculatedBlankCount.value }, (_, i) => i + 1)
)

// 用户答案数组（按逗号或分号分隔）
const userAnswers = ref([])

// 监听 props.userAnswer 变化
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer) {
    // 尝试逗号分隔
    userAnswers.value = newAnswer.split(/,|;/)
  }
}, { immediate: true })

// 输入框引用数组
const inputRefs = ref([])

// 检查单个填空是否正确
const isBlankCorrect = (index) => {
  if (!props.correctAnswer || !userAnswers.value[index]) return false
  const userVal = props.caseSensitive 
    ? userAnswers.value[index]
    : userAnswers.value[index].toLowerCase()
  const correctVal = props.caseSensitive
    ? props.correctAnswer
    : props.correctAnswer.toLowerCase()
  return userVal === correctVal
}

// 检查是否所有填空都正确
const isAllCorrect = computed(() => {
  if (calculatedBlankCount.value === 0) return false
  
  return blankIndices.value.every(index => isBlankCorrect(index))
})

// 检查是否有任何填空被填写
const hasAnyFilled = computed(() => {
  return userAnswers.value.some(answer => answer && answer.trim() !== '')
})

// 聚焦处理
const handleFocus = (index) => {
  // 下一个自动聚焦
  if (index < inputRefs.value.length - 1) {
    const nextInput = inputRefs.value[index + 1]
    if (nextInput) {
      nextInput.focus()
    }
  }
}

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
    <div class="knowledge-label" v-if="knowledgePoint">
      {{ knowledgePoint }}
    </div>

    <!-- 题目文本 -->
    <div class="question-text" v-html="renderQuestion"></div>

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
        <div class="blank-number">{{ index }}</div>
        
        <!-- 输入框 -->
        <input
          :ref="(el) => { if (el) inputRefs[index] = el }"
          type="text"
          class="blank-input"
          :placeholder="`第${index}空`"
          :value="userAnswers[index]"
          @input="handleInput(index, $event)"
          @focus="handleFocus(index)"
          :disabled="isSubmitted"
          :maxlength="maxLength"
        />
        
        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted" class="blank-status-icon">
            <el-icon v-if="isBlankCorrect(index)" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else-if="userAnswers[index]" color="#f56c6c">
              <CircleCloseFilled />
            </el-icon>
          </div>
        </transition>
      </div>
    </div>

    <!-- 答题后显示的解析 -->
    <transition name="fade">
      <div v-if="isSubmitted" class="explanation-box">
        <div class="explanation-content">
          <el-icon><InfoFilled /></el-icon>
          <div class="explanation-text">
            <template v-if="isAllCorrect">
              <div class="explanation-title">全部正确！</div>
              <div class="explanation-detail">太棒了，所有答案都正确。</div>
            </template>
            <template v-else-if="hasAnyFilled">
              <div class="explanation-title">部分正确</div>
              <div class="explanation-detail">
                正确率：{{ Math.round(correctCount / calculatedBlankCount * 100) }}%
                {{ correctCount }}/{{ calculatedBlankCount }}
              </div>
            </template>
            <template v-else>
              <div class="explanation-title">未作答</div>
              <div class="explanation-detail">请填写所有空格后提交。</div>
            </template>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.fill-blank-question {
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #67c23a;
  --danger-color: #f56c6c;
  --warning-color: #e6a23c;

  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 24px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: var(--transition-base);
  position: relative;

  &.compact-mode {
    padding: 16px;
  }

  &.is-submitted {
    .blank-input {
      color: var(--el-text-color-primary);
      background: var(--el-fill-color-light);
    }
  }

  .knowledge-label {
    font-size: 12px;
    color: var(--primary-color);
    background: var(--el-color-primary-light-9);
    padding: 4px 12px;
    border-radius: 8px;
    display: inline-block;
    margin-bottom: 12px;
    font-weight: 600;
  }

  .question-text {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.8;
    margin-bottom: 24px;

    :deep(.blank-placeholder) {
      color: var(--el-text-color-placeholder);
      font-weight: 500;
      font-style: italic;
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
    padding: 16px;
    border-radius: 12px;
    background: var(--el-fill-color-light);
    border: 2px dashed var(--el-border-color);
    transition: var(--transition-base);
  }

  .blank-number {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--el-fill-color-dark);
    color: var(--el-text-color-regular);
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
    padding: 12px 16px;
    font-size: 16px;
    font-family: monospace;
    background: var(--el-bg-color);
    border: none;
    border-radius: 8px;
    color: var(--el-text-color-primary);
    outline: none;
    transition: var(--transition-base);

    &::placeholder {
      color: var(--el-text-color-placeholder);
    }

    &:focus {
      background: var(--el-bg-color);
      border: 2px solid var(--primary-color);
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.7;
    }
  }

  .blank-status-icon {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
  }

  .blank-item.is-correct {
    background: var(--success-light);
    border-color: var(--success-color);
  }

  .blank-item.is-wrong {
    background: var(--danger-light);
    border-color: var(--danger-color);
    animation: shake 0.5s ease-in-out;
  }

  .blank-item.is-empty {
    opacity: 0.7;
  }

  .blank-item.is-disabled {
    opacity: 0.5;
    cursor: not-allowed;
    border-color: var(--el-border-color);
    background: var(--el-fill-color-blank);
  }

  @keyframes shake {
    0%, 100% { transform: translateX(0); }
    10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
    20%, 40%, 60%, 80% { transform: translateX(4px); }
  }

  .explanation-box {
    margin-top: 24px;
    padding: 20px;
    background: var(--el-fill-color-light);
    border-radius: 12px;
    border-left: 4px solid var(--primary-color);
  }

  .explanation-content {
    display: flex;
    gap: 12px;
    align-items: flex-start;
  }

  .explanation-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--primary-color);
    margin-bottom: 4px;
  }

  .explanation-detail {
    font-size: 14px;
    color: var(--el-text-color-regular);
    line-height: 1.6;
  }
}

// 深色模式
.dark .fill-blank-question {
  background: rgba(28, 28, 30, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.dark .knowledge-label {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 1);
}

.dark .question-text {
  color: rgba(255, 255, 255, 1);
}

.dark .blank-input {
  background: rgba(0, 0, 0, 0.8);
  color: rgba(255,  255, 255, 1);
}

.dark .blank-number {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.dark .explanation-box {
  background: rgba(255, 255, 255, 0.08);
  border-left-color: rgba(255, 255, 255, 0.3);
}

.dark .explanation-title {
  color: rgba(255, 255, 255, 1);
}

.dark .explanation-detail {
  color: rgba(255, 255, 255, 0.7);
}
</style>
