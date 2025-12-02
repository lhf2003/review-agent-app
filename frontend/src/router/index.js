import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const LoginPage = () => import('../pages/LoginPage.vue')
const RegisterPage = () => import('../pages/RegisterPage.vue')
const ConfigPage = () => import('../pages/ConfigPage.vue')
const TagPage = () => import('../pages/TagPage.vue')
const DataPage = () => import('../pages/DataPage.vue')
const FilePage = () => import('../pages/FilePage.vue')
const SyncHistoryPage = () => import('../pages/SyncHistoryPage.vue')
const FileDetailPage = () => import('../pages/FileDetailPage.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginPage },
    { path: '/register', component: RegisterPage },
    { path: '/config', component: ConfigPage },
    { path: '/tags', component: TagPage },
    { path: '/data', component: DataPage },
    { path: '/analysis', component: FilePage },
    { path: '/sync', component: SyncHistoryPage },
    { path: '/analysis/:id', component: FileDetailPage },
  ],
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path === '/login' || to.path === '/register') return next()
  if (!auth.isAuthenticated) return next('/login')
  next()
})

export default router
