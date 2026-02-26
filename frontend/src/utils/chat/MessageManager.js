/**
 * 消息管理器
 * 负责消息的发送、重试、取消和队列管理
 */

import { ChatStreamHandler } from './ChatStreamHandler.js'
import { defaultRetryStrategy } from './RetryStrategy.js'
import { errorHandler } from './ErrorHandler.js'

/**
 * 生成唯一ID
 */
function generateId() {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}

/**
 * 消息状态枚举
 */
export const MessageStatus = {
  PENDING: 'pending',       // 等待发送
  SENDING: 'sending',       // 发送中
  STREAMING: 'streaming',   // 流式接收中
  SUCCESS: 'success',       // 成功
  FAILED: 'failed',         // 失败
  CANCELLED: 'cancelled',   // 已取消
  RETRYING: 'retrying'      // 重试中
}

/**
 * 消息管理器
 */
export class MessageManager {
  constructor(options = {}) {
    this.messages = []               // 消息列表
    this.queue = []                  // 发送队列
    this.processing = false          // 是否正在处理队列
    this.currentController = null    // 当前请求的控制器

    // 依赖注入
    this.retryStrategy = options.retryStrategy || defaultRetryStrategy
    this.onMessagesUpdate = options.onMessagesUpdate || (() => {})

    // API 调用函数（需要从外部注入）
    this.apiCall = options.apiCall
  }

  /**
   * 发送消息
   * @param {string} content - 消息内容
   * @param {Object} options - 选项
   * @returns {Promise<Object>} 消息对象
   */
  async send(content, options = {}) {
    // 创建消息对象
    const message = {
      id: generateId(),
      content,
      role: 'user',
      status: MessageStatus.PENDING,
      timestamp: Date.now(),
      retryCount: 0,
      error: null,
      metadata: options.metadata || {}
    }

    // 添加到消息列表
    this.messages.push(message)
    this._notifyUpdate()

    // 添加到发送队列
    await this.enqueue(message)

    return message
  }

  /**
   * 将消息加入队列
   * @private
   */
  async enqueue(message) {
    this.queue.push(message)

    if (!this.processing) {
      this.processing = true
      await this._processQueue()
    }
  }

  /**
   * 处理发送队列
   * @private
   */
  async _processQueue() {
    while (this.queue.length > 0) {
      const message = this.queue.shift()

      try {
        await this._sendMessage(message)
      } catch (error) {
        errorHandler.log(error, { context: 'MessageManager._processQueue', messageId: message.id })
      }
    }

    this.processing = false
  }

  /**
   * 发送单条消息
   * @private
   */
  async _sendMessage(message) {
    let attemptNumber = message.retryCount
    let aiMessage = null

    while (attemptNumber <= this.retryStrategy.maxRetries) {
      try {
        // 更新状态
        message.status = attemptNumber === 0 ? MessageStatus.SENDING : MessageStatus.RETRYING
        message.retryCount = attemptNumber
        this._notifyUpdate()

        // 创建 AI 消息占位符
        if (message.role === 'user') {
          aiMessage = {
            id: generateId(),
            content: '',
            role: 'assistant',
            status: MessageStatus.STREAMING,
            timestamp: Date.now(),
            retryCount: 0,
            error: null,
            metadata: {}
          }
          this.messages.push(aiMessage)
          this._notifyUpdate()
        }

        // 调用 API（传递 AI 消息引用）
        const result = await this._callApi(message, aiMessage)

        // 成功
        message.status = MessageStatus.SUCCESS
        if (aiMessage) {
          aiMessage.status = MessageStatus.SUCCESS
        }
        this._notifyUpdate()

        return result

      } catch (error) {
        // 判断是否应该重试
        const shouldRetry = this.retryStrategy.shouldRetry(error, attemptNumber)

        if (shouldRetry && attemptNumber < this.retryStrategy.maxRetries) {
          // 等待后重试
          await this.retryStrategy.wait(attemptNumber)
          attemptNumber++
          continue
        } else {
          // 失败
          message.status = MessageStatus.FAILED
          message.error = errorHandler.classify(error)
          this._notifyUpdate()

          // 移除 AI 消息占位符
          if (aiMessage) {
            const index = this.messages.findIndex(m => m.id === aiMessage.id)
            if (index > -1) {
              this.messages.splice(index, 1)
            }
          }

          throw error
        }
      }
    }
  }

