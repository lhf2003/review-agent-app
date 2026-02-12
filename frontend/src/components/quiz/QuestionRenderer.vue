<script setup>
import { ref, computed } from 'vue'
import SingleChoiceQuestion from './SingleChoiceQuestion.vue'
import MultipleChoiceQuestion from './MultipleChoiceQuestion.vue'
import TrueFalseQuestion from './TrueFalseQuestion.vue'
import FillBlankQuestion from './FillBlankQuestion.vue'
import CodeSnippetQuestion from './CodeSnippetQuestion.vue'

/**
 * 题目统一渲染组件
 * 根据 question.type 动态渲染不同题型
 */
const props = defineProps({
  // 题目内容
  question: {
    type: String,
    required: true
  },
  // 题目类型
  type: {
    type: String,
    required: true,
    validator: (value) => ['single_choice', 'multiple_choice', 'true_false', 'fill_blank', 'code_snippet'].includes(value)
  },
  // 选项列表（单选、多选、判断题、代码题）
  options: {
    type: Array,
    default: () => []
  },
  // 用户答案
  userAnswer: {
    type: String,
    default: null
  },
  // 正确答案
  correctAnswer: {
    type: [String, Object],
    required: true
  },
  // 解析
  explanation: {
    type: String,
    default: ''
  },
  // 是否已提交
  isSubmitted: {
    type: Boolean,
    default: false
  },
  // 题目索引
  index: {
    type: Number,
    default: 1
  },
  // 知识点（可选）
  knowledgePoint: {
    type: String,
    default: null
  },
  // 紧凑模式（用于列表显示）
  compact: {
    type: Boolean,
    default: false
  },
  // 填空题的空位数量
  blankCount: {
    type: Number,
    default: null
  },
  // 题目ID（用于组件key，确保切换题目时重新创建组件实例）
  questionId: {
    type: [Number, String],
    required: true
  }
})

// 当前组件实例引用
const questionComponents = {
  single_choice: null,
  multiple_choice: null,
  true_false: null,
  fill_blank: null,
  code_snippet: null
}

// 组件加载状态
const componentsLoaded = ref({
  single_choice: false,
  multiple_choice: false,
  true_false: false,
  fill_blank: false,
  code_snippet: false
})

// 暴露给父组件的方法
const resetSelection = () => {
  const currentComponent = questionComponents[props.type]
  if (currentComponent && currentComponent.resetSelection) {
    currentComponent.resetSelection()
  }
}

const validateAnswer = () => {
  const currentComponent = questionComponents[props.type]
  if (currentComponent && currentComponent.validateAnswer) {
    return currentComponent.validateAnswer()
  }
  return false
}

defineExpose({
  resetSelection,
  validateAnswer
})
</script>

<template>
  <div class="question-renderer" :class="{ compact }">
    <!-- 单选题 -->
    <SingleChoiceQuestion
      v-if="type === 'single_choice'"
      :key="questionId"
      ref="questionComponents.single_choice"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :explanation="explanation"
      :show-explanation="isSubmitted"
      :is-submitted="isSubmitted"
      :has-explanation="!!explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      :compact="compact"
      @answer-selected="$emit('answer-selected', $event)"
      @answer-changed="$emit('answer-changed', $event)"
    />

    <!-- 多选题 -->
    <MultipleChoiceQuestion
      v-if="type === 'multiple_choice'"
      :key="questionId"
      ref="questionComponents.multiple_choice"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :explanation="explanation"
      :show-explanation="isSubmitted"
      :is-submitted="isSubmitted"
      :has-explanation="!!explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      :compact="compact"
      @answer-selected="$emit('answer-selected', $event)"
      @answer-changed="$emit('answer-changed', $event)"
    />

    <!-- 判断题 -->
    <TrueFalseQuestion
      v-if="type === 'true_false'"
      :key="questionId"
      ref="questionComponents.true_false"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :explanation="explanation"
      :show-explanation="isSubmitted"
      :is-submitted="isSubmitted"
      :has-explanation="!!explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      :compact="compact"
      @answer-selected="$emit('answer-selected', $event)"
      @answer-changed="$emit('answer-changed', $event)"
    />

    <!-- 填空题 -->
    <FillBlankQuestion
      v-if="type === 'fill_blank'"
      :key="questionId"
      ref="questionComponents.fill_blank"
      :question="question"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :explanation="explanation"
      :show-explanation="isSubmitted"
      :is-submitted="isSubmitted"
      :has-explanation="!!explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      :compact="compact"
      :blank-count="blankCount"
      @answer-selected="$emit('answer-selected', $event)"
      @answer-changed="$emit('answer-changed', $event)"
    />

    <!-- 代码识别题 -->
    <CodeSnippetQuestion
      v-if="type === 'code_snippet'"
      :key="questionId"
      ref="questionComponents.code_snippet"
      :question="question"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :explanation="explanation"
      :show-explanation="isSubmitted"
      :is-submitted="isSubmitted"
      :has-explanation="!!explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      :compact="compact"
      @answer-selected="$emit('answer-selected', $event)"
      @answer-changed="$emit('answer-changed', $event)"
    />

    <!-- 未知题型降级 -->
    <SingleChoiceQuestion
      v-if="!['single_choice', 'multiple_choice', 'true_false', 'fill_blank', 'code_snippet'].includes(type)"
      :key="questionId"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :explanation="explanation"
      :show-explanation="isSubmitted"
      :is-submitted="isSubmitted"
      :has-explanation="!!explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="$emit('answer-selected', $event)"
      @answer-changed="$emit('answer-changed', $event)"
      @mounted="() => {}"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.question-renderer {
  // 容器样式保持透明，布局由子组件自适应
  width: 100%;
  position: relative;
  
  &.compact {
    // 紧凑模式下的容器调整
    margin-bottom: 0;
  }
}
</style>
