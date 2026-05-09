<template>
  <div class="settings-view">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="General" name="general">
        <el-card>
          <template #header>General Settings</template>
          <el-form label-width="150px">
            <el-form-item label="Organization Name">
              <el-input v-model="settings.orgName" />
            </el-form-item>
            <el-form-item label="Default Language">
              <el-select v-model="settings.language">
                <el-option label="English" value="en" />
                <el-option label="Chinese" value="zh" />
              </el-select>
            </el-form-item>
            <el-form-item label="Timezone">
              <el-select v-model="settings.timezone">
                <el-option label="UTC" value="UTC" />
                <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveGeneral">Save</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Notifications" name="notifications">
        <el-card>
          <template #header>Notification Settings</template>
          <el-form label-width="150px">
            <el-form-item label="Email Notifications">
              <el-switch v-model="settings.emailNotifications" />
            </el-form-item>
            <el-form-item label="Sound Notifications">
              <el-switch v-model="settings.soundNotifications" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNotifications">Save</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="API Keys" name="apikeys">
        <el-card>
          <template #header>API Keys</template>
          <div class="api-key-list">
            <div v-for="key in apiKeys" :key="key.id" class="api-key-item">
              <div class="key-info">
                <span class="key-name">{{ key.name }}</span>
                <span class="key-value">{{ key.masked }}</span>
              </div>
              <el-button size="small" @click="copyKey(key.value)">Copy</el-button>
            </div>
          </div>
          <el-button type="primary" @click="generateKey">Generate New Key</el-button>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Integrations" name="integrations">
        <el-card>
          <template #header>Integrations</template>
          <div class="integrations-list">
            <div v-for="integration in integrations" :key="integration.id" class="integration-item">
              <div class="integration-icon">
                <el-icon><component :is="integration.icon" /></el-icon>
              </div>
              <div class="integration-info">
                <span class="integration-name">{{ integration.name }}</span>
                <span class="integration-status" :class="{ active: integration.active }">
                  {{ integration.active ? 'Connected' : 'Not connected' }}
                </span>
              </div>
              <el-button size="small" :type="integration.active ? 'danger' : 'primary'">
                {{ integration.active ? 'Disconnect' : 'Connect' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Link, Setting } from '@element-plus/icons-vue'

const activeTab = ref('general')

const settings = reactive({
  orgName: 'SmartAgent',
  language: 'en',
  timezone: 'UTC',
  emailNotifications: true,
  soundNotifications: true,
})

const apiKeys = ref([
  { id: '1', name: 'Production Key', masked: 'sk_live_****' },
  { id: '2', name: 'Development Key', masked: 'sk_test_****' },
])

const integrations = ref([
  { id: '1', name: 'Slack', icon: 'Connection', active: true },
  { id: '2', name: 'Zendesk', icon: 'Link', active: false },
  { id: '3', name: 'Salesforce', icon: 'Setting', active: false },
])

function saveGeneral() {
  ElMessage.success('General settings saved')
}

function saveNotifications() {
  ElMessage.success('Notification settings saved')
}

function copyKey(value: string) {
  navigator.clipboard.writeText(value)
  ElMessage.success('Copied to clipboard')
}

function generateKey() {
  ElMessage.success('New API key generated')
}
</script>

<style scoped lang="scss">
.settings-view {
  padding: 24px;
  max-width: 800px;
}

.api-key-list, .integrations-list {
  margin-bottom: 20px;
}

.api-key-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;

  .key-info {
    .key-name {
      font-weight: 500;
      margin-right: 12px;
    }
    .key-value {
      color: #909399;
      font-family: monospace;
    }
  }
}

.integration-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;

  .integration-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #409eff;
  }

  .integration-info {
    flex: 1;
    .integration-name {
      font-weight: 500;
      display: block;
    }
    .integration-status {
      font-size: 13px;
      color: #909399;
      &.active { color: #67c23a; }
    }
  }
}
</style>
