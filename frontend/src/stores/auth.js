import { defineStore } from 'pinia'
import { api } from '../api/http'
import router from '../router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthenticated: false,
    username: '',
    userId: null,
  }),
  actions: {
    async login(username, password) {
      const data = await api.login(username, password)
      if (data && data.code && data.code !== 0) throw new Error(data.message || '登录错误')

      // 登录成功，从 localStorage 更新状态（api.login 已经保存了 token 和 auth）
      this.hydrate()

      return data
    },
    async register(username, password) {
      const data = await api.register(username, password)
      if (data && data.code && data.code !== 0) throw new Error(data.message || '注册错误')
      return data
    },
    logout() {
      this.isAuthenticated = false
      this.username = ''
      this.userId = null
      // localStorage 清空已在 api.logout() 中完成
      router.push('/login')
    },
    routerPushAfterLogin() {
      router.push('/dashboard')
    },
    routerPushLogin() {
      router.push('/login')
    },
    hydrate() {
      // 从 auth 获取用户信息（api.login 已保存）
      const raw = localStorage.getItem('auth')
      if (raw) {
        const obj = JSON.parse(raw)
        this.isAuthenticated = obj.isAuthenticated !== undefined ? obj.isAuthenticated : !!obj.userId
        this.username = obj.username || ''
        this.userId = obj.userId ?? null
      } else {
        this.isAuthenticated = false
        this.username = ''
        this.userId = null
      }
    },
  },
})

