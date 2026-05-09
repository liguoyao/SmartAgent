<template>
  <div class="knowledge-editor">
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          Back
        </el-button>
        <h2>{{ isEdit ? 'Edit Article' : 'New Article' }}</h2>
      </div>
      <div class="header-actions">
        <el-button @click="saveDraft" :loading="saving">
          Save Draft
        </el-button>
        <el-button type="primary" @click="publish" :loading="publishing">
          Publish
        </el-button>
      </div>
    </div>

    <div class="editor-content">
      <el-row :gutter="20">
        <el-col :span="16">
          <!-- Title -->
          <el-input
            v-model="form.title"
            placeholder="Article title"
            size="large"
            class="title-input"
          />

          <!-- Category -->
          <div class="form-item">
            <label>Category</label>
            <el-select v-model="form.category" placeholder="Select category" filterable>
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </div>

          <!-- Content editor -->
          <div class="form-item content-editor">
            <label>Content</label>
            <div class="editor-toolbar">
              <el-button-group>
                <el-button @click="insertFormat('**', '**')" title="Bold">
                  <el-icon><Bold /></el-icon>
                </el-button>
                <el-button @click="insertFormat('*', '*')" title="Italic">
                  <el-icon><Italic /></el-icon>
                </el-button>
                <el-button @click="insertFormat('`', '`')" title="Code">
                  <el-icon><Coin /></el-icon>
                </el-button>
                <el-button @click="insertFormat('[', '](url)')" title="Link">
                  <el-icon><Link /></el-icon>
                </el-button>
              </el-button-group>
              <el-button-group>
                <el-button @click="insertFormat('# ', '')" title="Heading 1">H1</el-button>
                <el-button @click="insertFormat('## ', '')" title="Heading 2">H2</el-button>
                <el-button @click="insertFormat('### ', '')" title="Heading 3">H3</el-button>
              </el-button-group>
              <el-button-group>
                <el-button @click="insertFormat('- ', '')" title="List">
                  <el-icon><List /></el-icon>
                </el-button>
                <el-button @click="insertFormat('1. ', '')" title="Numbered List">
                  <el-icon><List /></el-icon>
                </el-button>
                <el-button @click="insertFormat('> ', '')" title="Quote">
                  <el-icon><ChatLineSquare /></el-icon>
                </el-button>
              </el-button-group>
              <el-button-group>
                <el-button @click="insertFormat('```\n', '\n```')" title="Code Block">
                  <el-icon><Document /></el-icon>
                </el-button>
                <el-button @click="insertFormat('---', '')" title="Divider">
                  <el-icon><Minus /></el-icon>
                </el-button>
              </el-button-group>
            </div>
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="20"
              placeholder="Write your article content in Markdown..."
              class="content-textarea"
            />
            <div class="editor-preview" v-if="showPreview">
              <div class="preview-header">
                <span>Preview</span>
                <el-button text size="small" @click="showPreview = false">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
              <div class="preview-content" v-html="renderedContent"></div>
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <!-- Settings panel -->
          <el-card class="settings-panel">
            <template #header>
              <div class="panel-header">
                <span>Article Settings</span>
                <el-button text @click="showPreview = !showPreview">
                  {{ showPreview ? 'Hide Preview' : 'Show Preview' }}
                </el-button>
              </div>
            </template>

            <!-- Status -->
            <div class="setting-item">
              <label>Status</label>
              <el-select v-model="form.status">
                <el-option label="Draft" value="draft" />
                <el-option label="Published" value="published" />
                <el-option label="Archived" value="archived" />
              </el-select>
            </div>

            <!-- Tags -->
            <div class="setting-item">
              <label>Tags</label>
              <el-select
                v-model="form.tags"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="Add tags"
              >
                <el-option
                  v-for="tag in availableTags"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </div>

            <!-- Visibility -->
            <div class="setting-item">
              <label>Visibility</label>
              <el-radio-group v-model="form.visibility">
                <el-radio label="public">Public</el-radio>
                <el-radio label="internal">Internal</el-radio>
              </el-radio-group>
            </div>

            <!-- Author info -->
            <div class="setting-item" v-if="isEdit">
              <label>Author</label>
              <span class="author-name">{{ article?.authorName }}</span>
            </div>

            <div class="setting-item" v-if="isEdit">
              <label>Created</label>
              <span class="date-info">{{ formatDate(article?.createdAt) }}</span>
            </div>

            <div class="setting-item" v-if="isEdit">
              <label>Last Updated</label>
              <span class="date-info">{{ formatDate(article?.updatedAt) }}</span>
            </div>

            <div class="setting-item" v-if="isEdit">
              <label>Statistics</label>
              <div class="stats">
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ article?.viewCount || 0 }} views</span>
                </div>
                <div class="stat-item">
                  <el-icon><CircleCheck /></el-icon>
                  <span>{{ article?.helpfulCount || 0 }} helpful</span>
                </div>
                <div class="stat-item">
                  <el-icon><CircleClose /></el-icon>
                  <span>{{ article?.notHelpfulCount || 0 }} not helpful</span>
                </div>
              </div>
            </div>
          </el-card>

          <!-- Attachments -->
          <el-card class="attachments-panel">
            <template #header>
              <span>Attachments</span>
            </template>
            <el-upload
              :action="uploadUrl"
              :headers="{ Authorization: `Bearer ${token}` }"
              :file-list="attachments"
              list-type="text"
              :on-success="handleAttachmentSuccess"
              :on-remove="handleAttachmentRemove"
            >
              <el-button size="small">
                <el-icon><Upload /></el-icon>
                Upload Files
              </el-button>
            </el-upload>
          </el-card>

          <!-- Related articles -->
          <el-card class="related-panel">
            <template #header>
              <span>Related Articles</span>
            </template>
            <el-select
              v-model="form.relatedArticles"
              multiple
              filterable
              placeholder="Link related articles"
            >
              <el-option
                v-for="art in articleList"
                :key="art.id"
                :label="art.title"
                :value="art.id"
              />
            </el-select>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useKnowledgeStore } from '@/stores/knowledge'
