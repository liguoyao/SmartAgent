<template>
  <div class="agent-console">
    <el-container>
      <!-- Sidebar - Conversation List -->
      <el-aside width="320px" class="conversation-sidebar">
        <div class="sidebar-header">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="Search conversations..."
              prefix-icon="Search"
              clearable
            />
          </div>
          <div class="filter-tabs">
            <el-tabs v-model="activeTab" @tab-change="handleTabChange">
              <el-tab-pane label="Waiting" name="waiting">
                <template #label>
                  <el-badge :value="waitingCount" :hidden="waitingCount === 0" :max="99">
                    <span>Waiting</span>
                  </el-badge>
                </template>
              </el-tab-pane>
              <el-tab-pane label="Active" name="active" />
              <el-tab-pane label="All" name="all" />
            </el-tabs>
          </div>
        </div>

        <div class="conversation-list" v-loading="loading">
          <div v-if="filteredConversations.length === 0" class="empty-list">
            <el-icon size="48"><ChatLineSquare /></el-icon>
            <p>No conversations</p>
          </div>

          <div
            v-for="conv in filteredConversations"
            :key="conv.id"
            class="conversation-item"
            :class="{ active: activeConversation?.id === conv.id }"
            @click="selectConversation(conv)"
          >
            <div class="conv-header">
              <span class="customer-name">{{ conv.customerName }}</span>
              <el-tag
                :type="getPriorityType(conv.priority)"
                size="small"
                effect="dark"
              >
                {{ conv.priority }}
              </el-tag>
            </div>
            <div class="conv-preview">
              {{ conv.lastMessage || 'No messages yet' }}
            </div>
            <div class="conv-meta">
              <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
              <span class="conv-source">{{ conv.source }}</span>
            </div>
          </div>
        </div>

        <div class="sidebar-footer">
          <div class="agent-status">
            <el-dropdown @command="handleStatusChange">
              <div class="status-indicator" :class="agentStatus">
                <span class="status-dot"></span>
                <span class="status-text">{{ statusText }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="online">
                    <span class="status-option online">Online</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="busy">
                    <span class="status-option busy">Busy</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="away">
                    <span class="status-option away">Away</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="offline" divided>
                    <span class="status-option offline">Offline</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="connection-status">
            <el-badge
              :status="wsConnected ? 'success' : 'danger'"
              :is-dot="true"
            />
            <span>{{ wsConnected ? 'Connected' : 'Disconnected' }}</span>
          </div>
        </div>
      </el-aside>

      <!-- Main Content - Chat Area -->
      <el-main class="chat-main">
        <template v-if="activeConversation">
          <!-- Chat header -->
          <div class="chat-header">
            <div class="customer-info">
              <div class="customer-avatar">
                <el-icon><User /></el-icon>
              </div>
              <div class="customer-details">
                <div class="customer-name">{{ activeConversation.customerName }}</div>
                <div class="conversation-info">
                  <span class="status-tag" :class="activeConversation.status">
                    {{ activeConversation.status }}
                  </span>
                  <span class="source-tag">{{ activeConversation.source }}</span>
                  <span v-if="activeConversation.assignedAgentName" class="agent-tag">
                    {{ activeConversation.assignedAgentName }}
                  </span>
                </div>
              </div>
            </div>
            <div class="chat-actions">
              <el-button
                v-if="activeConversation.status === 'waiting'"
                type="primary"
                @click="acceptConversation"
              >
                Accept
              </el-button>
              <el-button
                v-if="activeConversation.status === 'active'"
                type="danger"
                @click="closeConversation"
              >
                Close
              </el-button>
              <el-button @click="transferConversation">
                <el-icon><RefreshRight /></el-icon>
                Transfer
              </el-button>
              <el-button @click="addTag">
                <el-icon><PriceTag /></el-icon>
                Tag
              </el-button>
              <el-button @click="viewHistory">
                <el-icon><Clock /></el-icon>
                History
              </el-button>
            </div>
          </div>

          <!-- Messages area -->
          <div class="messages-container" ref="messagesContainer">
            <div v-if="loadingMessages" class="loading-messages">
              <el-icon class="is-loading" size="24"><Loading /></el-icon>
            </div>

            <template v-else>
              <div
                v-for="message in currentMessages"
                :key="message.id"
                class="message-wrapper"
                :class="[`message-${message.role}`]"
              >
                <div class="message-avatar">
                  <el-icon v-if="message.role === 'user'"><User /></el-icon>
                  <el-icon v-else-if="message.role === 'agent'"><UserFilled /></el-icon>
                  <el-icon v-else><Service /></el-icon>
                </div>
                <div class="message-body">
                  <div class="message-header">
                    <span class="sender-name">
                      {{ getSenderName(message.role) }}
                    </span>
                    <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                  </div>
                  <div class="message-content">
                    <div class="message-bubble" v-html="formatMessage(message.content)"></div>
                    <div v-if="message.attachments?.length" class="message-attachments">
                      <div
                        v-for="file in message.attachments"
                        :key="file.id"
                        class="attachment-item"
                      >
                        <el-icon><Document /></el-icon>
                        <a :href="file.url" target="_blank">{{ file.name }}</a>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Typing indicator -->
              <div v-if="isCustomerTyping" class="message-wrapper message-user">
                <div class="message-avatar">
                  <el-icon><User /></el-icon>
                </div>
                <div class="message-body">
                  <div class="typing-indicator">
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                    <span class="typing-dot"></span>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Input area -->
          <div class="input-area">
            <div class="input-toolbar">
              <el-button text @click="insertQuickReply">
                <el-icon><ChatLineSquare /></el-icon>
                Quick Reply
              </el-button>
              <el-button text @click="insertCannedResponse">
                <el-icon><Document /></el-icon>
                Canned Response
              </el-button>
              <el-button text @click="searchKnowledge">
                <el-icon><QuestionFilled /></el-icon>
                Knowledge Base
              </el-button>
              <el-button text @click="transferToHuman">
                <el-icon><User /></el-icon>
                Transfer to Human
              </el-button>
            </div>
            <div class="input-container">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                placeholder="Type your message..."
                resize="none"
                @keydown.enter.exact.prevent="sendMessage"
                @input="handleInput"
              />
            </div>
            <div class="input-actions">
              <div class="left-actions">
                <el-upload
                  :action="uploadUrl"
                  :headers="{ Authorization: `Bearer ${token}` }"
                  :before-upload="handleFileUpload"
                  :on-success="handleFileSuccess"
                  :on-error="handleFileError"
                  multiple
                  :show-file-list="false"
                >
                  <el-button text>
                    <el-icon><Paperclip /></el-icon>
                  </el-button>
                </el-upload>
                <el-button text @click="toggleEmoji">
                  <el-icon><Smile /></el-icon>
                </el-button>
              </div>
              <div class="right-actions">
                <span class="char-count">{{ inputMessage.length }}/2000</span>
                <el-button
                  type="primary"
                  :disabled="!inputMessage.trim() || sending"
                  @click="sendMessage"
                >
                  <el-icon v-if="!sending"><Promotion /></el-icon>
                  <el-icon v-else class="is-loading"><Loading /></el-icon>
                  Send
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <!-- Empty state -->
        <div v-else class="empty-state">
          <div class="empty-content">
            <el-icon size="64"><ChatDotRound /></el-icon>
            <h3>Select a conversation</h3>
            <p>Choose a conversation from the sidebar to start chatting</p>
          </div>
        </div>
      </el-main>

      <!-- Right Panel - Customer Info & Tools -->
      <el-aside width="300px" class="info-sidebar" v-if="activeConversation">
        <el-tabs v-model="rightPanelTab">
          <el-tab-pane label="Customer" name="customer">
            <div class="customer-profile">
              <div class="profile-avatar">
                <el-icon size="32"><User /></el-icon>
              </div>
              <div class="profile-name">{{ activeConversation.customerName }}</div>
              <div class="profile-id">ID: {{ activeConversation.customerId }}</div>
            </div>

            <div class="info-section">
              <h4>Conversation Info</h4>
              <div class="info-item">
                <span class="label">Status</span>
                <el-tag :type="getStatusType(activeConversation.status)">
                  {{ activeConversation.status }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">Priority</span>
                <el-tag :type="getPriorityType(activeConversation.priority)">
                  {{ activeConversation.priority }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">Source</span>
                <span>{{ activeConversation.source }}</span>
              </div>
              <div class="info-item">
                <span class="label">Created</span>
                <span>{{ formatDateTime(activeConversation.createdAt) }}</span>
              </div>
              <div class="info-item">
                <span class="label">Duration</span>
                <span>{{ getDuration(activeConversation.createdAt) }}</span>
              </div>
            </div>

            <div class="info-section">
              <h4>Tags</h4>
              <el-tag
                v-for="tag in activeConversation.tags"
                :key="tag"
                closable
                size="small"
                @close="removeTag(tag)"
              >
                {{ tag }}
              </el-tag>
              <el-button size="small" text @click="addTag">
                <el-icon><Plus /></el-icon>
                Add Tag
              </el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="History" name="history">
            <div class="history-list">
              <div
                v-for="item in conversationHistory"
                :key="item.id"
                class="history-item"
              >
                <div class="history-date">{{ formatDate(item.timestamp) }}</div>
                <div class="history-preview">{{ item.preview }}</div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="Notes" name="notes">
            <el-input
              v-model="agentNotes"
              type="textarea"
              :rows="10"
              placeholder="Add notes about this conversation..."
            />
            <el-button type="primary" @click="saveNotes" :loading="savingNotes">
              Save Notes
            </el-button>
          </el-tab-pane>
        </el-tabs>
      </el-aside>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useConversationStore } from '@/stores/conversation'
import { useWebSocket } from '@/services/websocket'
import { marked } from 'marked'
import hljs from 'highlight.js'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import {
  ChatLineSquare,
  User,
  UserFilled,
  Service,
  Document,
  Loading,
  Promotion,
  Paperclip,
  Smile,
  RefreshRight,
  PriceTag,
  Clock,
  QuestionFilled,
  Plus,
  Search,
} from '@element-plus/icons-vue'
import type { Conversation, Message } from '@/types'

dayjs.extend(relativeTime)

// Store
const authStore = useAuthStore()
const conversationStore = useConversationStore()

// WebSocket
const {
  isConnected: wsConnected,
  onTypingIndicator,
  onChatMessage,
  sendTyping,
  joinConversation,
} = useWebSocket() || {}

// Refs
const searchKeyword = ref('')
const activeTab = ref('waiting')
const loading = ref(false)
const loadingMessages = ref(false)
const activeConversation = ref<Conversation | null>(null)
const inputMessage = ref('')
const sending = ref(false)
const isCustomerTyping = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const rightPanelTab = ref('customer')
const agentNotes = ref('')
const savingNotes = ref(false)
const conversationHistory = ref<{ id: string; timestamp: number; preview: string }[]>([])
const token = computed(() => authStore.token || '')

const uploadUrl = computed(() => '/api/upload')

// Computed
const conversations = computed(() => conversationStore.conversations)
const waitingCount = computed(() => conversationStore.waitingCount)
const currentMessages = computed(() => {
  if (!activeConversation.value) return []
  return conversationStore.messages.get(activeConversation.value.id) || []
})

const filteredConversations = computed(() => {
  let list = conversations.value
  
  if (activeTab.value === 'waiting') {
    list = list.filter(c => c.status === 'waiting')
  } else if (activeTab.value === 'active') {
    list = list.filter(c => c.status === 'active')
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(c =>
      c.customerName.toLowerCase().includes(keyword) ||
      c.lastMessage?.toLowerCase().includes(keyword)
    )
  }
  
  return list
})

const agentStatus = computed(() => authStore.userInfo?.status || 'offline')
const statusText = computed(() => {
  const statusMap: Record<string, string> = {
    online: 'Online',
    busy: 'Busy',
    away: 'Away',
    offline: 'Offline',
  }
  return statusMap[agentStatus.value] || 'Offline'
})

// Lifecycle
onMounted(async () => {
  await loadConversations()
  
  // Setup WebSocket handlers
  onTypingIndicator?.((data) => {
    if (data.conversationId === activeConversation.value?.id) {
      isCustomerTyping.value = data.isTyping
    }
  })

  onChatMessage?.((data) => {
    if (data.conversationId === activeConversation.value?.id) {
      nextTick(() => scrollToBottom())
    }
  })
})

onUnmounted(() => {
  conversationStore.setActiveConversation(null)
})

// Watch active conversation
watch(activeConversation, async (conv) => {
  if (conv) {
    loadingMessages.value = true
    await conversationStore.fetchMessages(conv.id)
    await loadConversationHistory(conv.id)
    nextTick(() => scrollToBottom())
    loadingMessages.value = false
  }
})

// Methods
async function loadConversations() {
  loading.value = true
  try {
    await conversationStore.fetchConversations()
  } catch (error) {
    ElMessage.error('Failed to load conversations')
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  loadConversations()
}

async function selectConversation(conv: Conversation) {
  if (activeConversation.value?.id === conv.id) return
  
  activeConversation.value = conv
  conversationStore.setActiveConversation(conv)
  joinConversation?.(conv.id)
}

async function loadConversationHistory(conversationId: string) {
  // Mock data - in real app, fetch from API
  conversationHistory.value = [
    { id: '1', timestamp: Date.now() - 86400000, preview: 'Previous conversation summary...' },
    { id: '2', timestamp: Date.now() - 172800000, preview: 'Earlier interaction details...' },
  ]
}

async function acceptConversation() {
  if (!activeConversation.value) return
  
  try {
    await conversationStore.assignAgent(
      activeConversation.value.id,
      authStore.userInfo?.id || ''
    )
    activeConversation.value.status = 'active'
    ElMessage.success('Conversation accepted')
  } catch (error) {
    ElMessage.error('Failed to accept conversation')
  }
}

async function closeConversation() {
  if (!activeConversation.value) return
  
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to close this conversation?',
      'Close Conversation',
      { type: 'warning' }
    )
    
    await conversationStore.closeConversation(activeConversation.value.id)
    ElMessage.success('Conversation closed')
    activeConversation.value = null
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to close conversation')
    }
  }
}

