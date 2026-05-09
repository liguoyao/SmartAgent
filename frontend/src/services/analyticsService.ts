import apiClient from './apiClient'
import type { ApiResponse, AnalyticsOverview, AnalyticsTimeSeries, AgentPerformance, MessageMetrics } from '@/types'

// Analytics API
export const analyticsApi = {
  // Overview stats
  getOverview(params?: {
    startTime?: number
    endTime?: number
  }): Promise<ApiResponse<AnalyticsOverview>> {
    return apiClient.get('/analytics/overview', { params })
  },

  // Time series data
  getTimeSeries(params: {
    startTime: number
    endTime: number
    interval: 'hour' | 'day' | 'week' | 'month'
  }): Promise<ApiResponse<AnalyticsTimeSeries[]>> {
    return apiClient.get('/analytics/timeseries', { params })
  },

  // Agent performance
  getAgentPerformance(params?: {
    startTime?: number
    endTime?: number
    agentId?: string
  }): Promise<ApiResponse<AgentPerformance[]>> {
    return apiClient.get('/analytics/agent-performance', { params })
  },

  // Message metrics
  getMessageMetrics(params?: {
    startTime?: number
    endTime?: number
  }): Promise<ApiResponse<MessageMetrics>> {
    return apiClient.get('/analytics/messages', { params })
  },

  // Conversation trends
  getConversationTrends(params: {
    startTime: number
    endTime: number
    groupBy: 'hour' | 'day' | 'week' | 'month'
  }): Promise<ApiResponse<{ timestamp: number; count: number; resolved: number }[]>> {
    return apiClient.get('/analytics/conversation-trends', { params })
  },

  // Customer satisfaction
  getSatisfactionTrend(params: {
    startTime: number
    endTime: number
  }): Promise<ApiResponse<{ timestamp: number; score: number; count: number }[]>> {
    return apiClient.get('/analytics/satisfaction-trend', { params })
  },

  // Response time analytics
  getResponseTimeAnalytics(params?: {
    startTime?: number
    endTime?: number
  }): Promise<ApiResponse<{
    avgFirstResponseTime: number
    avgResponseTime: number
    p50ResponseTime: number
    p90ResponseTime: number
    p99ResponseTime: number
  }>> {
    return apiClient.get('/analytics/response-time', { params })
  },

  // Tag analytics
  getTagAnalytics(params?: {
    startTime?: number
    endTime?: number
    limit?: number
  }): Promise<ApiResponse<{ tag: string; count: number; percentage: number }[]>> {
    return apiClient.get('/analytics/tags', { params })
  },

  // Export report
  exportReport(params: {
    type: 'daily' | 'weekly' | 'monthly'
    format: 'csv' | 'excel' | 'pdf'
    startTime: number
    endTime: number
  }): Promise<Blob> {
    return apiClient.get('/analytics/export', { params, responseType: 'blob' })
  },
}
