<template>
  <div class="chat-widget" :class="[`theme-${theme}`, { 'is-open': isOpen }]">
    <!-- Chat toggle button -->
    <div 
      class="chat-toggle" 
      @click="toggleChat"
      v-if="!embedded"
    >
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <div class="toggle-button">
          <el-icon v-if="!isLoading"><ChatDotRound /></el-icon>
          <el-icon v-else class="is-loading"><Loading /></el-icon>
        </div>
      </el-badge>
    </div>

    <!-- Chat window -->
    <transition name="slide-fade">
      <div v-if="isOpen || embedded" class="chat-window">
        <!-- Header -->
        <div class="chat-header">
          <div class="header-info">
            <div class="agent-avatar">
              <img v-if="agentAvatar" :src="agentAvatar" alt="Agent" />
              <el-icon v-else><UserFilled /></el-icon>
            </div>
            <div class="header-text">
              <div class="agent-name">{{ agentName }}</div>
              <div class="status-text">
                <span class="status-dot" :class="connectionStatus"></span>
                {{ statusText }}
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button 
              v-if="!embedded" 
              text 
              @click="toggleChat"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- Messages area -->
        <div class="chat-messages" ref="messagesContainer">
          <div v-if="loadingHistory" class="loading-history">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>Loading messages...</span>
          </div>
          
          <template v-else>
            <!-- Welcome message -->
            <div v-if="messages.length === 0" class="welcome-message">
              <div class="welcome-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <h3>{{ welcomeMessage }}</h3>
              <p>How can I help you today?</p>
            </div>

            <!-- Message list -->
            <div 
              v-for="message in messages" 
              :key="message.id"
              class="message-item"
              :class="[`message-${message.role}`]"
            >
              <div class="message-avatar" v-if="message.role !== 'user'">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <div class="message-text" v-html="formatMessage(message.content)"></div>
                  <div class="message-time">
                    {{ formatTime(message.timestamp) }}
                  </div>
                </div>
              </div>
              <div class="message-avatar" v-if="message.role === 'user'">
                <el-icon><User /></el-icon>
              </div>
            </div>

            <!-- Typing indicator -->
            <div v-if="isAgentTyping" class="message-item message-assistant">
              <div class="message-avatar">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-bubble typing">
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- Quick actions -->
        <div v-if="quickActions.length > 0 && messages.length === 0" class="quick-actions">
          <el-button
            v-for="action in quickActions"
            :key="action.id"
            size="small"
            @click="executeQuickAction(action)"
          >
            {{ action.label }}
          </el-button>
        </div>

        <!-- Input area -->
        <div class="chat-input">
          <div class="input-container">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="1"
              :placeholder="placeholder"
              resize="none"
              @keydown.enter.exact.prevent="sendMessage"
              @input="handleInput"
            />
            <div class="input-actions">
              <el-upload
                :action="uploadUrl"
                :headers="{ Authorization: `Bearer ${token}` }"
                :before-upload="handleFileSelect"
                :show-file-list="false"
                accept="image/*,.pdf,.doc,.docx"
              >
                <el-button text><el-icon><Paperclip /></el-icon></el-button>
              </el-upload>
              <el-button 
                type="primary" 
                :disabled="!inputMessage.trim() || sending"
                @click="sendMessage"
              >
                <el-icon v-if="!sending"><Promotion /></el-icon>
                <el-icon v-else class="is-loading"><Loading /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-if="selectedFile" class="file-preview">
            <span>{{ selectedFile.name }}</span>
            <el-button text size="small" @click="removeFile">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useConversationStore } from '@/stores/conversation'
import { createWebSocketService } from '@/services/websocket'
import { marked } from 'marked'
import hljs from 'highlight.js'
import dayjs from 'dayjs'
import { ChatDotRound, UserFilled, User, Close, Paperclip, Promotion, Loading } from '@element-plus/icons-vue'
import type { Message, WidgetConfig, QuickAction } from '@/types'

interface Props {
  config: WidgetConfig
  embedded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  embedded: false,
})

// Stores
const authStore = useAuthStore()
const conversationStore = useConversationStore()

// Refs
const isOpen = ref(false)
const inputMessage = ref('')
const sending = ref(false)
const loadingHistory = ref(false)
const isAgentTyping = ref(false)
const selectedFile = ref<File | null>(null)
const messagesContainer = ref<HTMLElement | null>(null)
const messages = ref<Message[]>([])

