<template>
  <div class="analytics-dashboard">
    <!-- Header -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>Analytics Dashboard</h1>
        <p class="subtitle">Monitor performance and conversation metrics</p>
      </div>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="to"
          start-placeholder="Start date"
          end-placeholder="End date"
          @change="handleDateChange"
        />
        <el-select v-model="interval" @change="handleIntervalChange">
          <el-option label="Hourly" value="hour" />
          <el-option label="Daily" value="day" />
          <el-option label="Weekly" value="week" />
          <el-option label="Monthly" value="month" />
        </el-select>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button type="primary" @click="exportReport">
          <el-icon><Download /></el-icon>
          Export
        </el-button>
      </div>
    </div>

    <!-- Overview Cards -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon conversations">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(overview?.totalConversations || 0) }}</div>
            <div class="stat-label">Total Conversations</div>
            <div class="stat-change positive">
              <el-icon><TrendCharts /></el-icon>
              +12% vs last period
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon active">
            <el-icon><Message /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview?.activeConversations || 0 }}</div>
            <div class="stat-label">Active Now</div>
            <div class="stat-trend">
              <span class="trend-dot"></span>
              Live
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon resolved">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(overview?.resolvedToday || 0) }}</div>
            <div class="stat-label">Resolved Today</div>
            <div class="stat-change positive">
              <el-icon><TrendCharts /></el-icon>
              +8% vs yesterday
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon satisfaction">
            <el-icon><Star /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ (overview?.customerSatisfaction || 0).toFixed(1) }}%</div>
            <div class="stat-label">Satisfaction Rate</div>
            <div class="stat-change positive">
              <el-icon><TrendCharts /></el-icon>
              +2% vs last period
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 1 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>Conversation Trends</span>
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="7d">7 Days</el-radio-button>
                <el-radio-button label="30d">30 Days</el-radio-button>
                <el-radio-button label="90d">90 Days</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container" ref="conversationChartRef"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>Response Time Distribution</span>
          </template>
          <div class="chart-container" ref="responseTimeChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 2 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>Conversation Sources</span>
          </template>
          <div class="chart-container" ref="sourcesChartRef"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>Message Volume</span>
          </template>
          <div class="chart-container" ref="messageVolumeChartRef"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>Satisfaction Trend</span>
          </template>
          <div class="chart-container" ref="satisfactionChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Agent Performance Table -->
    <el-card class="performance-card">
      <template #header>
        <div class="chart-header">
          <span>Agent Performance</span>
          <el-input
            v-model="agentSearch"
            placeholder="Search agents"
            prefix-icon="Search"
            style="width: 200px"
          />
        </div>
      </template>

      <el-table :data="filteredAgentPerformance" v-loading="loading">
        <el-table-column prop="agentName" label="Agent" min-width="150">
          <template #default="{ row }">
            <div class="agent-cell">
              <el-avatar :size="32">{{ row.agentName.charAt(0) }}</el-avatar>
              <span>{{ row.agentName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="conversationsHandled" label="Conversations" sortable>
          <template #default="{ row }">
            {{ formatNumber(row.conversationsHandled) }}
          </template>
        </el-table-column>
        <el-table-column prop="avgResponseTime" label="Avg Response" sortable>
          <template #default="{ row }">
            {{ formatDuration(row.avgResponseTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="avgResolutionTime" label="Resolution Time" sortable>
          <template #default="{ row }">
            {{ formatDuration(row.avgResolutionTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="satisfaction" label="Satisfaction" sortable>
          <template #default="{ row }">
            <el-progress
              :percentage="row.satisfaction"
              :color="getSatisfactionColor(row.satisfaction)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewAgentDetail(row.agentId)">
              View
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Top Tags -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>Top Conversation Tags</span>
          </template>
          <div class="tags-list">
            <div
              v-for="(tag, index) in tagAnalytics"
              :key="tag.tag"
              class="tag-item"
            >
              <span class="tag-rank">{{ index + 1 }}</span>
              <span class="tag-name">{{ tag.tag }}</span>
              <span class="tag-count">{{ formatNumber(tag.count) }}</span>
              <el-progress
                :percentage="tag.percentage"
                :show-text="false"
                :stroke-width="8"
                style="width: 100px"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>Response Time Analytics</span>
          </template>
          <div class="response-stats">
            <div class="response-item">
              <span class="response-label">First Response (Avg)</span>
              <span class="response-value">{{ formatDuration(responseAnalytics?.avgFirstResponseTime || 0) }}</span>
            </div>
            <div class="response-item">
              <span class="response-label">Response Time (Avg)</span>
              <span class="response-value">{{ formatDuration(responseAnalytics?.avgResponseTime || 0) }}</span>
            </div>
            <div class="response-item">
              <span class="response-label">P50 Response</span>
              <span class="response-value">{{ formatDuration(responseAnalytics?.p50ResponseTime || 0) }}</span>
            </div>
            <div class="response-item">
              <span class="response-label">P90 Response</span>
              <span class="response-value">{{ formatDuration(responseAnalytics?.p90ResponseTime || 0) }}</span>
            </div>
            <div class="response-item">
              <span class="response-label">P99 Response</span>
              <span class="response-value">{{ formatDuration(responseAnalytics?.p99ResponseTime || 0) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useAnalyticsStore } from '@/stores/analytics'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import {
  ChatDotRound,
  Message,
  CircleCheck,
  Star,
  TrendCharts,
  Refresh,
  Download,
  Search,
} from '@element-plus/icons-vue'
import type { ECharts } from 'echarts'

const analyticsStore = useAnalyticsStore()

// Refs
const loading = ref(false)
const dateRange = ref<[number, number]>([
  dayjs().subtract(7, 'day').startOf('day').valueOf(),
  dayjs().endOf('day').valueOf(),
])
const interval = ref<'hour' | 'day' | 'week' | 'month'>('day')
const trendPeriod = ref('7d')
const agentSearch = ref('')

// Chart refs
const conversationChartRef = ref<HTMLElement | null>(null)
const responseTimeChartRef = ref<HTMLElement | null>(null)
const sourcesChartRef = ref<HTMLElement | null>(null)
const messageVolumeChartRef = ref<HTMLElement | null>(null)
const satisfactionChartRef = ref<HTMLElement | null>(null)

// Chart instances
let conversationChart: ECharts | null = null
let responseTimeChart: ECharts | null = null
let sourcesChart: ECharts | null = null
let messageVolumeChart: ECharts | null = null
let satisfactionChart: ECharts | null = null

// Computed
const overview = computed(() => analyticsStore.overview)
const conversationTrends = computed(() => analyticsStore.conversationTrends)
const agentPerformance = computed(() => analyticsStore.agentPerformance)
const tagAnalytics = computed(() => analyticsStore.tagAnalytics)
const responseAnalytics = computed(() => analyticsStore.responseTimeAnalytics)

const filteredAgentPerformance = computed(() => {
  if (!agentSearch.value) return agentPerformance.value
  const keyword = agentSearch.value.toLowerCase()
  return agentPerformance.value.filter(a => 
    a.agentName.toLowerCase().includes(keyword)
  )
})

// Lifecycle
onMounted(async () => {
  await loadAnalytics()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  conversationChart?.dispose()
  responseTimeChart?.dispose()
  sourcesChart?.dispose()
  messageVolumeChart?.dispose()
  satisfactionChart?.dispose()
})

// Watchers
watch(() => analyticsStore.timeSeriesData, () => {
  updateCharts()
}, { deep: true })

watch(trendPeriod, async (val) => {
  const days = parseInt(val)
  dateRange.value = [
    dayjs().subtract(days, 'day').startOf('day').valueOf(),
    dayjs().endOf('day').valueOf(),
  ]
  analyticsStore.setDateRange(dateRange.value)
  await loadAnalytics()
})

// Methods
async function loadAnalytics() {
  loading.value = true
  try {
    await analyticsStore.fetchAllAnalytics()
  } catch (error) {
    console.error('Failed to load analytics:', error)
  } finally {
    loading.value = false
  }
}

function handleDateChange() {
  analyticsStore.setDateRange(dateRange.value)
  loadAnalytics()
}

function handleIntervalChange() {
  analyticsStore.setInterval(interval.value)
  loadAnalytics()
}

async function refreshData() {
  await loadAnalytics()
}

async function exportReport() {
  try {
    await analyticsStore.exportReport('daily', 'excel')
  } catch (error) {
    console.error('Failed to export report:', error)
  }
}

function viewAgentDetail(agentId: string) {
  // Navigate to agent detail
  console.log('View agent:', agentId)
}

// Chart initialization
function initCharts() {
  if (conversationChartRef.value) {
    conversationChart = echarts.init(conversationChartRef.value)
  }
  if (responseTimeChartRef.value) {
    responseTimeChart = echarts.init(responseTimeChartRef.value)
  }
  if (sourcesChartRef.value) {
    sourcesChart = echarts.init(sourcesChartRef.value)
  }
  if (messageVolumeChartRef.value) {
    messageVolumeChart = echarts.init(messageVolumeChartRef.value)
  }
  if (satisfactionChartRef.value) {
    satisfactionChart = echarts.init(satisfactionChartRef.value)
  }
  updateCharts()
}

function updateCharts() {
  updateConversationChart()
  updateResponseTimeChart()
  updateSourcesChart()
  updateMessageVolumeChart()
  updateSatisfactionChart()
}

function updateConversationChart() {
  if (!conversationChart) return
  
  const data = conversationTrends.value.map(item => ({
    date: dayjs(item.timestamp).format('MM-DD'),
    conversations: item.count,
    resolved: item.resolved,
  }))

  conversationChart.setOption({
    tooltip: {
      trigger: 'axis',
    },
    legend: {
      data: ['Total', 'Resolved'],
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: 'Total',
        type: 'line',
        smooth: true,
        data: data.map(d => d.conversations),
        areaStyle: {
          opacity: 0.2,
        },
        itemStyle: {
          color: '#409eff',
        },
      },
      {
        name: 'Resolved',
        type: 'line',
        smooth: true,
        data: data.map(d => d.resolved),
        areaStyle: {
          opacity: 0.2,
        },
        itemStyle: {
          color: '#67c23a',
        },
      },
    ],
  })
}

function updateResponseTimeChart() {
  if (!responseTimeChart) return
  
  responseTimeChart.setOption({
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'category',
      data: ['< 1min', '1-5min', '5-15min', '15-30min', '> 30min'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        type: 'bar',
        data: [
          { value: 120, itemStyle: { color: '#67c23a' } },
          { value: 200, itemStyle: { color: '#409eff' } },
          { value: 150, itemStyle: { color: '#e6a23c' } },
          { value: 80, itemStyle: { color: '#f56c6c' } },
          { value: 30, itemStyle: { color: '#909399' } },
        ],
        barWidth: '50%',
      },
    ],
  })
}

function updateSourcesChart() {
  if (!sourcesChart) return
  
  sourcesChart.setOption({
    tooltip: {
      trigger: 'item',
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: [
          { value: 450, name: 'Web', itemStyle: { color: '#409eff' } },
          { value: 280, name: 'Mobile', itemStyle: { color: '#67c23a' } },
          { value: 120, name: 'API', itemStyle: { color: '#e6a23c' } },
        ],
      },
    ],
  })
}

function updateMessageVolumeChart() {
  if (!messageVolumeChart) return
  
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const data = Array.from({ length: 24 }, () => Math.floor(Math.random() * 100))
  
  messageVolumeChart.setOption({
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLabel: {
        interval: 3,
        rotate: 0,
      },
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        type: 'bar',
        data: data,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' },
          ]),
        },
      },
    ],
  })
}

function updateSatisfactionChart() {
  if (!satisfactionChart) return
  
  const data = Array.from({ length: 7 }, (_, i) => ({
    date: dayjs().subtract(6 - i, 'day').format('MM-DD'),
    score: 85 + Math.random() * 10,
  }))

  satisfactionChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.axisValue}<br/>Satisfaction: ${p.value.toFixed(1)}%`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
    },
    yAxis: {
      type: 'value',
      min: 80,
      max: 100,
    },
    series: [
      {
        type: 'line',
        smooth: true,
        data: data.map(d => d.score),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0)' },
          ]),
        },
        itemStyle: {
          color: '#67c23a',
        },
        lineStyle: {
          width: 3,
        },
      },
    ],
  })
}

function handleResize() {
  conversationChart?.resize()
  responseTimeChart?.resize()
  sourcesChart?.resize()
  messageVolumeChart?.resize()
  satisfactionChart?.resize()
}

// Helpers
function formatNumber(num: number): string {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${Math.round(seconds)}s`
  }
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return `${minutes}m ${Math.round(seconds % 60)}s`
  }
  const hours = Math.floor(minutes / 60)
  return `${hours}h ${minutes % 60}m`
}

