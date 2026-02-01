<script setup>
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
      v-if="type === 'single_choice' && !componentsLoaded.single_choice"
      ref="questionComponents.single_choice"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :show-explanation="isSubmitted && explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="() => $emit('answer-selected', $event.detail)"
      @answer-changed="() => $emit('answer-changed', $event.detail)"
      @mounted="componentsLoaded.single_choice = true"
    />

    <!-- 多选题 -->
    <MultipleChoiceQuestion
      v-if="type === 'multiple_choice' && !componentsLoaded.multiple_choice"
      ref="questionComponents.multiple_choice"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :show-explanation="isSubmitted && explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="() => $emit('answer-selected', $event.detail)"
      @answer-changed="() => $emit('answer-changed', $event.detail)"
      @mounted="componentsLoaded.multiple_choice = true"
    />

    <!-- 判断题 -->
    <TrueFalseQuestion
      v-if="type === 'true_false' && !componentsLoaded.true_false"
      ref="questionComponents.true_false"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :show-explanation="isSubmitted && explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="() => $emit('answer-selected', $event.detail)"
      @answer-changed="() => $emit('answer-changed', $event.detail)"
      @mounted="componentsLoaded.true_false = true"
    />

    <!-- 填空题 -->
    <FillBlankQuestion
      v-if="type === 'fill_blank' && !componentsLoaded.fill_blank"
      ref="questionComponents.fill_blank"
      :question="question"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :show-explanation="isSubmitted && explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="() => $emit('answer-selected', $event.detail)"
      @answer-changed="() => $emit('answer-changed', $event.detail)"
      @mounted="componentsLoaded.fill_blank = true"
    />

    <!-- 代码识别题 -->
    <CodeSnippetQuestion
      v-if="type === 'code_snippet' && !componentsLoaded.code_snippet"
      ref="questionComponents.code_snippet"
      :question="question"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :show-explanation="isSubmitted && explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="() => $emit('answer-selected', $event.detail)"
      @answer-changed="() => $emit('answer-changed', $event.detail)"
      @mounted="componentsLoaded.code_snippet = true"
    />

    <!-- 未知题型降级 -->
    <SingleChoiceQuestion
      v-if="!['single_choice', 'multiple_choice', 'true_false', 'fill_blank', 'code_snippet'].includes(type)"
      :question="question"
      :options="options"
      :user-answer="userAnswer"
      :correct-answer="correctAnswer"
      :show-explanation="isSubmitted && explanation"
      :index="index"
      :knowledge-point="knowledgePoint"
      @answer-selected="() => $emit('answer-selected', $event.detail)"
      @answer-changed="() => $emit('answer-changed', $event.detail)"
      @mounted="() => { console.warn(`Unknown question type: ${type}, falling back to single_choice`) }"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.question-renderer {
  // 无样式，由子组件提供
  
  .compact {
    padding: 12px;
    margin-bottom: 8px;
  }
}
</style>
