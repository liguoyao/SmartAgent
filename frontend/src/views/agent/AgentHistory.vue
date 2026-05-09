<template>
  <div class="agent-history">
    <div class="page-header">
      <h2>Conversation History</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="to"
          start-placeholder="Start date"
          end-placeholder="End date"
        />
        <el-input v-model="searchKeyword" placeholder="Search..." prefix-icon="Search" style="width: 200px" />
      </div>
    </div>

    <el-table :data="filteredHistory" v-loading="loading">
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="customerName" label="Customer" />
      <el-table-column prop="status" label="Status">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="messageCount" label="Messages" />
      <el-table-column prop="duration" label="Duration" />
      <el-table-column prop="resolvedAt" label="Resolved At">
        <template #default="{ row }">
          {{ formatDateTime(row.resolvedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetails(row)">Details</el-button>
          <el-button size="small" @click="exportChat(row)">Export</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const searchKeyword = ref('')
const dateRange = ref<[number, number] | null>(null)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(100)

// Mock data
const history = ref([
  { id: 'C001', customerName: 'John Doe', status: 'resolved', messageCount: 24, duration: '15m 30s', resolvedAt: Date.now() - 3600000 },
  { id: 'C002', customerName: 'Jane Smith', status: 'resolved', messageCount: 18, duration: '8m 45s', resolvedAt: Date.now() - 7200000 },
  { id: 'C003', customerName: 'Bob Wilson', status: 'closed', messageCount: 32, duration: '22m 10s', resolvedAt: Date.now() - 10800000 },
])

const filteredHistory = computed(() => {
  let list = history.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(h => h.customerName.toLowerCase().includes(keyword))
  }
  return list
})

function getStatusType(status: string) {
  const map: Record<string, string> = { resolved: 'success', closed: 'info' }
  return map[status] || ''
}

function formatDateTime(timestamp: number) {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm')
}

function viewDetails(row: any) {
  ElMessage.info('View details: ' + row.id)
}

function exportChat(row: any) {
  ElMessage.success('Exporting chat: ' + row.id)
}
</script>

<style scoped lang="scss">
.agent-history {
  padding: 24px;
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    h2 { margin: 0; }
    .header-actions { display: flex; gap: 12px; }
  }
}
</style>
