# SmartAgent - AI智能客服平台

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-orange.svg)](https://vuejs.org/)
[![AI](https://img.shields.io/badge/AI-Multi--Agent-yellow.svg)](https://deepseek.com/)

> 🤖 **AI-First全渠道智能客服平台** - Multi-Agent智能编排、RAG知识库、7×24小时AI客服团队

## 📋 项目简介

SmartAgent 是一套**深度AI融合**的智能客服平台，让每家企业都能拥有7×24小时的AI客服团队。

### 核心AI能力

| AI功能 | 描述 | 价值 |
|--------|------|------|
| 🧠 **Multi-Agent编排** |  Supervisor Agent 智能路由到专家Agent | 复杂问题精准回答 |
| 📚 **RAG知识库** | 向量数据库语义检索，结合企业知识 | 答案准确率95%+ |
| 💬 **多轮对话** | 记忆上下文，支持意图切换 | 对话连贯自然 |
| 🔄 **人工接管** | 无缝转接人工，AI与人工协同 | 客户体验最佳 |
| 📊 **数据分析** | 对话质量监控，AI性能分析 | 持续优化提升 |

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Vue 3 前端                            │
│   (客服工作台 / 客户聊天窗口 / 知识库管理 / 数据分析)          │
├─────────────────────────────────────────────────────────────┤
│                   Spring Cloud Alibaba 后端                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │
│  │ Gateway     │ │ Agent       │ │ Conversation        │  │
│  │ Service     │ │ Service     │ │ Service             │  │
│  └─────────────┘ └─────────────┘ └─────────────────────┘  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │
│  │ Knowledge   │ │ Ticket      │ │ AI Service          │  │
│  │ Service     │ │ Service     │ │ (Java)              │  │
│  └─────────────┘ └─────────────┘ └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Java AI 服务层                            │
│  (SupervisorAgent / SpecialistAgent / RAG / ToolCall)       │
├─────────────────────────────────────────────────────────────┤
│                    DeepSeek API                              │
└─────────────────────────────────────────────────────────────┘
```

## 📂 目录结构

```
SmartAgent/
├── ai-service/                    # Java AI 服务层 ⭐
│   ├── src/main/java/
│   │   └── com/smartagent/aiservice/
│   │       ├── controller/         # REST API
│   │       ├── service/            # AI 业务逻辑
│   │       │   ├── SupervisorAgentService.java   # 主管Agent
│   │       │   ├── SpecialistAgentService.java   # 专家Agent
│   │       │   ├── RAGService.java              # 知识库检索
│   │       │   ├── ConversationMemoryService.java # 对话记忆
│   │       │   ├── LLMClientService.java        # LLM调用
│   │       │   └── ToolRegistryService.java     # 工具注册
│   │       ├── model/             # 数据模型
│   │       └── config/             # 配置类
│   └── pom.xml
│
├── agent-service/                  # Agent管理服务
├── conversation-service/           # 对话服务 (WebSocket)
├── knowledge-service/              # 知识库服务
├── ticket-service/                 # 工单服务
├── gateway/                        # API网关
├── frontend/                       # Vue 3前端
│   └── src/
│       ├── components/chat/       # 聊天组件
│       ├── views/agent/           # 客服工作台
│       ├── views/admin/           # 管理后台
│       └── stores/                # Pinia状态
└── pom.xml                         # 父POM
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Redis
- Nacos

### 1. 启动后端

```bash
git clone https://github.com/nplszfl/SmartAgent.git
cd SmartAgent

mvn clean install -DskipTests

# AI服务
cd ai-service && mvn spring-boot:run

# 其他微服务
cd ../gateway && mvn spring-boot:run
cd ../agent-service && mvn spring-boot:run
cd ../conversation-service && mvn spring-boot:run
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 3. 配置

```yaml
# ai-service/src/main/resources/application.yml
spring:
  ai:
    deepseek:
      api-key: your-api-key
      model: deepseek-chat
```

## 📡 核心API

### 聊天接口

```
POST /api/v1/chat

{
  "sessionId": "sess_123",
  "message": "我想查一下订单状态",
  "userId": "user_456"
}

响应：
{
  "content": "您好！让我帮您查询订单状态...",
  "agentType": "OrderAgent",
  "confidence": 0.95,
  "needsHuman": false,
  "suggestions": ["查看物流", "申请退款"]
}
```

### Agent类型

| Agent | 职责 | 触发关键词 |
|-------|------|-----------|
| ProductAgent | 产品咨询 | 产品、规格、功能 |
| ComplaintAgent | 投诉处理 | 投诉、不满意、差评 |
| RefundAgent | 退款处理 | 退款、退货、退钱 |
| OrderAgent | 订单查询 | 订单、物流、发货 |
| TechnicalAgent | 技术支持 | 故障、报错、技术 |
| BillingAgent | 账单问题 | 账单、付款、发票 |

## 🎯 Multi-Agent工作流程

```
用户消息
    ↓
[Supervisor Agent]
    ↓ 意图分类
┌──┬──┬──┬──┬──┐
↓  ↓  ↓  ↓  ↓  ↓
产 投 退 订 技 账
品 诉 款 单 术 单
↓  ↓  ↓  ↓  ↓  ↓
[RAG知识库检索] → 返回答案 → 用户
    ↓ 无答案
[转接人工] → 人工处理 → 完成
```

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + WebSocket |
| 后端 | Java 21 + Spring Cloud Alibaba + MyBatis Plus |
| AI服务 | Spring Boot 3.2 + WebClient + DeepSeek |
| 数据库 | MySQL |
| 缓存 | Redis |
| 注册中心 | Nacos |

## 📊 功能列表

### 客服工作台
- 实时对话（WebSocket）
- 快捷回复模板
- 工单创建
- 对话记录查看

### 客户聊天窗口
- 表情、文件上传
- 消息已读/未读
- 满意度评价
- 历史会话

### 知识库管理
- 文档上传（PDF、Word、TXT）
- 智能分类打标
- 问答对管理
- 知识库检索测试

### 数据分析
- 对话量统计
- AI回答率
- 平均响应时间
- 热点问题分析

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👨‍💻 作者

**黄辉翔** - [GitHub](https://github.com/nplszfl)

---

⭐ 如果对你有帮助，请给项目一个 Star！
