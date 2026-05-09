// Chat message types
export interface Message {
  id: string
  conversationId: string
  role: 'user' | 'assistant' | 'system' | 'agent'
  content: string
  timestamp: number
  status: 'sending' | 'sent' | 'error'
  attachments?: Attachment[]
  metadata?: Record<string, any>
}

export interface Attachment {
  id: string
  name: string
  url: string
  type: string
  size: number
}

export interface Conversation {
  id: string
  customerId: string
  customerName: string
  status: 'waiting' | 'active' | 'resolved' | 'closed'
  priority: 'low' | 'normal' | 'high' | 'urgent'
  assignedAgentId?: string
  assignedAgentName?: string
  createdAt: number
  updatedAt: number
  lastMessage?: string
  tags?: string[]
  source: 'web' | 'mobile' | 'api'
}

export interface Agent {
  id: string
  name: string
  email: string
  avatar?: string
  status: 'online' | 'busy' | 'away' | 'offline'
  role: 'agent' | 'supervisor' | 'admin'
  maxConcurrentChats: number
  currentChats: number
  skills: string[]
  createdAt: number
}

// Knowledge base types
export interface KnowledgeArticle {
  id: string
  title: string
  content: string
  category: string
  tags: string[]
  status: 'draft' | 'published' | 'archived'
  authorId: string
  authorName: string
  createdAt: number
  updatedAt: number
  publishedAt?: number
  viewCount: number
  helpfulCount: number
  notHelpfulCount: number
}

export interface KnowledgeCategory {
  id: string
  name: string
  parentId?: string
  description?: string
  sortOrder: number
  articleCount: number
}

// Agent configuration types
export interface AgentConfig {
  id: string
  name: string
  description: string
  model: string
  temperature: number
  maxTokens: number
  systemPrompt: string
  capabilities: string[]
  knowledgeBaseIds: string[]
  routingRules: RoutingRule[]
  isActive: boolean
  createdAt: number
  updatedAt: number
}

export interface RoutingRule {
  id: string
  name: string
  condition: RoutingCondition
  action: RoutingAction
  priority: number
}

export interface RoutingCondition {
  type: 'keyword' | 'intent' | 'time' | 'customer_type'
  operator: 'equals' | 'contains' | 'starts_with' | 'ends_with'
  value: string
}

export interface RoutingAction {
  type: 'assign_agent' | 'assign_team' | 'priority_boost' | 'auto_reply'
  value: string
}

// Analytics types
export interface AnalyticsOverview {
  totalConversations: number
  activeConversations: number
  resolvedToday: number
  avgResponseTime: number
  avgResolutionTime: number
  customerSatisfaction: number
}

export interface AnalyticsTimeSeries {
  timestamp: number
  conversations: number
  messages: number
  resolved: number
}

export interface AgentPerformance {
  agentId: string
  agentName: string
  conversationsHandled: number
  avgResponseTime: number
  avgResolutionTime: number
  satisfaction: number
}

export interface MessageMetrics {
  totalMessages: number
  byChannel: Record<string, number>
  byHour: Record<number, number>
}

// WebSocket message types
export interface WSMessage {
  type: 'message' | 'typing' | 'status' | 'agent_assigned' | 'conversation_updated' | 'ping' | 'pong'
  payload: any
  timestamp: number
}

export interface ChatState {
  isConnected: boolean
  isTyping: boolean
  unreadCount: number
}

// API response types
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T = any> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// Form types
export interface LoginForm {
  username: string
  password: string
  rememberMe?: boolean
}

export interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
}

// Widget types
export interface WidgetConfig {
  apiUrl: string
  wsUrl: string
  botId: string
  theme?: 'light' | 'dark' | 'auto'
  position?: 'bottom-right' | 'bottom-left'
  welcomeMessage?: string
  placeholder?: string
}
