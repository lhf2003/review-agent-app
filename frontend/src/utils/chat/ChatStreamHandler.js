/**
 * 聊天流式响应处理器
 * 处理 SSE (Server-Sent Events) 流式响应
 */

import { errorHandler } from './ErrorHandler.js'

/**
 * 流控制器
 */
export class StreamController {
  constructor(abortController) {
    this.abortController = abortController
    this.isAborted = false
  }

  abort() {
    if (!this.isAborted) {
      this.isAborted = true
      this.abortController.abort()
    }
  }

  get signal() {
    return this.abortController.signal
  }
}

/**
 * 聊天流式响应处理器
 */
export class ChatStreamHandler {
  constructor() {
    // 在构造时就创建 AbortController，这样可以立即获取 signal
    const abortController = new AbortController()
    this.controller = new StreamController(abortController)
    this.eventListeners = new Map()
  }

  /**
   * 开始处理流式响应
   * @param {Response} response - Fetch 响应对象
   * @param {Object} options - 选项
   * @returns {StreamController} 流控制器
   */
  start(response, options = {}) {
    const { onChunk, onError, onComplete } = options

    // 处理流
    this._processStream(response, {
      onChunk: (data) => {
        this._emit('chunk', data)
        if (onChunk) onChunk(data)
      },
      onError: (error) => {
        this._emit('error', error)
        if (onError) onError(error)
      },
      onComplete: () => {
        this._emit('complete')
        if (onComplete) onComplete()
      }
    }).catch(error => {
      errorHandler.log(error, { context: 'ChatStreamHandler.start' })
      this._emit('error', error)
      if (onError) onError(error)
    })

    return this.controller
  }

  /**
   * 处理流式响应
   * @private
   */
  async _processStream(response, callbacks) {
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    try {
      while (true) {
        // 检查是否已取消
        if (this.controller?.isAborted) {
          throw new Error('Stream aborted by user')
        }

        const { done, value } = await reader.read()

        if (done) {
          // 处理缓冲区中剩余的数据
          if (buffer.trim()) {
            this._parseData(buffer, callbacks.onChunk)
          }
          callbacks.onComplete()
          break
        }

        // 解码数据
        buffer += decoder.decode(value, { stream: true })

        // 按行分割
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留不完整的行

        for (const line of lines) {
          this._parseLine(line, callbacks)
        }
      }
    } catch (error) {
      callbacks.onError(error)
    } finally {
      reader.releaseLock()
    }
  }

  /**
   * 解析 SSE 行
   * @private
   */
  _parseLine(line, callbacks) {
    const trimmedLine = line.trim()

    // 跳过空行和注释
    if (!trimmedLine || trimmedLine.startsWith(':')) {
      return
    }

    // 解析 data: 行（兼容有空格和无空格的情况）
    if (trimmedLine.startsWith('data:')) {
      // 移除 'data:' 前缀，保留可能存在的空格
      const content = trimmedLine.slice(5).replace(/^\s?/, '')
      this._parseData(content, callbacks.onChunk)
    }
  }

  /**
   * 解析 data 内容
   * @private
   */
  _parseData(data, onChunk) {
    const trimmedData = data.trim()

    // 检查是否为结束标记
    if (trimmedData === '[DONE]') {
      return
    }

    // 触发数据事件
    try {
      // 尝试解析 JSON
      const parsed = JSON.parse(trimmedData)
      onChunk(parsed)
    } catch {
      // 如果不是 JSON，直接传递原始文本
      onChunk(trimmedData)
    }
  }

  /**
   * 取消流
   */
  abort() {
    if (this.controller) {
      this.controller.abort()
    }
  }

  /**
   * 添加事件监听器
   * @param {string} event - 事件名称 ('chunk', 'error', 'complete')
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, [])
    }
    this.eventListeners.get(event).push(callback)
  }

  /**
   * 移除事件监听器
   * @param {string} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  off(event, callback) {
    if (!this.eventListeners.has(event)) {
      return
    }

    const listeners = this.eventListeners.get(event)
    const index = listeners.indexOf(callback)

    if (index > -1) {
      listeners.splice(index, 1)
    }
  }

  /**
   * 触发事件
   * @private
   */
  _emit(event, data) {
    if (!this.eventListeners.has(event)) {
      return
    }

    const listeners = this.eventListeners.get(event)
    listeners.forEach(callback => {
      try {
        callback(data)
      } catch (error) {
        errorHandler.log(error, { context: `ChatStreamHandler._emit(${event})` })
      }
    })
  }

  /**
   * 清理资源
   */
  dispose() {
    this.abort()
    this.eventListeners.clear()
    this.controller = null
  }
}
