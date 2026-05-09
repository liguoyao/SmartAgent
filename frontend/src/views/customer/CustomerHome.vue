<template>
  <div class="customer-home">
    <div class="hero-section">
      <h1>How can we help you today?</h1>
      <el-input
        v-model="searchQuery"
        placeholder="Search for help..."
        size="large"
        class="search-input"
        @keydown.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="quick-links">
        <el-button v-for="link in quickLinks" :key="link.text" @click="handleQuickLink(link)">
          {{ link.text }}
        </el-button>
      </div>
    </div>

    <div class="content-section">
      <h2>Popular Topics</h2>
      <div class="topics-grid">
        <el-card v-for="topic in topics" :key="topic.id" class="topic-card" @click="handleTopic(topic)">
          <div class="topic-icon">
            <el-icon><component :is="topic.icon" /></el-icon>
          </div>
          <h3>{{ topic.title }}</h3>
          <p>{{ topic.description }}</p>
        </el-card>
      </div>
    </div>

    <!-- Chat Widget -->
    <ChatWidget
      :config="widgetConfig"
      :embedded="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import ChatWidget from '@/components/chat/ChatWidget.vue'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()

const searchQuery = ref('')

const widgetConfig = ref({
  apiUrl: '/api',
  wsUrl: 'ws://localhost:8080/ws',
  botId: 'default',
  theme: 'light',
  welcomeMessage: 'Hello! How can I assist you today?',
})

const quickLinks = ref([
  { text: 'Getting Started', action: 'start' },
  { text: 'Account Help', action: 'account' },
  { text: 'Billing', action: 'billing' },
  { text: 'Technical Support', action: 'tech' },
])

const topics = ref([
  { id: '1', title: 'Getting Started', description: 'Learn the basics and set up your account', icon: 'Guide', action: 'start' },
  { id: '2', title: 'Account & Billing', description: 'Manage your subscription and payments', icon: 'Wallet', action: 'billing' },
  { id: '3', title: 'API Documentation', description: 'Integrate with our powerful API', icon: 'Document', action: 'api' },
  { id: '4', title: 'Troubleshooting', description: 'Solve common issues and get help', icon: 'Tools', action: 'help' },
])

function handleSearch() {
  console.log('Search:', searchQuery.value)
}

function handleQuickLink(link: { action: string }) {
  console.log('Quick link:', link.action)
}

function handleTopic(topic: { action: string }) {
  console.log('Topic:', topic.action)
}
</script>

<style scoped lang="scss">
.customer-home {
  min-height: 100vh;
  background: #f5f7fa;
}

.hero-section {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  padding: 80px 40px;
  text-align: center;
  color: #fff;

  h1 {
    font-size: 36px;
    margin: 0 0 32px;
  }

  .search-input {
    max-width: 600px;
    margin: 0 auto 32px;

    :deep(.el-input__wrapper) {
      border-radius: 24px;
      padding: 12px 20px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }
  }

  .quick-links {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;

    .el-button {
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
      color: #fff;

      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
  }
}

.content-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 40px;

  h2 {
    font-size: 24px;
    margin: 0 0 32px;
    color: #303133;
  }
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;

  .topic-card {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    }

    .topic-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: #ecf5ff;
      color: #409eff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      margin-bottom: 16px;
    }

    h3 {
      margin: 0 0 8px;
      font-size: 16px;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }
}
</style>