function transferConversation() {
  // Show transfer dialog
  ElMessage.info('Transfer feature coming soon')
}

function addTag() {
  ElMessage.info('Add tag feature coming soon')
}

function removeTag(tag: string) {
  ElMessage.info('Remove tag feature coming soon')
}

function viewHistory() {
  rightPanelTab.value = 'history'
}

async function saveNotes() {
  savingNotes.value = true
  try {
    // Save notes to backend
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('Notes saved')
  } catch (error) {
    ElMessage.error('Failed to save notes')
  } finally {
    savingNotes.value = false
  }
}

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || !activeConversation.value || sending.value) return
  
  sending.value = true
  inputMessage.value = ''
  
  try {
    await conversationStore.sendChatMessage(activeConversation.value.id, content)
    nextTick(() => scrollToBottom())
  } catch (error) {
    ElMessage.error('Failed to send message')
  } finally {
    sending.value = false
  }
}

let typingTimer: number | null = null
function handleInput() {
  if (typingTimer) clearTimeout(typingTimer)
  
  sendTyping?.(activeConversation.value?.id || '', true)
  
  typingTimer = window.setTimeout(() => {
    sendTyping?.(activeConversation.value?.id || '', false)
  }, 1000)
}

function insertQuickReply() {
  ElMessage.info('Quick reply feature coming soon')
}

