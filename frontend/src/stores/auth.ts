import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/services/apiClient'
import type { Agent } from '@/types'

interface AuthUser {
  id?: number | string
  userId?: number | string
  username: string
  nickname?: string
  email?: string
  role: string
}

function normalizeUser(user: AuthUser): Agent {
  const role = user.role.toLowerCase()
  if (!['admin', 'agent', 'supervisor'].includes(role)) {
    throw new Error('Unsupported user role')
  }
  return {
    id: String(user.id ?? user.userId),
    name: user.nickname || user.username,
    email: user.email || '',
    role: role as Agent['role'],
    status: 'online',
    maxConcurrentChats: 0,
    currentChats: 0,
    skills: [],
    createdAt: 0,
  }
}

export const useAuthStore = defineStore('auth', () => {
  const savedToken = localStorage.getItem('token')
  const token = ref<string | null>(savedToken && !['undefined', 'null'].includes(savedToken) ? savedToken : null)
  const refreshTokenValue = ref<string | null>(localStorage.getItem('refreshToken'))
  const userInfo = ref<Agent | null>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')
  const isAgent = computed(() => userInfo.value?.role === 'agent' || userInfo.value?.role === 'supervisor' || userInfo.value?.role === 'admin')

  async function login(username: string, password: string): Promise<void> {
    try {
      const response = await apiClient.post('/auth/login', { username, password })
      const { accessToken: newToken, refreshToken, user } = response.data.data
      if (!newToken || !user) throw new Error('Invalid login response')
      const normalizedUser = normalizeUser(user)
      
      token.value = newToken
      userInfo.value = normalizedUser
      refreshTokenValue.value = refreshToken || null
      localStorage.setItem('token', newToken)
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
      else localStorage.removeItem('refreshToken')
    } catch (error) {
      throw error
    }
  }

  async function logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout')
    } catch (error) {
      // Ignore logout errors
    } finally {
      clearSession()
    }
  }

  async function fetchUserInfo(): Promise<void> {
    try {
      const response = await apiClient.get('/auth/validate')
      userInfo.value = normalizeUser(response.data.data)
    } catch (error) {
      throw error
    }
  }

  async function refreshToken(): Promise<void> {
    try {
      if (!refreshTokenValue.value) throw new Error('Missing refresh token')
      const response = await apiClient.post('/auth/refresh', { refreshToken: refreshTokenValue.value })
      const data = response.data.data
      if (!data.accessToken || !data.refreshToken || !data.user) throw new Error('Invalid refresh response')
      const normalizedUser = normalizeUser(data.user)
      token.value = data.accessToken
      refreshTokenValue.value = data.refreshToken
      userInfo.value = normalizedUser
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
    } catch (error) {
      clearSession()
      throw error
    }
  }

  function clearSession(): void {
    token.value = null
    refreshTokenValue.value = null
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  function hasAnyPermission(requiredPermissions: string[]): boolean {
    return requiredPermissions.some(p => permissions.value.includes(p))
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    isAdmin,
    isAgent,
    login,
    logout,
    clearSession,
    fetchUserInfo,
    refreshToken,
    hasPermission,
    hasAnyPermission,
  }
})
