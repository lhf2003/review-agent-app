<script setup>
import { ref, computed } from 'vue'

/**
 * 代码识别题组件
 * 展示代码片段并标记错误位置
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
  // 正确答案（可能是行号、函数名、关键词等）
  correctAnswer: {
    type: String,
    required: true
  },
  // 错误位置信息（字符串形式，如 "Line 5: missing semicolon"）
  errorLocation: {
    type: String,
    default: null
  },
  // 代码语言
  language: {
    type: String,
    default: 'javascript'
  },
  // 是否紧凑模式
  compact: {
    type: Boolean,
    default: false
  },
  // 知识点（可选）
  knowledgePoint: {
    type: String,
    default: null
  }
})

const emit = defineEmits([
  'answer-selected',
  'answer-changed'
])

// 当前选中的代码行（多选时可能有多个）
const selectedLines = ref(new Set())

// 解析代码内容
const codeLines = computed(() => {
  return props.question.split('\n').map(line => line.trim()).filter(line => line !== '')
})

// 检查是否正确
const isCorrect = computed(() => {
  if (!props.userAnswer) return false
  
  const userAnswer = props.userAnswer.trim().toLowerCase()
  const correctAnswer = props.correctAnswer.trim().toLowerCase()
  
  // 简单比较
  if (userAnswer === correctAnswer) {
    return true
  }
  
  // 对于行号，支持 "line 5", "第5行" 等格式
  const lineNumMatch = userAnswer.match(/line\s*(\d+)/i)
  if (lineNumMatch && lineNumMatch[1] === correctAnswer.toLowerCase()) {
    const lineNum = parseInt(lineNumMatch[2])
    if (!isNaN(lineNum) && codeLines.value[lineNum - 1]) {
      return true
    }
  }
  
  return false
})

// 检查是否应该显示正确答案
const shouldShowCorrect = computed(() => {
  return props.isSubmitted && isCorrect.value
})

// 检查是否错误（已提交但答案不正确）
const isWrong = computed(() => {
  return props.isSubmitted && props.userAnswer && !isCorrect.value
})

// 处理行点击
const handleLineClick = (lineIndex, lineNumber) => {
  if (props.isSubmitted) return
  
  if (selectedLines.value.has(lineIndex)) {
    selectedLines.value.delete(lineIndex)
  } else {
    selectedLines.value.add(lineIndex)
  }
  
  const selectedLinesArray = Array.from(selectedLines.value)
  const answer = selectedLinesArray.map(idx => {
    const line = codeLines.value[idx]
    // 尝试匹配 "line X" 格式
    const match = line.match(/line\s*(\d+)/i)
    if (match) {
      return `line ${match[2]}`
    }
    return line
  })
  emit('answer-selected', answer.join(','))
  emit('answer-changed', answer.join(','))
}

// 监听 props.userAnswer 变化（同步选择状态）
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer) {
    // 解析用户答案
    const answerArray = newAnswer.split(',').map(a => a.trim())
    selectedLines.value.clear()
    
    answerArray.forEach(answer => {
      // 匹配行号 "line X" 格式
      const lineMatch = answer.match(/line\s*(\d+)/i)
      if (lineMatch) {
        const lineNum = parseInt(lineMatch[2])
        // 查找匹配的代码行
        const lineIndex = codeLines.value.findIndex(line => {
          const lineMatch2 = line.match(/line\s*(\d+)/i)
          if (lineMatch2) {
            return parseInt(lineMatch2[2]) === lineNum
          }
          return false
        })
        if (lineIndex >= 0) {
          selectedLines.value.add(lineIndex)
          return
        }
      }
      
      // 精确匹配代码内容
      const exactMatch = codeLines.value.findIndex(line => line.trim().toLowerCase() === answer.toLowerCase())
      if (exactMatch >= 0) {
        selectedLines.value.add(exactMatch)
      }
    })
  }
}, { immediate: true })

// 暴露方法
defineExpose({
  resetSelection: () => {
    selectedLines.value.clear()
  }
})
</script>

<template>
  <div 
    class="code-snippet-question"
    :class="{
      'compact-mode': compact,
      'is-submitted': isSubmitted,
      'is-correct': shouldShowCorrect,
      'is-wrong': isWrong,
      'has-selection': selectedLines.size > 0
    }"
  >
    <!-- 知识点标签 -->
    <div v-if="knowledgePoint && !compact" class="knowledge-badge">
      {{ knowledgePoint }}
    </div>

    <!-- 题目文本 -->
    <div class="question-text">
      {{ question }}
    </div>

    <!-- 代码片段显示 -->
    <div class="code-container">
      <div class="code-header">
        <span class="code-language">{{ language.toUpperCase() }}</span>
        <span class="error-hint" v-if="errorLocation">
          错误位置：{{ errorLocation }}
        </span>
      </div>
      
      <!-- 行号指示器 -->
      <div class="line-numbers">
        <div 
          v-for="(line, idx) in codeLines"
          :key="idx"
          class="line-number"
          :class="{
            'is-selected': selectedLines.has(idx),
            'is-correct': isSubmitted && isCorrect,
            'is-wrong': isSubmitted && !isCorrect && selectedLines.has(idx)
          }"
          @click="handleLineClick(idx, line)"
        >
          {{ idx + 1 }}
        </div>
      </div>
      
      <!-- 代码内容 -->
      <div class="code-content">
        <div 
          v-for="(line, idx) in codeLines"
          :key="idx"
          class="code-line"
          :class="{
            'is-selected': selectedLines.has(idx),
            'is-correct': isSubmitted && isCorrect,
            'is-wrong': isSubmitted && !isCorrect && selectedLines.has(idx)
          }"
          @click="handleLineClick(idx, line)"
        >
          <pre class="code-line-text">{{ line }}</pre>
        </div>
      </div>
      </div>
    <!-- 答题后显示的解析 -->
    <transition name="fade">
      <div v-if="isSubmitted" class="explanation-box">
        <div class="explanation-header">
          <el-icon><InfoFilled /></el-icon>
          <div class="explanation-title">
            {{ isCorrect ? '答案正确！' : '答案错误' }}
          </div>
        </div>
        <div class="explanation-content">
          <div v-if="isCorrect">
            <div class="explanation-text">
              <strong>正确答案：</strong>{{ correctAnswer }}
            </div>
            <div v-if="errorLocation" class="explanation-detail">
              <p><strong>错误位置：</strong>{{ errorLocation }}</p>
            </div>
            <div class="explanation-detail">
              <p><strong>说明：</strong>请找出代码中的问题并选择对应的代码行。</p>
            </div>
          </div>
          <div v-else>
            <div class="explanation-text">
              <strong>正确答案：</strong>{{ correctAnswer }}
            </div>
            <div class="explanation-detail">
              <p><strong>说明：</strong>您的答案不正确。请再次检查代码。</p>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.code-snippet-question {
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  --primary-color: var(--el-color-primary);
  --success-color: #67c23a;
  --danger-color: #f56c6c;
  --code-bg: #1e1e1e;
  --code-line-height: 28px;
  --code-font-size: 14px;

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
  
  .knowledge-badge {
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
    margin-bottom: 20px;
  }
  
  .code-container {
    display: grid;
    grid-template-columns: 60px 1fr;
    gap: 0;
    background: var(--code-bg);
    border-radius: 8px;
    padding: 20px;
    overflow-x: auto;
  }
  
  .code-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--el-border-color-light);
    font-size: 13px;
  }
  
  .code-language {
    font-weight: 600;
    color: var(--el-text-color-regular);
    text-transform: uppercase;
    padding: 4px 12px;
    background: var(--el-fill-color-dark);
    border-radius: 4px;
  }
  
  .error-hint {
    color: var(--el-color-warning);
    font-size: 12px;
  }
  
  .line-numbers {
    text-align: right;
    font-family: 'Fira Code', 'Monaco', 'Courier New', monospace;
    font-size: var(--code-font-size);
    line-height: var(--code-line-height);
    color: var(--el-text-color-regular);
    user-select: none;
  }
  
  .line-number {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: var(--code-line-height);
    border-radius: 4px;
    margin-right: 8px;
    cursor: pointer;
    transition: var(--transition-base);
    
    &.is-selected {
      background: var(--primary-color);
      color: white;
      transform: scale(1.05);
    }
    
    &:hover:not(.is-disabled) {
      border-color: var(--primary-color);
    }
    
    &.is-correct {
      border-color: var(--success-color);
      background: var(--success-color);
    }
    
    &.is-wrong {
      border-color: var(--danger-color);
      background: var(--danger-color);
      animation: shake 0.5s ease-in-out;
    }
    
    &.is-disabled {
      opacity: 0.4;
      cursor: not-allowed;
      
      &:hover {
        border-color: var(--el-border-color);
      }
    }
  }
  
  .code-content {
    font-family: 'Fira Code', 'Monaco', 'Courier New', monospace;
    font-size: var(--code-font-size);
    line-height: var(--code-line-height);
    background: transparent;
    position: relative;
    white-space: pre-wrap;
    word-break: break-all;
  }
  
  .code-line {
    display: block;
    padding: 0 4px 0 36px;
    border-radius: 4px;
    transition: var(--transition-base);
    
    &.is-selected {
      background: rgba(103, 194, 58, 0.05);
      cursor: pointer;
    }
    
    &:hover:not(.is-disabled) {
      background: rgba(103, 194, 58, 0.1);
    }
    
    &.is-correct {
      background: rgba(103, 194, 58, 0.08);
    }
    
    &.is-wrong {
      background: rgba(248, 113, 113, 0.08);
    }
    
    &.is-disabled {
      opacity: 0.3;
    cursor: not-allowed;
    }
  }
  
  .code-line-text {
    white-space: pre-wrap;
    word-break: break-all;
  }
  
  @keyframes shake {
    0%, 100% { transform: translateX(0); }
    10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
    20%, 40%, 60%, 80% { transform: translateX(4px); }
  }
  
  .explanation-box {
    margin-top: 20px;
    padding: 20px;
    background: var(--el-fill-color-light);
    border-radius: 12px;
    border-left: 4px solid var(--primary-color);
  }
  
  .explanation-header {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-bottom: 12px;
  }
  
  .explanation-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--primary-color);
  }
  
  .explanation-content {
    font-size: 14px;
    color: var(--el-text-color-regular);
    line-height: 1.6;
  }
  
  .explanation-text {
    margin-bottom: 12px;
  }
  
  .explanation-detail {
    color: var(--el-text-color-secondary);
    font-size: 13px;
    line-height: 1.5;
  }
}

// 深色模式
.dark .code-snippet-question {
    background: rgba(28, 28, 30, 0.75);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  }
  
  .dark .code-container {
    background: #1a1a1a;
    }
  
  .dark .line-number {
    color: rgba(255, 255, 255, 0.6);
  }
  
  .dark .code-line-text {
    color: rgba(255, 255, 255, 1);
  }
  
  .dark .code-line.is-selected {
    background: rgba(103, 194, 58, 0.1);
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
