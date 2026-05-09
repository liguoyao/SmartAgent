import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/services/apiClient'
import type { Agent } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<Agent | null>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')
  const isAgent = computed(() => userInfo.value?.role === 'agent' || userInfo.value?.role === 'supervisor' || userInfo.value?.role === 'admin')

  async function login(username: string, password: string): Promise<void> {
    try {
      const response = await apiClient.post('/auth/login', { username, password })
      const { token: newToken, user } = response.data.data
      
      token.value = newToken
      userInfo.value = user
      localStorage.setItem('token', newToken)
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
      token.value = null
      userInfo.value = null
      permissions.value = []
      localStorage.removeItem('token')
    }
  }

  async function fetchUserInfo(): Promise<void> {
    try {
      const response = await apiClient.get('/auth/current')
      userInfo.value = response.data.data
    } catch (error) {
      throw error
    }
  }

  async function refreshToken(): Promise<void> {
    try {
      const response = await apiClient.post('/auth/refresh')
      token.value = response.data.data
      localStorage.setItem('token', response.data.data)
    } catch (error) {
      logout()
      throw error
    }
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
    fetchUserInfo,
    refreshToken,
    hasPermission,
    hasAnyPermission,
  }
})
