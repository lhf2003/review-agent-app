import { computed, type Ref } from 'vue'

/**
 * 答案验证策略配置
 */
export interface AnswerValidationOptions {
  /** 单选题验证策略 */
  singleChoice?: (option: string, correctAnswer: string) => boolean
  /** 多选题验证策略 */
  multipleChoice?: (answers: string[], correctAnswer: string) => boolean
  /** 判断题验证策略 */
  trueFalse?: (answer: string, correctAnswer: string) => boolean
  /** 填空题验证策略 */
  fillBlank?: (answers: string[], correctAnswer: string, caseSensitive?: boolean) => boolean
}

/**
 * 单选题默认验证策略
 */
export function singleChoiceValidation(userAnswer: string, correctAnswer: string): boolean {
  if (!userAnswer) return false
  const userLetter = userAnswer.charAt(0).toUpperCase()
  const correctLetter = correctAnswer.charAt(0).toUpperCase()
  return userLetter === correctLetter
}

/**
 * 多选题默认验证策略
 */
export function multipleChoiceValidation(userAnswers: string[], correctAnswer: string): boolean {
  if (!userAnswers || userAnswers.length === 0) return false

  const correctAnswers = correctAnswer.split(',').map(a => a.trim().toUpperCase())
  const userAnswersUpper = userAnswers.map(a => a.toUpperCase())

  // 数组长度相同
  if (correctAnswers.length !== userAnswersUpper.length) return false

  // 包含所有正确答案
  const hasAllCorrect = correctAnswers.every(ca => userAnswersUpper.includes(ca))

  // 没有多余的错误答案
  const hasNoExtra = userAnswersUpper.every(ua => correctAnswers.includes(ua))

  return hasAllCorrect && hasNoExtra
}

/**
 * 判断题默认验证策略
 */
export function trueFalseValidation(userAnswer: string, correctAnswer: string): boolean {
  if (!userAnswer) return false
  return userAnswer.toLowerCase() === correctAnswer.toLowerCase()
}

/**
 * 填空题默认验证策略
 */
export function fillBlankValidation(
  userAnswers: string[],
  correctAnswer: string,
  caseSensitive: boolean = true
): boolean {
  if (!userAnswers || userAnswers.length === 0) return false

  const correctAnswers = correctAnswer.split(/,|;/).map(a => a.trim())

  // 检查每个填空是否正确
  return userAnswers.every((answer, index) => {
    if (!correctAnswers[index]) return false

    const userVal = caseSensitive ? answer.trim() : answer.trim().toLowerCase()
    const correctVal = caseSensitive
      ? correctAnswers[index].trim()
      : correctAnswers[index].trim().toLowerCase()

    return userVal === correctVal
  })
}

/**
 * 题型组件答案验证 Composable
 * 提供统一的答案验证逻辑
 *
 * @param userAnswer - 用户答案
 * @param correctAnswer - 正确答案
 * @param strategy - 验证策略配置
 * @returns 答案验证结果
 */
export function useAnswerValidation(
  userAnswer: Ref<string | null>,
  correctAnswer: string,
  strategy: AnswerValidationOptions
) {
  /**
   * 检查答案是否正确
   */
  const isCorrect = computed(() => {
    if (!userAnswer.value) return false

    if (strategy.singleChoice) {
      return strategy.singleChoice(userAnswer.value, correctAnswer)
    }

    if (strategy.multipleChoice) {
      const answers = userAnswer.value.split(',').map(a => a.trim())
      return strategy.multipleChoice(answers, correctAnswer)
    }

    if (strategy.trueFalse) {
      return strategy.trueFalse(userAnswer.value, correctAnswer)
    }

    if (strategy.fillBlank) {
      const answers = userAnswer.value.split(/,|;/).map(a => a.trim())
      return strategy.fillBlank(answers, correctAnswer)
    }

    return false
  })

  /**
   * 检查是否错误
   */
  const isWrong = computed(() => {
    return userAnswer.value !== null && userAnswer.value !== '' && !isCorrect.value
  })

  return {
    isCorrect,
    isWrong
  }
}

/**
 * 导出默认验证策略
 */
export const defaultValidationStrategies = {
  singleChoice: singleChoiceValidation,
  multipleChoice: multipleChoiceValidation,
  trueFalse: trueFalseValidation,
  fillBlank: fillBlankValidation
} as const
