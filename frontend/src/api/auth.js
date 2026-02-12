/**
 * 认证相关 API
 * 包含登录、注册、登出、用户配置等功能
 */
import { request, encryptPassword, getToken } from './base'

export const authApi = {
  /**
   * 用户登录
   */
  async login(username, password) {
    const encryptedPassword = await encryptPassword(password)
    const response = await request('/user/login', { method: 'POST', body: { username, password: encryptedPassword } })

    if (response && response.token) {
      localStorage.setItem('token', response.token)
      const auth = {
        userId: response.userInfo?.id,
        username: response.userInfo?.username
      }
      localStorage.setItem('auth', JSON.stringify(auth))
    }

    return response
  },

  /**
   * 用户注册
   */
  async register(username, password) {
    const encryptedPassword = await encryptPassword(password)
    return request('/user/register', { method: 'POST', body: { username, password: encryptedPassword } })
  },

  /**
   * 退出登录
   */
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('auth')
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
  },

  // ========== 用户配置 ==========

  /**
   * 获取用户配置
   */
  getConfig() {
    return request('/user/config/get')
  },

  /**
   * 更新用户配置
   */
  updateConfig(body) {
    return request('/user/config/update', { method: 'POST', body })
  },

  /**
   * 获取用户模型配置
   */
  getUserModelConfig() {
    return request('/user/config/model/get')
  },

  /**
   * 更新用户模型配置
   */
  updateUserModelConfig(userId, list) {
    return request('/user/config/model/update', { method: 'POST', body: list })
  },

  // ========== 用户信息 ==========

  /**
   * 获取用户信息
   */
  getUserInfo(id) {
    return request('/user/info', { params: { id } })
  },

  /**
   * 更新用户信息
   */
  updateUserInfo(body) {
    return request('/user/info/update', { method: 'POST', body })
  },

  /**
   * 更新用户密码
   */
  async updateUserPassword(oldPassword, newPassword) {
    const encOld = await encryptPassword(oldPassword)
    const encNew = await encryptPassword(newPassword)
    return request('/user/info/update/password', { method: 'POST', body: { oldPassword: encOld, newPassword: encNew } })
  },

  /**
   * 获取用户统计数据
   */
  getUserStats() {
    return request('/user/stats')
  }
}

export default authApi
