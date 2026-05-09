<template>
  <div class="knowledge-base-view">
    <div class="page-header">
      <div class="header-left">
        <el-input v-model="searchKeyword" placeholder="Search articles..." prefix-icon="Search" style="width: 300px" />
        <el-select v-model="categoryFilter" placeholder="Category" clearable>
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="Status" clearable>
          <el-option label="Draft" value="draft" />
          <el-option label="Published" value="published" />
          <el-option label="Archived" value="archived" />
        </el-select>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createArticle">
          <el-icon><Plus /></el-icon>
          New Article
        </el-button>
      </div>
    </div>

    <el-table :data="filteredArticles" v-loading="loading" @row-click="editArticle">
      <el-table-column prop="title" label="Title" min-width="200" />
      <el-table-column prop="category" label="Category" width="150">
        <template #default="{ row }">
          {{ getCategoryName(row.category) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tags" label="Tags" width="200">
        <template #default="{ row }">
          <el-tag v-for="tag in row.tags?.slice(0, 2)" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          <span v-if="row.tags?.length > 2">+{{ row.tags.length - 2 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="Views" width="100" sortable />
      <el-table-column prop="updatedAt" label="Updated" width="150">
        <template #default="{ row }">
          {{ formatDate(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150">
        <template #default="{ row }">
          <el-button size="small" @click.stop="editArticle(row)">Edit</el-button>
          <el-button size="small" type="danger" @click.stop="deleteArticle(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import type { KnowledgeArticle } from '@/types'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const searchKeyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const loading = computed(() => knowledgeStore.loading)
const total = computed(() => knowledgeStore.totalCount)

const categories = computed(() => knowledgeStore.categories)
const articles = computed(() => knowledgeStore.articles)

const filteredArticles = computed(() => {
  let list = articles.value
  if (categoryFilter.value) {
    list = list.filter(a => a.category === categoryFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(a => a.status === statusFilter.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(a => a.title.toLowerCase().includes(keyword) || a.content.toLowerCase().includes(keyword))
  }
  return list
})

function getCategoryName(categoryId: string) {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat?.name || categoryId
}

function getStatusType(status: string) {
  const map: Record<string, string> = { draft: 'info', published: 'success', archived: 'warning' }
  return map[status] || ''
}

function formatDate(timestamp: number) {
  return dayjs(timestamp).format('YYYY-MM-DD')
}

function createArticle() {
  router.push('/admin/knowledge/new/edit')
}

function editArticle(article: KnowledgeArticle) {
  router.push(`/admin/knowledge/${article.id}/edit`)
}

async function deleteArticle(article: KnowledgeArticle) {
  try {
    await ElMessageBox.confirm('Are you sure you want to delete this article?', 'Confirm Delete', { type: 'warning' })
    await knowledgeStore.deleteArticle(article.id)
    ElMessage.success('Article deleted')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Failed to delete article')
    }
  }
}

onMounted(async () => {
  await Promise.all([
    knowledgeStore.fetchArticles(),
    knowledgeStore.fetchCategories(),
  ])
})
</script>

<style scoped lang="scss">
.knowledge-base-view {
  padding: 24px;
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    .header-left { display: flex; gap: 12px; }
  }
}
</style>
