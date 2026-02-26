/**
 * 重试策略
 * 实现指数退避算法，判断是否应该重试
 */

import { errorHandler, ErrorType } from './ErrorHandler.js'

/**
 * 指数退避重试策略
 */
export class RetryStrategy {
  constructor(options = {}) {
    this.maxRetries = options.maxRetries || 3           // 最大重试次数
    this.baseDelay = options.baseDelay || 1000          // 基础延迟（毫秒）
    this.maxDelay = options.maxDelay || 30000           // 最大延迟（毫秒）
  }

  /**
   * 判断是否应该重试
   * @param {Error} error - 错误对象
   * @param {number} attemptNumber - 当前重试次数
   * @returns {boolean} 是否应该重试
   */
  shouldRetry(error, attemptNumber) {
    // 超过最大重试次数
    if (attemptNumber >= this.maxRetries) {
      return false
    }

    // 分类错误
    const classification = errorHandler.classify(error)

    // 只有可重试的错误才重试
    return classification.retryable
  }

  /**
   * 计算重试延迟（指数退避）
   * @param {number} attemptNumber - 当前重试次数
   * @returns {number} 延迟时间（毫秒）
   */
  getDelay(attemptNumber) {
    // 指数退避：baseDelay * 2^attemptNumber
    // 例如：1s, 2s, 4s, 8s, 16s, 30s(max)
    const delay = Math.min(this.baseDelay * Math.pow(2, attemptNumber), this.maxDelay)

    // 添加随机抖动（±25%），避免同时重试
    const jitter = delay * 0.25 * (Math.random() * 2 - 1)

    return Math.max(0, Math.floor(delay + jitter))
  }

  /**
   * 等待延迟
   * @param {number} attemptNumber - 当前重试次数
   * @returns {Promise<void>}
   */
  async wait(attemptNumber) {
    const delay = this.getDelay(attemptNumber)
    await new Promise(resolve => setTimeout(resolve, delay))
  }

  /**
   * 重置重试状态
   */
  reset() {
    // 当前实现是无状态的，不需要重置
    // 如果需要添加限流等功能，可以在这里实现
  }
}

/**
 * 创建默认的重试策略实例
 */
export const defaultRetryStrategy = new RetryStrategy({
  maxRetries: 3,
  baseDelay: 1000,
  maxDelay: 30000
})
