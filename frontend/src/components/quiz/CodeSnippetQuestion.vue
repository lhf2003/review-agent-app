<script setup>
import { ref, computed, watch } from 'vue'
import { InfoFilled, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'

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
  'answer-selected',
  'answer-changed'
])

// 当前选中的代码行（多选时可能有多个）
const selectedLines = ref(new Set())

// 提取题目描述和代码内容
const parsedContent = computed(() => {
  const question = props.question || ''

  // 检查是否包含代码块标记
  const codeBlockMatch = question.match(/```[\w]*\n([\s\S]*?)```/)

  if (codeBlockMatch) {
    // 提取代码块内容和前后的描述文本
    const codeContent = codeBlockMatch[1]
    const description = question.replace(/```[\w]*\n[\s\S]*?```/, '').trim()

    return {
      description,
      codeContent
    }
  }

  // 如果没有代码块标记，检查是否直接是代码
  const lines = question.split('\n').filter(line => line.trim())

  // 如果第一行看起来像代码（包含常见代码特征），则全部作为代码
  const codeIndicators = ['function', 'const', 'let', 'var', 'if', 'for', 'while', 'class', 'import', 'export', 'return', '//', '/*']
  const firstLine = lines[0] || ''
  const hasCodeIndicator = codeIndicators.some(indicator => firstLine.includes(indicator))

  if (hasCodeIndicator || lines.length > 1) {
    return {
      description: '',
      codeContent: question
    }
  }

  // 否则作为纯文本描述
  return {
    description: question,
    codeContent: ''
  }
})

// 题目描述文本
const questionDescription = computed(() => parsedContent.value.description)

