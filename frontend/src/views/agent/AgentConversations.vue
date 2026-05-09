<template>
  <div class="agent-conversations">
    <div class="page-header">
      <h2>Conversations</h2>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="Search..." prefix-icon="Search" style="width: 200px" />
        <el-select v-model="statusFilter" placeholder="Status" clearable>
          <el-option label="Waiting" value="waiting" />
          <el-option label="Active" value="active" />
          <el-option label="Resolved" value="resolved" />
          <el-option label="Closed" value="closed" />
        </el-select>
      </div>
    </div>
    <el-table :data="filteredConversations" v-loading="loading">
      <el-table-column prop="customerName" label="Customer" />
      <el-table-column prop="status" label="Status">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="Priority">
        <template #default="{ row }">
          <el-tag :type="getPriorityType(row.priority)">{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastMessage" label="Last Message" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="Updated">
        <template #default="{ row }">
          {{ formatTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="viewConversation(row)">View</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useConversationStore } from '@/stores/conversation'
import dayjs from 'dayjs'
import type { Conversation } from '@/types'

const conversationStore = useConversationStore()
const searchKeyword = ref('')
const statusFilter = ref('')
const loading = computed(() => conversationStore.loading)

const filteredConversations = computed(() => {
  let list = conversationStore.conversations
  if (statusFilter.value) {
    list = list.filter(c => c.status === statusFilter.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(c => c.customerName.toLowerCase().includes(keyword))
  }
  return list
})

function getStatusType(status: string) {
  const map: Record<string, string> = { waiting: 'warning', active: 'success', resolved: 'info', closed: 'info' }
  return map[status] || ''
}

function getPriorityType(priority: string) {
  const map: Record<string, string> = { low: 'info', normal: '', high: 'warning', urgent: 'danger' }
  return map[priority] || ''
}

function formatTime(timestamp: number) {
  return dayjs(timestamp).format('MM-DD HH:mm')
}

function viewConversation(conv: Conversation) {
  conversationStore.setActiveConversation(conv)
}

onMounted(() => {
  conversationStore.fetchConversations()
})
</script>

<style scoped lang="scss">
.agent-conversations {
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
