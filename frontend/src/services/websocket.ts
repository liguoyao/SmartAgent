import { ref, reactive, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { WSMessage, Message, Conversation } from '@/types'

type MessageHandler = (message: WSMessage) => void
type StatusChangeHandler = (status: 'connected' | 'disconnected' | 'error') => void

interface WebSocketServiceOptions {
  url: string
  reconnectInterval?: number
  maxReconnectAttempts?: number
  heartbeatInterval?: number
}

class WebSocketService {
  private ws: WebSocket | null = null
  private url: string
  private reconnectInterval: number
  private maxReconnectAttempts: number
  private heartbeatInterval: number
  private reconnectAttempts = 0
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private messageHandlers: Set<MessageHandler> = new Set()
  private statusHandlers: Set<StatusChangeHandler> = new Set()
  
  public isConnected = ref(false)
  public connectionStatus = reactive({
    status: 'disconnected' as 'connected' | 'disconnected' | 'error',
    lastError: '' as string,
    reconnecting: false,
  })

  constructor(options: WebSocketServiceOptions) {
    this.url = options.url
    this.reconnectInterval = options.reconnectInterval || 3000
    this.maxReconnectAttempts = options.maxReconnectAttempts || 5
    this.heartbeatInterval = options.heartbeatInterval || 30000
  }

  connect(token?: string): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return
    }

    const wsUrl = token ? `${this.url}?token=${token}` : this.url
    
    try {
      this.ws = new WebSocket(wsUrl)
      this.setupEventListeners()
    } catch (error) {
      this.handleError(error as Error)
    }
  }

  private setupEventListeners(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      this.isConnected.value = true
      this.connectionStatus.status = 'connected'
      this.connectionStatus.reconnecting = false
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.notifyStatusChange('connected')
    }

    this.ws.onclose = (event) => {
      this.isConnected.value = false
      this.connectionStatus.status = 'disconnected'
      this.stopHeartbeat()
      this.notifyStatusChange('disconnected')
      
      if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (event) => {
      this.connectionStatus.status = 'error'
      this.connectionStatus.lastError = 'WebSocket connection error'
      this.notifyStatusChange('error')
    }

    this.ws.onmessage = (event) => {
      try {
        const message: WSMessage = JSON.parse(event.data)
        
        // Handle pong
        if (message.type === 'pong') {
          return
        }
        
        this.notifyMessageHandlers(message)
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error)
      }
    }
  }

  private handleError(error: Error): void {
    this.connectionStatus.status = 'error'
    this.connectionStatus.lastError = error.message
    this.notifyStatusChange('error')
    
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    this.connectionStatus.reconnecting = true
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectAttempts++
      this.connect()
    }, this.reconnectInterval)
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      this.send({ type: 'ping', payload: null, timestamp: Date.now() })
    }, this.heartbeatInterval)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  send(message: WSMessage): boolean {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      return false
    }

    try {
      this.ws.send(JSON.stringify(message))
      return true
    } catch (error) {
      console.error('Failed to send WebSocket message:', error)
      return false
    }
  }

  // Send chat message
  sendChatMessage(conversationId: string, content: string): boolean {
    return this.send({
      type: 'message',
      payload: {
        conversationId,
        content,
        timestamp: Date.now(),
      },
      timestamp: Date.now(),
    })
  }

  // Send typing indicator
  sendTyping(conversationId: string, isTyping: boolean): boolean {
    return this.send({
      type: 'typing',
      payload: {
        conversationId,
        isTyping,
      },
      timestamp: Date.now(),
    })
  }

  // Join conversation room
  joinConversation(conversationId: string): boolean {
    return this.send({
      type: 'status',
      payload: {
        action: 'join',
        conversationId,
      },
      timestamp: Date.now(),
    })
  }

  // Leave conversation room
  leaveConversation(conversationId: string): boolean {
    return this.send({
      type: 'status',
      payload: {
        action: 'leave',
        conversationId,
      },
      timestamp: Date.now(),
    })
  }

  onMessage(handler: MessageHandler): () => void {
    this.messageHandlers.add(handler)
    return () => this.messageHandlers.delete(handler)
  }

  onStatusChange(handler: StatusChangeHandler): () => void {
    this.statusHandlers.add(handler)
    return () => this.statusHandlers.delete(handler)
  }

  private notifyMessageHandlers(message: WSMessage): void {
    this.messageHandlers.forEach(handler => {
      try {
        handler(message)
      } catch (error) {
        console.error('Message handler error:', error)
      }
    })
  }

  private notifyStatusChange(status: 'connected' | 'disconnected' | 'error'): void {
    this.statusHandlers.forEach(handler => {
      try {
        handler(status)
      } catch (error) {
        console.error('Status handler error:', error)
      }
    })
  }

  disconnect(): void {
    this.stopHeartbeat()
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.ws) {
      this.ws.close(1000, 'Client disconnect')
      this.ws = null
    }

    this.isConnected.value = false
    this.connectionStatus.status = 'disconnected'
    this.connectionStatus.reconnecting = false
  }

  destroy(): void {
    this.disconnect()
    this.messageHandlers.clear()
    this.statusHandlers.clear()
  }
}

// Singleton instance
let wsService: WebSocketService | null = null

export function createWebSocketService(options: WebSocketServiceOptions): WebSocketService {
  if (wsService) {
    wsService.destroy()
  }
  wsService = new WebSocketService(options)
  return wsService
}

export function getWebSocketService(): WebSocketService | null {
  return wsService
}

export function useWebSocket() {
  const service = getWebSocketService()
  
  const onChatMessage = (handler: (data: { conversationId: string; message: Message }) => void) => {
    if (!service) return () => {}
    
    return service.onMessage((wsMessage) => {
      if (wsMessage.type === 'message') {
        handler(wsMessage.payload)
      }
    })
  }

  const onTypingIndicator = (handler: (data: { conversationId: string; isTyping: boolean }) => void) => {
    if (!service) return () => {}
    
    return service.onMessage((wsMessage) => {
      if (wsMessage.type === 'typing') {
        handler(wsMessage.payload)
      }
    })
  }

  const onAgentAssigned = (handler: (data: { conversationId: string; agentId: string; agentName: string }) => void) => {
    if (!service) return () => {}
    
    return service.onMessage((wsMessage) => {
      if (wsMessage.type === 'agent_assigned') {
        handler(wsMessage.payload)
      }
    })
  }

  const onConversationUpdated = (handler: (data: { conversation: Conversation }) => void) => {
    if (!service) return () => {}
    
    return service.onMessage((wsMessage) => {
      if (wsMessage.type === 'conversation_updated') {
        handler(wsMessage.payload)
      }
    })
  }

  return {
    isConnected: service?.isConnected,
    connectionStatus: service?.connectionStatus,
    connect: (token?: string) => service?.connect(token),
    disconnect: () => service?.disconnect(),
    sendMessage: (conversationId: string, content: string) => service?.sendChatMessage(conversationId, content),
    sendTyping: (conversationId: string, isTyping: boolean) => service?.sendTyping(conversationId, isTyping),
    joinConversation: (conversationId: string) => service?.joinConversation(conversationId),
    leaveConversation: (conversationId: string) => service?.leaveConversation(conversationId),
    onChatMessage,
    onTypingIndicator,
    onAgentAssigned,
    onConversationUpdated,
  }
}

export default WebSocketService