import { marked } from 'marked'
import hljs from 'highlight.js'
import dayjs from 'dayjs'
import {
  ArrowLeft,
  Bold,
  Italic,
  Coin,
  Link,
  List,
  ChatLineSquare,
  Document,
  Minus,
  View,
  CircleCheck,
  CircleClose,
  Upload,
  Close,
} from '@element-plus/icons-vue'
import type { KnowledgeArticle, KnowledgeCategory } from '@/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const knowledgeStore = useKnowledgeStore()

// Refs
const saving = ref(false)
const publishing = ref(false)
const showPreview = ref(false)
const attachments = ref<any[]>([])

const isEdit = computed(() => !!route.params.id)
const articleId = computed(() => route.params.id as string)
const token = computed(() => authStore.token || '')

const uploadUrl = computed(() => '/api/knowledge/upload')

// Form data
const form = ref({
  title: '',
  content: '',
  category: '',
  tags: [] as string[],
  status: 'draft' as 'draft' | 'published' | 'archived',
  visibility: 'public' as 'public' | 'internal',
  relatedArticles: [] as string[],
})

// Mock data
const categories = ref<KnowledgeCategory[]>([
  { id: '1', name: 'Getting Started', sortOrder: 1, articleCount: 5 },
  { id: '2', name: 'Account & Billing', sortOrder: 2, articleCount: 8 },
  { id: '3', name: 'Technical Support', sortOrder: 3, articleCount: 12 },
  { id: '4', name: 'Product Guides', sortOrder: 4, articleCount: 15 },
])

const availableTags = ref([
  'getting-started',
  'account',
  'billing',
  'technical',
  'faq',
  'how-to',
  'troubleshooting',
  'api',
  'integration',
])

const articleList = ref<{ id: string; title: string }[]>([])

const article = ref<KnowledgeArticle | null>(null)

// Computed
const renderedContent = computed(() => {
  marked.setOptions({
    highlight: (code: string) => hljs.highlightAuto(code).value,
    breaks: true,
  })
  return marked.parse(form.value.content) as string
})

// Lifecycle
onMounted(async () => {
  await loadCategories()
  
  if (isEdit.value) {
    await loadArticle()
  }
  
  await loadArticleList()
})

// Watch for edit mode
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await loadArticle()
  }
})

