import { computed, type Ref } from 'vue'

/**
 * 题型组件公共 Props 类型定义
 */
export interface QuestionBaseProps {
  /** 题目内容 */
  question: string
  /** 用户答案 */
  userAnswer: string | null
  /** 是否已提交 */
  isSubmitted: boolean
  /** 正确答案 */
  correctAnswer: string
  /** 解析内容 */
  explanation: string
  /** 是否显示解析 */
  showExplanation: boolean
  /** 是否有解析内容 */
  hasExplanation: boolean
  /** 是否紧凑模式 */
  compact: boolean
  /** 知识点 */
  knowledgePoint: string | null
  /** 题目索引 */
  index?: number
}

/**
 * 题型组件公共 Events 类型定义
 */
export interface QuestionEmits {
  (e: 'answer-selected', value: any): void
  (e: 'answer-changed', value: any): void
}

/**
 * 题型组件基础 Composable
 * 提供公共的计算属性和方法
 *
 * @param props - 题型组件 props
 * @returns 公共计算属性和方法
 */
export function useQuestionBase(
  props: QuestionBaseProps
) {
  /**
   * 检查是否应该显示解析
   */
  const shouldShowExplanation = computed(() => {
    return props.isSubmitted && props.showExplanation
  })

  /**
   * 检查是否有解析内容
   */
  const hasExplanationContent = computed(() => {
    return props.hasExplanation && props.explanation
  })

  /**
   * 检查是否显示知识点标签
   */
  const shouldShowKnowledgePoint = computed(() => {
    return !props.compact && props.knowledgePoint
  })

  return {
    shouldShowExplanation,
    hasExplanationContent,
    shouldShowKnowledgePoint
  }
}