// Config
const theme = computed(() => props.config.theme || 'light')
const welcomeMessage = computed(() => props.config.welcomeMessage || 'Welcome to SmartAgent')
const placeholder = computed(() => props.config.placeholder || 'Type your message...')
const agentName = computed(() => 'Smart Assistant')
const agentAvatar = ref('')
const token = computed(() => authStore.token || '')
const uploadUrl = computed(() => `${props.config.apiUrl}/upload`)

// Quick actions
const quickActions = ref<QuickAction[]>([])

// Connection status
const connectionStatus = computed(() => {
  if (!conversationStore.isConnected) return 'disconnected'
  return 'connected'
})

const statusText = computed(() => {
  if (conversationStore.connectionStatus?.reconnecting) return 'Reconnecting...'
  if (!conversationStore.isConnected) return 'Disconnected'
  return 'Online'
})

const unreadCount = computed(() => {
  if (props.embedded) return 0
  return conversationStore.unreadCount
})

// Initialize WebSocket and conversation
onMounted(async () => {
  // Create WebSocket connection
  createWebSocketService({
    url: props.config.wsUrl,
    reconnectInterval: 3000,
    maxReconnectAttempts: 5,
    heartbeatInterval: 30000,
  })

  // Setup message handlers
  conversationStore.setupWebSocketHandlers()

  // Setup typing indicator handler
  const { onTypingIndicator, onChatMessage } = await import('@/services/websocket')
  onTypingIndicator?.((data) => {
    if (data.conversationId === conversationStore.activeConversation?.id) {
      isAgentTyping.value = data.isTyping
    }
  })

  onChatMessage?.((data) => {
    if (data.conversationId === conversationStore.activeConversation?.id) {
      messages.value.push(data.message)
      scrollToBottom()
    }
  })

  // If embedded, start conversation immediately
  if (props.embedded) {
    await startConversation()
  }
})

onUnmounted(() => {
  conversationStore.clearStore()
})

// Watch for new messages
watch(() => conversationStore.messages, (newMessages) => {
  if (conversationStore.activeConversation) {
    const convMessages = newMessages.get(conversationStore.activeConversation.id) || []
    messages.value = convMessages
  }
}, { deep: true })

// Start a new conversation
async function startConversation() {
  try {
    // Create conversation
    const response = await conversationStore.createConversation({
      customerId: `anon-${Date.now()}`,
      customerName: 'Guest User',
      source: 'widget',
    })

    if (response?.data?.data) {
      conversationStore.setActiveConversation(response.data.data)
      conversationStore.fetchMessages(response.data.data.id)
      
      // Connect WebSocket
      conversationStore.connect?.(token.value)
    }
  } catch (error) {
    console.error('Failed to start conversation:', error)
  }
}

// Toggle chat window
function toggleChat() {
  isOpen.value = !isOpen.value
  
  if (isOpen.value && !conversationStore.activeConversation) {
    startConversation()
  }
}

// Handle input change
let typingTimer: number | null = null
function handleInput() {
  if (typingTimer) clearTimeout(typingTimer)
  
  conversationStore.sendTyping?.(
    conversationStore.activeConversation?.id || '',
    true
  )
  
  typingTimer = window.setTimeout(() => {
    conversationStore.sendTyping?.(
      conversationStore.activeConversation?.id || '',
      false
    )
  }, 1000)
}

// Send message
async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || sending.value) return
  
  const conversationId = conversationStore.activeConversation?.id
  if (!conversationId) {
    await startConversation()
    return
  }

  sending.value = true
  inputMessage.value = ''
  
  try {
    await conversationStore.sendChatMessage(conversationId, content)
    scrollToBottom()
  } catch (error) {
    console.error('Failed to send message:', error)
  } finally {
    sending.value = false
  }
}

// Execute quick action
function executeQuickAction(action: QuickAction) {
  inputMessage.value = action.payload
  sendMessage()
}

// Handle file selection
function handleFileSelect(file: File) {
  selectedFile.value = file
  return false // Prevent auto upload
}

// Remove selected file
function removeFile() {
  selectedFile.value = null
}

