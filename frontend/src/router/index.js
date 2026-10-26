import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guestOnly: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { guestOnly: true, title: '注册' },
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/profile',
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { requiresAuth: true, title: '个人中心' },
      },
      {
        path: 'appointments',
        name: 'appointments',
        component: () => import('@/views/AppointmentsView.vue'),
        meta: { requiresAuth: true, title: '我的预约' },
      },
      {
        path: 'departments',
        name: 'departments',
        component: () => import('@/views/DepartmentsView.vue'),
        meta: { requiresAuth: true, title: '科室导航' },
      },
      {
        path: 'doctors',
        name: 'doctors',
        component: () => import('@/views/DoctorsView.vue'),
        meta: { requiresAuth: true, title: '医生团队' },
      },
      {
        path: 'schedules',
        name: 'schedules',
        component: () => import('@/views/SchedulesView.vue'),
        meta: { requiresAuth: true, title: '预约挂号' },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard',
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '管理概览' },
      },
      {
        path: 'departments',
        name: 'admin-departments',
        component: () => import('@/views/admin/AdminDepartmentsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '科室管理' },
      },
      {
        path: 'doctors',
        name: 'admin-doctors',
        component: () => import('@/views/admin/AdminDoctorsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '医生管理' },
      },
      {
        path: 'schedules',
        name: 'admin-schedules',
        component: () => import('@/views/admin/AdminSchedulesView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '排班管理' },
      },
      {
        path: 'appointments',
        name: 'admin-appointments',
        component: () => import('@/views/admin/AdminAppointmentsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '预约管理' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth) {
    if (!auth.state.token) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    const valid = await auth.ensureSession()
    if (!valid) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAdmin && !auth.isAdmin.value) {
      return { name: 'profile' }
    }
  }

  if (to.meta.guestOnly && auth.state.token) {
    const valid = await auth.ensureSession()
    if (valid) {
      return auth.isAdmin.value ? { name: 'admin-dashboard' } : { name: 'profile' }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} | 安和医院预约挂号`
    : '安和医院预约挂号'
})

export default router
