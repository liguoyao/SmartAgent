<template>
  <main class="not-found">
    <el-result icon="warning" title="404" sub-title="页面不存在或无权访问">
      <template #extra>
        <el-button type="primary" @click="$router.push('/login')">返回登录页</el-button>
      </template>
    </el-result>
  </main>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  place-items: center;
}
</style>