// Methods
async function loadCategories() {
  try {
    await knowledgeStore.fetchCategories()
    categories.value = knowledgeStore.categories
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

async function loadArticleList() {
  try {
    await knowledgeStore.fetchArticles()
    articleList.value = knowledgeStore.articles.map(a => ({
      id: a.id,
      title: a.title,
    }))
  } catch (error) {
    console.error('Failed to load articles:', error)
  }
}

async function loadArticle() {
  try {
    await knowledgeStore.fetchArticle(articleId.value)
    const art = knowledgeStore.currentArticle
    
    if (art) {
      article.value = art
      form.value = {
        title: art.title,
        content: art.content,
        category: art.category,
        tags: art.tags,
        status: art.status,
        visibility: 'public',
        relatedArticles: [],
      }
    }
  } catch (error) {
    ElMessage.error('Failed to load article')
    router.push('/admin/knowledge')
  }
}

async function saveDraft() {
  if (!validateForm()) return
  
  saving.value = true
  try {
    if (isEdit.value) {
      await knowledgeStore.updateArticle(articleId.value, {
        ...form.value,
        status: 'draft',
      })
    } else {
      await knowledgeStore.createArticle({
        ...form.value,
        status: 'draft',
      })
    }
    ElMessage.success('Draft saved')
  } catch (error) {
    ElMessage.error('Failed to save draft')
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!validateForm()) return
  
  publishing.value = true
  try {
    if (isEdit.value) {
      await knowledgeStore.updateArticle(articleId.value, form.value)
      await knowledgeStore.publishArticle(articleId.value)
    } else {
      const newArticle = await knowledgeStore.createArticle({
        ...form.value,
        status: 'published',
      })
      await knowledgeStore.publishArticle(newArticle.id)
    }
    ElMessage.success('Article published')
    router.push('/admin/knowledge')
  } catch (error) {
    ElMessage.error('Failed to publish article')
  } finally {
    publishing.value = false
  }
}

function validateForm(): boolean {
  if (!form.value.title.trim()) {
    ElMessage.warning('Please enter a title')
    return false
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('Please enter content')
    return false
  }
  if (!form.value.category) {
    ElMessage.warning('Please select a category')
    return false
  }
  return true
}

function goBack() {
  router.push('/admin/knowledge')
}

function insertFormat(prefix: string, suffix: string) {
  const textarea = document.querySelector('.content-textarea textarea') as HTMLTextAreaElement
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const text = form.value.content
  const selectedText = text.substring(start, end)
  
  const newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
  form.value.content = newText
  
  // Restore cursor position
  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, end + prefix.length)
  }, 0)
}

function handleAttachmentSuccess(response: any, file: any) {
  attachments.value.push({
    name: file.name,
    url: response.url,
  })
  ElMessage.success('File uploaded')
}

function handleAttachmentRemove(file: any) {
  attachments.value = attachments.value.filter(a => a.name !== file.name)
}

function formatDate(timestamp?: number): string {
  return timestamp ? dayjs(timestamp).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped lang="scss">
.knowledge-editor {
  height: 100vh;
  background: #f5f7fa;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    h2 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.title-input {
  margin-bottom: 20px;

  :deep(.el-input__inner) {
    font-size: 24px;
    font-weight: 600;
    border: none;
    padding: 0;

    &:focus {
      box-shadow: none;
    }
  }
}

.form-item {
  margin-bottom: 20px;

  label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #606266;
  }
}

.content-editor {
  .editor-toolbar {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }

  .content-textarea {
    :deep(.el-textarea__inner) {
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      font-size: 14px;
      line-height: 1.6;
      resize: none;
    }
  }
}

.editor-preview {
  margin-top: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;

  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;
    font-weight: 500;
  }

  .preview-content {
    padding: 16px;
    min-height: 300px;
    background: #fff;

    :deep(h1) {
      font-size: 24px;
      margin: 0 0 16px;
    }

    :deep(h2) {
      font-size: 20px;
      margin: 24px 0 16px;
    }

    :deep(h3) {
      font-size: 16px;
      margin: 20px 0 12px;
    }

    :deep(p) {
      margin: 0 0 12px;
      line-height: 1.7;
    }

    :deep(ul), :deep(ol) {
      margin: 0 0 12px;
      padding-left: 24px;
    }

    :deep(li) {
      margin-bottom: 4px;
    }

    :deep(code) {
      background: #f0f0f0;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: monospace;
    }

    :deep(pre) {
      background: #f0f0f0;
      padding: 16px;
      border-radius: 8px;
      overflow-x: auto;
      margin: 12px 0;

      code {
        background: none;
        padding: 0;
      }
    }

    :deep(blockquote) {
      margin: 12px 0;
      padding: 12px 16px;
      border-left: 4px solid #409eff;
      background: #f0f9eb;
    }
  }
}

.settings-panel, .attachments-panel, .related-panel {
  margin-bottom: 20px;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.setting-item {
  margin-bottom: 16px;

  label {
    display: block;
    margin-bottom: 6px;
    font-size: 13px;
    color: #909399;
  }

  .author-name, .date-info {
    font-size: 14px;
    color: #606266;
  }

  .stats {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: #606266;
    }
  }
}
</style>
