import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const LoginPage = () => import('../pages/LoginPage.vue')
const RegisterPage = () => import('../pages/RegisterPage.vue')
const ConfigPage = () => import('../pages/ConfigPage.vue')
const TagPage = () => import('../pages/TagPage.vue')
const DataPage = () => import('../pages/DataPage.vue')
const AnalysisResultPage = () => import('../pages/AnalysisResult.vue')
const SyncHistoryPage = () => import('../pages/SyncHistoryPage.vue')
const FileDetailPage = () => import('../pages/FileDetailPage.vue')
const WordCloudPage = () => import('../pages/WordCloudPage.vue')
const ReportPage = () => import('../pages/ReportPage.vue')

const isFileProtocol = typeof window !== 'undefined' && window.location && window.location.protocol === 'file:'
const history = isFileProtocol ? createWebHashHistory(import.meta.env.BASE_URL) : createWebHistory(import.meta.env.BASE_URL)

const router = createRouter({
  history,
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginPage },
    { path: '/register', component: RegisterPage },
    { path: '/config', component: ConfigPage },
    { path: '/tags', component: TagPage },
    { path: '/data', component: DataPage },
    { path: '/analysis', component: AnalysisResultPage },
    { path: '/sync', component: SyncHistoryPage },
    { path: '/analysis/:id', component: FileDetailPage },
    { path: '/word-cloud', component: WordCloudPage },
    { path: '/report', component: ReportPage }
  ],
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path === '/login' || to.path === '/register' || to.path === '/glow-demo') return next()
  if (!auth.isAuthenticated) return next('/login')
  next()
})

export default router
