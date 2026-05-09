import apiClient from './apiClient'
import type { ApiResponse, PageResult, KnowledgeArticle, KnowledgeCategory } from '@/types'

// Knowledge Base API
export const knowledgeApi = {
  // Article CRUD
  getArticles(params?: {
    page?: number
    pageSize?: number
    category?: string
    status?: string
    keyword?: string
    tags?: string[]
  }): Promise<ApiResponse<PageResult<KnowledgeArticle>>> {
    return apiClient.get('/knowledge/articles', { params })
  },

  getArticleById(id: string): Promise<ApiResponse<KnowledgeArticle>> {
    return apiClient.get(`/knowledge/articles/${id}`)
  },

  createArticle(data: {
    title: string
    content: string
    category: string
    tags?: string[]
    status?: string
  }): Promise<ApiResponse<KnowledgeArticle>> {
    return apiClient.post('/knowledge/articles', data)
  },

  updateArticle(id: string, data: Partial<KnowledgeArticle>): Promise<ApiResponse<KnowledgeArticle>> {
    return apiClient.put(`/knowledge/articles/${id}`, data)
  },

  deleteArticle(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete(`/knowledge/articles/${id}`)
  },

  publishArticle(id: string): Promise<ApiResponse<void>> {
    return apiClient.post(`/knowledge/articles/${id}/publish`)
  },

  archiveArticle(id: string): Promise<ApiResponse<void>> {
    return apiClient.post(`/knowledge/articles/${id}/archive`)
  },

  // Categories
  getCategories(): Promise<ApiResponse<KnowledgeCategory[]>> {
    return apiClient.get('/knowledge/categories')
  },

  createCategory(data: {
    name: string
    parentId?: string
    description?: string
    sortOrder?: number
  }): Promise<ApiResponse<KnowledgeCategory>> {
    return apiClient.post('/knowledge/categories', data)
  },

  updateCategory(id: string, data: Partial<KnowledgeCategory>): Promise<ApiResponse<KnowledgeCategory>> {
    return apiClient.put(`/knowledge/categories/${id}`, data)
  },

  deleteCategory(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete(`/knowledge/categories/${id}`)
  },

  // Search
  search(keyword: string, params?: {
    category?: string
    tags?: string[]
    limit?: number
  }): Promise<ApiResponse<KnowledgeArticle[]>> {
    return apiClient.get('/knowledge/search', { params: { keyword, ...params } })
  },

  // Feedback
  markHelpful(id: string, isHelpful: boolean): Promise<ApiResponse<void>> {
    return apiClient.post(`/knowledge/articles/${id}/feedback`, { isHelpful })
  },
}