function getSatisfactionColor(score: number): string {
  if (score >= 90) return '#67c23a'
  if (score >= 70) return '#409eff'
  if (score >= 50) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped lang="scss">
.analytics-dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;

  .header-left {
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
      font-weight: 600;
      color: #303133;
    }

    .subtitle {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

.overview-cards {
  margin-bottom: 20px;

  .el-col {
    margin-bottom: 20px;
  }
}

.stat-card {
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    margin-bottom: 16px;

    &.conversations { background: linear-gradient(135deg, #409eff, #79bbff); }
    &.active { background: linear-gradient(135deg, #67c23a, #95d475); }
    &.resolved { background: linear-gradient(135deg, #e6a23c, #f3d19e); }
    &.satisfaction { background: linear-gradient(135deg, #f56c6c, #fab6b6); }
  }

  .stat-content {
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      line-height: 1;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }

    .stat-change {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      margin-top: 8px;

      &.positive {
        color: #67c23a;
      }

      &.negative {
        color: #f56c6c;
      }
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      margin-top: 8px;
      color: #67c23a;

      .trend-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #67c23a;
        animation: pulse 2s infinite;
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 100%;

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .chart-container {
    height: 300px;
  }
}

.performance-card {
  margin-bottom: 20px;

  .agent-cell {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.tags-list {
  .tag-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .tag-rank {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: #f0f0f0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      color: #606266;
    }

    .tag-name {
      flex: 1;
      font-weight: 500;
      color: #303133;
    }

    .tag-count {
      font-weight: 600;
      color: #606266;
      min-width: 60px;
      text-align: right;
    }
  }
}

.response-stats {
  .response-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .response-label {
      color: #606266;
      font-size: 14px;
    }

    .response-value {
      font-weight: 600;
      color: #303133;
      font-size: 16px;
    }
  }
}
</style>
