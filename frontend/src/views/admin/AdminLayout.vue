<template>
  <div class="admin-layout">
    <el-container>
      <!-- Sidebar -->
      <el-aside width="240px" class="admin-sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <el-icon size="32"><ChatDotRound /></el-icon>
            <span>SmartAgent</span>
          </div>
        </div>

        <el-menu
          :default-active="activeMenu"
          router
          class="admin-menu"
          :collapse="false"
        >
          <el-menu-item index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <span>Dashboard</span>
          </el-menu-item>
          <el-menu-item index="/admin/knowledge">
            <el-icon><Document /></el-icon>
            <span>Knowledge Base</span>
          </el-menu-item>
          <el-menu-item index="/admin/agents">
            <el-icon><Service /></el-icon>
            <span>Agent Configuration</span>
          </el-menu-item>
          <el-menu-item index="/admin/analytics">
            <el-icon><TrendCharts /></el-icon>
            <span>Analytics</span>
          </el-menu-item>
          <el-menu-item index="/admin/settings">
            <el-icon><Setting /></el-icon>
            <span>Settings</span>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer">
          <div class="user-info">
            <el-avatar :size="36">{{ userName?.charAt(0) }}</el-avatar>
            <div class="user-details">
              <div class="user-name">{{ userName }}</div>
              <div class="user-role">{{ userRole }}</div>
            </div>
          </div>
        </div>
      </el-aside>

      <el-container>
        <!-- Header -->
        <el-header class="admin-header">
          <div class="header-left">
            <h2>{{ pageTitle }}</h2>
          </div>
          <div class="header-right">
            <el-button text @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
            <el-badge :value="notificationCount" :hidden="notificationCount === 0">
              <el-button text>
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-button text @click="refresh">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </el-header>

        <!-- Main Content -->
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  ChatDotRound,
  DataAnalysis,
  Document,
  Service,
  TrendCharts,
  Setting,
  FullScreen,
  Bell,
  Refresh,
} from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)
const userName = computed(() => authStore.userInfo?.name || 'Admin')
const userRole = computed(() => {
  const roleMap: Record<string, string> = {
    admin: 'Administrator',
    supervisor: 'Supervisor',
    agent: 'Agent',
  }
  return roleMap[authStore.userInfo?.role || 'admin']
})

const pageTitle = computed(() => {
  const titleMap: Record<string, string> = {
    '/admin': 'Dashboard',
    '/admin/knowledge': 'Knowledge Base',
    '/admin/agents': 'Agent Configuration',
    '/admin/analytics': 'Analytics',
    '/admin/settings': 'Settings',
  }
  return titleMap[route.path] || 'Admin'
})

const notificationCount = computed(() => 0)

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

function refresh() {
  window.location.reload()
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;

  :deep(.el-container) {
    height: 100%;
  }
}

.admin-sidebar {
  background: #1a1a2e;
  display: flex;
  flex-direction: column;

  .sidebar-header {
    padding: 20px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #fff;
      font-size: 20px;
      font-weight: 600;
    }
  }

  .admin-menu {
    flex: 1;
    background: transparent;
    border-right: none;

    :deep(.el-menu-item) {
      color: rgba(255, 255, 255, 0.7);
      height: 52px;
      line-height: 52px;

      &:hover {
        background: rgba(255, 255, 255, 0.08);
        color: #fff;
      }

      &.is-active {
        background: rgba(64, 158, 255, 0.2);
        color: #409eff;

        .el-icon {
          color: #409eff;
        }
      }

      .el-icon {
        margin-right: 12px;
        font-size: 18px;
      }
    }
  }

  .sidebar-footer {
    padding: 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .el-avatar {
        background: #409eff;
      }

      .user-details {
        .user-name {
          color: #fff;
          font-weight: 500;
          font-size: 14px;
        }

        .user-role {
          color: rgba(255, 255, 255, 0.5);
          font-size: 12px;
        }
      }
    }
  }
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  .header-left h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .header-right {
    display: flex;
    gap: 8px;
  }
}

.admin-main {
  padding: 0;
  background: #f5f7fa;
}
</style>
