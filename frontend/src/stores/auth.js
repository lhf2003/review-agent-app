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
      
      const user = data.data || data // Handle potential direct data return or wrapped response
      
      this.isAuthenticated = true
      this.username = user?.username || ''
      this.userId = user?.id || null
      localStorage.setItem('auth', JSON.stringify({ isAuthenticated: this.isAuthenticated, username: this.username, userId: this.userId }))
    },
    async register(username, password) {
      const data = await api.register(username, password)
      if (data && data.code && data.code !== 0) throw new Error(data.message || '注册错误')
    },
    logout() {
      this.isAuthenticated = false
      this.username = ''
      this.userId = null
      localStorage.removeItem('auth')
      router.push('/login')
    },
    routerPushAfterLogin() {
      router.push('/data')
    },
    routerPushLogin() {
      router.push('/login')
    },
    hydrate() {
      const raw = localStorage.getItem('auth')
      if (raw) {
        const obj = JSON.parse(raw)
        this.isAuthenticated = !!obj.isAuthenticated
        this.username = obj.username || ''
        this.userId = obj.userId ?? null
      }
    }
  },
})
