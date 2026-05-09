import apiClient from './apiClient'
import type { ApiResponse, PageResult, Conversation, Message, Agent } from '@/types'

// Conversation API
export const conversationApi = {
  // Get conversation list
  getList(params: {
    page?: number
    pageSize?: number
    status?: string
    agentId?: string
    startTime?: number
    endTime?: number
    keyword?: string
  }): Promise<ApiResponse<PageResult<Conversation>>> {
    return apiClient.get('/conversations', { params })
  },

  // Get conversation detail
  getById(id: string): Promise<ApiResponse<Conversation>> {
    return apiClient.get(`/conversations/${id}`)
  },

  // Create new conversation
  create(data: {
    customerId: string
    customerName: string
    source?: string
    metadata?: Record<string, any>
  }): Promise<ApiResponse<Conversation>> {
    return apiClient.post('/conversations', data)
  },

  // Update conversation status
  updateStatus(id: string, status: string): Promise<ApiResponse<void>> {
    return apiClient.patch(`/conversations/${id}/status`, { status })
  },

  // Assign agent to conversation
  assignAgent(id: string, agentId: string): Promise<ApiResponse<void>> {
    return apiClient.post(`/conversations/${id}/assign`, { agentId })
  },

  // Close conversation
  close(id: string): Promise<ApiResponse<void>> {
    return apiClient.post(`/conversations/${id}/close`)
  },

  // Get conversation messages
  getMessages(conversationId: string, params?: {
    page?: number
    pageSize?: number
  }): Promise<ApiResponse<PageResult<Message>>> {
    return apiClient.get(`/conversations/${conversationId}/messages`, { params })
  },

  // Send message
  sendMessage(conversationId: string, content: string, attachments?: File[]): Promise<ApiResponse<Message>> {
    const formData = new FormData()
    formData.append('content', content)
    if (attachments) {
      attachments.forEach(file => formData.append('attachments', file))
    }
    return apiClient.post(`/conversations/${conversationId}/messages`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  // Get waiting conversations
  getWaiting(params?: { page?: number; pageSize?: number }): Promise<ApiResponse<PageResult<Conversation>>> {
    return apiClient.get('/conversations/waiting', { params })
  },

  // Get agent's active conversations
  getAgentActive(agentId: string): Promise<ApiResponse<Conversation[]>> {
    return apiClient.get(`/agents/${agentId}/active-conversations`)
  },
}

// Agent API
export const agentApi = {
  // Get agent list
  getList(params?: {
    page?: number
    pageSize?: number
    status?: string
    role?: string
  }): Promise<ApiResponse<PageResult<Agent>>> {
    return apiClient.get('/agents', { params })
  },

  // Get agent detail
  getById(id: string): Promise<ApiResponse<Agent>> {
    return apiClient.get(`/agents/${id}`)
  },

  // Update agent status
  updateStatus(id: string, status: string): Promise<ApiResponse<void>> {
    return apiClient.patch(`/agents/${id}/status`, { status })
  },

  // Get online agents
  getOnline(): Promise<ApiResponse<Agent[]>> {
    return apiClient.get('/agents/online')
  },

  // Update agent settings
  updateSettings(id: string, settings: Partial<Agent>): Promise<ApiResponse<void>> {
    return apiClient.patch(`/agents/${id}/settings`, settings)
  },
}
