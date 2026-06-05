import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/user/ForgotPassword.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePassword.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'homework',
        name: 'HomeworkList',
        component: () => import('@/views/homework/HomeworkList.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER', 'STUDENT'] }
      },
      {
        path: 'homework/create',
        name: 'CreateHomework',
        component: () => import('@/views/homework/CreateHomework.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'] }
      },
      {
        path: 'homework/:id',
        name: 'HomeworkDetail',
        component: () => import('@/views/homework/HomeworkDetail.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER', 'STUDENT'] }
      },
      {
        path: 'homework/:id/edit',
        name: 'EditHomework',
        component: () => import('@/views/homework/EditHomework.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'] }
      },
      {
        path: 'submission',
        name: 'SubmitHomework',
        component: () => import('@/views/submission/SubmitHomework.vue'),
        meta: { requiresAuth: true, roles: ['STUDENT'] }
      },
      {
        path: 'submission/:homeworkId',
        name: 'SubmitHomeworkWithId',
        component: () => import('@/views/submission/SubmitHomework.vue'),
        meta: { requiresAuth: true, roles: ['STUDENT'] }
      },
      {
        path: 'submission-detail/:submissionId',
        name: 'SubmissionDetail',
        component: () => import('@/views/submission/SubmitHomework.vue'),
        meta: { requiresAuth: true, roles: ['STUDENT'] }
      },

      {
        path: 'grade',
        name: 'GradeList',
        component: () => import('@/views/grade/GradeList.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'] }
      },
      {
        path: 'grade/:submissionId',
        name: 'GradeSubmission',
        component: () => import('@/views/grade/GradeSubmission.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'] }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/stats/Statistics.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'] }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/Message.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/user/UserManagement.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'profile-requests',
        name: 'ProfileRequests',
        component: () => import('@/views/user/ProfileRequests.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  
  const userStore = useUserStore()
  const token = userStore.token
  const userRole = userStore.role
  
  console.log('路由守卫检查:', {
    to: to.path,
    requiresAuth: to.meta.requiresAuth,
    hasToken: !!token,
    roles: to.meta.roles,
    userRole: userRole
  })
  
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    NProgress.done()
  } else if (to.meta.requiresAuth && to.meta.roles && !to.meta.roles.includes(userRole)) {
    console.error('角色不匹配:', { required: to.meta.roles, current: userRole })
    ElMessage.error('您没有权限访问该页面')
    next({ name: 'Dashboard' })
    NProgress.done()
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