// Format message content (markdown)
function formatMessage(content: string): string {
  marked.setOptions({
    highlight: (code: string) => {
      return hljs.highlightAuto(code).value
    },
    breaks: true,
  })
  return marked.parse(content) as string
}

// Format timestamp
function formatTime(timestamp: number): string {
  return dayjs(timestamp).format('HH:mm')
}

// Scroll to bottom
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// Expose methods for embedding
defineExpose({
  open: () => { isOpen.value = true },
  close: () => { isOpen.value = false },
  sendMessage: (content: string) => {
    inputMessage.value = content
    sendMessage()
  },
})
</script>

<style scoped lang="scss">
.chat-widget {
  --widget-bg: #ffffff;
  --widget-header-bg: #409eff;
  --widget-text: #303133;
  --widget-text-secondary: #909399;
  --widget-border: #ebeef5;
  --widget-primary: #409eff;
  --widget-danger: #f56c6c;
  --widget-success: #67c23a;

  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;

  &.theme-dark {
    --widget-bg: #1a1a1a;
    --widget-header-bg: #2d2d2d;
    --widget-text: #ffffff;
    --widget-text-secondary: #a0a0a0;
    --widget-border: #3d3d3d;
  }

  &.is-open {
    .chat-toggle {
      display: none;
    }
  }
}

.chat-toggle {
  cursor: pointer;
}

.toggle-button {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--widget-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: scale(1.05);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  }
}

.chat-window {
  width: 380px;
  height: 560px;
  background: var(--widget-bg);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.slide-fade-enter-active,
  &.slide-fade-leave-active {
    transition: all 0.3s ease;
  }

  &.slide-fade-enter-from,
  &.slide-fade-leave-to {
    opacity: 0;
    transform: translateY(20px);
  }
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: var(--widget-header-bg);
  color: white;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.header-text {
  .agent-name {
    font-size: 16px;
    font-weight: 600;
  }

  .status-text {
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
    opacity: 0.9;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--widget-success);

    &.disconnected {
      background: var(--widget-danger);
    }
  }
}

.header-actions {
  :deep(.el-button) {
    color: white;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--widget-bg);
}

.loading-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--widget-text-secondary);
  gap: 8px;
}

.welcome-message {
  text-align: center;
  padding: 40px 20px;

  .welcome-icon {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, #409eff, #79bbff);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    font-size: 28px;
  }

  h3 {
    margin: 0 0 8px;
    color: var(--widget-text);
    font-size: 18px;
  }

  p {
    margin: 0;
    color: var(--widget-text-secondary);
    font-size: 14px;
  }
}

.message-item {
  display: flex;
  margin-bottom: 16px;
  gap: 8px;

  &.message-user {
    flex-direction: row-reverse;

    .message-bubble {
      background: var(--widget-primary);
      color: white;
      border-radius: 16px 16px 4px 16px;
    }

    .message-time {
      color: rgba(255, 255, 255, 0.7);
    }
  }

  &.message-assistant {
    .message-bubble {
      background: var(--widget-border);
      color: var(--widget-text);
      border-radius: 16px 16px 16px 4px;
    }
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--widget-border);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
  color: var(--widget-text-secondary);
}

.message-content {
  max-width: 75%;
}

.message-bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.5;

  &.typing {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 12px 16px;
  }

  .message-text {
    word-break: break-word;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child {
        margin-bottom: 0;
      }
    }

    :deep(code) {
      background: rgba(0, 0, 0, 0.06);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 13px;
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
}

.message-time {
  font-size: 11px;
  color: var(--widget-text-secondary);
  margin-top: 4px;
  text-align: right;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: var(--widget-text-secondary);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;

  &:nth-child(2) {
    animation-delay: 0.2s;
  }

  &:nth-child(3) {
    animation-delay: 0.4s;
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-4px);
  }
}

.quick-actions {
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 1px solid var(--widget-border);
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid var(--widget-border);
  background: var(--widget-bg);
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;

  :deep(.el-textarea) {
    flex: 1;

    .el-textarea__inner {
      border-radius: 20px;
      padding: 8px 12px;
      resize: none;
    }
  }
}

.input-actions {
  display: flex;
  gap: 4px;

  :deep(.el-button) {
    padding: 8px;
  }
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--widget-border);
  border-radius: 8px;
  font-size: 13px;
  color: var(--widget-text);
}
</style>
