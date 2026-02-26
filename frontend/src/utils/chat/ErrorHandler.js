/**
 * 错误处理器
 * 负责错误分类、用户友好提示和错误日志记录
 */

// 错误类型枚举
export const ErrorType = {
  NETWORK: 'network',       // 网络错误
  TIMEOUT: 'timeout',       // 超时
  AUTH: 'auth',             // 认证失败
  SERVER: 'server',         // 服务器错误
  LLM: 'llm',               // LLM服务错误
  CANCEL: 'cancel',         // 用户取消
  UNKNOWN: 'unknown'        // 未知错误
}

// 用户友好的错误消息
const ERROR_MESSAGES = {
  [ErrorType.NETWORK]: '网络连接失败，请检查网络设置',
  [ErrorType.TIMEOUT]: '请求超时，请稍后重试',
  [ErrorType.AUTH]: '登录已过期，请重新登录',
  [ErrorType.SERVER]: '服务器错误，请稍后重试',
  [ErrorType.LLM]: 'AI 服务暂时不可用，请稍后重试',
  [ErrorType.CANCEL]: '已取消发送',
  [ErrorType.UNKNOWN]: '发生未知错误，请稍后重试'
}

export class ErrorHandler {
  /**
   * 分类错误
   * @param {Error} error - 错误对象
   * @returns {Object} { type, retryable, message }
   */
  classify(error) {
    const errorMsg = error?.message || ''
    const errorName = error?.name || ''

    // 用户取消
    if (errorName === 'AbortError') {
      return {
        type: ErrorType.CANCEL,
        retryable: false,
        message: ERROR_MESSAGES[ErrorType.CANCEL]
      }
    }

    // 超时错误
    if (errorMsg.includes('timeout') || errorMsg.includes('Timeout')) {
      return {
        type: ErrorType.TIMEOUT,
        retryable: true,
        message: ERROR_MESSAGES[ErrorType.TIMEOUT]
      }
    }

    // 认证错误
    if (errorMsg.includes('401') || errorMsg.includes('403') || errorMsg.includes('Unauthorized')) {
      return {
        type: ErrorType.AUTH,
        retryable: false,
        message: ERROR_MESSAGES[ErrorType.AUTH]
      }
    }

    // 服务器错误
    if (errorMsg.includes('500') || errorMsg.includes('502') || errorMsg.includes('503')) {
      return {
        type: ErrorType.SERVER,
        retryable: true,
        message: ERROR_MESSAGES[ErrorType.SERVER]
      }
    }

    // 网络错误
    if (errorMsg.includes('Failed to fetch') || errorMsg.includes('NetworkError')) {
      return {
        type: ErrorType.NETWORK,
        retryable: true,
        message: ERROR_MESSAGES[ErrorType.NETWORK]
      }
    }

    // LLM 服务错误
    if (errorMsg.includes('LLM') || errorMsg.includes('model') || errorMsg.includes('AI')) {
      return {
        type: ErrorType.LLM,
        retryable: true,
        message: ERROR_MESSAGES[ErrorType.LLM]
      }
    }

    // 未知错误
    return {
      type: ErrorType.UNKNOWN,
      retryable: false,
      message: ERROR_MESSAGES[ErrorType.UNKNOWN]
    }
  }

  /**
   * 获取用户友好的错误消息
   * @param {Error} error - 错误对象
   * @returns {string} 用户友好的错误消息
   */
  getUserMessage(error) {
    const classification = this.classify(error)
    return classification.message
  }

  /**
   * 记录错误日志
   * @param {Error} error - 错误对象
   * @param {Object} context - 错误上下文
   */
  log(error, context = {}) {
    const classification = this.classify(error)
    const logData = {
      timestamp: new Date().toISOString(),
      type: classification.type,
      message: error?.message || 'Unknown error',
      stack: error?.stack,
      context
    }

    // 在开发环境下打印详细日志
    if (import.meta.env.DEV) {
      console.error('[Chat Error]', logData)
    }

    // 在生产环境可以发送到错误监控服务
    if (!import.meta.env.DEV) {
      // TODO: 发送到错误监控服务（如 Sentry）
      // Sentry.captureException(error, { extra: context })
    }
  }
}

// 导出单例
export const errorHandler = new ErrorHandler()