  /**
   * 调用 API
   * @private
   * @param {Object} message - 用户消息
   * @param {Object} aiMessage - AI 消息占位符（可选）
   */
  async _callApi(message, aiMessage = null) {
    if (!this.apiCall) {
      throw new Error('API call function not provided')
    }

    return new Promise((resolve, reject) => {
      const streamHandler = new ChatStreamHandler()

      // 设置事件监听
      streamHandler.on('chunk', (data) => {
        // 使用传入的 AI 消息引用，而不是查找
        if (aiMessage) {
          // 处理不同类型的数据
          if (typeof data === 'string') {
            aiMessage.content += data
          } else if (data?.content) {
            aiMessage.content += data.content
          } else if (data?.text) {
            aiMessage.content += data.text
          }

          this._notifyUpdate()
        }
      })

      streamHandler.on('complete', () => {
        this.currentController = null
        resolve()
      })

      streamHandler.on('error', (error) => {
        this.currentController = null
        reject(error)
      })

      // 调用 API
      const response = this.apiCall(message.content, {
        signal: streamHandler.controller.signal
      })

      // 启动流处理
      response.then(resp => {
        this.currentController = streamHandler.start(resp)
      }).catch(error => {
        this.currentController = null
        reject(error)
      })
    })
  }

  /**
   * 重试失败的消息
   * @param {string} messageId - 消息ID
   * @returns {Promise<Object>} 消息对象
   */
  async retry(messageId) {
    const message = this.getMessage(messageId)

    if (!message) {
      throw new Error('Message not found')
    }

    if (message.status !== MessageStatus.FAILED) {
      throw new Error('Only failed messages can be retried')
    }

    // 重置消息状态
    message.status = MessageStatus.PENDING
    message.error = null
    this._notifyUpdate()

    // 重新发送
    await this.enqueue(message)

    return message
  }

  /**
   * 取消正在发送的消息
   * @param {string} messageId - 消息ID
   */
  cancel(messageId) {
    const message = this.getMessage(messageId)

    if (!message) {
      return
    }

    // 取消当前请求
    if (this.currentController) {
      this.currentController.abort()
      this.currentController = null
    }

    // 更新消息状态
    if (message.status === MessageStatus.SENDING ||
        message.status === MessageStatus.RETRYING ||
        message.status === MessageStatus.STREAMING) {
      message.status = MessageStatus.CANCELLED
      this._notifyUpdate()
    }

    // 从队列中移除
    const queueIndex = this.queue.findIndex(m => m.id === messageId)
    if (queueIndex > -1) {
      this.queue.splice(queueIndex, 1)
    }
  }

  /**
   * 获取消息
   * @param {string} messageId - 消息ID
   * @returns {Object|null} 消息对象
   */
  getMessage(messageId) {
    return this.messages.find(m => m.id === messageId) || null
  }

  /**
   * 获取所有消息
   * @returns {Array} 消息列表
   */
  getMessages() {
    return this.messages
  }

  /**
   * 清空所有消息
   */
  clear() {
    // 取消所有进行中的请求
    if (this.currentController) {
      this.currentController.abort()
      this.currentController = null
    }

    // 清空消息和队列
    this.messages = []
    this.queue = []
    this.processing = false

    this._notifyUpdate()
  }

  /**
   * 设置 API 调用函数
   * @param {Function} apiCall - API 调用函数
   */
  setApiCall(apiCall) {
    this.apiCall = apiCall
  }

  /**
   * 通知消息更新
   * @private
   */
  _notifyUpdate() {
    if (this.onMessagesUpdate) {
      this.onMessagesUpdate(this.messages)
    }
  }
}
