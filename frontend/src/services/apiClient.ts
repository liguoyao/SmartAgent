import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import type { ApiResponse } from '@/types'

// Create axios instance
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// Response interceptor
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code !== 200 && res.code !== 0) {
      ElMessage.error(res.message || 'Request failed')
      if (res.code === 401) {
        const authStore = useAuthStore()
        authStore.logout()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return response
  },
  (error: AxiosError<ApiResponse>) => {
    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.message || 'Network error'
      
      switch (status) {
        case 400:
          ElMessage.error(message || 'Invalid request')
          break
        case 401:
          ElMessage.error('Authentication expired, please login again')
          const authStore = useAuthStore()
          authStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('Access denied')
          break
        case 404:
          ElMessage.error('Resource not found')
          break
        case 500:
          ElMessage.error('Server error')
          break
        default:
          ElMessage.error(message || 'Request failed')
      }
    } else if (error.request) {
      ElMessage.error('Network connection failed')
    } else {
      ElMessage.error('Request setup failed')
    }
    return Promise.reject(error)
  }
)

export default apiClient
