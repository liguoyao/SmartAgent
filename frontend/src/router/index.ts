import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/admin',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: 'Login' },
  },
  // Customer routes
  {
    path: '/customer',
    name: 'Customer',
    component: () => import('@/views/customer/CustomerLayout.vue'),
    meta: { requiresAuth: false, title: 'Customer Support' },
    children: [
      {
        path: '',
        name: 'CustomerHome',
        component: () => import('@/views/customer/CustomerHome.vue'),
      },
    ],
  },
  // Agent routes
  {
    path: '/agent',
    name: 'Agent',
    component: () => import('@/views/agent/AgentLayout.vue'),
    meta: { requiresAuth: true, role: 'agent', title: 'Agent Console' },
    children: [
      {
        path: '',
        name: 'AgentConsole',
        component: () => import('@/views/agent/AgentConsole.vue'),
        meta: { title: 'Live Console' },
      },
      {
        path: 'conversations',
        name: 'AgentConversations',
        component: () => import('@/views/agent/AgentConversations.vue'),
        meta: { title: 'Conversations' },
      },
      {
        path: 'history',
        name: 'AgentHistory',
        component: () => import('@/views/agent/AgentHistory.vue'),
        meta: { title: 'Conversation History' },
      },
    ],
  },
  // Admin routes
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin', title: 'Admin Dashboard' },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: 'Dashboard' },
      },
      {
        path: 'knowledge',
        name: 'KnowledgeBase',
        component: () => import('@/views/admin/KnowledgeBase.vue'),
        meta: { title: 'Knowledge Base' },
      },
      {
        path: 'knowledge/:id/edit',
        name: 'KnowledgeEditor',
        component: () => import('@/views/admin/KnowledgeEditor.vue'),
        meta: { title: 'Edit Article' },
      },
      {
        path: 'agents',
        name: 'AgentBuilder',
        component: () => import('@/views/admin/AgentBuilder.vue'),
        meta: { title: 'Agent Configuration' },
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/admin/Analytics.vue'),
        meta: { title: 'Analytics' },
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: 'Settings' },
      },
    ],
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: 'Not Found' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard
router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  
  // Update page title
  document.title = `${to.meta.title || 'SmartAgent'} - SmartAgent`

  // Check authentication
  if (to.meta.requiresAuth !== false) {
    if (!authStore.userInfo) {
      // Try to restore session
      if (authStore.token) {
        try {
          await authStore.fetchUserInfo()
        } catch {
          authStore.clearSession()
          return next({ name: 'Login', query: { redirect: to.fullPath } })
        }
      } else {
        return next({ name: 'Login', query: { redirect: to.fullPath } })
      }
    }

    // Check role
    if (to.meta.role) {
      const allowedRoles = Array.isArray(to.meta.role) ? to.meta.role : [to.meta.role]
      if (!allowedRoles.includes(authStore.userInfo?.role || '')) {
        return next({ name: 'NotFound' })
      }
    }
  }

  next()
})

export default router
