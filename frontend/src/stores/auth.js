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
      const res = await fetch('/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      if (!res.ok) throw new Error(await res.text())
      const data = await res.json()
      if (data.code && data.code !== 0) throw new Error(data.message || '登录错误')
      this.isAuthenticated = true
      this.username = data.data?.username || ''
      this.userId = data.data?.id || null
      localStorage.setItem('auth', JSON.stringify({ isAuthenticated: this.isAuthenticated, username: this.username, userId: this.userId }))
    },
    async register(username, password) {
      const res = await fetch('/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      if (!res.ok) throw new Error(await res.text())
      const data = await res.json()
      if (data.code && data.code !== 0) throw new Error(data.message || '注册错误')
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
