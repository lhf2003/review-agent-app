<script setup>
import { ref, computed, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

/**
 * 判断题组件
 * 简洁的是/否选择
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
  // 正确答案
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

// 当前选中值
const selectedValue = ref(null)

// 悬停状态
const isHoveredTrue = ref(false)
const isHoveredFalse = ref(false)

// 检查当前选中是否正确
const isCorrect = computed(() => {
  if (!selectedValue.value) return false
  return selectedValue.value.toLowerCase() === props.correctAnswer.toLowerCase()
})

// 检查已提交后的显示答案
const displayAnswer = computed(() => {
  if (!props.userAnswer) return null
  return props.userAnswer.toLowerCase()
})

// 检查是否应该显示正确答案
const shouldShowCorrect = computed(() => {
  return props.isSubmitted && isCorrect.value
})

// 处理选择
const handleSelect = (value) => {
  if (props.isSubmitted) return
  
  selectedValue.value = value
  
  emit('answer-selected', value.toLowerCase())
  emit('answer-changed', value.toLowerCase())
}

// 监听 props.userAnswer 变化
watch(() => props.userAnswer, (newAnswer) => {
  if (newAnswer) {
    selectedValue.value = newAnswer.toLowerCase()
  } else {
    // userAnswer 为空或 null 时，重置选中状态
    selectedValue.value = null
  }
}, { immediate: true })

// 暴露重置方法
defineExpose({
  resetSelection: () => {
    selectedValue.value = null
    isHoveredTrue.value = false
    isHoveredFalse.value = false
  }
})
</script>

<template>
  <div 
    class="true-false-question"
    :class="{
      'compact-mode': compact,
      'is-submitted': isSubmitted
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

    <!-- 选项卡片 -->
    <div class="options-container">
      <!-- 选项 -->
      <div 
        class="option-item option-true"
        :class="{
          'is-selected': displayAnswer === 'true' && selectedValue === 'true',
          'is-show-correct': shouldShowCorrect && props.correctAnswer.toLowerCase() === 'true',
          'is-hovered': isHoveredTrue,
          'is-disabled': isSubmitted
        }"
        @mouseenter="isHoveredTrue = true"
        @mouseleave="isHoveredTrue = false"
        @click="handleSelect('true')"
      >
        <div class="option-content">
          <span class="option-label">是</span>
          <span class="option-value">YES</span>
        </div>
        
        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted && selectedValue === 'true'" class="answer-icon">
            <el-icon v-if="isCorrect" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else color="#f56c6c">
              <CircleCloseFilled />
            </el-icon>
          </div>
        </transition>
      </div>

      <!-- 否选项 -->
      <div 
        class="option-item option-false"
        :class="{
          'is-selected': displayAnswer === 'false' && selectedValue === 'false',
          'is-show-correct': shouldShowCorrect && props.correctAnswer.toLowerCase() === 'false',
          'is-hovered': isHoveredFalse,
          'is-disabled': isSubmitted
        }"
        @mouseenter="isHoveredFalse = true"
        @mouseleave="isHoveredFalse = false"
        @click="handleSelect('false')"
      >
        <div class="option-content">
          <span class="option-label">否</span>
          <span class="option-value">NO</span>
        </div>
        
        <!-- 正确/错误图标 -->
        <transition name="icon-fade">
          <div v-if="isSubmitted && selectedValue === 'false'" class="answer-icon">
            <el-icon v-if="isCorrect" color="#67c23a">
              <SuccessFilled />
            </el-icon>
            <el-icon v-else color="#f56c6c">
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
            {{ isCorrect ? '答案正确！' : '答案错误' }}
          </div>
        </div>
        <div class="explanation-content">
          <!-- 答错时显示正确答案 -->
          <div v-if="!isCorrect" class="explanation-text">
            <strong>正确答案：</strong>{{ correctAnswer === 'true' ? '是' : '否' }}
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
@import '../../styles/quiz-common';
@import '../../styles/variables';

.true-false-question {
  // 使用共享的 CSS 变量
  --card-radius: var(--quiz-card-radius);
  --transition-spring: var(--quiz-transition-spring);
  --transition-smooth: var(--quiz-transition-smooth);
  --primary-color: var(--quiz-primary-color);
  --success-color: var(--quiz-success-color);
  --danger-color: var(--quiz-danger-color);

  // Specific colors for True/False
  --true-color: #34c759;
  --false-color: #ff3b30;

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

    .option-item { min-height: 80px; padding: 16px; }
    .option-value { font-size: 20px; }
  }

  &.is-submitted .option-item {
    cursor: default;
  }

  .options-container {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
      gap: 16px;
    }
  }
  
  .option-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 32px;
    border-radius: 20px;
    border: 1px solid rgba(0, 0, 0, 0.06);
    background: var(--el-fill-color-light);
    cursor: pointer;
    transition: var(--transition-spring);
    min-height: 160px;
    position: relative;
    overflow: hidden;
    
    // Glass shine effect
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 100%;
      background: linear-gradient(180deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 100%);
      opacity: 0;
      transition: opacity 0.3s ease;
    }
    
    &:hover:not(.is-disabled) {
      transform: translateY(-4px) scale(1.02);
      box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.15);
      background: var(--el-bg-color);
      z-index: 1;

      &::before { opacity: 1; }
    }
    
    &.option-true {
      &:hover:not(.is-disabled) {
        // 移除悬浮时的特定颜色边框
      }
    }

    &.option-false {
      &:hover:not(.is-disabled) {
        // 移除悬浮时的特定颜色边框
      }
    }
    
    &.is-selected {
      border-width: 2px;
      transform: scale(1.02);
      z-index: 2;

      // 未提交时：使用统一的主色调，不暴露答案
      &:not(.is-show-correct) {
        border-color: var(--primary-color);
        background: var(--el-color-primary-light-9);
        box-shadow: 0 8px 16px -4px rgba(var(--el-color-primary-rgb), 0.25);

        .option-value {
          color: var(--primary-color);
        }
      }
    }

    &.option-true.is-selected.is-show-correct {
      border-color: var(--true-color);
      background: rgba(52, 199, 89, 0.1);
      box-shadow: 0 12px 24px -8px rgba(52, 199, 89, 0.3);

      .option-value { color: var(--true-color); }
    }

    &.option-false.is-selected.is-show-correct {
      border-color: var(--false-color);
      background: rgba(255, 59, 48, 0.1);
      box-shadow: 0 12px 24px -8px rgba(255, 59, 48, 0.3);

      .option-value { color: var(--false-color); }
    }
    
    &.is-show-correct {
      border-color: var(--success-color) !important;
      background: rgba(52, 199, 89, 0.2) !important;
      box-shadow: 0 0 0 4px rgba(52, 199, 89, 0.2);
    }
    
    &.is-disabled {
      opacity: 0.6;
      cursor: default;
      filter: grayscale(0.5);
      
      &:hover {
        transform: none;
        box-shadow: none;
        border-color: rgba(0, 0, 0, 0.06);
        background: var(--el-fill-color-light);
      }
    }
  }
  
  .option-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    position: relative;
    z-index: 1;
  }
  
  .option-label {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
    text-transform: uppercase;
    letter-spacing: 2px;
  }
  
  .option-value {
    font-size: 36px;
    font-weight: 800;
    color: var(--el-text-color-primary);
    transition: color 0.3s ease;
  }
  
  .answer-icon {
    position: absolute;
    right: 20px;
    top: 20px;
    font-size: 32px;
    filter: drop-shadow(0 4px 8px rgba(0,0,0,0.1));
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
    
    .option-item {
      background: rgba(44, 44, 46, 0.4);
      border-color: rgba(255, 255, 255, 0.08);
      
      &::before {
        background: linear-gradient(180deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0) 100%);
      }
      
      &:hover:not(.is-disabled) {
        background: rgba(58, 58, 60, 0.8);
        border-color: rgba(255, 255, 255, 0.2);
      }
    }
    
    .option-value {
      color: rgba(255, 255, 255, 0.9);
    }
    
    .option-label {
      color: rgba(255, 255, 255, 0.5);
    }

    // 暗色模式下也保持选中状态的一致性
    .option-item.is-selected:not(.is-show-correct) {
      border-color: var(--primary-color);
      background: rgba(var(--el-color-primary-rgb), 0.2);

      .option-value {
        color: var(--el-color-primary);
      }
    }

    .option-item.option-true.is-selected.is-show-correct {
      border-color: var(--true-color);
      background: rgba(52, 199, 89, 0.2);

      .option-value { color: #4cd964; }
    }

    .option-item.option-false.is-selected.is-show-correct {
      border-color: var(--false-color);
      background: rgba(255, 69, 58, 0.2);

      .option-value { color: #ff453a; }
    }
  }
}
</style>
