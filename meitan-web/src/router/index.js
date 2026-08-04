import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/', redirect: '/home' },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页', icon: 'HomeFilled' } },
      { path: 'introduction', name: 'Introduction', component: () => import('@/views/Introduction.vue'), meta: { title: '瓦斯介绍', icon: 'InfoFilled' } },
      { path: 'analysis', name: 'Analysis', component: () => import('@/views/Analysis.vue'), meta: { title: '瓦斯吸附含量计算与分析', icon: 'TrendCharts' } },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/Statistics.vue'), meta: { title: '煤样瓦斯吸附参数统计分析', icon: 'DataAnalysis' } },
      { path: 'detection', name: 'Detection', component: () => import('@/views/Detection.vue'), meta: { title: '煤层瓦斯突出危险性检测', icon: 'WarningFilled' } },
      { path: 'files', name: 'Files', component: () => import('@/views/Files.vue'), meta: { title: '数据文件管理', icon: 'FolderOpened' } },
      { path: 'reports', name: 'Reports', component: () => import('@/views/Reports.vue'), meta: { title: '瓦斯数据导出与报告', icon: 'Document' } },
      { path: 'feedback', name: 'Feedback', component: () => import('@/views/Feedback.vue'), meta: { title: '用户反馈', icon: 'ChatLineSquare' } },
      { path: 'user-center', name: 'UserCenter', component: () => import('@/views/UserCenter.vue'), meta: { title: '个人中心', icon: 'UserFilled' } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('@/views/AdminUsers.vue'), meta: { title: '用户管理', icon: 'UserFilled', requiresAdmin: true } },
      { path: 'admin/feedback', name: 'AdminFeedback', component: () => import('@/views/AdminFeedback.vue'), meta: { title: '用户反馈处理', icon: 'ChatDotRound', requiresAdmin: true } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 煤层瓦斯智能分析平台` : '煤层瓦斯智能分析平台'
  const userStore = useUserStore()
  const authRequired = !['/login', '/'].includes(to.path)
  if (authRequired && !userStore.token) next('/login')
  else if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') next('/home')
  else next()
})

export default router