function insertCannedResponse() {
  ElMessage.info('Canned response feature coming soon')
}

function searchKnowledge() {
  ElMessage.info('Knowledge search coming soon')
}

function transferToHuman() {
  ElMessage.info('Transfer to human feature coming soon')
}

function handleFileUpload(file: File) {
  // Handle file upload
  return true
}

function handleFileSuccess() {
  ElMessage.success('File uploaded')
}

function handleFileError() {
  ElMessage.error('File upload failed')
}

function toggleEmoji() {
  ElMessage.info('Emoji picker coming soon')
}

function handleStatusChange(status: string) {
  authStore.updateStatus?.(authStore.userInfo?.id || '', status)
}

// Helpers
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function formatMessage(content: string): string {
  marked.setOptions({
    highlight: (code: string) => hljs.highlightAuto(code).value,
    breaks: true,
  })
  return marked.parse(content) as string
}

function formatTime(timestamp: number): string {
  return dayjs(timestamp).format('HH:mm')
}

function formatDate(timestamp: number): string {
  return dayjs(timestamp).format('MMM D, YYYY')
}

function formatDateTime(timestamp: number): string {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm')
}

function getDuration(startTime: number): string {
  return dayjs().to(dayjs(startTime))
}

function getPriorityType(priority: string): string {
  const typeMap: Record<string, string> = {
    low: 'info',
    normal: '',
    high: 'warning',
    urgent: 'danger',
  }
  return typeMap[priority] || ''
}

