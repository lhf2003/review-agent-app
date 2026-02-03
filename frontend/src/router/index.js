import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const LoginPage = () => import('../pages/LoginPage.vue')
const RegisterPage = () => import('../pages/RegisterPage.vue')
const IndexPage = () => import('../pages/profile/index.vue')
const ConfigPage = () => import('../pages/ConfigPage.vue')
const ModelProviderConfig = () => import('../pages/ModelProviderConfig.vue')
const DefaultModelConfig = () => import('../pages/DefaultModelConfig.vue')
const TagPage = () => import('../pages/TagPage.vue')
const DataPage = () => import('../pages/DataPage.vue')
const AnalysisResultPage = () => import('../pages/AnalysisResult.vue')
const SyncHistoryPage = () => import('../pages/SyncHistoryPage.vue')
const FileDetailPage = () => import('../pages/FileDetailPage.vue')
const WordCloudPage = () => import('../pages/WordCloudPage.vue')
const ReportPage = () => import('../pages/ReportPage.vue')
const SessionTracePage = () => import('../pages/SessionTracePage.vue')
const AboutUsPage = () => import('../pages/AboutUs.vue')

const isFileProtocol = typeof window !== 'undefined' && window.location && window.location.protocol === 'file:'
const history = isFileProtocol ? createWebHashHistory(import.meta.env.BASE_URL) : createWebHistory(import.meta.env.BASE_URL)
const CollectionListPage = () => import('../pages/CollectionListPage.vue')
const CollectionDetailPage = () => import('../pages/CollectionDetailPage.vue')
const QuizPage = () => import('../pages/QuizPage.vue')
const LearningPage = () => import('../pages/quiz/index.vue')
const QuizDetailPage = () => import('../pages/quiz/QuizDetailPage.vue')
const MistakeBookPage = () => import('../pages/MistakeBookPage.vue')

const router = createRouter({
  history,
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginPage },
    { path: '/register', component: RegisterPage },
    { path: '/profile', component: IndexPage },
    {
      path: '/config',
      component: ConfigPage,
      children: [
        { path: 'model-provider', component: ModelProviderConfig },
        { path: 'default-model', component: DefaultModelConfig },
        { path: 'about', component: AboutUsPage }
      ]
    },
    { path: '/tags', component: TagPage },
    { path: '/data', component: DataPage },
    { path: '/analysis', component: AnalysisResultPage },
    { path: '/sync', component: SyncHistoryPage },
    { path: '/analysis/:id', component: FileDetailPage },
    { path: '/word-cloud', component: WordCloudPage },
    { path: '/report', component: ReportPage },
    { path: '/trace/:fileId', component: SessionTracePage },
    { path: '/collections', component: CollectionListPage },
    { path: '/collections/:id', component: CollectionDetailPage },
    { path: '/collections/:id/quiz', component: QuizPage },
    { path: '/quiz-history', component: LearningPage },
    { path: '/quiz-history/:quizId', component: QuizDetailPage },
    { path: '/mistake-book', component: MistakeBookPage },
  ],
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path === '/login' || to.path === '/register' || to.path === '/glow-demo') return next()
  if (!auth.isAuthenticated) return next('/login')
  next()
})

export default router
