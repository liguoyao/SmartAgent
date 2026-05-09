import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { conversationApi } from '@/services/conversationService'
import { useWebSocket } from '@/services/websocket'
import type { Conversation, Message } from '@/types'

export const useConversationStore = defineStore('conversation', () => {
  // State
  const conversations = ref<Conversation[]>([])
  const activeConversation = ref<Conversation | null>(null)
  const messages = ref<Map<string, Message[]>>(new Map())
  const loading = ref(false)
  const totalCount = ref(0)
  const page = ref(1)
  const pageSize = ref(20)

  // WebSocket
  const { 
    isConnected, 
    connect, 
    disconnect, 
    sendMessage, 
    sendTyping, 
    joinConversation,
    leaveConversation,
    onChatMessage,
    onTypingIndicator,
    onAgentAssigned,
    onConversationUpdated,
  } = useWebSocket() || {}

  const unreadCount = computed(() => {
    return conversations.value.filter(c => c.status === 'active').length
  })

  const waitingCount = computed(() => {
    return conversations.value.filter(c => c.status === 'waiting').length
  })

  // Fetch conversations
  async function fetchConversations(params?: {
    status?: string
    agentId?: string
    startTime?: number
    endTime?: number
    keyword?: string
  }): Promise<void> {
    loading.value = true
    try {
      const response = await conversationApi.getList({
        page: page.value,
        pageSize: pageSize.value,
        ...params,
      })
      conversations.value = response.data.data.list
      totalCount.value = response.data.data.total
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }

  // Fetch single conversation
  async function fetchConversation(id: string): Promise<void> {
    try {
      const response = await conversationApi.getById(id)
      activeConversation.value = response.data.data
    } catch (error) {
      throw error
    }
  }

  // Fetch messages for a conversation
  async function fetchMessages(conversationId: string, pageNum = 1): Promise<void> {
    try {
      const response = await conversationApi.getMessages(conversationId, { page: pageNum })
      const conversationMessages = response.data.data.list.reverse()
      
      const existingMessages = messages.value.get(conversationId) || []
      messages.value.set(conversationId, [...existingMessages, ...conversationMessages])
    } catch (error) {
      throw error
    }
  }

  // Send message
  async function sendChatMessage(conversationId: string, content: string): Promise<void> {
    // Add optimistic message
    const tempMessage: Message = {
      id: `temp-${Date.now()}`,
      conversationId,
      role: 'user',
      content,
      timestamp: Date.now(),
      status: 'sending',
    }
    
    const conversationMessages = messages.value.get(conversationId) || []
    messages.value.set(conversationId, [...conversationMessages, tempMessage])

    try {
      // Send via WebSocket if connected
      if (isConnected?.value) {
        sendMessage?.(conversationId, content)
      } else {
        // Fallback to HTTP
        const response = await conversationApi.sendMessage(conversationId, content)
        const sentMessage = response.data.data
        updateMessage(conversationId, tempMessage.id, sentMessage)
      }
    } catch (error) {
      updateMessageStatus(conversationId, tempMessage.id, 'error')
      throw error
    }
  }

  // Update message status
  function updateMessageStatus(conversationId: string, messageId: string, status: 'sending' | 'sent' | 'error'): void {
    const conversationMessages = messages.value.get(conversationId)
    if (conversationMessages) {
      const message = conversationMessages.find(m => m.id === messageId)
      if (message) {
        message.status = status
      }
    }
  }

  // Update message (for optimistic updates)
  function updateMessage(conversationId: string, tempId: string, newMessage: Message): void {
    const conversationMessages = messages.value.get(conversationId)
    if (conversationMessages) {
      const index = conversationMessages.findIndex(m => m.id === tempId)
      if (index !== -1) {
        conversationMessages[index] = newMessage
      }
    }
  }

  // Add incoming message
  function addMessage(conversationId: string, message: Message): void {
    const conversationMessages = messages.value.get(conversationId) || []
    
    // Check if message already exists
    const exists = conversationMessages.some(m => m.id === message.id)
    if (!exists) {
      messages.value.set(conversationId, [...conversationMessages, message])
    }
  }

  // Set active conversation
  function setActiveConversation(conversation: Conversation | null): void {
    if (activeConversation.value) {
      leaveConversation?.(activeConversation.value.id)
    }
    
    activeConversation.value = conversation
    
    if (conversation) {
      joinConversation?.(conversation.id)
    }
  }

  // Update conversation status
  function updateConversationStatus(conversationId: string, status: Conversation['status']): void {
    const conversation = conversations.value.find(c => c.id === conversationId)
    if (conversation) {
      conversation.status = status
    }
    if (activeConversation.value?.id === conversationId) {
      activeConversation.value.status = status
    }
  }

  // Assign agent to conversation
  async function assignAgent(conversationId: string, agentId: string): Promise<void> {
    await conversationApi.assignAgent(conversationId, agentId)
    const conversation = conversations.value.find(c => c.id === conversationId)
    if (conversation) {
      conversation.assignedAgentId = agentId
    }
  }

  // Close conversation
  async function closeConversation(conversationId: string): Promise<void> {
    await conversationApi.close(conversationId)
    updateConversationStatus(conversationId, 'closed')
  }

  // WebSocket event handlers setup
  function setupWebSocketHandlers(): void {
    onChatMessage?.((data) => {
      if (data.conversationId === activeConversation.value?.id) {
        addMessage(data.conversationId, data.message)
      }
    })

    onTypingIndicator?.((data) => {
      // Handle typing indicator
    })

    onAgentAssigned?.((data) => {
      const conversation = conversations.value.find(c => c.id === data.conversationId)
      if (conversation) {
        conversation.assignedAgentId = data.agentId
        conversation.assignedAgentName = data.agentName
        conversation.status = 'active'
      }
    })

    onConversationUpdated?.((data) => {
      const index = conversations.value.findIndex(c => c.id === data.conversation.id)
      if (index !== -1) {
        conversations.value[index] = data.conversation
      }
      if (activeConversation.value?.id === data.conversation.id) {
        activeConversation.value = data.conversation
      }
    })
  }

  // Clear store
  function clearStore(): void {
    conversations.value = []
    activeConversation.value = null
    messages.value.clear()
    loading.value = false
  }

  return {
    // State
    conversations,
    activeConversation,
    messages,
    loading,
    totalCount,
    page,
    pageSize,
    unreadCount,
    waitingCount,
    isConnected,
    
    // Actions
    fetchConversations,
    fetchConversation,
    fetchMessages,
    sendChatMessage,
    addMessage,
    setActiveConversation,
    updateConversationStatus,
    assignAgent,
    closeConversation,
    setupWebSocketHandlers,
    clearStore,
  }
})
