import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'elders', component: () => import('../views/Elders.vue'), meta: { title: '老人档案', roles: ['ADMIN', 'STAFF'] } },
      { path: 'orders', component: () => import('../views/Orders.vue'), meta: { title: '服务单' } },
      { path: 'orders/create', component: () => import('../views/OrderCreate.vue'), meta: { title: '新建预约', roles: ['ADMIN', 'STAFF'] } },
      { path: 'orders/:id', component: () => import('../views/OrderDetail.vue'), meta: { title: '服务单详情' } },
      { path: 'barber', component: () => import('../views/BarberWorkbench.vue'), meta: { title: '理发师工作台', roles: ['BARBER'] } },
      { path: 'volunteer', component: () => import('../views/VolunteerWorkbench.vue'), meta: { title: '志愿者工作台', roles: ['VOLUNTEER'] } },
      { path: 'family', component: () => import('../views/FamilyHome.vue'), meta: { title: '家属服务', roles: ['FAMILY'] } },
      { path: 'exceptions', component: () => import('../views/Exceptions.vue'), meta: { title: '异常处理中心' } },
      { path: 'tool-disinfection', component: () => import('../views/ToolDisinfection.vue'), meta: { title: '工具消毒与备用包', roles: ['ADMIN', 'STAFF', 'BARBER'] } },
      { path: 'infection-trace', component: () => import('../views/InfectionTrace.vue'), meta: { title: '交叉感染追溯', roles: ['ADMIN', 'STAFF', 'FINANCE', 'VOLUNTEER', 'GRID'] } },
      { path: 'finance', component: () => import('../views/Finance.vue'), meta: { title: '补贴财务', roles: ['FINANCE', 'ADMIN', 'STAFF'] } },
      { path: 'follow-ups', component: () => import('../views/FollowUps.vue'), meta: { title: '回访管理', roles: ['ADMIN', 'STAFF', 'GRID', 'VOLUNTEER'] } },
      { path: 'care-tasks', component: () => import('../views/CareTasks.vue'), meta: { title: '关怀任务', roles: ['ADMIN', 'STAFF', 'GRID'] } },
      { path: 'welfare', component: () => import('../views/Welfare.vue'), meta: { title: '公益服务', roles: ['ADMIN', 'STAFF'] } },
      { path: 'stats', component: () => import('../views/Stats.vue'), meta: { title: '复盘统计', roles: ['ADMIN', 'STAFF', 'FINANCE', 'GRID'] } },
      { path: 'users', component: () => import('../views/Users.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'notifications', component: () => import('../views/Notifications.vue'), meta: { title: '消息通知' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/'
  }
  if (to.meta?.roles) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (!user || !to.meta.roles.includes(user.role)) {
      return '/dashboard'
    }
  }
  return true
})

export default router
