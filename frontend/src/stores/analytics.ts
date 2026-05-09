import { defineStore } from 'pinia'
import { ref } from 'vue'
import { analyticsApi } from '@/services/analyticsService'
import type { AnalyticsOverview, AnalyticsTimeSeries, AgentPerformance, MessageMetrics } from '@/types'
import dayjs from 'dayjs'

export const useAnalyticsStore = defineStore('analytics', () => {
  // State
  const overview = ref<AnalyticsOverview | null>(null)
  const timeSeriesData = ref<AnalyticsTimeSeries[]>([])
  const agentPerformance = ref<AgentPerformance[]>([])
  const messageMetrics = ref<MessageMetrics | null>(null)
  const conversationTrends = ref<{ timestamp: number; count: number; resolved: number }[]>([])
  const satisfactionTrend = ref<{ timestamp: number; score: number; count: number }[]>([])
  const responseTimeAnalytics = ref<{
    avgFirstResponseTime: number
    avgResponseTime: number
    p50ResponseTime: number
    p90ResponseTime: number
    p99ResponseTime: number
  } | null>(null)
  const tagAnalytics = ref<{ tag: string; count: number; percentage: number }[]>([])
  
  const loading = ref(false)
  const dateRange = ref<[number, number]>([
    dayjs().subtract(7, 'day').startOf('day').valueOf(),
    dayjs().endOf('day').valueOf(),
  ])
  const interval = ref<'hour' | 'day' | 'week' | 'month'>('day')

  // Fetch overview
  async function fetchOverview(): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getOverview({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
      })
      overview.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch time series
  async function fetchTimeSeries(): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getTimeSeries({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
        interval: interval.value,
      })
      timeSeriesData.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch agent performance
  async function fetchAgentPerformance(agentId?: string): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getAgentPerformance({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
        agentId,
      })
      agentPerformance.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch message metrics
  async function fetchMessageMetrics(): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getMessageMetrics({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
      })
      messageMetrics.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch conversation trends
  async function fetchConversationTrends(): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getConversationTrends({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
        groupBy: interval.value,
      })
      conversationTrends.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch satisfaction trend
  async function fetchSatisfactionTrend(): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getSatisfactionTrend({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
      })
      satisfactionTrend.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch response time analytics
  async function fetchResponseTimeAnalytics(): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getResponseTimeAnalytics({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
      })
      responseTimeAnalytics.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch tag analytics
  async function fetchTagAnalytics(limit = 10): Promise<void> {
    loading.value = true
    try {
      const response = await analyticsApi.getTagAnalytics({
        startTime: dateRange.value[0],
        endTime: dateRange.value[1],
        limit,
      })
      tagAnalytics.value = response.data.data
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch all analytics data
  async function fetchAllAnalytics(agentId?: string): Promise<void> {
    loading.value = true
    try {
      await Promise.all([
        fetchOverview(),
        fetchTimeSeries(),
        fetchAgentPerformance(agentId),
        fetchMessageMetrics(),
        fetchConversationTrends(),
        fetchSatisfactionTrend(),
        fetchResponseTimeAnalytics(),
        fetchTagAnalytics(),
      ])
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Set date range
  function setDateRange(range: [number, number]): void {
    dateRange.value = range
  }

  // Set interval
  function setInterval(newInterval: 'hour' | 'day' | 'week' | 'month'): void {
    interval.value = newInterval
  }

  // Export report
  async function exportReport(type: 'daily' | 'weekly' | 'monthly', format: 'csv' | 'excel' | 'pdf'): Promise<void> {
    const blob = await analyticsApi.exportReport({
      type,
      format,
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
    })
    
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `report-${type}.${format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  // Clear store
  function clearStore(): void {
    overview.value = null
    timeSeriesData.value = []
    agentPerformance.value = []
    messageMetrics.value = null
    conversationTrends.value = []
    satisfactionTrend.value = []
    responseTimeAnalytics.value = null
    tagAnalytics.value = []
    loading.value = false
  }

  return {
    // State
    overview,
    timeSeriesData,
    agentPerformance,
    messageMetrics,
    conversationTrends,
    satisfactionTrend,
    responseTimeAnalytics,
    tagAnalytics,
    loading,
    dateRange,
    interval,

    // Actions
    fetchOverview,
    fetchTimeSeries,
    fetchAgentPerformance,
    fetchMessageMetrics,
    fetchConversationTrends,
    fetchSatisfactionTrend,
    fetchResponseTimeAnalytics,
    fetchTagAnalytics,
    fetchAllAnalytics,
    setDateRange,
    setInterval,
    exportReport,
    clearStore,
  }
})