function getStatusType(status: string): string {
  const typeMap: Record<string, string> = {
    waiting: 'warning',
    active: 'success',
    resolved: 'info',
    closed: 'info',
  }
  return typeMap[status] || ''
}

function getSenderName(role: string): string {
  const nameMap: Record<string, string> = {
    user: 'Customer',
    assistant: 'Bot',
    agent: authStore.userInfo?.name || 'Agent',
  }
  return nameMap[role] || 'Unknown'
}
</script>

<style scoped lang="scss">
.agent-console {
  height: 100vh;
  background: #f5f7fa;

  :deep(.el-container) {
    height: 100%;
  }
}

.conversation-sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;

  .search-box {
    margin-bottom: 12px;
  }

  .filter-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }

    :deep(.el-tabs__nav) {
      width: 100%;
    }

    :deep(.el-tabs__item) {
      flex: 1;
      text-align: center;
    }
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;

  p {
    margin-top: 12px;
  }
}

.conversation-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: #ecf5ff;
    border-left: 3px solid #409eff;
  }

  .conv-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
  }

  .customer-name {
    font-weight: 600;
    color: #303133;
  }

  .conv-preview {
    font-size: 13px;
    color: #909399;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
  }

  .conv-meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #c0c4cc;
  }
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .agent-status {
    .status-indicator {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;

      &:hover {
        background: #f5f7fa;
      }
    }

    .status-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;

      &.online { background: #67c23a; }
      &.busy { background: #e6a23c; }
      &.away { background: #909399; }
      &.offline { background: #c0c4cc; }
    }

    .status-text {
      font-size: 13px;
      color: #606266;
    }
  }

  .connection-status {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #909399;
  }
}

.chat-main {
  display: flex;
  flex-direction: column;
  padding: 0;
  background: #fff;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;

  .customer-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .customer-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: #f0f0f0;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #909399;
    }

    .customer-details {
      .customer-name {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .conversation-info {
        display: flex;
        gap: 8px;
        margin-top: 4px;

        .status-tag {
          font-size: 12px;
          padding: 2px 8px;
          border-radius: 4px;

          &.waiting { background: #fdf6ec; color: #e6a23c; }
          &.active { background: #f0f9eb; color: #67c23a; }
          &.resolved { background: #ecf5ff; color: #409eff; }
        }

        .source-tag, .agent-tag {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  .chat-actions {
    display: flex;
    gap: 8px;
  }
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.loading-messages {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.message-wrapper {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;

  &.message-user {
    flex-direction: row-reverse;

    .message-bubble {
      background: #409eff;
      color: #fff;
      border-radius: 16px 16px 4px 16px;
    }

    .sender-name {
      text-align: right;
    }
  }

  &.message-agent, &.message-assistant {
    .message-bubble {
      background: #f0f2f5;
      color: #303133;
      border-radius: 16px 16px 16px 4px;
    }
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #909399;
}

.message-body {
  max-width: 70%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;

  .sender-name {
    font-size: 13px;
    font-weight: 600;
    color: #606266;
  }

  .message-time {
    font-size: 12px;
    color: #c0c4cc;
  }
}

.message-bubble {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;

  :deep(p) {
    margin: 0 0 8px;
    &:last-child { margin-bottom: 0; }
  }

  :deep(code) {
    background: rgba(0, 0, 0, 0.06);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: monospace;
  }

  :deep(pre) {
    background: rgba(0, 0, 0, 0.06);
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;

    code {
      background: none;
      padding: 0;
    }
  }
}

.message-attachments {
  margin-top: 8px;

  .attachment-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 10px;
    background: rgba(0, 0, 0, 0.04);
    border-radius: 6px;
    font-size: 13px;

    a {
      color: inherit;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f0f2f5;
  border-radius: 16px;

  .typing-dot {
    width: 8px;
    height: 8px;
    background: #909399;
    border-radius: 50%;
    animation: typing 1.4s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 16px 20px;
  background: #fff;

  .input-toolbar {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .input-container {
    :deep(.el-textarea__inner) {
      border: none;
      padding: 0;
      font-size: 14px;

      &:focus {
        box-shadow: none;
      }
    }
  }

  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;

    .left-actions, .right-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .char-count {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  .empty-content {
    text-align: center;
    color: #909399;

    h3 {
      margin: 16px 0 8px;
      font-size: 18px;
      color: #606266;
    }

    p {
      margin: 0;
    }
  }
}

.info-sidebar {
  background: #fff;
  border-left: 1px solid #e4e7ed;
  
  :deep(.el-tabs__header) {
    padding: 0 16px;
    margin-bottom: 0;
  }

  :deep(.el-tabs__content) {
    padding: 16px;
  }
}

.customer-profile {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;

  .profile-avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 12px;
  }

  .profile-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .profile-id {
    font-size: 13px;
    color: #909399;
    margin-top: 4px;
  }
}

.info-section {
  margin-top: 20px;

  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px solid #f5f5f5;

    .label {
      font-size: 13px;
      color: #909399;
    }

    span:last-child {
      font-size: 13px;
      color: #606266;
    }
  }
}

.history-list {
  .history-item {
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    .history-date {
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }

    .history-preview {
      font-size: 13px;
      color: #606266;
    }
  }
}

.status-option {
  &.online::before {
    content: '';
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #67c23a;
    margin-right: 8px;
  }

  &.busy::before {
    content: '';
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #e6a23c;
    margin-right: 8px;
  }

  &.away::before {
    content: '';
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #909399;
    margin-right: 8px;
  }

  &.offline::before {
    content: '';
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #c0c4cc;
    margin-right: 8px;
  }
}
</style>
