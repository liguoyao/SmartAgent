<template>
  <div class="agent-builder">
    <div class="builder-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>{{ isEdit ? 'Edit Agent' : 'Create New Agent' }}</h2>
      </div>
      <div class="header-actions">
        <el-button @click="saveDraft" :loading="saving">Save Draft</el-button>
        <el-button type="primary" @click="activate" :loading="activating">
          {{ form.isActive ? 'Deactivate' : 'Activate' }}
        </el-button>
      </div>
    </div>

    <div class="builder-content">
      <el-row :gutter="20">
        <el-col :span="16">
          <!-- Basic Info -->
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <span>Basic Information</span>
                <el-tag :type="form.isActive ? 'success' : 'info'">
                  {{ form.isActive ? 'Active' : 'Inactive' }}
                </el-tag>
              </div>
            </template>

            <el-form :model="form" label-width="120px">
              <el-form-item label="Agent Name">
                <el-input v-model="form.name" placeholder="Enter agent name" />
              </el-form-item>

              <el-form-item label="Description">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="3"
                  placeholder="Describe what this agent does"
                />
              </el-form-item>

              <el-form-item label="Model">
                <el-select v-model="form.model" placeholder="Select model">
                  <el-option label="GPT-4" value="gpt-4" />
                  <el-option label="GPT-4 Turbo" value="gpt-4-turbo" />
                  <el-option label="GPT-3.5 Turbo" value="gpt-3.5-turbo" />
                  <el-option label="Claude 3 Opus" value="claude-3-opus" />
                  <el-option label="Claude 3 Sonnet" value="claude-3-sonnet" />
                  <el-option label="Claude 3 Haiku" value="claude-3-haiku" />
                </el-select>
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="Temperature">
                    <el-slider
                      v-model="form.temperature"
                      :min="0"
                      :max="2"
                      :step="0.1"
                      :format-tooltip="(val: number) => val.toFixed(1)"
                    />
                    <span class="param-hint">Controls randomness (0 = focused, 2 = creative)</span>
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item label="Max Tokens">
                    <el-input-number
                      v-model="form.maxTokens"
                      :min="100"
                      :max="32000"
                      :step="100"
                    />
                    <span class="param-hint">Maximum response length</span>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>

          <!-- System Prompt -->
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <span>System Prompt</span>
                <el-button text size="small" @click="showPromptTemplates = true">
                  Use Template
                </el-button>
              </div>
            </template>

            <el-input
              v-model="form.systemPrompt"
              type="textarea"
              :rows="8"
              placeholder="Define the agent's persona, behavior, and response guidelines..."
            />
            <div class="prompt-tips">
              <el-icon><InfoFilled /></el-icon>
              <span>Be specific about tone, expertise, and response format</span>
            </div>
          </el-card>

          <!-- Capabilities -->
          <el-card class="config-card">
            <template #header>
              <span>Capabilities</span>
            </template>

            <div class="capabilities-grid">
              <el-checkbox
                v-for="cap in availableCapabilities"
                :key="cap.id"
                v-model="cap.enabled"
                :label="cap.id"
                :disabled="cap.required"
              >
                <div class="capability-item">
                  <div class="cap-icon">
                    <el-icon><component :is="cap.icon" /></el-icon>
                  </div>
                  <div class="cap-info">
                    <div class="cap-name">{{ cap.name }}</div>
                    <div class="cap-desc">{{ cap.description }}</div>
                  </div>
                </div>
              </el-checkbox>
            </div>
          </el-card>

          <!-- Knowledge Bases -->
          <el-card class="config-card">
            <template #header>
              <span>Knowledge Base Integration</span>
            </template>

            <el-transfer
              v-model="form.knowledgeBaseIds"
              :data="knowledgeBaseList"
              :titles="['Available', 'Connected']"
              placeholder="Select knowledge bases"
            />

            <div class="kb-config" v-if="form.knowledgeBaseIds.length > 0">
              <h4>Retrieval Settings</h4>
              <el-form label-width="140px">
                <el-form-item label="Max Results">
                  <el-input-number
                    v-model="form.retrievalConfig.maxResults"
                    :min="1"
                    :max="20"
                  />
                </el-form-item>
                <el-form-item label="Similarity Threshold">
                  <el-slider
                    v-model="form.retrievalConfig.similarityThreshold"
                    :min="0"
                    :max="1"
                    :step="0.05"
                  />
                </el-form-item>
              </el-form>
            </div>
          </el-card>

          <!-- Routing Rules -->
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <span>Routing Rules</span>
                <el-button type="primary" size="small" @click="addRoutingRule">
                  <el-icon><Plus /></el-icon>
                  Add Rule
                </el-button>
              </div>
            </template>

            <div v-if="form.routingRules.length === 0" class="empty-rules">
              <el-icon size="48"><Guide /></el-icon>
              <p>No routing rules configured</p>
              <el-button @click="addRoutingRule">Add First Rule</el-button>
            </div>

            <div v-else class="routing-rules">
              <el-collapse>
                <el-collapse-item
                  v-for="(rule, index) in form.routingRules"
                  :key="index"
                  :name="index"
                >
                  <template #title>
                    <div class="rule-header">
                      <span class="rule-name">{{ rule.name || `Rule ${index + 1}` }}</span>
                      <el-tag size="small" :type="getPriorityType(rule.priority)">
                        Priority {{ rule.priority }}
                      </el-tag>
                      <el-button
                        text
                        size="small"
                        @click.stop="removeRoutingRule(index)"
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </template>

                  <el-form label-width="100px" size="small">
                    <el-form-item label="Rule Name">
                      <el-input v-model="rule.name" placeholder="Enter rule name" />
                    </el-form-item>

                    <el-form-item label="Condition Type">
                      <el-select v-model="rule.condition.type">
                        <el-option label="Keyword" value="keyword" />
                        <el-option label="Intent" value="intent" />
                        <el-option label="Time-based" value="time" />
                        <el-option label="Customer Type" value="customer_type" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="Operator">
                      <el-select v-model="rule.condition.operator">
                        <el-option label="Equals" value="equals" />
                        <el-option label="Contains" value="contains" />
                        <el-option label="Starts with" value="starts_with" />
                        <el-option label="Ends with" value="ends_with" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="Value">
                      <el-input v-model="rule.condition.value" placeholder="Enter value" />
                    </el-form-item>

                    <el-form-item label="Action">
                      <el-select v-model="rule.action.type">
                        <el-option label="Assign to Agent" value="assign_agent" />
                        <el-option label="Assign to Team" value="assign_team" />
                        <el-option label="Boost Priority" value="priority_boost" />
                        <el-option label="Auto Reply" value="auto_reply" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="Action Value">
                      <el-input v-model="rule.action.value" placeholder="Agent ID or message" />
                    </el-form-item>

                    <el-form-item label="Priority">
                      <el-input-number v-model="rule.priority" :min="1" :max="100" />
                    </el-form-item>
                  </el-form>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <!-- Preview Card -->
          <el-card class="preview-card">
            <template #header>
              <span>Agent Preview</span>
            </template>

            <div class="preview-content">
              <div class="preview-avatar">
                <el-icon v-if="!form.avatar" size="48"><User /></el-icon>
                <img v-else :src="form.avatar" alt="Agent Avatar" />
              </div>
              <div class="preview-name">{{ form.name || 'Agent Name' }}</div>
              <div class="preview-desc">{{ form.description || 'Agent description' }}</div>

              <el-divider />

              <div class="preview-stats">
                <div class="stat-item">
                  <span class="stat-label">Model</span>
                  <span class="stat-value">{{ form.model || '-' }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Temperature</span>
                  <span class="stat-value">{{ form.temperature }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Max Tokens</span>
                  <span class="stat-value">{{ form.maxTokens }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Capabilities</span>
                  <span class="stat-value">{{ enabledCapabilitiesCount }}/{{ availableCapabilities.length }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Knowledge Bases</span>
                  <span class="stat-value">{{ form.knowledgeBaseIds.length }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Routing Rules</span>
                  <span class="stat-value">{{ form.routingRules.length }}</span>
                </div>
              </div>
            </div>
          </el-card>

          <!-- Test Agent -->
          <el-card class="test-card">
            <template #header>
              <span>Test Agent</span>
            </template>

            <div class="test-messages" ref="testMessagesContainer">
              <div
                v-for="msg in testMessages"
                :key="msg.id"
                class="test-message"
                :class="[`msg-${msg.role}`]"
              >
                <div class="msg-content">{{ msg.content }}</div>
              </div>
              <div v-if="testLoading" class="test-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                Agent is typing...
              </div>
            </div>

            <div class="test-input">
              <el-input
                v-model="testInput"
                placeholder="Type a test message..."
                @keydown.enter="sendTestMessage"
              >
                <template #append>
                  <el-button @click="sendTestMessage" :loading="testLoading">
                    <el-icon><Promotion /></el-icon>
                  </el-button>
                </template>
              </el-input>
            </div>
          </el-card>

          <!-- Configuration History -->
          <el-card class="history-card">
            <template #header>
              <span>Recent Changes</span>
            </template>

            <div class="history-list">
              <div
                v-for="item in configHistory"
                :key="item.id"
                class="history-item"
              >
                <div class="history-action">
                  <el-tag size="small" :type="getActionType(item.action)">
                    {{ item.action }}
                  </el-tag>
                  {{ item.field }}
                </div>
                <div class="history-time">{{ formatTime(item.timestamp) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Prompt Templates Dialog -->
    <el-dialog v-model="showPromptTemplates" title="Prompt Templates" width="600px">
      <div class="template-list">
        <div
          v-for="template in promptTemplates"
          :key="template.id"
          class="template-item"
          @click="useTemplate(template)"
        >
          <div class="template-name">{{ template.name }}</div>
          <div class="template-desc">{{ template.description }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Plus,
  Delete,
  User,
  InfoFilled,
  Guide,
  Loading,
  Promotion,
  ChatLineSquare,
  Document,
  QuestionFilled,
  Setting,
  Connection,
  DataAnalysis,
} from '@element-plus/icons-vue'
import type { AgentConfig, RoutingRule } from '@/types'

const router = useRouter()

// Refs
const saving = ref(false)
const activating = ref(false)
const showPromptTemplates = ref(false)
const testInput = ref('')
const testLoading = ref(false)
const testMessages = ref<{ id: string; role: string; content: string }[]>([])
const testMessagesContainer = ref<HTMLElement | null>(null)

const isEdit = computed(() => !!router.currentRoute.value.params.id)

// Form
const form = reactive<{
  name: string
  description: string
  model: string
  temperature: number
  maxTokens: number
  systemPrompt: string
  capabilities: string[]
  knowledgeBaseIds: string[]
  isActive: boolean
  avatar?: string
  retrievalConfig: {
    maxResults: number
    similarityThreshold: number
  }
  routingRules: RoutingRule[]
}>({
  name: '',
  description: '',
  model: 'gpt-4',
  temperature: 0.7,
  maxTokens: 2000,
  systemPrompt: '',
  capabilities: [],
  knowledgeBaseIds: [],
  isActive: false,
  retrievalConfig: {
    maxResults: 5,
    similarityThreshold: 0.7,
  },
  routingRules: [],
})

// Available capabilities
const availableCapabilities = ref([
  { id: 'chat', name: 'Chat', description: 'Handle text conversations', icon: 'ChatLineSquare', enabled: true, required: true },
  { id: 'knowledge', name: 'Knowledge Search', description: 'Search knowledge base', icon: 'Document', enabled: false },
  { id: 'faq', name: 'FAQ Responses', description: 'Answer common questions', icon: 'QuestionFilled', enabled: false },
  { id: 'ticket', name: 'Ticket Creation', description: 'Create support tickets', icon: 'Setting', enabled: false },
  { id: 'handoff', name: 'Human Handoff', description: 'Transfer to human agents', icon: 'Connection', enabled: false },
  { id: 'analytics', name: 'Analytics', description: 'Track conversation metrics', icon: 'DataAnalysis', enabled: false },
])

const enabledCapabilitiesCount = computed(() => {
  return availableCapabilities.value.filter(c => c.enabled).length
})

// Knowledge bases
const knowledgeBaseList = ref([
  { key: '1', label: 'Product Documentation', disabled: false },
  { key: '2', label: 'FAQ Database', disabled: false },
  { key: '3', label: 'Troubleshooting Guides', disabled: false },
  { key: '4', label: 'API Reference', disabled: false },
  { key: '5', label: 'Best Practices', disabled: false },
])

// Prompt templates
const promptTemplates = ref([
  {
    id: '1',
    name: 'Customer Support Agent',
    description: 'General customer support with friendly tone',
    prompt: 'You are a helpful customer support agent. Be friendly, patient, and professional...',
  },
  {
    id: '2',
    name: 'Technical Support Agent',
    description: 'Technical assistance with code examples',
    prompt: 'You are a technical support specialist. Provide accurate solutions with code examples...',
  },
  {
    id: '3',
    name: 'Sales Assistant',
    description: 'Help with product inquiries and sales',
    prompt: 'You are a knowledgeable sales assistant. Help customers find the right product...',
  },
])

// Config history
const configHistory = ref([
  { id: '1', action: 'Create', field: 'Agent', timestamp: Date.now() - 3600000 },
  { id: '2', action: 'Update', field: 'System Prompt', timestamp: Date.now() - 1800000 },
  { id: '3', action: 'Update', field: 'Routing Rules', timestamp: Date.now() - 600000 },
])

// Methods
async function saveDraft() {
  if (!validateForm()) return
  
  saving.value = true
  try {
    // Save agent configuration
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('Agent configuration saved')
  } catch (error) {
    ElMessage.error('Failed to save agent')
  } finally {
    saving.value = false
  }
}

async function activate() {
  activating.value = true
  try {
    form.isActive = !form.isActive
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success(form.isActive ? 'Agent activated' : 'Agent deactivated')
  } catch (error) {
    ElMessage.error('Failed to update agent status')
  } finally {
    activating.value = false
  }
}

function validateForm(): boolean {
  if (!form.name.trim()) {
    ElMessage.warning('Please enter an agent name')
    return false
  }
  if (!form.systemPrompt.trim()) {
    ElMessage.warning('Please enter a system prompt')
    return false
  }
  return true
}

function goBack() {
  router.push('/admin/agents')
}

function addRoutingRule() {
  form.routingRules.push({
    id: `rule-${Date.now()}`,
    name: '',
    condition: {
      type: 'keyword',
      operator: 'contains',
      value: '',
    },
    action: {
      type: 'assign_agent',
      value: '',
    },
    priority: form.routingRules.length + 1,
  })
}

function removeRoutingRule(index: number) {
  form.routingRules.splice(index, 1)
}

function useTemplate(template: { prompt: string }) {
  form.systemPrompt = template.prompt
  showPromptTemplates.value = false
  ElMessage.success('Template applied')
}

async function sendTestMessage() {
  const content = testInput.value.trim()
  if (!content || testLoading.value) return

  testMessages.value.push({
    id: `msg-${Date.now()}`,
    role: 'user',
    content,
  })
  testInput.value = ''
  testLoading.value = true

  // Scroll to bottom
  setTimeout(() => {
    if (testMessagesContainer.value) {
      testMessagesContainer.value.scrollTop = testMessagesContainer.value.scrollHeight
    }
  }, 0)

  // Simulate agent response
  await new Promise(resolve => setTimeout(resolve, 1500))
  
  testMessages.value.push({
    id: `msg-${Date.now()}`,
    role: 'assistant',
    content: 'This is a simulated response. In production, this would call the configured AI model with the agent\'s system prompt and settings.',
  })
  
  testLoading.value = false
  
  setTimeout(() => {
    if (testMessagesContainer.value) {
      testMessagesContainer.value.scrollTop = testMessagesContainer.value.scrollHeight
    }
  }, 0)
}

function getPriorityType(priority: number): string {
  if (priority <= 2) return 'danger'
  if (priority <= 5) return 'warning'
  return 'info'
}

function getActionType(action: string): string {
  const typeMap: Record<string, string> = {
    Create: 'success',
    Update: 'primary',
    Delete: 'danger',
  }
  return typeMap[action] || 'info'
}

function formatTime(timestamp: number): string {
  const diff = Date.now() - timestamp
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

onMounted(() => {
  // Load agent configuration if editing
  if (isEdit.value) {
    // Mock load
  }
})
</script>

<style scoped lang="scss">
.agent-builder {
  height: 100vh;
  background: #f5f7fa;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.builder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    h2 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.builder-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.config-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .param-hint {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.prompt-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 13px;
  color: #409eff;
}

.capabilities-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  :deep(.el-checkbox) {
    height: auto;
    padding: 12px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-right: 0;
    width: 100%;

    &:hover {
      border-color: #409eff;
    }

    .el-checkbox__input {
      margin-top: 20px;
    }

    .el-checkbox__label {
      flex: 1;
    }
  }

  .capability-item {
    display: flex;
    gap: 12px;

    .cap-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background: #ecf5ff;
      color: #409eff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .cap-info {
      .cap-name {
        font-weight: 600;
        color: #303133;
        margin-bottom: 4px;
      }

      .cap-desc {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.kb-config {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;

  h4 {
    margin: 0 0 16px;
    font-size: 14px;
    font-weight: 600;
  }
}

.empty-rules {
  text-align: center;
  padding: 40px 20px;
  color: #909399;

  p {
    margin: 12px 0;
  }
}

.routing-rules {
  .rule-header {
    display: flex;
    align-items: center;
    gap: 12px;

    .rule-name {
      font-weight: 500;
    }

    .el-button {
      margin-left: auto;
    }
  }
}

.preview-card {
  position: sticky;
  top: 24px;

  .preview-content {
    text-align: center;
  }

  .preview-avatar {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    color: #909399;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .preview-name {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .preview-desc {
    font-size: 13px;
    color: #909399;
    margin-top: 4px;
  }

  .preview-stats {
    text-align: left;
    margin-top: 16px;

    .stat-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;

      .stat-label {
        color: #909399;
        font-size: 13px;
      }

      .stat-value {
        color: #606266;
        font-size: 13px;
        font-weight: 500;
      }
    }
  }
}

.test-card {
  margin-top: 20px;

  .test-messages {
    height: 250px;
    overflow-y: auto;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 12px;
  }

  .test-message {
    margin-bottom: 12px;

    &.msg-user {
      text-align: right;

      .msg-content {
        background: #409eff;
        color: #fff;
        display: inline-block;
        padding: 8px 12px;
        border-radius: 12px 12px 4px 12px;
      }
    }

    &.msg-assistant {
      text-align: left;

      .msg-content {
        background: #fff;
        display: inline-block;
        padding: 8px 12px;
        border-radius: 12px 12px 12px 4px;
        border: 1px solid #e4e7ed;
      }
    }
  }

  .test-loading {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #909399;
    font-size: 13px;
  }
}

.history-card {
  margin-top: 20px;

  .history-list {
    .history-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .history-action {
        font-size: 13px;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .history-time {
        font-size: 12px;
        color: #c0c4cc;
      }
    }
  }
}

.template-list {
  .template-item {
    padding: 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
      background: #ecf5ff;
    }

    .template-name {
      font-weight: 600;
      color: #303133;
      margin-bottom: 4px;
    }

    .template-desc {
      font-size: 13px;
      color: #909399;
    }
  }
}
</style>
