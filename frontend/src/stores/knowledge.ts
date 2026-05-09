import { defineStore } from 'pinia'
import { ref } from 'vue'
import { knowledgeApi } from '@/services/knowledgeService'
import type { KnowledgeArticle, KnowledgeCategory } from '@/types'

export const useKnowledgeStore = defineStore('knowledge', () => {
  // State
  const articles = ref<KnowledgeArticle[]>([])
  const categories = ref<KnowledgeCategory[]>([])
  const currentArticle = ref<KnowledgeArticle | null>(null)
  const loading = ref(false)
  const totalCount = ref(0)
  const page = ref(1)
  const pageSize = ref(20)

  // Search state
  const searchResults = ref<KnowledgeArticle[]>([])
  const searchKeyword = ref('')

  // Fetch articles
  async function fetchArticles(params?: {
    category?: string
    status?: string
    keyword?: string
    tags?: string[]
  }): Promise<void> {
    loading.value = true
    try {
      const response = await knowledgeApi.getArticles({
        page: page.value,
        pageSize: pageSize.value,
        ...params,
      })
      articles.value = response.data.data.list
      totalCount.value = response.data.data.total
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch single article
  async function fetchArticle(id: string): Promise<void> {
    try {
      const response = await knowledgeApi.getArticleById(id)
      currentArticle.value = response.data.data
    } catch (error) {
      throw error
    }
  }

  // Create article
  async function createArticle(data: {
    title: string
    content: string
    category: string
    tags?: string[]
    status?: string
  }): Promise<KnowledgeArticle> {
    const response = await knowledgeApi.createArticle(data)
    return response.data.data
  }

  // Update article
  async function updateArticle(id: string, data: Partial<KnowledgeArticle>): Promise<void> {
    const response = await knowledgeApi.updateArticle(id, data)
    const index = articles.value.findIndex(a => a.id === id)
    if (index !== -1) {
      articles.value[index] = response.data.data
    }
    if (currentArticle.value?.id === id) {
      currentArticle.value = response.data.data
    }
  }

  // Delete article
  async function deleteArticle(id: string): Promise<void> {
    await knowledgeApi.deleteArticle(id)
    articles.value = articles.value.filter(a => a.id !== id)
    if (currentArticle.value?.id === id) {
      currentArticle.value = null
    }
  }

  // Publish article
  async function publishArticle(id: string): Promise<void> {
    await knowledgeApi.publishArticle(id)
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.status = 'published'
    }
  }

  // Archive article
  async function archiveArticle(id: string): Promise<void> {
    await knowledgeApi.archiveArticle(id)
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.status = 'archived'
    }
  }

  // Fetch categories
  async function fetchCategories(): Promise<void> {
    try {
      const response = await knowledgeApi.getCategories()
      categories.value = response.data.data
    } catch (error) {
      throw error
    }
  }

  // Create category
  async function createCategory(data: {
    name: string
    parentId?: string
    description?: string
    sortOrder?: number
  }): Promise<KnowledgeCategory> {
    const response = await knowledgeApi.createCategory(data)
    return response.data.data
  }

  // Update category
  async function updateCategory(id: string, data: Partial<KnowledgeCategory>): Promise<void> {
    const response = await knowledgeApi.updateCategory(id, data)
    const index = categories.value.findIndex(c => c.id === id)
    if (index !== -1) {
      categories.value[index] = response.data.data
    }
  }

  // Delete category
  async function deleteCategory(id: string): Promise<void> {
    await knowledgeApi.deleteCategory(id)
    categories.value = categories.value.filter(c => c.id !== id)
  }

  // Search articles
  async function search(keyword: string, params?: {
    category?: string
    tags?: string[]
    limit?: number
  }): Promise<void> {
    loading.value = true
    searchKeyword.value = keyword
    try {
      const response = await knowledgeApi.search(keyword, params)
      searchResults.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Clear search
  function clearSearch(): void {
    searchResults.value = []
    searchKeyword.value = ''
  }

  // Clear store
  function clearStore(): void {
    articles.value = []
    currentArticle.value = null
    categories.value = []
    loading.value = false
    totalCount.value = 0
    searchResults.value = []
    searchKeyword.value = ''
  }

  return {
    // State
    articles,
    categories,
    currentArticle,
    loading,
    totalCount,
    page,
    pageSize,
    searchResults,
    searchKeyword,

    // Actions
    fetchArticles,
    fetchArticle,
    createArticle,
    updateArticle,
    deleteArticle,
    publishArticle,
    archiveArticle,
    fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
    search,
    clearSearch,
    clearStore,
  }
})
