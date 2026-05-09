<template>
  <div class="agent-builder-view">
    <div class="page-header">
      <div class="header-left">
        <h2>Agent Configuration</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createAgent">
          <el-icon><Plus /></el-icon>
          Create Agent
        </el-button>
      </div>
    </div>

    <el-table :data="agents" v-loading="loading">
      <el-table-column prop="name" label="Name" min-width="150" />
      <el-table-column prop="description" label="Description" show-overflow-tooltip />
      <el-table-column prop="model" label="Model" width="150" />
      <el-table-column prop="capabilities" label="Capabilities" width="150">
        <template #default="{ row }">
          {{ row.capabilities?.length || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="isActive" label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? 'Active' : 'Inactive' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="Updated" width="150">
        <template #default="{ row }">
          {{ formatDate(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="editAgent(row)">Edit</el-button>
          <el-button size="small" type="danger" @click="deleteAgent(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import type { AgentConfig } from '@/types'

const router = useRouter()
const loading = ref(false)

const agents = ref<AgentConfig[]>([])

function formatDate(timestamp?: number) {
  return timestamp ? dayjs(timestamp).format('YYYY-MM-DD') : '-'
}

function createAgent() {
  router.push('/admin/agents/new/edit')
}

function editAgent(agent: AgentConfig) {
  router.push(`/admin/agents/${agent.id}/edit`)
}

async function deleteAgent(agent: AgentConfig) {
  try {
    await ElMessageBox.confirm('Are you sure you want to delete this agent?', 'Confirm Delete', { type: 'warning' })
    agents.value = agents.value.filter(a => a.id !== agent.id)
    ElMessage.success('Agent deleted')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete agent')
    }
  }
}

onMounted(() => {
  // Load agents
  agents.value = [
    {
      id: '1',
      name: 'Customer Support Bot',
      description: 'General customer support agent',
      model: 'gpt-4',
      temperature: 0.7,
      maxTokens: 2000,
      systemPrompt: 'You are a helpful assistant...',
      capabilities: ['chat', 'faq'],
      knowledgeBaseIds: ['1'],
      routingRules: [],
      isActive: true,
      createdAt: Date.now() - 86400000,
      updatedAt: Date.now() - 3600000,
    },
  ]
})
</script>

<style scoped lang="scss">
.agent-builder-view {
  padding: 24px;
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    h2 { margin: 0; }
  }
}
</style>