// 解析代码内容
const codeLines = computed(() => {
  const codeContent = parsedContent.value.codeContent
  if (!codeContent) return []

  return codeContent.split('\n')
    .map(line => line.trimEnd()) // 保留左侧缩进，只去除右侧空白
    .filter(line => line !== '')
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
      return `line ${match[1]}`
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
        const lineNum = parseInt(lineMatch[1])
        // 查找匹配的代码行
        const lineIndex = codeLines.value.findIndex(line => {
          const lineMatch2 = line.match(/line\s*(\d+)/i)
          if (lineMatch2) {
            return parseInt(lineMatch2[1]) === lineNum
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
  } else {
    // userAnswer 为空或 null 时，清空选中状态
    selectedLines.value.clear()
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
    <div v-if="questionDescription" class="question-text">
      {{ questionDescription }}
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
      <div
        v-if="isSubmitted"
        class="explanation-box"
        :class="{ 'is-correct': isCorrect, 'is-wrong': !isCorrect }"
      >
        <div class="explanation-header">
          <div class="status-icon" :class="isCorrect ? 'is-correct' : 'is-wrong'">
            <el-icon v-if="isCorrect"><SuccessFilled /></el-icon>
            <el-icon v-else><CircleCloseFilled /></el-icon>
          </div>
          <div class="explanation-title">
            {{ isCorrect ? '答案正确' : '答案错误' }}
          </div>
        </div>
        <div class="explanation-content">
          <!-- 正确答案 -->
          <div class="correct-answer-section">
            <span class="section-label">正确答案</span>
            <span class="section-value">{{ correctAnswer }}</span>
          </div>
          <!-- 错误位置（如果有） -->
          <div v-if="errorLocation" class="explanation-detail" style="margin-bottom: 12px; border-left-color: #ff9500;">
            <div class="detail-label">错误位置</div>
            <div class="detail-content">{{ errorLocation }}</div>
          </div>
          <!-- 解析内容 -->
          <div v-if="explanation" class="explanation-detail">
            <div class="detail-label">解析</div>
            <div class="detail-content">{{ explanation }}</div>
          </div>
          <div v-else class="explanation-detail">
            <div class="detail-label">说明</div>
            <div class="detail-content">
              {{ isCorrect ? '请找出代码中的问题并选择对应的代码行。' : '您的答案不正确。请再次检查代码。' }}
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

.code-snippet-question {
  // 使用共享的 CSS 变量
  --card-radius: var(--quiz-card-radius);
  --transition-spring: var(--quiz-transition-spring);
  --transition-smooth: var(--quiz-transition-smooth);
  --primary-color: var(--quiz-primary-color);
  --success-color: var(--quiz-success-color);
  --danger-color: var(--quiz-danger-color);
  --code-bg: var(--quiz-code-bg);
  --code-line-height: 28px;
  --code-font-size: 14px;

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
    background: transparent;
    box-shadow: none;
    border: 1px solid var(--el-border-color-lighter);
    backdrop-filter: none;
  }
  
  .code-container {
    display: grid;
    grid-template-columns: 60px 1fr;
    gap: 0;
    background: #282c34; // Atom One Dark like
    border-radius: 16px;
    padding: 24px 0;
    overflow: hidden;
    box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(0, 0, 0, 0.2);
    position: relative;

    // Window controls
    &::before {
      content: '';
      position: absolute;
      top: 16px;
      left: 20px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: #ff5f56;
      box-shadow: 20px 0 0 #ffbd2e, 40px 0 0 #27c93f;
      z-index: 2;
    }
  }
  
  .code-header {
    grid-column: 1 / -1;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding: 0 20px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    font-size: 12px;
    background: rgba(0, 0, 0, 0.2);
    margin-bottom: 12px;
    margin-top: -10px; // Offset padding
  }
  
  .code-language {
    font-weight: 600;
    color: rgba(255, 255, 255, 0.6);
    text-transform: uppercase;
    letter-spacing: 1px;
    font-size: 11px;
  }
  
  .error-hint {
    color: #ff3b30;
    font-size: 12px;
    margin-right: auto;
    margin-left: 80px; // Clear window controls
    background: rgba(255, 59, 48, 0.1);
    padding: 2px 8px;
    border-radius: 4px;
  }
  
  .line-numbers {
    text-align: right;
    font-family: 'SF Mono', 'Fira Code', 'Monaco', monospace;
    font-size: var(--code-font-size);
    line-height: var(--code-line-height);
    color: rgba(255, 255, 255, 0.3);
    user-select: none;
    padding-right: 16px;
    border-right: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.1);
  }
  
  .line-number {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    height: var(--code-line-height);
    padding-right: 8px;
    cursor: pointer;
    transition: var(--transition-smooth);
    
    &.is-selected {
      color: white;
      font-weight: 700;
    }
    
    &:hover:not(.is-disabled) {
      color: white;
    }
  }
  
  .code-content {
    font-family: 'SF Mono', 'Fira Code', 'Monaco', monospace;
    font-size: var(--code-font-size);
    line-height: var(--code-line-height);
    background: transparent;
    position: relative;
    white-space: pre-wrap;
    word-break: break-all;
    padding-left: 16px;
  }
  
  .code-line {
    display: block;
    padding: 0 16px 0 4px;
    transition: var(--transition-smooth);
    color: #abb2bf;
    
    &.is-selected {
      background: rgba(var(--el-color-primary-rgb), 0.2);
      box-shadow: inset 3px 0 0 var(--primary-color);
      cursor: pointer;
    }
    
    &:hover:not(.is-disabled) {
      background: rgba(255, 255, 255, 0.05);
    }
    
    &.is-correct {
      background: rgba(52, 199, 89, 0.15);
      box-shadow: inset 3px 0 0 var(--success-color);
    }
    
    &.is-wrong {
      background: linear-gradient(90deg, rgba(255, 59, 48, 0.2) 0%, rgba(255, 59, 48, 0.1) 100%);
      box-shadow: inset 3px 0 0 var(--danger-color), inset 0 0 0 1px rgba(255, 59, 48, 0.3);
      animation: shake 0.5s ease-in-out;
    }
    
    &.is-disabled {
      opacity: 0.5;
      cursor: default;
    }
  }
  
  .code-line-text {
    margin: 0;
    font-family: inherit;
    white-space: pre-wrap;
    word-break: break-all;
  }
  
  @keyframes shake {
    10%, 90% { transform: translate3d(-1px, 0, 0); }
    20%, 80% { transform: translate3d(2px, 0, 0); }
    30%, 50%, 70% { transform: translate3d(-4px, 0, 0); }
    40%, 60% { transform: translate3d(4px, 0, 0); }
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
      text-shadow: 0 1px 2px rgba(0,0,0,0.5);
    }
    
    .code-container {
      background: #1e1e1e;
      border-color: rgba(255, 255, 255, 0.1);
    }
  }
}
</style>
