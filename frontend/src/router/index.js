import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const LoginPage = () => import('../pages/LoginPage.vue')
const RegisterPage = () => import('../pages/RegisterPage.vue')
const IndexPage = () => import('../pages/profile/index.vue')
const ConfigPage = () => import('../pages/config/index.vue')
const BasicInfoPage = () => import('../pages/config/BasicInfoPage.vue')
const ScanConfigPage = () => import('../pages/config/ScanConfigPage.vue')
const PushConfigPage = () => import('../pages/config/PushConfigPage.vue')
const ModelProviderConfig = () => import('../pages/config/ModelProviderConfig.vue')
const DefaultModelConfig = () => import('../pages/config/DefaultModelConfig.vue')
const TagPage = () => import('../pages/TagPage.vue')
const DataPage = () => import('../pages/DataPage.vue')
const SyncHistoryPage = () => import('../pages/SyncHistoryPage.vue')
const FileDetailPage = () => import('../pages/FileDetailPage.vue')
const ReportPage = () => import('../pages/ReportPage.vue')
const SessionTracePage = () => import('../pages/SessionTracePage.vue')
const AboutUsPage = () => import('../pages/config/AboutUs.vue')
const NotificationSettingsPage = () => import('../pages/config/NotificationSettings.vue')

const isFileProtocol = typeof window !== 'undefined' && window.location && window.location.protocol === 'file:'
const history = isFileProtocol ? createWebHashHistory(import.meta.env.BASE_URL) : createWebHistory(import.meta.env.BASE_URL)
const CollectionListPage = () => import('../pages/collection/CollectionListPage.vue')
const CollectionDetailPage = () => import('../pages/collection/CollectionDetailPage.vue') 
const QuizPage = () => import('../pages/collection/QuizPage.vue')
const LearningPage = () => import('../pages/quiz/index.vue')
const QuizDetailPage = () => import('../pages/quiz/QuizDetailPage.vue')
const SimpleKnowledgeGraphPage = () => import('../pages/knowledge-graph/SimpleKnowledgeGraphPage.vue')
const DashboardPage = () => import('../pages/dashboard/index.vue')

const router = createRouter({
  history,
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: LoginPage },
    { path: '/register', component: RegisterPage },
    { path: '/profile', component: IndexPage },
    {
      path: '/config',
      component: ConfigPage,
      redirect: '/config/basic',
      children: [
        { path: 'basic', component: BasicInfoPage },
        { path: 'scan', component: ScanConfigPage },
        { path: 'push', component: PushConfigPage },
        { path: 'notification', component: NotificationSettingsPage },
        { path: 'model-provider', component: ModelProviderConfig },
        { path: 'default-model', component: DefaultModelConfig },
        { path: 'about', component: AboutUsPage }
      ]
    },
    { path: '/tags', component: TagPage },
    { path: '/data', component: DataPage },
    { path: '/sync', component: SyncHistoryPage },
    { path: '/analysis/:id', component: FileDetailPage },
    { path: '/report', component: ReportPage },
    { path: '/trace/:fileId', component: SessionTracePage },
    { path: '/collections', component: CollectionListPage },
    { path: '/collections/:id', component: CollectionDetailPage },
    { path: '/collections/:id/quiz', component: QuizPage },
    { path: '/quiz-history', component: LearningPage },
    { path: '/quiz-history/:quizId', component: QuizDetailPage },
    { path: '/mistake-book', component: LearningPage },
    { path: '/knowledge-graph', component: SimpleKnowledgeGraphPage },
    { path: '/dashboard', component: DashboardPage },
  ],
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path === '/login' || to.path === '/register' || to.path === '/glow-demo') return next()
  if (!auth.isAuthenticated) return next('/login')
  next()
})

export default router
