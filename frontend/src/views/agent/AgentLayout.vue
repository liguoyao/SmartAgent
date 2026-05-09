<template>
  <div class="agent-layout">
    <el-container>
      <!-- Header -->
      <el-header class="agent-header">
        <div class="header-left">
          <div class="logo">
            <el-icon><ChatDotRound /></el-icon>
            <span>SmartAgent</span>
          </div>
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="agent-menu"
          >
            <el-menu-item index="/agent">Console</el-menu-item>
            <el-menu-item index="/agent/conversations">Conversations</el-menu-item>
            <el-menu-item index="/agent/history">History</el-menu-item>
          </el-menu>
        </div>
        <div class="header-right">
          <el-badge :value="waitingCount" :hidden="waitingCount === 0" :max="99">
            <el-button text>
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32">{{ userName?.charAt(0) }}</el-avatar>
              <span class="user-name">{{ userName }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  Profile
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  Settings
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  Logout
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main class="agent-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useConversationStore } from '@/stores/conversation'
import { ChatDotRound, Bell, User, Setting, SwitchButton } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const conversationStore = useConversationStore()

const activeMenu = computed(() => route.path)
const userName = computed(() => authStore.userInfo?.name || 'Agent')
const waitingCount = computed(() => conversationStore.waitingCount)

async function handleCommand(command: string) {
  switch (command) {
    case 'logout':
      await authStore.logout()
      router.push('/login')
      break
    case 'profile':
      break
    case 'settings':
      break
  }
}
</script>

<style scoped lang="scss">
.agent-layout {
  height: 100vh;

  :deep(.el-container) {
    height: 100%;
  }
}

.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  .header-left {
    display: flex;
    align-items: center;
    gap: 40px;

    .logo {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: #409eff;
    }

    .agent-menu {
      border-bottom: none;

      .el-menu-item {
        font-weight: 500;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;

      &:hover {
        background: #f5f7fa;
      }

      .user-name {
        font-weight: 500;
      }
    }
  }
}

.agent-main {
  padding: 0;
  background: #f5f7fa;
}
</style>
